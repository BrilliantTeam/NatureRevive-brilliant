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
    @Override
    public void run() {
        if (queue.size() > 0 && isSuitableForChunkRegeneration()) {
            for (int i = 0; i < readonlyConfig.taskPerProcess && queue.hasNext(); i++) {
                BukkitPositionInfo task = queue.pop();

                if (!readonlyConfig.allowedWorld.isEmpty() && !readonlyConfig.allowedWorld.contains(task.getLocation().getWorld().getName()))
                    continue;

                if (readonlyConfig.ignoredWorld.contains(task.getLocation().getWorld().getName()))
                    continue;

                List<ILandPluginIntegration> integrations = IntegrationUtil.getLandIntegrations();

                if (!integrations.isEmpty() &&
                        integrations.stream().anyMatch(integration -> integration.checkHasLand(task.getLocation().getChunk()) && !integration.isStrictMode()))
                    continue;

                ScheduleUtil.REGION.runTask(NatureRevivePlugin.instance, task.getLocation(), () -> {
                    task.regenerateChunk();

                    NatureReviveComponentLogger.debug("%s was regenerated.", TextColor.fromHexString("#AAAAAA"), task);
                });
            }
        } else {
            // 未達成 無法生成區塊 清除序列
            while (queue.hasNext()){
                queue.pop();
            }
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
        // 新增時間閥
        return Bukkit.getServer().getOnlinePlayers().size() < readonlyConfig.maxPlayersCountForRegeneration && (Util.isFolia() ?
                Bukkit.getTPS()[0] : nmsWrapper.getRecentTps()[0]) > readonlyConfig.minTPSCountForRegeneration &&
                enableRevive && readonlyConfig.isCurrentTimeAllowForRSC();
    }
}
