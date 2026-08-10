package engineer.skyouo.plugins.naturerevive.spigot.integration.engine;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IDependency;
import org.bukkit.Chunk;
import org.bukkit.plugin.Plugin;

public interface IEngineIntegration extends IDependency {
    /** The {@code regeneration-engine} config value that selects this engine. */
    String getEngineName();

    @Override
    default boolean isEnabled() {
        return NatureRevivePlugin.readonlyConfig.regenerationEngine.equalsIgnoreCase(getEngineName());
    }

    void regenerateChunk(Plugin plugin, Chunk chunk, Runnable postTask);
}
