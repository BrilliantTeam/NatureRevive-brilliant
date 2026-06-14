package engineer.skyouo.plugins.naturerevive.spigot.commands.regen;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.commands.SubCommand;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ToggleChunkRegenerationCommand implements SubCommand {
    public ToggleChunkRegenerationCommand() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (NatureRevivePlugin.enableRevive) {
            NatureRevivePlugin.suspendedZone.frozen();
        } else {
            NatureRevivePlugin.suspendedZone.resume();
        }

        sender.sendMessage(
                ChatColor.translateAlternateColorCodes(
                        '&', Lang.get(NatureRevivePlugin.enableRevive ? "messages.start-regeneration" : "messages.stop-regeneration")
                )
        );
        return true;
    }

    @Override
    public String getName() {
        return NatureRevivePlugin.enableRevive ? "pause" : "resume";
    }

    @Override
    public boolean hasPermissionToExecute(CommandSender sender) {
        return sender.hasPermission("naturerevive.togglerevive");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
