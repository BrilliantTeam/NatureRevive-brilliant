package engineer.skyouo.plugins.naturerevive.spigot.tasks.regen;

import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.sql.SQLException;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.*;

public class RegenQueueCheckTask implements Task {
    @Override
    public void run() {
        if (databaseConfig == null) return;

        if (!readonlyConfig.regenerationStrategy.equalsIgnoreCase("passive") && !readonlyConfig.regenerationStrategy.equalsIgnoreCase("average")) {
            List<BukkitPositionInfo> positionInfos;
            
            try {
                positionInfos = databaseConfig.values();
            } catch (Exception e) {
                positionInfos = Collections.emptyList();
            }

            for (BukkitPositionInfo positionInfo : positionInfos) {
                if (positionInfo.isOverTTL()) {
                    queue.add(positionInfo);
                    try {
                        databaseConfig.unset(positionInfo);
                    } catch (Exception e) {
                    }
                }
            }
        }

        if (readonlyConfig.regenerationStrategy.equalsIgnoreCase("average")) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                for (int x = -1; x < readonlyConfig.chunkRegenerateRadiusOnAverageApplied; x++)
                    for (int z = -1; z < readonlyConfig.chunkRegenerateRadiusOnAverageApplied; z++) {
                        if (x == z && x == 0)
                            continue;

                        try {
                            BukkitPositionInfo positionInfo =
                                    databaseConfig.get(new BukkitPositionInfo(player.getWorld().getName(), player.getLocation().getChunk().getX() + x, player.getLocation().getChunk().getZ() + z, 0).getLocation());

                            if (positionInfo == null)
                                continue;

                            if (positionInfo.isOverTTL()) {
                                queue.add(positionInfo);
                                databaseConfig.unset(positionInfo);
                            }
                        } catch (Exception e) {
                            return; 
                        }
                    }
            }
        }
    }

    @Override
    public boolean isAsync() {
        return false;
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