package engineer.skyouo.plugins.naturerevive.spigot.commands.regen;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.commands.SubCommand;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import engineer.skyouo.plugins.naturerevive.spigot.managers.ChunkRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ForceRegenAllCommand implements SubCommand {
    public ForceRegenAllCommand() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!NatureRevivePlugin.enableRevive) {
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                            '&', Lang.get("messages.force-regen-fail-due-to-regeneration-stop")
                    )
            );
            return true;
        }

        List<BukkitPositionInfo> positionInfos = NatureRevivePlugin.databaseConfig.values();
        for (BukkitPositionInfo positionInfo : positionInfos) {
            positionInfo.setTTL(0);
            ChunkRegeneration.enqueue(positionInfo);
        }

        return true;
    }

    @Override
    public String getName() {
        return "forceregenall";
    }

    @Override
    public boolean hasPermissionToExecute(CommandSender sender) {
        return sender.hasPermission("naturerevive.forceregenall");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
