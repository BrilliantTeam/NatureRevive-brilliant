package engineer.skyouo.plugins.naturerevive.spigot.integration.engine;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.managers.ChunkRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class InPlaceEngineIntegration implements IEngineIntegration {
    @Override
    public String getPluginName() {
        return "NatureRevive (in-place)";
    }

    @Override
    public Type getType() {
        return Type.ENGINE;
    }

    @Override
    public boolean load() {
        return NatureRevivePlugin.nmsWrapper != null && NatureRevivePlugin.nmsWrapper.supportsInPlaceRegeneration();
    }

    @Override
    public String getEngineName() {
        return "inplace";
    }

    @Override
    public boolean shouldExitOnFatal() {
        return isEnabled();
    }

    @Override
    public void regenerateChunk(Plugin plugin, Chunk chunk, Runnable postTask) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // The structure snapshot has to be taken on the chunk's own thread, the generation must not
        // be (it is the expensive part), and the write back has to be again.
        ScheduleUtil.REGION.runTask(plugin, chunk, () -> {
            Object context;

            try {
                context = NatureRevivePlugin.nmsWrapper.captureRegenerationContext(world, chunkX, chunkZ);
            } catch (Exception e) {
                e.printStackTrace();
                ChunkRegeneration.releaseInFlightWithTickets(world, chunkX, chunkZ);
                return;
            }

            ScheduleUtil.GLOBAL.runTaskAsynchronously(plugin, () -> {
                Object prepared;

                try {
                    prepared = NatureRevivePlugin.nmsWrapper.prepareRegeneratedChunk(world, chunkX, chunkZ, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    ChunkRegeneration.releaseInFlightWithTickets(world, chunkX, chunkZ);
                    return;
                }

                ScheduleUtil.REGION.runTask(plugin, chunk, () -> {
                    try {
                        NatureRevivePlugin.nmsWrapper.applyRegeneratedChunk(world, chunkX, chunkZ, prepared);

                        if (postTask != null)
                            postTask.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                        ChunkRegeneration.releaseInFlightWithTickets(world, chunkX, chunkZ);
                    }
                });
            });
        });
    }
}
