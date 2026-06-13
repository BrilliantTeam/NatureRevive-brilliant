package engineer.skyouo.plugins.naturerevive.spigot.config.adapters;

import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.structs.SQLCommand;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SQLDatabaseAdapter {
    void massUpdate(Set<BukkitPositionInfo> positionInfoSet);

    void massInsert(Set<BukkitPositionInfo> positionInfoSet);

    boolean massExecute(List<SQLCommand> sqlCommandList);

    default Map<String, SQLCommand> collapseToLatestPerChunk(List<SQLCommand> sqlCommandList) {
        Map<String, SQLCommand> latestByChunk = new LinkedHashMap<>();

        for (SQLCommand sqlCommand : sqlCommandList) {
            if (sqlCommand == null) {
                continue;
            }

            BukkitPositionInfo pos = sqlCommand.getBukkitPositionInfo();
            String key = pos.getWorldName() + ":" + pos.getX() + ":" + pos.getZ();
            latestByChunk.put(key, sqlCommand);
        }

        return latestByChunk;
    }
}