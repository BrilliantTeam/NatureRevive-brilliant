package engineer.skyouo.plugins.naturerevive.spigot;

import engineer.skyouo.plugins.naturerevive.common.INMSWrapper;
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
import java.util.concurrent.Callable;

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
    public static DatabaseConfig databaseConfig;

    public static SuspendedZone suspendedZone;

    public static Queue<BukkitPositionInfo> queue = null;
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
            NatureReviveComponentLogger.error("無法載入配置檔案！");
        }

        try {
            databaseConfig = readonlyConfig.determineDatabase();
        } catch (Exception ex) {
            NatureReviveComponentLogger.error("&c資料庫初始化失敗！");
            NatureReviveComponentLogger.warning("&c倘若您使用 MySQL，請確認好您以創建對應的 database。");

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        nmsWrapper = Util.getNMSWrapper();

        if (nmsWrapper == null) {
            NatureReviveComponentLogger.error("&c無法加載 NMS 兼容項目！");
            NatureReviveComponentLogger.warning("&c您的版本有可能不支援 NatureRevive：" + getServer().getVersion());

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
            NatureReviveComponentLogger.error("由於您於設置中開啟了部分功能，且 NatureRevive 無法載入對應的依賴插件，因此 NatureRevive 將會停止載入。");
            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        IntegrationUtil.reloadCache();

        if (IntegrationUtil.getRegenEngine() == null) {
            NatureReviveComponentLogger.error("找不到可用的重生引擎，請您確定是否正確設置 regenerate-engine 選項！");

            Bukkit.getPluginManager().disablePlugin(this);

            return;
        }

        if (!Util.isPaper()) {
            NatureReviveComponentLogger.error("您運行的軟體不包含 NatureRevive 運行所需的修補！");
            NatureReviveComponentLogger.error("建議您切換至 Paper，Paper 是 Spigot 的分支之一，包含眾多優化修補，也不須透過 BuildTools 來獲取軟體構建。");

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
            NatureReviveComponentLogger.warning("請將 regeneration-strategy 修正為 bukkit 或 fawe。");
            return false;
        }

        return integrationManager.init(instance);
    }

    @Override
    public void onDisable() {

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
