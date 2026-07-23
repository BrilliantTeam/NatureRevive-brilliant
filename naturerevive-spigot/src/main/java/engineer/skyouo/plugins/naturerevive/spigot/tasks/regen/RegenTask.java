package engineer.skyouo.plugins.naturerevive.spigot.tasks.regen;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationUtil;
import engineer.skyouo.plugins.naturerevive.spigot.integration.land.ILandPluginIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import engineer.skyouo.plugins.naturerevive.spigot.util.Util;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;

import java.util.List;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.*;

public class RegenTask implements Task {
    private String lastSkipReason = "";

    @Override
    public void run() {
        if (!isSuitableForChunkRegeneration()) {
            return;
        }

        for (int i = 0; i < readonlyConfig.taskPerProcess && queue.hasNext(); i++) {
            BukkitPositionInfo task = queue.pop();

            if (!readonlyConfig.allowedWorld.isEmpty() && !readonlyConfig.allowedWorld.contains(task.getWorldName())) {
                regenInFlight.remove(task.getChunkKey());
                continue;
            }

            if (readonlyConfig.ignoredWorld.contains(task.getWorldName())) {
                regenInFlight.remove(task.getChunkKey());
                continue;
            }

            if (Bukkit.getWorld(task.getWorldName()) == null) {
                regenInFlight.remove(task.getChunkKey());
                continue;
            }

            ScheduleUtil.REGION.runTask(NatureRevivePlugin.instance, task.getLocation(), () -> {
                List<ILandPluginIntegration> integrations = IntegrationUtil.getLandIntegrations();
                boolean hasLandProtection = !integrations.isEmpty() &&
                        integrations.stream().anyMatch(integration ->
                            integration.checkHasLand(task.getLocation().getChunk()) && !integration.isStrictMode()
                        );

                if (hasLandProtection) {
                    databaseConfig.unset(task);
                    regenInFlight.remove(task.getChunkKey());
                    NatureReviveComponentLogger.debug("Skipped (land) and removed from DB: %s", TextColor.fromHexString("#AAAAAA"), task);
                    return;
                }
                task.regenerateChunk();

                NatureReviveComponentLogger.debug("%s was regenerated.", TextColor.fromHexString("#AAAAAA"), task);
            });
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public long getDelay() {
        return 20;
    }

    @Override
    public long getRepeatTime() {
        return readonlyConfig.queuePerNTick;
    }

    private boolean isSuitableForChunkRegeneration() {
        int players = Bukkit.getServer().getOnlinePlayers().size();
        double tps = Util.isFolia() ? Bukkit.getTPS()[0] : nmsWrapper.getRecentTps()[0];
        boolean timeOk = readonlyConfig.isCurrentTimeAllowForRSC();

        String reason = null;
        if (players >= readonlyConfig.maxPlayersCountForRegeneration)
            reason = "players " + players + " >= max " + readonlyConfig.maxPlayersCountForRegeneration;
        else if (tps <= readonlyConfig.minTPSCountForRegeneration)
            reason = String.format("TPS %.2f <= min %.2f", tps, readonlyConfig.minTPSCountForRegeneration);
        else if (!enableRevive)
            reason = "enableRevive=false (paused)";
        else if (!timeOk)
            reason = "outside spawn-timer (" + readonlyConfig.spawnTimer + ")";

        if (reason != null) {
            if (!reason.equals(lastSkipReason)) {
                NatureReviveComponentLogger.debug("Regen skipped: %s", reason);
                lastSkipReason = reason;
            }
            return false;
        }
        if (!lastSkipReason.isEmpty()) {
            NatureReviveComponentLogger.debug("Regen resumed");
            lastSkipReason = "";
        }
        return true;
    }
}