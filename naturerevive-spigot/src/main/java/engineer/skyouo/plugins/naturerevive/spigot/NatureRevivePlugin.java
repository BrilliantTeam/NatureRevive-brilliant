package engineer.skyouo.plugins.naturerevive.spigot;

import engineer.skyouo.plugins.naturerevive.common.INMSWrapper;
import engineer.skyouo.plugins.naturerevive.common.VersionUtil;
import engineer.skyouo.plugins.naturerevive.common.structs.Queue;
import engineer.skyouo.plugins.naturerevive.spigot.api.IAPIMain;
import engineer.skyouo.plugins.naturerevive.spigot.api.IIntegrationManager;
import engineer.skyouo.plugins.naturerevive.spigot.commands.NatureReviveMainCommand;
import engineer.skyouo.plugins.naturerevive.spigot.commands.regen.ForceRegenAllCommand;
import engineer.skyouo.plugins.naturerevive.spigot.commands.regen.TestRandomizeOreCommand;
import engineer.skyouo.plugins.naturerevive.spigot.commands.regen.ToggleChunkRegenerationCommand;
import engineer.skyouo.plugins.naturerevive.spigot.commands.utility.DebugCommand;
import engineer.skyouo.plugins.naturerevive.spigot.commands.utility.MigrateCommand;
import engineer.skyouo.plugins.naturerevive.spigot.commands.regen.RegenThisChunkCommand;
import engineer.skyouo.plugins.naturerevive.spigot.commands.utility.ReloadCommand;
import engineer.skyouo.plugins.naturerevive.spigot.config.DatabaseConfig;
import engineer.skyouo.plugins.naturerevive.spigot.config.ReadonlyConfig;
import engineer.skyouo.plugins.naturerevive.spigot.constants.OreBlocksCompat;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationManager;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationUtil;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import engineer.skyouo.plugins.naturerevive.spigot.lang.LanguageManager;
import engineer.skyouo.plugins.naturerevive.spigot.listeners.ChunkRelatedEventListener;
import engineer.skyouo.plugins.naturerevive.spigot.listeners.ObfuscateLootListener;
import engineer.skyouo.plugins.naturerevive.spigot.stats.Metrics;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockDataChangeWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockStateWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.structs.SQLCommand;
import engineer.skyouo.plugins.naturerevive.spigot.tasks.TaskManager;
import engineer.skyouo.plugins.naturerevive.spigot.util.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class NatureRevivePlugin extends JavaPlugin implements IAPIMain {
    public static boolean enableRevive = true;

    static {
        ConfigurationSerialization.registerClass(BukkitPositionInfo.class, "PositionInfo");
    }

    public static NatureRevivePlugin instance;
    public static IntegrationManager integrationManager;
    public static TaskManager taskManager;
    public static INMSWrapper nmsWrapper;
    public static ReadonlyConfig readonlyConfig;
    public static LanguageManager languageManager;
    public static DatabaseConfig databaseConfig;

    public static SuspendedZone suspendedZone;

    public static Queue<BukkitPositionInfo> queue = null;
    public static final Set<String> regenInFlight = ConcurrentHashMap.newKeySet();
    public static Queue<Location> blockQueue = new Queue<>();
    public static final Queue<BlockStateWithPos> blockStateWithPosQueue = new Queue<>();
    public static final Queue<BlockDataChangeWithPos> blockDataChangeWithPos = new Queue<>();
    public static final Queue<SQLCommand> sqlCommandQueue = new Queue<>();

    @Override
    public void onEnable() {
        instance = this;

        try {
            readonlyConfig = new ReadonlyConfig();
        } catch (IOException e) {
            e.printStackTrace();
            NatureReviveComponentLogger.error("無法載入配置檔案！ / Failed to load the configuration file!");
        }

        languageManager = new LanguageManager(this);

        try {
            databaseConfig = readonlyConfig.determineDatabase();
        } catch (Exception ex) {
            NatureReviveComponentLogger.error(Lang.get("console.database-init-failed"));
            NatureReviveComponentLogger.warning(Lang.get("console.database-init-failed-mysql-hint"));

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        nmsWrapper = Util.getNMSWrapper();

        if (nmsWrapper == null) {
            NatureReviveComponentLogger.error(Lang.get("console.nms-load-failed"));
            NatureReviveComponentLogger.warning(Lang.get("console.nms-version-unsupported", getServer().getVersion()));

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        for (Material ore : nmsWrapper.getOreBlocks()) {
            OreBlocksCompat.addMaterial(ore);
        }

        queue = new Queue<>(new PriorityQueue<>((i, j) -> {
            long comp = i.getTTL() - j.getTTL();

            return comp > 0 ? 1 : comp < 0 ? -1 : 0;
        }));

        suspendedZone = new SuspendedZone();
        integrationManager = new IntegrationManager();

        if (!checkSoftDependPlugins()) {
            NatureReviveComponentLogger.error(Lang.get("console.soft-depend-missing"));
            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        IntegrationUtil.reloadCache();

        if (IntegrationUtil.getRegenEngine() == null) {
            NatureReviveComponentLogger.error(Lang.get("console.no-regen-engine"));

            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        if (!Util.isPaper()) {
            NatureReviveComponentLogger.error(Lang.get("console.not-paper-1"));
            NatureReviveComponentLogger.error(Lang.get("console.not-paper-2"));

            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        /*
        registerCommand("forceregenall", new ForceRegenAllCommand());
        registerCommand("regenthischunk", new RegenThisChunkCommand());
        registerCommand("testrandomizeore", new TestRandomizeOreCommand());
        registerCommand("reloadreviveconfig", new ReloadCommand());
        registerCommand("togglerevive", new ToggleChunkRegenerationCommand());
        registerCommand("navdebug", new DebugCommand());
        registerCommand("navmigrate", new MigrateCommand());
         */

        NatureReviveMainCommand commandHandler = new NatureReviveMainCommand();
        registerCommand("naturerevive", commandHandler);
        commandHandler.addSubCommand(new ForceRegenAllCommand());
        commandHandler.addSubCommand(new RegenThisChunkCommand());
        commandHandler.addSubCommand(new TestRandomizeOreCommand());
        commandHandler.addSubCommand(new ReloadCommand());
        commandHandler.addSubCommand(new ToggleChunkRegenerationCommand());
        commandHandler.addSubCommand(new DebugCommand());
        commandHandler.addSubCommand(new MigrateCommand());

        getServer().getPluginManager().registerEvents(new ChunkRelatedEventListener(), this);
        getServer().getPluginManager().registerEvents(new ObfuscateLootListener(), this);

        // todo: move this to another class

        taskManager = new TaskManager();
        taskManager.init();

        new Metrics(this, 16446)
                .addCustomChart(new Metrics.SimplePie("regeneration_engine", new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return readonlyConfig.regenerationEngine;
                    }
                }));
    }

    public static boolean checkSoftDependPlugins() {
        if (!readonlyConfig.regenerationEngine.equalsIgnoreCase("fawe") &&
                !readonlyConfig.regenerationEngine.equalsIgnoreCase("bukkit")) {
            NatureReviveComponentLogger.warning(Lang.get("console.invalid-engine"));
            return false;
        }

        if (VersionUtil.isAtLeast(1, 21, 0) &&
                readonlyConfig.regenerationEngine.equalsIgnoreCase("bukkit")) {
            NatureReviveComponentLogger.warning(Lang.get("console.bukkit-unsupported-1-21-1"));
            NatureReviveComponentLogger.warning(Lang.get("console.bukkit-unsupported-1-21-2"));
            readonlyConfig.regenerationEngine = "fawe";
            readonlyConfig.saveRegenerationEngine("fawe");

            if (instance.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
                NatureReviveComponentLogger.error(Lang.get("console.fawe-not-installed-1"));
                NatureReviveComponentLogger.error(Lang.get("console.fawe-not-installed-2"));
                return false;
            }
        }

        return integrationManager.init(instance);
    }

    @Override
    public void onDisable() {
        if (taskManager != null) {
            taskManager.unregisterTasks();
        }

        if (blockQueue != null)            blockQueue.clear();
        if (blockStateWithPosQueue != null) blockStateWithPosQueue.clear();
        if (blockDataChangeWithPos != null) blockDataChangeWithPos.clear();
        if (sqlCommandQueue != null)        sqlCommandQueue.clear();
        regenInFlight.clear();
    }

    private boolean registerCommand(String commandName, CommandExecutor executor) {
        try {
            PluginCommand command = getCommand(commandName);

            if (command == null)
                return false;

            command.setExecutor(executor);

            if (executor instanceof TabExecutor tabExecutor) {
                command.setTabCompleter(tabExecutor);
            }

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public @NotNull IIntegrationManager getIntegrationManager() {
        return integrationManager;
    }
}
