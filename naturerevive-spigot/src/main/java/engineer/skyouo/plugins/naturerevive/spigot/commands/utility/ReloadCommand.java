package engineer.skyouo.plugins.naturerevive.spigot.commands.utility;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.commands.SubCommand;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationManager;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationUtil;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReloadCommand implements SubCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            NatureRevivePlugin.readonlyConfig.reloadConfig();
            NatureRevivePlugin.languageManager.load();
            NatureRevivePlugin.webhookConfig.load();
            NatureRevivePlugin.integrationManager = new IntegrationManager();
            NatureRevivePlugin.integrationManager.clearDependency();
            NatureRevivePlugin.checkSoftDependPlugins();
            IntegrationUtil.reloadCache();

            NatureRevivePlugin.taskManager.unregisterTasks();
            NatureRevivePlugin.taskManager.init();

            sender.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&', Lang.get("messages.reload-success")
            ));
        } catch (Exception e) {
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                    '&', Lang.get("messages.reload-failure")
                    )
            );

            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public boolean hasPermissionToExecute(CommandSender sender) {
        return sender.hasPermission("naturerevive.reloadreviveconfig");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}