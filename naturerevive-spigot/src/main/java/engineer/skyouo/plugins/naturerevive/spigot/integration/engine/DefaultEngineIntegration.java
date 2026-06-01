package engineer.skyouo.plugins.naturerevive.spigot.integration.engine;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.util.Util;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class DefaultEngineIntegration implements IEngineIntegration {
    @Override
    public String getPluginName() {
        return "NatureRevive";
    }

    @Override
    public Type getType() {
        return Type.ENGINE;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Util.isPaper() && NatureRevivePlugin.readonlyConfig.regenerationEngine.equalsIgnoreCase("bukkit");
    }

    @Override
    public boolean shouldExitOnFatal() {
        return false;
    }

    @Override
    public void regenerateChunk(Plugin plugin, Chunk chunk, Runnable postTask) {
        World world = chunk.getWorld();
        int x = chunk.getX(), z = chunk.getZ();

        world.removePluginChunkTicket(x, z, plugin);
        try {
            NatureRevivePlugin.nmsWrapper.regenerateChunk(world, x, z);
        } finally {
            world.addPluginChunkTicket(x, z, plugin);
        }

        if (postTask != null)
            postTask.run();
    }
}
