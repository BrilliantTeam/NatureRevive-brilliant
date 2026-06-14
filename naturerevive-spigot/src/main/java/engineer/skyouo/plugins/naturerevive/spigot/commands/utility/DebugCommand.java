package engineer.skyouo.plugins.naturerevive.spigot.commands.utility;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.commands.SubCommand;
import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.MySQLDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;

public class DebugCommand implements SubCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        /*
        sender.sendMessage("============================");
        sender.sendMessage("Queue tasks: ");
        for (Iterator<BukkitPositionInfo> it = NatureRevivePlugin.queue.iterator(); it.hasNext(); ) {
            BukkitPositionInfo task = it.next();
            sender.sendMessage(task.getLocation().toString() + " - " + task.getTTL());
        }

        sender.sendMessage(" ");

        sender.sendMessage("Database tasks: ");
        for (BukkitPositionInfo positionInfo : NatureRevivePlugin.databaseConfig.values()) {
            sender.sendMessage(positionInfo.getLocation().toString() + " - " + positionInfo.getTTL());
        }

        sender.sendMessage(" ");
        sender.sendMessage("Database no cache tasks: ");
        try {
            for (BukkitPositionInfo positionInfo : NatureRevivePlugin.databaseConfig.values()) {
                BukkitPositionInfo positionInfoNoCache = ((MySQLDatabaseAdapter) NatureRevivePlugin.databaseConfig).getNoCache(positionInfo);
                sender.sendMessage(positionInfoNoCache.getLocation().toString() + " - " + positionInfoNoCache.getTTL());
            }
        } catch (Exception ignored) {}

        sender.sendMessage("Time now is: " + System.currentTimeMillis());
        sender.sendMessage("============================");
        return true;
         */

        sender.sendMessage(Lang.get("command.debug.status-header"));
        sender.sendMessage(Lang.get("command.debug.queue-size", NatureRevivePlugin.queue.size()));

        if ((sender instanceof Player player)) {
            BukkitPositionInfo positionInfo = NatureRevivePlugin.databaseConfig.get(player.getLocation());
            Chunk pChunk = player.getChunk();

            if (positionInfo == null) return true;

            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(positionInfo.getTTL()), ZoneId.systemDefault());
            // Duration duration = Duration.of(NatureRevivePlugin.readonlyConfig.ttlDuration, ChronoUnit.MILLIS);

            sender.sendMessage(Lang.get("command.debug.chunk-regen-date",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(time)));


            int i = 0;
            BukkitPositionInfo bPosition = null;
            for (Iterator<BukkitPositionInfo> it = NatureRevivePlugin.queue.iterator(); it.hasNext(); ) {
                i++;
                BukkitPositionInfo task = it.next();

                if (task.getX() == pChunk.getX() && task.getZ() == pChunk.getZ()) {
                    bPosition = task;
                    break;
                }
            }

            sender.sendMessage(Lang.get("command.debug.chunk-queue-position",
                    bPosition == null
                            ? Lang.get("command.debug.queue-position-none")
                            : Lang.get("command.debug.queue-position-nth", i)));
        } else {
            sender.sendMessage(Lang.get("command.debug.chunk-mark-date-unmarked"));
        }

        return true;
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public boolean hasPermissionToExecute(CommandSender sender) {
        return sender.hasPermission("naturerevive.navdebug");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
