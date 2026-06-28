package engineer.skyouo.plugins.naturerevive.spigot;

import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.SQLDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.managers.ChunkRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SuspendedZone {
    private static Path filePath = new File(NatureRevivePlugin.instance.getDataFolder(), ".suspension_zone_record_do_not_delete").toPath();
    private long frozenTimeStartMs = -1;

    public SuspendedZone() {
        try {
            frozenTimeStartMs = Long.parseLong(Files.readString(filePath));
            if (frozenTimeStartMs != -1)
                NatureRevivePlugin.enableRevive = false;
        } catch (NoSuchFileException ignored) {

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void frozen() {
        if (!NatureRevivePlugin.enableRevive)
            throw new RuntimeException("Cannot frozen the queue since the regeneration system is paused!");

        NatureRevivePlugin.enableRevive = false;

        frozenTimeStartMs = System.currentTimeMillis();
    }

    public void resume() {
        if (NatureRevivePlugin.enableRevive)
            throw new RuntimeException("Cannot resume the queue since the regeneration system is not paused!");

        List<BukkitPositionInfo> drained = new ArrayList<>();
        while (NatureRevivePlugin.queue.hasNext()) {
            BukkitPositionInfo task = NatureRevivePlugin.queue.pop();
            if (task != null) {
                drained.add(task);
                NatureRevivePlugin.regenInFlight.remove(task.getChunkKey());
            }
        }

        Set<BukkitPositionInfo> toUpdate = new HashSet<>();

        for (BukkitPositionInfo task : drained) {
            if ((task.getTTL() - NatureRevivePlugin.readonlyConfig.ttlDuration) < frozenTimeStartMs) {
                ChunkRegeneration.enqueue(task);
            } else {
                task.setTTL(System.currentTimeMillis() + (task.getTTL() - frozenTimeStartMs));
                toUpdate.add(task);
            }
        }

        if (NatureRevivePlugin.databaseConfig instanceof SQLDatabaseAdapter adapter) {
            adapter.massUpdate(toUpdate);
        } else {
            for (BukkitPositionInfo positionInfo : toUpdate)
                NatureRevivePlugin.databaseConfig.set(positionInfo);
        }

        NatureRevivePlugin.enableRevive = true;

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        frozenTimeStartMs = -1;
    }

    public void close() throws IOException {
        if (frozenTimeStartMs != -1)
            Files.write(
                    new File(NatureRevivePlugin.instance.getDataFolder(), ".suspension_zone_record_do_not_delete").toPath(),
                    String.valueOf(frozenTimeStartMs).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE
            );
    }
}
