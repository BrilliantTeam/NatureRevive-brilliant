package engineer.skyouo.plugins.naturerevive.spigot.integration.land;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import org.ashin.chunkClaimPlugin2.api.ChunkClaimAPI;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class ChunkClaimIntegration implements ILandPluginIntegration {
    private static ChunkClaimAPI chunkClaimAPI;

    @Override
    public boolean checkHasLand(Chunk chunk) {
        return chunkClaimAPI.isChunkClaimed(chunk);
    }

    @Override
    public boolean isInLand(Location location) {
        return chunkClaimAPI.isChunkClaimed(location.getChunk());
    }

    @Override
    public boolean isStrictMode() {
        return false;
    }

    @Override
    public String getPluginName() {
        return "ChunkClaimPlugin2";
    }

    @Override
    public Type getType() {
        return Type.LAND;
    }

    @Override
    public boolean load() {
        Plugin chunkClaimPlugin = NatureRevivePlugin.instance.getServer().getPluginManager().getPlugin("CCP");
        chunkClaimAPI = chunkClaimPlugin != null ? ChunkClaimAPI.getInstance() : null;
        return chunkClaimAPI != null;
    }

    @Override
    public boolean isEnabled() {
        return chunkClaimAPI != null;
    }

    @Override
    public boolean shouldExitOnFatal() {
        return isStrictMode();
    }
}
