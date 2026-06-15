package engineer.skyouo.plugins.naturerevive.spigot.tasks;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.block.BlockLoggingTask;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.block.BlockPutTask;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.data.DatabaseSaveTask;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.data.ElytraResetTask;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.data.WebhookFlushTask;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.regen.RegenDelayedFlagChunkTask;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.regen.RegenQueueCheckTask;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.regen.RegenTask;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private List<Task> registeredTasks = new ArrayList<>();
    private List<WrappedTask> wrappedTasks = new ArrayList<>();

    public TaskManager() {

    }

    public void init() {
        if (!registeredTasks.isEmpty() || !wrappedTasks.isEmpty()) {
            unregisterTasks();
        }

        for (Task task : getDefaultTasks()) {
            registerTask(task);
        }
    }

    public void registerTask(Task task) {
        if (task.isAsync()) {
            wrappedTasks.add(
                    ScheduleUtil.GLOBAL.runTaskTimerAsynchronously(
                            NatureRevivePlugin.instance,
                            task,
                            task.getDelay(),
                            task.getRepeatTime()
                    )
            );
        } else {
            wrappedTasks.add(
                    ScheduleUtil.GLOBAL.runTaskTimer(
                            NatureRevivePlugin.instance,
                            task,
                            task.getDelay(),
                            task.getRepeatTime()
                    )
            );
        }

        NatureReviveComponentLogger.debug("Registered task [name=%s, delay=%d, fixedRate=%d]",
                task.getClass(), task.getDelay(), task.getRepeatTime());
    }

    public void unregisterTasks() {
        for (WrappedTask task : wrappedTasks) {
            task.cancel();
        }

        registeredTasks.clear();
        wrappedTasks.clear();
    }

    private List<Task> getDefaultTasks() {
        return List.of(new RegenDelayedFlagChunkTask(), new RegenQueueCheckTask(), new RegenTask(),
                new DatabaseSaveTask(), new ElytraResetTask(), new BlockLoggingTask(), new BlockPutTask(),
                new WebhookFlushTask());
    }
}
