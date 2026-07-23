package engineer.skyouo.plugins.naturerevive.spigot.managers;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import engineer.skyouo.plugins.naturerevive.spigot.constants.OreBlocksCompat;
import engineer.skyouo.plugins.naturerevive.spigot.events.ChunkRegenEvent;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationUtil;
import engineer.skyouo.plugins.naturerevive.spigot.integration.engine.IEngineIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.integration.land.ILandPluginIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.listeners.ObfuscateLootListener;
import engineer.skyouo.plugins.naturerevive.spigot.managers.features.ElytraRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.managers.features.StructureRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockDataChangeWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockStateWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.structs.NbtWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import engineer.skyouo.plugins.naturerevive.spigot.util.Util;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.*;

public class ChunkRegeneration {
    private static int radius = 8;

    private static final int MAX_LOGGING_QUEUE  = 50_000;
    private static final int MAX_BLOCK_PUT_QUEUE = 10_000;

    public static boolean enqueue(BukkitPositionInfo bukkitPositionInfo) {
        if (!regenInFlight.add(bukkitPositionInfo.getChunkKey()))
            return false;

        queue.add(bukkitPositionInfo);
        return true;
    }

    private static void finalizeRegen(String worldName, int chunkX, int chunkZ) {
        if (databaseConfig != null) {
            try {
                databaseConfig.unset(new BukkitPositionInfo(worldName, chunkX, chunkZ, 0));
            } catch (Exception ignored) {
            }
        }

        regenInFlight.remove(worldName + ":" + chunkX + ":" + chunkZ);
    }

    public static void releaseInFlight(String worldName, int chunkX, int chunkZ) {
        regenInFlight.remove(worldName + ":" + chunkX + ":" + chunkZ);
    }

    /**
     * FAWE 異常路徑用：同時釋放 regenInFlight 和當初加的 81 個 chunk tickets。
     * 必須從安全的執行緒呼叫 removePluginChunkTicket，因此排程到 GLOBAL（非 async）。
     */
    public static void releaseInFlightWithTickets(org.bukkit.World world, int chunkX, int chunkZ) {
        releaseInFlight(world.getName(), chunkX, chunkZ);
        ScheduleUtil.GLOBAL.runTask(instance, () -> {
            for (int x = -radius; x < (radius + 1); x++) {
                for (int z = -radius; z < (radius + 1); z++) {
                    world.removePluginChunkTicket(chunkX + x, chunkZ + z, instance);
                }
            }
        });
    }

    public static void regenerateChunk(BukkitPositionInfo bukkitPositionInfo) {
        regenerateChunk(bukkitPositionInfo, IntegrationUtil.getRegenEngine(), false);
    }

    public static void regenerateChunk(BukkitPositionInfo bukkitPositionInfo, IEngineIntegration engine) {
        regenerateChunk(bukkitPositionInfo, engine, false);
    }

