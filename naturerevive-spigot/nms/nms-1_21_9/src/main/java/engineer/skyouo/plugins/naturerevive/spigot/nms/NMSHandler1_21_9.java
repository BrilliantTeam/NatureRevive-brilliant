package engineer.skyouo.plugins.naturerevive.spigot.nms;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import engineer.skyouo.plugins.naturerevive.common.INMSWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

public class NMSHandler1_21_9 implements INMSWrapper {
    @Override
    public List<String> getCompatibleNMSVersion() {
        return List.of("1.21.9", "1.21.10");
    }

    @Override
    public String getNbtAsString(World world, BlockState blockState) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        return level.getBlockEntity(new BlockPos(blockState.getX(), blockState.getY(), blockState.getZ()))
                .saveWithFullMetadata(level.registryAccess()).toString();
    }

    @Override
    public void setBlockNMS(World world, int x, int y, int z, BlockData data) {
        ((CraftWorld) world).getHandle().setBlock(
                new BlockPos(x, y, z), ((CraftBlockData) data).getState(), 3
        );
    }

    @Override
    public void loadTileEntity(World world, int x, int y, int z, String nbt) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        BlockEntity entity = level.getBlockEntity(new BlockPos(x, y, z));
        if (entity == null)
            throw new IllegalStateException("Expect block entity not to be null. At world = %s, x = %d, y = %d, z = %d".formatted(world.getName(), x, y, z));

        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), MinecraftServer.LOGGER)) {
            entity.loadCustomOnly(TagValueInput.create(scopedCollector, level.registryAccess(), TagParser.parseCompoundFully(nbt)));
        } catch (CommandSyntaxException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createTileEntity(World world, int x, int y, int z, BlockData data, String nbt) {
        setBlockNMS(world, x, y, z, data);
        loadTileEntity(world, x, y, z, nbt);
    }

    @Override
    public double[] getRecentTps() {
        return MinecraftServer.getServer().getTPS();
    }

    @Override
    public double getLuckForPlayer(Player player) {
        return ((CraftPlayer) player).getHandle().getLuck();
    }

    @Override
    public BlockState convertBlockDataToBlockState(BlockData blockData) {
        return CraftBlockStates.getBlockState(((CraftBlockData) blockData).getState(), null);
    }

    @Override
    public int getWorldMinHeight(World world) {
        return ((CraftWorld) world).getHandle().getMinY();
    }

    Material[] oreBlocks = new Material[] {
            Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS
    };

    @Override
    public Material[] getOreBlocks() {
        return oreBlocks;
    }

    @Override
    public void regenerateChunk(World world, int chunkX, int chunkZ) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        ServerChunkCache source = level.getChunkSource();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        world.unloadChunk(chunkX, chunkZ, false);
        deleteChunkAndFlush(level, source, chunkPos);
        if (world.isChunkLoaded(chunkX, chunkZ)) {
            markChunkNotNeedingSave(level, chunkX, chunkZ);
        } else {
            world.getChunkAt(chunkX, chunkZ);
        }
    }

    private static void markChunkNotNeedingSave(ServerLevel level, int chunkX, int chunkZ) {
        try {
            Method getChunk = level.getClass().getMethod("getChunk", int.class, int.class);
            Object chunk = getChunk.invoke(level, chunkX, chunkZ);
            if (chunk != null) {
                chunk.getClass().getMethod("setUnsaved", boolean.class).invoke(chunk, false);
            }
        } catch (Exception ignored) {}
    }

    private static void deleteChunkAndFlush(ServerLevel level, ServerChunkCache source, ChunkPos pos) {
        for (Field field : source.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object chunkMap = field.get(source);
                if (chunkMap == null) continue;
                for (Method m : chunkMap.getClass().getMethods()) {
                    if (m.getName().equals("write") && m.getParameterCount() == 2
                            && ChunkPos.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        Object arg = Supplier.class.isAssignableFrom(m.getParameterTypes()[1])
                                ? (Supplier<?>) () -> null : null;
                        m.invoke(chunkMap, pos, arg);
                        flushChunkIO(level, chunkMap);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static void flushChunkIO(ServerLevel level, Object chunkMap) {
        try {
            chunkMap.getClass().getMethod("flushWorker").invoke(chunkMap);
            return;
        } catch (Exception ignored) {}
        for (String cls : new String[]{
                "ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO",
                "ca.spottedleaf.moonrise.patches.chunk_system.io.RegionFileIOThread"}) {
            try {
                Class<?> c = Class.forName(cls);
                try { c.getMethod("flush", ServerLevel.class).invoke(null, level); return; }
                catch (NoSuchMethodException ignored) {}
                try { c.getMethod("flush").invoke(null); return; }
                catch (NoSuchMethodException ignored) {}
            } catch (Exception ignored) {}
        }
    }
}
