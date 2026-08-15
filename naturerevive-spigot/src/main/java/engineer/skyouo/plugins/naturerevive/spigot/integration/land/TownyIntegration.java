package engineer.skyouo.plugins.naturerevive.spigot.integration.land;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class TownyIntegration implements ILandPluginIntegration {
    private static TownyAPI townyAPI;

    @Override
    public boolean checkHasLand(Chunk chunk) {
        int step = Math.max(1, Math.min(TownySettings.getTownBlockSize(), 16));

        for (int x = 0; x < 16; x += step) {
            for (int z = 0; z < 16; z += step) {
                Location location = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, 64, (chunk.getZ() << 4) + z);
                if (!townyAPI.isWilderness(location)) return true;
            }
        }

        return false;
    }

    @Override
    public boolean isInLand(Location location) {
        return !townyAPI.isWilderness(location);
    }

    @Override
    public boolean isStrictMode() {
        return NatureRevivePlugin.readonlyConfig.townyStrictCheck;
    }

    @Override
    public String getPluginName() {
        return "Towny";
    }

    @Override
    public Type getType() {
        return Type.LAND;
    }

    @Override
    public boolean load() {
        Plugin townyPlugin = NatureRevivePlugin.instance.getServer().getPluginManager().getPlugin("Towny");
        townyAPI = townyPlugin != null ? TownyAPI.getInstance() : null;
        return townyAPI != null;
    }

    @Override
    public boolean isEnabled() {
        return townyAPI != null;
    }

    @Override
    public boolean shouldExitOnFatal() {
        return isStrictMode();
    }
}
