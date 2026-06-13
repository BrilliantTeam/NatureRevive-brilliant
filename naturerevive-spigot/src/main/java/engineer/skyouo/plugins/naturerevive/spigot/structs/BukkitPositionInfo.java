package engineer.skyouo.plugins.naturerevive.spigot.structs;

import engineer.skyouo.plugins.naturerevive.common.structs.PositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.managers.ChunkRegeneration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.*;

/*
* The BukkitPositionInfo class is quite different from old PositionInfo class in 1.x version,
* The new class allow chunk position instead of fixed x-z position for reduce class usage.
* */
@SerializableAs("PositionInfo")
public final class BukkitPositionInfo extends PositionInfo implements ConfigurationSerializable {
    public BukkitPositionInfo(Location location, long ttl) {
        super(location.getWorld().getName(), location.getChunk().getX(), location.getChunk().getZ(), ttl);
    }

    public BukkitPositionInfo(String worldName, int x, int z, long ttl) {
        super(worldName, x, z, ttl);
    }

    public BukkitPositionInfo(Map<String, Object> map) {
        super((String) map.get("world"), (int) map.get("chunkX"), (int) map.get("chunkZ"), (long) map.get("ttl"));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("world", worldName);
        result.put("chunkX", x);
        result.put("chunkZ", z);
        result.put("ttl", ttl);

        return result;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(worldName), (x << 4), -64, (z << 4));
    }

    public long getTTL() {
        return ttl;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getChunkKey() {
        return worldName + ":" + x + ":" + z;
    }

    public void setTTL(long ttl) {
        this.ttl = ttl;
    }

    public void regenerateChunk() {
        ChunkRegeneration.regenerateChunk(this);
    }

    @Override
    public String toString() {
        return "BukkitPositionInfo{x = " + x + ", z = " + z + ", ttl = " + ttl + "}";
    }
}
