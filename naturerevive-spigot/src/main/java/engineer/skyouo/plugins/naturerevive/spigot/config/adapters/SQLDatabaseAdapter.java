package engineer.skyouo.plugins.naturerevive.spigot.config.adapters;

import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.structs.SQLCommand;

import java.util.List;
import java.util.Set;

public interface SQLDatabaseAdapter {
    void massUpdate(Set<BukkitPositionInfo> positionInfoSet);

    void massInsert(Set<BukkitPositionInfo> positionInfoSet);

    boolean massExecute(List<SQLCommand> sqlCommandList);
}