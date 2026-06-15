package engineer.skyouo.plugins.naturerevive.spigot.tasks.regen;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.listeners.ChunkRelatedEventListener;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import engineer.skyouo.plugins.naturerevive.spigot.webhook.FlaggedLocation;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.blockQueue;
import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.readonlyConfig;

public class RegenDelayedFlagChunkTask implements Task {
    @Override
    public void run() {
        NatureRevivePlugin plugin = JavaPlugin.getPlugin(NatureRevivePlugin.class);

        for (int i = 0; i < readonlyConfig.blockProcessingAmountPerProcessing && blockQueue.hasNext(); i++) {
            FlaggedLocation flagged = blockQueue.pop();

            if (flagged == null) continue;

            Location location = flagged.getLocation();

            if (location != null && location.getWorld() != null) {
                ScheduleUtil.REGION.runTask(plugin, location, () -> {
                    ChunkRelatedEventListener.flagChunk(location, flagged.getReason());
                });
            }
        }
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public long getDelay() {
        return 20L;
    }

    @Override
    public long getRepeatTime() {
        return readonlyConfig.blockProcessingTick;
    }
}