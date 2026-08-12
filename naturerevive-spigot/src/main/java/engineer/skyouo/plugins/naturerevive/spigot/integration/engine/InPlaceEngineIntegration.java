package engineer.skyouo.plugins.naturerevive.spigot.integration.engine;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.managers.ChunkRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class InPlaceEngineIntegration implements IEngineIntegration {
    @Override
    public String getPluginName() {
        return "NatureRevive (in-place)";
    }

    @Override
    public Type getType() {
        return Type.ENGINE;
    }

    @Override
    public boolean load() {
        return NatureRevivePlugin.nmsWrapper != null && NatureRevivePlugin.nmsWrapper.supportsInPlaceRegeneration();
    }

    @Override
    public String getEngineName() {
        return "inplace";
    }

    @Override
    public boolean shouldExitOnFatal() {
        return isEnabled();
    }

    @Override
    public void regenerateChunk(Plugin plugin, Chunk chunk, Runnable postTask) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // The structure snapshot has to be taken on the chunk's own thread, the generation must not
        // be (it is the expensive part), and the write back has to be again.
        ScheduleUtil.REGION.runTask(plugin, chunk, () -> {
            Object context;

            try {
                context = NatureRevivePlugin.nmsWrapper.captureRegenerationContext(world, chunkX, chunkZ);
            } catch (Exception e) {
                e.printStackTrace();
                ChunkRegeneration.releaseInFlightWithTickets(world, chunkX, chunkZ);
                return;
            }

            ScheduleUtil.GLOBAL.runTaskAsynchronously(plugin, () -> {
                Object prepared;

                try {
                    prepared = NatureRevivePlugin.nmsWrapper.prepareRegeneratedChunk(world, chunkX, chunkZ, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    ChunkRegeneration.releaseInFlightWithTickets(world, chunkX, chunkZ);
                    return;
                }

                ScheduleUtil.REGION.runTask(plugin, chunk, () -> {
                    try {
                        clearNearbyEntitiesThenApply(plugin, world, chunk, chunkX, chunkZ, prepared, postTask);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ChunkRegeneration.releaseInFlightWithTickets(world, chunkX, chunkZ);
                    }
                });
            });
        });
    }

    private static int clearChunkPersistentData(Chunk chunk) {
        HashSet<NamespacedKey> keys = new HashSet<>(chunk.getPersistentDataContainer().getKeys());
        for (NamespacedKey key : keys) chunk.getPersistentDataContainer().remove(key);
        return keys.size();
    }

    private static int clearChunkEntities(Chunk chunk) {
        int cleared = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) continue;
            entity.remove();
            cleared++;
        }
        return cleared;
    }

    private static void clearNearbyEntitiesThenApply(Plugin plugin, World world, Chunk chunk, int chunkX, int chunkZ,
                                                      Object prepared, Runnable postTask) {
        boolean clearEntities = NatureRevivePlugin.readonlyConfig.inPlaceRegenerateEntities
                && NatureRevivePlugin.readonlyConfig.inPlaceClearNearbyEntitiesBeforeRegeneration;
        boolean clearDroppedItems = NatureRevivePlugin.readonlyConfig.inPlaceRegenerateEntities
                && NatureRevivePlugin.readonlyConfig.inPlaceClearNearbyDroppedItemsBeforeRegeneration;
        if (!clearEntities && !clearDroppedItems) {
            applyPreparedChunk(world, chunk, chunkX, chunkZ, prepared, postTask, 0);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(9);
        AtomicInteger cleared = new AtomicInteger();
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int nearbyChunkX = chunkX + offsetX;
                int nearbyChunkZ = chunkZ + offsetZ;
                ScheduleUtil.REGION.runTask(plugin, new org.bukkit.Location(world, nearbyChunkX << 4, 0, nearbyChunkZ << 4), () -> {
                    if (world.isChunkLoaded(nearbyChunkX, nearbyChunkZ)) {
                        cleared.addAndGet(clearNearbyChunkEntities(world.getChunkAt(nearbyChunkX, nearbyChunkZ), clearEntities, clearDroppedItems));
                    }
                    if (remaining.decrementAndGet() == 0) {
                        ScheduleUtil.REGION.runTask(plugin, chunk, () -> applyPreparedChunk(world, chunk, chunkX, chunkZ,
                                prepared, postTask, cleared.get()));
                    }
                });
            }
        }
    }

    private static void applyPreparedChunk(World world, Chunk chunk, int chunkX, int chunkZ, Object prepared,
                                           Runnable postTask, int clearedNearbyEntities) {
        try {
            int clearedPersistentData = NatureRevivePlugin.readonlyConfig.inPlaceClearChunkPersistentData
                    ? clearChunkPersistentData(chunk) : 0;
            int clearedEntities = NatureRevivePlugin.readonlyConfig.inPlaceRegenerateEntities
                    && NatureRevivePlugin.readonlyConfig.inPlaceClearEntitiesBeforeRegeneration
                    ? clearChunkEntities(chunk) : 0;
            int entitiesBeforeApply = chunk.getEntities().length;
            NatureRevivePlugin.nmsWrapper.applyRegeneratedChunk(world, chunkX, chunkZ, prepared,
                    NatureRevivePlugin.readonlyConfig.inPlaceRegenerateEntities);
            NatureReviveComponentLogger.debug("In-place regenerated %s (%d, %d): cleared %d PDC keys, cleared %d entities, cleared %d nearby entities, entities %d -> %d.",
                    world.getName(), chunkX, chunkZ, clearedPersistentData, clearedEntities, clearedNearbyEntities,
                    entitiesBeforeApply, chunk.getEntities().length);

            if (postTask != null) postTask.run();
        } catch (Exception e) {
            e.printStackTrace();
            ChunkRegeneration.releaseInFlightWithTickets(world, chunkX, chunkZ);
        }
    }

    private static int clearNearbyChunkEntities(Chunk chunk, boolean clearEntities, boolean clearDroppedItems) {
        int cleared = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) continue;
            if (entity instanceof Item ? clearDroppedItems : clearEntities) {
                entity.remove();
                cleared++;
            }
        }
        return cleared;
    }

}
