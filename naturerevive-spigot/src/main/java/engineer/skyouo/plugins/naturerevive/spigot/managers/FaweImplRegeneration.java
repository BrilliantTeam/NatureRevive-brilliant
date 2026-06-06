package engineer.skyouo.plugins.naturerevive.spigot.managers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Chunk;
import org.jetbrains.annotations.Nullable;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.nmsWrapper;

public class FaweImplRegeneration {
    public static void regenerate(Chunk chunk, boolean regenBiomes, @Nullable Runnable afterTask) {
        long o = System.currentTimeMillis();
        BukkitWorld bukkitWorld = new BukkitWorld(chunk.getWorld());

        int minY = nmsWrapper.getWorldMinHeight(chunk.getWorld());
        int maxY = chunk.getWorld().getMaxHeight();
        BlockVector3 one = BlockVector3.at(chunk.getX() << 4, minY, chunk.getZ() << 4);
        BlockVector3 two = BlockVector3.at((chunk.getX() << 4) + 15, maxY, (chunk.getZ() << 4) + 15);

        NatureReviveComponentLogger.debug("Regenerating From (%d, %d, %d) to (%d, %d, %d) in %s",
                TextColor.fromHexString("#AAAAAA"),
                chunk.getX() << 4, minY, chunk.getZ() << 4,
                (chunk.getX() << 4) + 15, maxY, (chunk.getZ() << 4) + 15, bukkitWorld.getName());

        Region region = new CuboidRegion(bukkitWorld, one, two);
        RegenOptions options = RegenOptions.builder()
                .seed(chunk.getWorld().getSeed())
                .regenBiomes(regenBiomes)
                .build();

        try (EditSession session = WorldEdit.getInstance().newEditSession(bukkitWorld)) {
            Mask mask = session.getMask();
            try {
                session.setMask(null);
                session.setSourceMask(null);
                bukkitWorld.regenerate(region, session, options);
            } finally {
                session.setMask(mask);
                session.setSourceMask(mask);
            }
        }

        NatureReviveComponentLogger.debug("Regen time cost %d ms", TextColor.fromHexString("#AAAAAA"),
                System.currentTimeMillis() - o);

        if (afterTask != null)
            ScheduleUtil.REGION.runTask(NatureRevivePlugin.instance, chunk, afterTask);
    }
}
