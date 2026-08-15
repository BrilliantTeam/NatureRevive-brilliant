package engineer.skyouo.plugins.naturerevive.spigot.integration.land;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class WorldGuardIntegration implements ILandPluginIntegration {
    private static RegionContainer regionContainer;

    @Override
    public boolean checkHasLand(Chunk chunk) {
        World world = chunk.getWorld();
        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
        if (regionManager == null) return false;

        return ChunkProbe.intersectsRegion(regionManager, world, chunk.getX(), chunk.getZ());
    }

    private static final class ChunkProbe {
        static boolean intersectsRegion(RegionManager regionManager, World world, int chunkX, int chunkZ) {
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;

            ProtectedRegion chunkRegion = new ProtectedCuboidRegion("__naturerevive_chunk__", true,
                    BlockVector3.at(minX, world.getMinHeight(), minZ),
                    BlockVector3.at(minX + 15, world.getMaxHeight(), minZ + 15));

            return regionManager.getApplicableRegions(chunkRegion).size() > 0;
        }
    }

    @Override
    public boolean isInLand(Location location) {
        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));
        return regionManager != null && regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location)).size() > 0;
    }

    @Override
    public boolean isStrictMode() {
        return NatureRevivePlugin.readonlyConfig.worldGuardStrictCheck;
    }

    @Override
    public String getPluginName() {
        return "WorldGuard";
    }

    @Override
    public Type getType() {
        return Type.LAND;
    }

    @Override
    public boolean load() {
        Plugin worldGuardPlugin = NatureRevivePlugin.instance.getServer().getPluginManager().getPlugin("WorldGuard");
        regionContainer = worldGuardPlugin != null ? WorldGuard.getInstance().getPlatform().getRegionContainer() : null;
        return regionContainer != null;
    }

    @Override
    public boolean isEnabled() {
        return regionContainer != null;
    }

    @Override
    public boolean shouldExitOnFatal() {
        return isStrictMode();
    }
}
