package engineer.skyouo.plugins.naturerevive.spigot.tasks.data;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
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
            int limit = readonlyConfig.sqlProcessingCount > 0 ? readonlyConfig.sqlProcessingCount : Integer.MAX_VALUE;

            List<SQLCommand> sqlCommands = new ArrayList<>();

            int i = 0;

            while (sqlCommandQueue.hasNext() && i < limit) {
                SQLCommand cmd = sqlCommandQueue.pop();
                if (cmd != null) {
                    sqlCommands.add(cmd);
                    i++;
                }
            }

            if (!sqlCommands.isEmpty()) {
                boolean ok = adapter.massExecute(sqlCommands);

                if (ok) {
                    int distinctChunks = adapter.collapseToLatestPerChunk(sqlCommands).size();
                    NatureReviveComponentLogger.debug("DatabaseSaveTask 已處理 %d 筆佇列指令（收斂為 %d 個區塊）寫入資料庫。", sqlCommands.size(), distinctChunks);
                } else {
                    for (SQLCommand cmd : sqlCommands) {
                        sqlCommandQueue.add(cmd);
                    }
                }
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
