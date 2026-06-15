package engineer.skyouo.plugins.naturerevive.spigot.tasks.data;

import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.webhook.WebhookManager;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.readonlyConfig;

public class WebhookFlushTask implements Task {
    @Override
    public void run() {
        WebhookManager.flush();
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public long getDelay() {
        return readonlyConfig.dataSaveTime;
    }

    @Override
    public long getRepeatTime() {
        return readonlyConfig.dataSaveTime;
    }
}