    public static void regenerateChunk(BukkitPositionInfo bukkitPositionInfo, IEngineIntegration engine, boolean bypassClaimCheck) {
        Location location = bukkitPositionInfo.getLocation();

        List<NbtWithPos> nbtWithPos = new ArrayList<>();

        World world = location.getWorld();

        if (world == null) {
            finalizeRegen(bukkitPositionInfo.getWorldName(), bukkitPositionInfo.getX(), bukkitPositionInfo.getZ());
            return;
        }

        // Thanks to xuan
        int centerX = location.getBlockX() >> 4;
        int centerZ = location.getBlockZ() >> 4;

        if (!world.isChunkGenerated(centerX, centerZ)) {
            finalizeRegen(world.getName(), centerX, centerZ);
            return;
        }

        for (int x = -radius; x < (radius + 1); x++) {
            for (int z = -radius; z < (radius + 1); z++) {
                if (world.isChunkGenerated(centerX + x, centerZ + z))
                    world.addPluginChunkTicket(centerX + x, centerZ + z, instance);
            }
        }

        Chunk chunk = location.getChunk();

        boolean checkBiomes = !readonlyConfig.ignoredBiomes.isEmpty();
        ChunkSnapshot oldChunkSnapshot = chunk.getChunkSnapshot(checkBiomes, checkBiomes, false);

        if (checkBiomes) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()) + 1; y <= oldChunkSnapshot.getHighestBlockYAt(x, z); y++) {
                        Biome biome = oldChunkSnapshot.getBiome(x, y, z);

                        if (readonlyConfig.ignoredBiomes.contains(biome.getKey().getKey())) {
                            for (x = -radius; x < (radius + 1); x++) {
                                for (z = -radius; z < (radius + 1); z++) {
                                    chunk.getWorld().removePluginChunkTicket(chunk.getX() + x, chunk.getZ() + z, instance);
                                }
                            }

                            finalizeRegen(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                            return;
                        }
                    }
                }
            }
        }

        // todo: make this asynchronous.
        List<ILandPluginIntegration> integrations = IntegrationUtil.getLandIntegrations();

        if (!bypassClaimCheck) {
            for (ILandPluginIntegration integration : integrations) {
                if (!integration.checkHasLand(chunk)) continue;

                for (BlockState blockState : chunk.getTileEntities()) {
                    if (integration.
                            isInLand(new Location(location.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()))) {
                        String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);

                        nbtWithPos.add(new NbtWithPos(nbt, chunk.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()));
                    }
                }
            }
        }

        try {
            engine.regenerateChunk(instance, chunk, () -> {
                regenerateAfterWork(chunk, oldChunkSnapshot, integrations, nbtWithPos, bypassClaimCheck);
            });
        } catch (Exception ex) {
            releaseInFlight(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

            NatureReviveComponentLogger.warning(Lang.get("console.chunk-regen-error",
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
            ex.printStackTrace();
        }
    }

    private static void regenerateAfterWork(Chunk chunk, ChunkSnapshot oldChunkSnapshot, List<ILandPluginIntegration> integrations, List<NbtWithPos> nbtWithPos, boolean bypassClaimCheck) {
        for (int x = -radius; x < (radius + 1); x++) {
            for (int z = -radius; z < (radius + 1); z++) {
                chunk.getWorld().removePluginChunkTicket(chunk.getX() + x, chunk.getZ() + z, instance);
            }
        }

        finalizeRegen(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

        if (readonlyConfig.enableOreObfuscation)
            ObfuscateLootListener.randomizeChunkOre(chunk);

        ChunkSnapshot newChunkSnapshot = chunk.getChunkSnapshot();

        // We can offload it to other thread if not on folia
        if (!Util.isFolia()) {
            AtomicReference<ChunkSnapshot> oldRef = new AtomicReference<>(oldChunkSnapshot);
            AtomicReference<ChunkSnapshot> newRef = new AtomicReference<>(newChunkSnapshot);
            ScheduleUtil.GLOBAL.runTaskAsynchronously(instance, () -> {
                ChunkSnapshot old = oldRef.getAndSet(null);
                ChunkSnapshot nw  = newRef.getAndSet(null);

                ElytraRegeneration.isEndShip(integrations, chunk, nw);

                StructureRegeneration.savingMovableStructure(chunk, old);

                if (!integrations.isEmpty() && !bypassClaimCheck)
                    landOldStateRevert(integrations, chunk, old, nbtWithPos);

                if (!IntegrationUtil.getLoggingIntegrations().isEmpty())
                    coreProtectAPILogging(chunk, old);
            });
        } else {
            ElytraRegeneration.isEndShip(integrations, chunk, newChunkSnapshot);

            StructureRegeneration.savingMovableStructure(chunk, oldChunkSnapshot);

            if (!integrations.isEmpty() && !bypassClaimCheck)
                landOldStateRevert(integrations, chunk, oldChunkSnapshot, nbtWithPos);

            if (IntegrationUtil.hasValidLoggingIntegration())
                coreProtectAPILogging(chunk, oldChunkSnapshot);
        }

        ScheduleUtil.GLOBAL.runTaskLater(instance, () -> Bukkit.getPluginManager().callEvent(new ChunkRegenEvent(chunk, LocalDateTime.now())), 4L);
    }

    private static void coreProtectAPILogging(Chunk chunk, ChunkSnapshot oldChunkSnapshot) {
        synchronized (blockDataChangeWithPos) {
            // 計算本次可新增的預算，避免 ConcurrentLinkedQueue.size() 在迴圈內被重複呼叫（O(n)）
            int budget = MAX_LOGGING_QUEUE - blockDataChangeWithPos.size();
            if (budget <= 0) {
                NatureReviveComponentLogger.warning("blockDataChangeWithPos 佇列過大，跳過此區塊的日誌記錄 (%s %d %d)",
                        chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                return;
            }

            int added = 0;
            int minY = nmsWrapper.getWorldMinHeight(chunk.getWorld());
            int maxY = chunk.getWorld().getMaxHeight();
            outer:
            for (int x = 0; x < 16; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block newBlock = chunk.getBlock(x, y, z);

                        Material oldBlockType = oldChunkSnapshot.getBlockType(x, y, z);
                        Material newBlockType = newBlock.getType();

                        if (OreBlocksCompat.contains(oldBlockType)) continue;
                        if (oldBlockType.equals(newBlockType)) continue;

                        if (added >= budget) {
                            NatureReviveComponentLogger.warning("blockDataChangeWithPos 佇列預算耗盡，略過剩餘方塊日誌 (%s %d %d)",
                                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                            break outer;
                        }

                        Location location = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
                        BlockData oldBlockData = oldChunkSnapshot.getBlockData(x, y, z);
                        BlockData newBlockData = newBlock.getBlockData();
                        if (oldBlockType.equals(Material.AIR)) {
                            blockDataChangeWithPos.add(new BlockDataChangeWithPos(location, oldBlockData, newBlockData, BlockDataChangeWithPos.Type.PLACEMENT));
                        } else {
                            blockDataChangeWithPos.add(new BlockDataChangeWithPos(location, oldBlockData, newBlockData,
                                    newBlockType.equals(Material.AIR) ? BlockDataChangeWithPos.Type.REMOVAL : BlockDataChangeWithPos.Type.REPLACE));
                        }
                        added++;
                    }
                }
            }
        }
    }

    private static void landOldStateRevert(List<ILandPluginIntegration> integration, Chunk chunk, ChunkSnapshot oldChunkSnapshot, List<NbtWithPos> tileEntities) {
        if (blockStateWithPosQueue.size() > MAX_BLOCK_PUT_QUEUE) {
            NatureReviveComponentLogger.warning("blockStateWithPosQueue 佇列過大 (%d)，跳過此區塊的領地還原 (%s %d %d)",
                    blockStateWithPosQueue.size(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            return;
        }
        Map<Location, BlockData> preservedBlocks = new HashMap<>();

        if (integration.stream().anyMatch(i -> i.checkHasLand(chunk))) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y < chunk.getWorld().getMaxHeight(); y++) {
                        Location targetLocation = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
                        if (integration.stream().noneMatch(i -> i.isInLand(targetLocation)))
                            continue;

                        BlockData block = oldChunkSnapshot.getBlockData(x, y, z);
                        if (!chunk.getBlock(x, y, z).getBlockData().equals(block)) {
                            preservedBlocks.put(targetLocation, block);
                        }
                    }
                }
            }

            setBlocksSynchronous(preservedBlocks, tileEntities);
        }
    }

    public static void setBlocksSynchronous(Map<Location, BlockData> preservedBlocks, List<NbtWithPos> tileEntities) {
        synchronized (blockStateWithPosQueue) {
            int budget = MAX_BLOCK_PUT_QUEUE - blockStateWithPosQueue.size();
            if (budget <= 0) {
                NatureReviveComponentLogger.warning("blockStateWithPosQueue 佇列過大，略過 %d 個方塊還原項目", preservedBlocks.size());
                return;
            }
            int added = 0;
            for (Location location : preservedBlocks.keySet()) {
                if (added >= budget) {
                    NatureReviveComponentLogger.warning("blockStateWithPosQueue 預算耗盡，略過剩餘 %d 個方塊還原項目", preservedBlocks.size() - added);
                    break;
                }
                boolean findTheNbt = isFindTheNbt(preservedBlocks, tileEntities, location);

                if (!findTheNbt)
                    blockStateWithPosQueue.add(new BlockStateWithPos(nmsWrapper.convertBlockDataToBlockState(preservedBlocks.get(location)), location));
                added++;
            }
        }
    }

    private static boolean isFindTheNbt(Map<Location, BlockData> preservedBlocks, List<NbtWithPos> tileEntities, Location location) {
        boolean findTheNbt = false;
        for (NbtWithPos nbtWithPos : tileEntities) {
            if (nbtWithPos.getLocation().equals(location)) {
                blockStateWithPosQueue.add(new BlockStateWithPos(nmsWrapper.convertBlockDataToBlockState(preservedBlocks.get(location)), location, nbtWithPos.getNbt()));
                findTheNbt = true;
                break;
            }
        }
        return findTheNbt;
    }
}
