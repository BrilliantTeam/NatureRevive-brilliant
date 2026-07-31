package engineer.skyouo.plugins.naturerevive.spigot.config;

import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import org.bukkit.Location;


import java.io.IOException;
import java.util.Collection;

public interface DatabaseConfig {
    void set(BukkitPositionInfo positionInfo);

    void unset(BukkitPositionInfo positionInfo);

    BukkitPositionInfo get(Location location);

    BukkitPositionInfo get(BukkitPositionInfo positionInfo);

    Collection<BukkitPositionInfo> values();

    void save() throws IOException;

    void close();
}
