package engineer.skyouo.plugins.naturerevive.spigot.tasks.data;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.SQLDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.structs.SQLCommand;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.Task;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.*;

public class DatabaseSaveTask implements Task {
    @Override
    public void run() {
        if (databaseConfig instanceof SQLDatabaseAdapter adapter) {
            List<SQLCommand> sqlCommands = new ArrayList<>();

            int i = 0;

            while (sqlCommandQueue.hasNext() && i < readonlyConfig.sqlProcessingCount) {
                SQLCommand cmd = sqlCommandQueue.pop();
                if (cmd != null) {
                    sqlCommands.add(cmd);
                    i++;
                }
            }

            if (!sqlCommands.isEmpty()) {
                adapter.massExecute(sqlCommands);
            }
        } else {
            ScheduleUtil.GLOBAL.runTask(instance, () -> {
                try {
                    databaseConfig.save();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
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
