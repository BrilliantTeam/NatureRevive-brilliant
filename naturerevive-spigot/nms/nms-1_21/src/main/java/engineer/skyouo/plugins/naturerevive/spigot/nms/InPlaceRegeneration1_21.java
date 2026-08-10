package engineer.skyouo.plugins.naturerevive.spigot.nms;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/** Replays the vanilla worldgen pipeline into detached chunks, then diffs the result onto the live chunk. */
final class InPlaceRegeneration1_21 {
    private InPlaceRegeneration1_21() {
    }

    /** One chunk's structure bookkeeping, copied out of the live world. */
    record Structures(Map<Structure, StructureStart> starts, Map<Structure, LongSet> references) {
    }

    /** Hands out a detached chunk without touching the real chunk system, so nothing can collide with it. */
    private static final class DetachedHolder extends GenerationChunkHolder {
        private final Scratch scratch;
        private final int x;
        private final int z;

        DetachedHolder(Scratch scratch, int x, int z) {
            super(new ChunkPos(x, z));
            this.scratch = scratch;
            this.x = x;
            this.z = z;
        }

        @Override
        public ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
            return this.scratch.chunk(this.x, this.z);
        }

        @Override
        public int getTicketLevel() {
            return 0;
        }

        @Override
        public int getQueueLevel() {
            return 0;
        }
    }

    private static final class Scratch {
        private final ServerLevel level;
        private final Map<Long, Structures> captured;
        private final Map<Long, ProtoChunk> chunks = new HashMap<>();
        private final Map<Long, GenerationChunkHolder> holders = new HashMap<>();

        Scratch(ServerLevel level, Map<Long, Structures> captured) {
            this.level = level;
            this.captured = captured;
        }

        /** Created on first read: a region's cache spans 289 positions, most of which stay untouched. */
        ProtoChunk chunk(int x, int z) {
            return this.chunks.computeIfAbsent(ChunkPos.asLong(x, z),
                    key -> new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, this.level, level.registryAccess().registryOrThrow(Registries.BIOME), null));
        }

        private GenerationChunkHolder holder(int x, int z) {
            return this.holders.computeIfAbsent(ChunkPos.asLong(x, z), key -> new DetachedHolder(this, x, z));
        }

        /** Per step: WorldGenRegion#getChunk refuses distances >= step.directDependencies().size(). */
        WorldGenRegion region(ChunkStep step, int x, int z) {
            StaticCache2D<GenerationChunkHolder> cache =
                    StaticCache2D.create(x, z, step.directDependencies().size() - 1, this::holder);

            return new WorldGenRegion(this.level, cache, step, chunk(x, z));
        }

        /** Scoped to the region so nothing can reach out and load a real chunk off-thread. */
        StructureManager structures(WorldGenRegion region) {
            return this.level.structureManager().forWorldGenRegion(region);
        }

        /**
         * Freshly generated starts win: a captured start's pieces have their one-shot placement flags
         * already set (DesertPyramidPiece#hasPlacedChest and friends) and would skip their chests.
         * The captured ones stay as a fallback for structures this version no longer generates.
         */
        void mergeCapturedStarts(ProtoChunk chunk, int x, int z) {
            Structures structures = this.captured.get(ChunkPos.asLong(x, z));
            if (structures == null) return;

            Map<Structure, StructureStart> starts = new HashMap<>(structures.starts());
            starts.putAll(chunk.getAllStarts());
            chunk.setAllStarts(starts);
        }

        void mergeCapturedReferences(ProtoChunk chunk, int x, int z) {
            Structures structures = this.captured.get(ChunkPos.asLong(x, z));
            if (structures == null) return;

            Map<Structure, LongSet> references = new HashMap<>(structures.references());
            references.putAll(chunk.getAllReferences());
            chunk.setAllReferences(references);
        }
    }

    /** Largest chessboard distance at which {@code step} needs a neighbour at {@code status} or later. */
    private static int halo(ChunkStep step, ChunkStatus status) {
        ChunkDependencies dependencies = step.directDependencies();
        for (int distance = dependencies.size() - 1; distance >= 0; distance--) {
            if (dependencies.get(distance).isOrAfter(status)) return distance;
        }

        return 0;
    }

    /**
     * Chunk thread, before {@link #prepare}. Structure starts live in the chunk's bookkeeping rather
     * than in its blocks, so they survive whatever players did to the terrain; an unloaded neighbour
     * simply means a structure starting there will not be replaced.
     */
    static Map<Long, Structures> capture(ServerLevel level, int chunkX, int chunkZ) {
        // A start can sit up to MAX_STRUCTURE_DISTANCE away, plus the rings we generate references for.
        int radius = ChunkStatus.MAX_STRUCTURE_DISTANCE + 2;
        Map<Long, Structures> captured = new HashMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk live = level.getChunkIfLoaded(chunkX + dx, chunkZ + dz);
                if (live == null) continue;

                Map<Structure, StructureStart> starts = live.getAllStarts();
                Map<Structure, LongSet> references = live.getAllReferences();
                if (starts.isEmpty() && references.isEmpty()) continue;

                captured.put(ChunkPos.asLong(chunkX + dx, chunkZ + dz),
                        new Structures(new HashMap<>(starts), new HashMap<>(references)));
            }
        }

        return captured;
    }

    /** Off-thread. Vanilla runs these same calls on its worldgen executor. */
    static ProtoChunk prepare(ServerLevel level, int chunkX, int chunkZ, Map<Long, Structures> captured) {
        ChunkPyramid pyramid = ChunkPyramid.GENERATION_PYRAMID;
        ChunkStep referencesStep = pyramid.getStepTo(ChunkStatus.STRUCTURE_REFERENCES);
        ChunkStep biomesStep = pyramid.getStepTo(ChunkStatus.BIOMES);
        ChunkStep noiseStep = pyramid.getStepTo(ChunkStatus.NOISE);
        ChunkStep surfaceStep = pyramid.getStepTo(ChunkStatus.SURFACE);
        ChunkStep carversStep = pyramid.getStepTo(ChunkStatus.CARVERS);
        ChunkStep featuresStep = pyramid.getStepTo(ChunkStatus.FEATURES);

        // Walk the pyramid backwards. Do NOT use directDependencies().size() - 1: that is the
        // structure search radius (8) for every step, which over-generates and still fails at the
        // edges with "Requested chunk unavailable during world generation".
        int carversRadius = halo(featuresStep, ChunkStatus.CARVERS);
        int surfaceRadius = carversRadius + halo(carversStep, ChunkStatus.SURFACE);
        int noiseRadius = surfaceRadius + halo(surfaceStep, ChunkStatus.NOISE);
        int biomesRadius = noiseRadius + halo(noiseStep, ChunkStatus.BIOMES);
        int referencesRadius = biomesRadius + halo(biomesStep, ChunkStatus.STRUCTURE_REFERENCES);
        int startsRadius = referencesRadius + halo(referencesStep, ChunkStatus.STRUCTURE_STARTS);

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        Scratch scratch = new Scratch(level, captured);

        for (int dx = -startsRadius; dx <= startsRadius; dx++) {
            for (int dz = -startsRadius; dz <= startsRadius; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                ProtoChunk chunk = scratch.chunk(x, z);

                generator.createStructures(level.registryAccess(), level.getChunkSource().getGeneratorState(),
                        level.structureManager(), chunk, level.getServer().getStructureManager());
                scratch.mergeCapturedStarts(chunk, x, z);
                chunk.setPersistedStatus(ChunkStatus.STRUCTURE_STARTS);
            }
        }

        for (int dx = -referencesRadius; dx <= referencesRadius; dx++) {
            for (int dz = -referencesRadius; dz <= referencesRadius; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                ProtoChunk chunk = scratch.chunk(x, z);
                WorldGenRegion region = scratch.region(referencesStep, x, z);

                generator.createReferences(region, scratch.structures(region), chunk);
                scratch.mergeCapturedReferences(chunk, x, z);
                chunk.setPersistedStatus(ChunkStatus.STRUCTURE_REFERENCES);
            }
        }

        for (int dx = -biomesRadius; dx <= biomesRadius; dx++) {
            for (int dz = -biomesRadius; dz <= biomesRadius; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                ProtoChunk chunk = scratch.chunk(x, z);
                WorldGenRegion region = scratch.region(biomesStep, x, z);

                generator.createBiomes(randomState, Blender.empty(), scratch.structures(region), chunk).join();
                // Vanilla's ChunkStep stamps this; ProtoChunk gates biome reads on it.
                chunk.setPersistedStatus(ChunkStatus.BIOMES);
            }
        }

        for (int dx = -noiseRadius; dx <= noiseRadius; dx++) {
            for (int dz = -noiseRadius; dz <= noiseRadius; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                ProtoChunk chunk = scratch.chunk(x, z);
                WorldGenRegion region = scratch.region(noiseStep, x, z);

                generator.fillFromNoise(Blender.empty(), randomState, scratch.structures(region), chunk).join();
                chunk.setPersistedStatus(ChunkStatus.NOISE);
            }
        }

        for (int dx = -surfaceRadius; dx <= surfaceRadius; dx++) {
            for (int dz = -surfaceRadius; dz <= surfaceRadius; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                ProtoChunk chunk = scratch.chunk(x, z);
                WorldGenRegion region = scratch.region(surfaceStep, x, z);

                generator.buildSurface(region, scratch.structures(region), randomState, chunk);
                chunk.setPersistedStatus(ChunkStatus.SURFACE);
            }
        }

        for (int dx = -carversRadius; dx <= carversRadius; dx++) {
            for (int dz = -carversRadius; dz <= carversRadius; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                ProtoChunk chunk = scratch.chunk(x, z);
                WorldGenRegion region = scratch.region(carversStep, x, z);

                for (GenerationStep.Carving carving : GenerationStep.Carving.values())
                    generator.applyCarvers(region, level.getSeed(), randomState, region.getBiomeManager(), scratch.structures(region), chunk, carving);
                chunk.setPersistedStatus(ChunkStatus.CARVERS);
            }
        }

        ProtoChunk center = scratch.chunk(chunkX, chunkZ);
        WorldGenRegion region = scratch.region(featuresStep, chunkX, chunkZ);

        generator.applyBiomeDecoration(region, center, scratch.structures(region));

        return center;
    }

    /** Chunk thread. {@code blockEntityLoader} is the handler's own loadTileEntity. */
    static void apply(ServerLevel level, int chunkX, int chunkZ, ProtoChunk generated, BiConsumer<BlockPos, String> blockEntityLoader) {
        LevelChunk live = level.getChunkIfLoaded(chunkX, chunkZ);
        if (live == null)
            throw new IllegalStateException("Chunk %d,%d unloaded before its regeneration could be applied.".formatted(chunkX, chunkZ));

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int minY = level.getMinBuildHeight();
        int maxY = minY + level.getHeight();

        // ponytail: block-by-block diff, letting setBlock do lighting, block entities and client
        // updates. Ceiling: ~98k setBlock calls on a fully rewritten chunk. Upgrade path is swapping
        // the LevelChunkSection arrays and sending one ClientboundLevelChunkWithLightPacket.
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    pos.set(baseX + x, y, baseZ + z);

                    BlockState next = generated.getBlockState(pos);
                    if (next == live.getBlockState(pos)) continue;

                    level.setBlock(pos, next, Block.UPDATE_ALL);
                }
            }
        }

        // setBlock only makes bare block entities, so push the generated NBT in for the LootTable tag.
        Set<BlockPos> blockEntities = new HashSet<>(generated.getBlockEntities().keySet());
        blockEntities.addAll(generated.getBlockEntityNbts().keySet());

        for (BlockPos blockEntityPos : blockEntities) {
            if (level.getBlockEntity(blockEntityPos) == null) continue;

            CompoundTag nbt = generated.getBlockEntityNbtForSaving(blockEntityPos, level.registryAccess());
            if (nbt != null) blockEntityLoader.accept(blockEntityPos, nbt.toString());
        }
    }
}
