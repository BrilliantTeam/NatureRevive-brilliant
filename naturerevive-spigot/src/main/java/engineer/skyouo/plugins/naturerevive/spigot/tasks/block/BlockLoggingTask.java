package engineer.skyouo.plugins.naturerevive.spigot.tasks.block;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationUtil;
import engineer.skyouo.plugins.naturerevive.spigot.integration.logging.ILoggingIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockDataChangeWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;

import java.util.concurrent.atomic.AtomicInteger;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.blockDataChangeWithPos;

public class BlockLoggingTask implements Task {
    private final AtomicInteger inflightTasks = new AtomicInteger(0);
    private static final int MAX_INFLIGHT = 200;
    private static final int MAX_RETRY = 3;

    @Override
    public void run() {
        if (blockDataChangeWithPos.hasNext()) {
            for (int i = 0; i < 200 && blockDataChangeWithPos.hasNext()
                    && inflightTasks.get() < MAX_INFLIGHT; i++) {
                BlockDataChangeWithPos blockDataChangeWithPosObject = blockDataChangeWithPos.pop();
                inflightTasks.incrementAndGet();

                ScheduleUtil.REGION.runTask(NatureRevivePlugin.instance, blockDataChangeWithPosObject.getLocation(), () -> {
                    try {
                        synchronized (blockDataChangeWithPosObject) {
                            for (ILoggingIntegration integration : IntegrationUtil.getLoggingIntegrations()) {
                                if (!integration.isEnabled()) continue;

                                integration.log(blockDataChangeWithPosObject);
                            }
                        }
                    } catch (IllegalStateException e) {
                        if (e.getMessage().contains("asynchronous")) {
                            blockDataChangeWithPosObject.addFailedTime();
                            if (blockDataChangeWithPosObject.getFailedTime() <= MAX_RETRY) {
                                blockDataChangeWithPos.add(blockDataChangeWithPosObject);
                            }
                        }

                        e.printStackTrace();
                    } finally {
                        inflightTasks.decrementAndGet();
                    }
                });
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
        return 2L;
    }
}
