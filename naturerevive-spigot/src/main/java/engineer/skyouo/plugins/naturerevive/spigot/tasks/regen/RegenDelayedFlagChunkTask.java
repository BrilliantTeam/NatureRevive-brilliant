package engineer.skyouo.plugins.naturerevive.spigot.tasks.regen;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.listeners.ChunkRelatedEventListener;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.blockQueue;
import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.readonlyConfig;

public class RegenDelayedFlagChunkTask implements Task {
    @Override
    public void run() {
        NatureRevivePlugin plugin = JavaPlugin.getPlugin(NatureRevivePlugin.class);

        for (int i = 0; i < readonlyConfig.blockProcessingAmountPerProcessing && blockQueue.hasNext(); i++) {
            Location location = blockQueue.pop();

            if (location != null && location.getWorld() != null) {
                ScheduleUtil.REGION.runTask(plugin, location, () -> {
                    ChunkRelatedEventListener.flagChunk(location);
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