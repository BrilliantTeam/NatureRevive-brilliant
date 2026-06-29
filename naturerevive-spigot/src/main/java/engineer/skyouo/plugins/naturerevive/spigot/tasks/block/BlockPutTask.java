package engineer.skyouo.plugins.naturerevive.spigot.tasks.block;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockStateWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import org.bukkit.Location;

import java.util.concurrent.atomic.AtomicInteger;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.*;

public class BlockPutTask implements Task {
    private final AtomicInteger inflightTasks = new AtomicInteger(0);
    private static final int MAX_INFLIGHT = 500;

    @Override
    public void run() {
        for (int i = 0; i < readonlyConfig.blockPutPerTick && blockStateWithPosQueue.hasNext()
                && inflightTasks.get() < MAX_INFLIGHT; i++) {
            BlockStateWithPos blockStateWithPos = blockStateWithPosQueue.pop();

            Location location = blockStateWithPos.getLocation();
            inflightTasks.incrementAndGet();

            ScheduleUtil.REGION.runTask(instance, location, () -> {
                try {
                    nmsWrapper.setBlockNMS(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), blockStateWithPos.getBlockState().getBlockData());

                    if (blockStateWithPos.getTileEntityNbt() != null) {
                        try {
                            nmsWrapper.loadTileEntity(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), blockStateWithPos.getTileEntityNbt());
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                } finally {
                    inflightTasks.decrementAndGet();
                }
            });
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
        return readonlyConfig.blockPutActionPerNTick;
    }
}
