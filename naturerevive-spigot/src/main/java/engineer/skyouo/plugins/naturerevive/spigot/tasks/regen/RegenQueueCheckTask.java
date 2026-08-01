package engineer.skyouo.plugins.naturerevive.spigot.tasks.regen;

import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.SQLDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.managers.ChunkRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.managers.ExpiryIndex;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.*;

public class RegenQueueCheckTask implements Task {

    private static final int MAX_PER_RUN = 4096;

    @Override
    public void run() {
        if (databaseConfig == null) return;

        if (!readonlyConfig.regenerationStrategy.equalsIgnoreCase("passive")
                && !readonlyConfig.regenerationStrategy.equalsIgnoreCase("average")) {
            try {
                for (BukkitPositionInfo positionInfo : ExpiryIndex.drainExpired(System.currentTimeMillis(), MAX_PER_RUN)) {
                    ChunkRegeneration.enqueue(positionInfo);
                }
            } catch (Exception ignored) {
            }
        }

        if (readonlyConfig.regenerationStrategy.equalsIgnoreCase("average")) {
            ScheduleUtil.GLOBAL.runTask(instance, RegenQueueCheckTask::checkNearbyChunks);
        }
    }

    private static void checkNearbyChunks() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            for (int x = -1; x < readonlyConfig.chunkRegenerateRadiusOnAverageApplied; x++)
                for (int z = -1; z < readonlyConfig.chunkRegenerateRadiusOnAverageApplied; z++) {
                    if (x == z && x == 0)
                        continue;

                    try {
                        BukkitPositionInfo positionInfo = databaseConfig.get(
                                new BukkitPositionInfo(player.getWorld().getName(),
                                        player.getLocation().getChunk().getX() + x,
                                        player.getLocation().getChunk().getZ() + z, 0));

                        if (positionInfo == null)
                            continue;

                        if (positionInfo.isOverTTL()) {
                            ChunkRegeneration.enqueue(positionInfo);
                        }
                    } catch (Exception e) {
                        return;
                    }
                }
        }
    }

    @Override
    public boolean isAsync() {
        return databaseConfig instanceof SQLDatabaseAdapter;
    }

    @Override
    public long getDelay() {
        return 20L;
    }

    @Override
    public long getRepeatTime() {
        return readonlyConfig.checkChunkTTLTick;
    }
}
