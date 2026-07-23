package engineer.skyouo.plugins.naturerevive.spigot.config.adapters;

import org.bukkit.Location;

import java.io.IOException;
import java.util.Base64;

import engineer.skyouo.plugins.naturerevive.spigot.config.DatabaseConfig;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

public class YamlDatabaseAdapter implements DatabaseConfig {
    private final File file = new File("plugins/NatureRevive/database.yml");

    private final YamlConfiguration configuration;

    public YamlDatabaseAdapter() {
        new File("plugins/NatureRevive").mkdirs();
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void set(BukkitPositionInfo positionInfo) {
        configuration.set(safeFormatLocation(positionInfo), positionInfo);
    }

    public void unset(BukkitPositionInfo positionInfo) {
        configuration.set(safeFormatLocation(positionInfo), null);
    }

    public BukkitPositionInfo get(Location location) {
        return (BukkitPositionInfo) configuration.get(formatLocation(location));
    }

    public BukkitPositionInfo get(BukkitPositionInfo positionInfo) {
        return (BukkitPositionInfo) configuration.get(safeFormatLocation(positionInfo), positionInfo);
    }

    public List<BukkitPositionInfo> values() {
        ArrayList<BukkitPositionInfo> positionInfos = new ArrayList<>();
        Set<String> keys = configuration.getKeys(false);

        for (String key : keys) {
            BukkitPositionInfo positionInfo = (BukkitPositionInfo) configuration.get(key);
            positionInfos.add(positionInfo);
        }

        return positionInfos;
    }

    public void save() throws IOException {
        configuration.save(file);
    }

    public void close() {
    }

    private String safeFormatLocation(BukkitPositionInfo positionInfo) {
        return new String(Base64.getEncoder().encode((positionInfo.getWorldName() + "|" + positionInfo.getX() + "|" + positionInfo.getZ()).getBytes()));
    }

    private String formatLocation(Location location) {
        return new String(Base64.getEncoder().encode((location.getWorld().getName() + "|" + (location.getBlockX() >> 4) + "|" + (location.getBlockZ() >> 4)).getBytes()));
    }
}