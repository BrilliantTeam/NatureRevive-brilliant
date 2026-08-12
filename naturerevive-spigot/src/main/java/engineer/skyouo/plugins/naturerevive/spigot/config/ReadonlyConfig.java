package engineer.skyouo.plugins.naturerevive.spigot.config;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.MySQLDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.SQLiteDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.YamlDatabaseAdapter;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ReadonlyConfig {

    private final File file = new File("plugins/NatureRevive/config.yml");

    private YamlFile configuration;

    public static final int CONFIG_VERSION = 20;

    public boolean debug;

    public boolean residenceStrictCheck;

    public boolean griefPreventionStrictCheck;

    public boolean griefDefenderStrictCheck;

    public boolean saferOreObfuscation;

    public boolean coreProtectLogging;

    public boolean adaptiveLootChestReplacement;

    public boolean enableOreObfuscation;

    public boolean inPlaceRegenerateEntities;

    public boolean inPlaceClearChunkPersistentData;

    public boolean inPlaceClearEntitiesBeforeRegeneration;

    public boolean inPlaceClearNearbyEntitiesBeforeRegeneration;

    public boolean inPlaceClearNearbyDroppedItemsBeforeRegeneration;

    public double minTPSCountForRegeneration;

    public long ttlDuration;

    public int taskPerProcess;

    public int queuePerNTick;

    public int blockPutPerTick;

    public int blockPutActionPerNTick;

    public int checkChunkTTLTick;

    public int dataSaveTime;

    public int suppressNearbyChunkCount;

    public int maxPlayersCountForRegeneration;

    public int blockProcessingTick;

    public int blockProcessingAmountPerProcessing;

    public int sqlProcessingCount;

    public int chunkRegenerateRadiusOnAverageApplied;

    public int maxElytraPerDay;

    public long regenOffsetDuration;

    public long elytraExceedLimitOffsetDuration;

    public String coreProtectUserName;

    public String language;

    public String regenerationStrategy;

    public String regenerationEngine;

    public List<String> ignoredWorld;

    public List<String> allowedWorld;

    public List<String> ignoredBiomes;

    // MySQL info

    public String databaseName;

    public String databaseTableName;

    public String databaseUsername;

    public String databaseIp;

    public int databasePort;

    public String databasePassword;

    public String jdbcConnectionString;

    public String spawnTimer;

    static final String[][] LEGACY_KEYS = {
            {"ttl-duration", "regeneration.regenerate-after"},
            {"spawn-timer", "regeneration.regenerate-at"},
            {"regen-offset-max-duration", "regeneration.offset-max-duration"},
            {"regeneration-strategy", "regeneration.strategy"},
            {"regeneration-engine", "regeneration.engine"},
            {"min-tps-for-regenerate-chunk", "regeneration.min-tps"},
            {"max-players-for-regenerate-chunk", "regeneration.max-players"},
            {"average-chunk-radius", "regeneration.check-chunk-radius"},
            {"suppress-chunk-refresh-radius", "regeneration.track-nearby-n-chunks"},

            {"task-process-per-tick", "performance.regenerate-n-chunks-per-time"},
            {"queue-process-per-n-tick", "performance.regenerate-chunks-per-n-ticks"},
            {"check-chunk-ttl-per-n-tick", "performance.check-expires-chunks-every-n-ticks"},
            {"data-save-time-tick", "performance.save-per-n-ticks"},
            {"block-put-per-tick", "performance.block-process-batch-size"},
            {"block-put-action-per-n-tick", "performance.block-process-per-n-ticks"},
            {"block-explosion-queue-process-per-n-tick", "performance.flag-blocks-per-n-ticks"},
            {"block-explosion-queue-process-per-time", "performance.flag-n-blocks-per-time"},
            {"block-queue-process-per-n-tick", "performance.flag-blocks-per-n-ticks"},
            {"block-queue-process-per-time", "performance.flag-n-blocks-per-time"},
            {"sql-processing-count", "performance.sql-process-batch-size"},

            {"blacklist-worlds", "world-list.blacklist"},
            {"whitelist-worlds", "world-list.whitelist"},
            {"blacklist-biomes", "biome-list.blacklist"},

            {"residence-strict-check", "lands.residence-strict"},
            {"griefprevention-strict-check", "lands.griefprevention-strict"},
            {"griefdefender-strict-check", "lands.griefdefender-strict"},

            {"enable-ore-obfuscation", "ore-obfuscation.enable"},
            {"safer-ore-obfuscation", "ore-obfuscation.safer"},

            {"adaptive-loot-chest-replacement", "loot-chest.adaptive-replacement"},

            {"max-elytra-per-day", "elytra.max-per-day"},
            {"elytra-exceed-limit-offset-duration", "elytra.exceed-limit-offset-duration"},

            {"inplace-regenerate-entities", "inplace.regenerate-entities"},
            {"inplace-clear-chunk-persistent-data", "inplace.clear-chunk-persistent-data"},
            {"inplace-clear-entities-before-regeneration", "inplace.clear-entities"},
            {"inplace-clear-nearby-entities-before-regeneration", "inplace.clear-nearby-entities"},
            {"inplace-clear-nearby-dropped-items-before-regeneration", "inplace.clear-nearby-dropped-items"},

            {"coreprotect-logging-enable", "coreprotect.logging"},
            {"coreprotect-log-username", "coreprotect.username"},

            {"storage.database-domain-or-ip", "storage.database-hostname"},
    };

    private static final String[] LEGACY_REMOVED_KEYS = {
            "messages",
            "sql-processing-tick",
    };

    public ReadonlyConfig() throws IOException {
        new File("plugins/NatureRevive").mkdirs();

        file.createNewFile();

        this.configuration = new YamlFile(file);

        this.configuration.createOrLoadWithComments();

        int version = configuration.getInt("config-version", CONFIG_VERSION);

        boolean changed = applyDefaults(configuration);

        if (version < CONFIG_VERSION) changed |= migrateLegacyKeys(configuration);

        if (changed || version != CONFIG_VERSION) {
            configuration.set("config-version", CONFIG_VERSION);
            configuration.save(file);
        }

        reloadConfig();
    }

    static boolean applyDefaults(YamlFile c) {
        boolean changed = false;

        changed |= def(c, "config-version", CONFIG_VERSION,
                "配置檔案版本，請不要更改此數值！",
                "Config version, DO NOT CHANGE IT MANUALLY AS IT MIGHT OVERWRITE ENTIRE CONFIGURATION.");

        changed |= def(c, "debug", false,
                "除錯模式，將輸出大量除錯訊息。",
                "Debug mode, displaying verbose messages for debugging purposes.");

        changed |= def(c, "language", "en_US",
                "插件語言，對應 plugins/NatureRevive/lang/ 資料夾內的檔案名稱（如 zh_TW, en_US）。",
                "所有可顯示的訊息皆定義於該語言檔內，您可自由編輯或新增語言檔。",
                "The plugin language, corresponding to the file name in the plugins/NatureRevive/lang/ folder (e.g. zh_TW, en_US).",
                "All displayable messages are defined in that language file; you may freely edit it or add new language files.");

        changed |= def(c, "regeneration.regenerate-after", "7d",
                "一個區塊在最後一次被變更後，經過多久會過期並排入重生佇列。",
                "可使用 d (天) / h (小時) / m (分) / s (秒)，例如 \"1d3h\" 表示 1 天 3 小時。",
                "How long a chunk stays alive after its last change before being queued for regeneration.",
                "Units: d (days) / h (hours) / m (minutes) / s (seconds), e.g. \"1d3h\".");

        changed |= def(c, "regeneration.regenerate-at", "00:00:00-23:59:59",
                "限定進行重生的時間區間，區塊僅會在此時段內重生（通常設定為半夜）。",
                "格式為 24 小時制，0 不可省略，支援 HH:mm-HH:mm 或 HH:mm:ss-HH:mm:ss，設為 -1 可停用限制。",
                "Limit chunk regeneration to this time window (usually midnight) to avoid performance spikes.",
                "Format: HH:mm-HH:mm or HH:mm:ss-HH:mm:ss. Set to -1 to disable the restriction.");

        changed |= def(c, "regeneration.offset-max-duration", "0d",
                "重生時隨機額外增加的時長上限，避免玩家掐準重生時間搜刮物資，格式同 regenerate-after。",
                "A random extra delay (up to this value) added to the regeneration time, so players cannot",
                "predict exactly when a chunk comes back. Same format as regenerate-after.");

        changed |= def(c, "regeneration.strategy", "passive",
                "重生策略，可選 aggressive（激進）、average（均衡）、passive（緩和）。",
                "aggressive：主動載入並重生過期區塊，能清光所有過期區塊，但較耗效能。",
                "average：定期檢查玩家周圍的區塊是否過期（範圍見 check-chunk-radius），適合人數平均的伺服器。",
                "passive：不主動載入區塊，等玩家自己走進去才重生，效能衝擊最小，但偏遠區塊可能長期不重生。",
                "Regeneration strategy, one of 'aggressive', 'average' and 'passive'.",
                "aggressive: actively loads and regenerates expired chunks; clears the backlog but costs the most performance.",
                "average: periodically checks the chunks around each player (radius: check-chunk-radius).",
                "passive: only regenerates a chunk when a player loads it; cheapest, but remote chunks may never be regenerated.");

        changed |= def(c, "regeneration.engine", "bukkit",
                "重生區塊所使用的引擎，可選 bukkit / fawe / inplace。",
                "bukkit：原版引擎，速度快，但跨版本地形可能出現斷層。",
                "fawe：呼叫 FastAsyncWorldEdit，速度較慢但可維持地形銜接，需安裝 FastAsyncWorldEdit。",
                "inplace：內建引擎，無需前置插件，重生時區塊不會卸載，玩家可留在區塊內，僅部分 Minecraft 版本支援。",
                "The engine used to regenerate chunks: 'bukkit', 'fawe' or 'inplace'.",
                "bukkit: vanilla method, fast, but may break terrain continuity across versions.",
                "fawe: uses FastAsyncWorldEdit, slower but keeps terrain seamless. Requires FastAsyncWorldEdit installed.",
                "inplace: built-in engine, needs no dependency and never unloads the chunk, so players may stay inside",
                "while it regenerates. Only available on some Minecraft versions.");

        changed |= def(c, "regeneration.min-tps", 16.0,
                "重生所需的最低 TPS，低於此數值時重生將暫停。",
                "The minimum TPS required to regenerate; regeneration is paused below this value.");

        changed |= def(c, "regeneration.max-players", 40,
                "重生所允許的最高線上人數，高於此數值時重生將暫停。",
                "The maximum online player count allowed to regenerate; regeneration is paused above this value.");

        changed |= def(c, "regeneration.check-chunk-radius", 2,
                "strategy 為 average 時，檢查玩家周圍幾格範圍的區塊。",
                "Radius of chunks checked around each player when strategy is 'average'.",
                "Formula: f(x) = (2x - 1) ^ 2 - 1 ((2x - 1) ^ 2 is the area, minus the chunk the player stands in.)");

        changed |= def(c, "regeneration.track-nearby-n-chunks", 0,
                "某區塊發生變更而重置其重生時間時，一併重置周圍 n 格區塊的時間。",
                "When a chunk is changed and its expiration is refreshed, also refresh the chunks within n chunks of it.");

        // ---- performance：批次大小與執行頻率，一般情況不需調整 ----

        changed |= def(c, "performance.regenerate-n-chunks-per-time", 1,
                "每次處理重生佇列時最多重生幾個區塊。",
                "How many chunk(s) to regenerate per queue process period.");

        changed |= def(c, "performance.regenerate-chunks-per-n-ticks", 200,
                "每 n 個 tick 處理一次重生佇列 (1 tick = 50ms)。",
                "Invoke the queue processing function every n tick(s).");

        changed |= def(c, "performance.check-expires-chunks-every-n-ticks", 200,
                "每 n 個 tick 檢查一次有哪些區塊已過期（過期 = 需要被重生）。",
                "Check for expired chunks every n tick(s).");

        changed |= def(c, "performance.save-per-n-ticks", 1200,
                "每 n 個 tick 將待重生的區塊資料寫入資料庫。",
                "Save pending chunk data to the database every n tick(s).");

        changed |= def(c, "performance.block-process-batch-size", 1024,
                "每次放置保留方塊（領地／建築物）時的批次數量，無特殊情況請保持預設值。",
                "How many blocks to put per batch for the residences/structures reserving action.",
                "Please leave it as it is unless your server has a lot of structures/residences in a single chunk.");

        changed |= def(c, "performance.block-process-per-n-ticks", 10,
                "每 n 個 tick 檢查一次是否有保留方塊等待放置；數值過大時，玩家可能看到終界折躍門等方塊短暫消失又出現。",
                "Check the pending reserved-block queue every n tick(s). If set too high, players may see blocks such as",
                "the end gateway briefly vanish and reappear.");

        changed |= def(c, "performance.flag-blocks-per-n-ticks", 10,
                "每 n 個 tick 處理一次「事件影響到哪些區塊」的計算 (1 tick = 50ms)。",
                "Process the chunk-flagging (which chunks were affected by events) function every n tick(s).");

        changed |= def(c, "performance.flag-n-blocks-per-time", 200,
                "每次計算最多處理幾個被事件影響的方塊。",
                "How many block(s) to process per chunk-flagging period.");

        changed |= def(c, "performance.sql-process-batch-size", 500,
                "每次可以執行多少個 SQL 指令。",
                "How many queries to execute per execution period.");

        // ---- 世界與生態域名單 ----

        changed |= def(c, "world-list.blacklist", Arrays.asList("世界 1", "World 2"),
                "此列表內的世界不會被重生。",
                "Worlds in this list are skipped by the regeneration system.");

        changed |= def(c, "world-list.whitelist", List.of(),
                "若此列表非空，NatureRevive 將只重生列表內的世界；同時出現在 blacklist 的世界仍然會被忽略。",
                "If this list is not empty, only the worlds listed here are regenerated.",
                "A world present in both lists is still skipped.");

        changed |= def(c, "biome-list.blacklist", Arrays.asList("naturerevive"),
                "此列表內的生態域不會被重生。",
                "Biomes in this list are skipped by the regeneration system.");

        changed |= def(c, "lands.residence-strict", false,
                "重生含有 Residence 領地的區塊，但保留領地範圍內的方塊。",
                "演示影片: https://www.youtube.com/watch?v=OOm7FVhG7fk&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=5",
                "Regenerate chunks containing Residence claims, but keep the blocks inside the claims.",
                "Demo: https://www.youtube.com/watch?v=OOm7FVhG7fk&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=5");

        changed |= def(c, "lands.griefprevention-strict", false,
                "重生含有 GriefPrevention 領地的區塊，但保留領地範圍內的方塊。",
                "演示影片: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=7",
                "Regenerate chunks containing GriefPrevention claims, but keep the blocks inside the claims.",
                "Demo: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=7");

        changed |= def(c, "lands.griefdefender-strict", false,
                "重生含有 GriefDefender 領地的區塊，但保留領地範圍內的方塊。",
                "演示影片: https://www.youtube.com/watch?v=euKrueUrD_4&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=9",
                "Regenerate chunks containing GriefDefender claims, but keep the blocks inside the claims.",
                "Demo: https://www.youtube.com/watch?v=euKrueUrD_4&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=9");

        changed |= def(c, "ore-obfuscation.enable", false,
                "開啟礦物混淆，重生後會調換礦物位置，避免玩家記下座標後重複挖同一批礦。",
                "Shuffle ore positions after a chunk is regenerated, so players cannot farm ores by",
                "remembering their coordinates (or by knowing the world seed).");

        changed |= def(c, "ore-obfuscation.safer", true,
                "限制混淆僅在主世界 y < 40 進行，且被替換的方塊僅限石頭／深板岩（主世界）或地獄石（地獄）。",
                "當混淆後的礦物出現在奇怪的位置（懸空、水面上）時再開啟；開啟後 y > 40 的區域不會生成任何礦物。",
                "Restrict obfuscation to y < 40 in the overworld and only replace stone/deepslate (overworld)",
                "or netherrack (nether). Enable this only if obfuscated ores appear in odd places;",
                "while enabled, no ores are generated above y = 40.");

        changed |= def(c, "loot-chest.adaptive-replacement", false,
                "偵測到戰利品箱時僅更新其種子碼，而不進行物品填充。",
                "當您遇到每次打開寶藏箱都會重新生成物品的情況時，請開啟此設定。",
                "When a loot chest is detected, only refresh its loot seed instead of filling the items.",
                "Turn this on if loot chests get refilled unexpectedly.");

        changed |= def(c, "elytra.max-per-day", 10,
                "每日可重生的鞘翅數量上限，於本地時間 00:00 重置。",
                "Maximum number of elytras regenerated per day, reset at 00:00 local time.");

        changed |= def(c, "elytra.exceed-limit-offset-duration", "1d",
                "當鞘翅重生數量超過上限時，額外延後的時間，格式同 regeneration.regenerate-after。",
                "Extra delay applied to elytra regeneration once the daily limit is reached.",
                "Same format as regeneration.regenerate-after.");

        changed |= def(c, "inplace.regenerate-entities", true,
                "是否再生實體（如終界水晶、戰利品礦車、展示框等）。",
                "Whether to regenerate entities, such as end crystals, loot minecarts and item frames.");

        changed |= def(c, "inplace.clear-chunk-persistent-data", true,
                "是否清除該區塊的 PDC（適用於如礦物代換）。",
                "Whether to clear this chunk's persistent data container.");

        changed |= def(c, "inplace.clear-entities", false,
                "是否先移除該區塊中的所有實體與掉落物，然後再生實體。",
                "Whether to remove all entities and dropped items in this chunk before regenerating entities.");

        changed |= def(c, "inplace.clear-nearby-entities", false,
                "是否再生時也清理附近 3×3 範圍內的一般實體。",
                "Whether to also clear non-item entities in the surrounding 3x3 chunk area during regeneration.");

        changed |= def(c, "inplace.clear-nearby-dropped-items", false,
                "是否再生時也清理附近 3×3 範圍內的掉落物。",
                "Whether to also clear dropped items in the surrounding 3x3 chunk area during regeneration.");

        changed |= def(c, "coreprotect.logging", false,
                "是否啟用 CoreProtect 的紀錄功能。",
                "Whether to enable the CoreProtect logging integration.");

        changed |= def(c, "coreprotect.username", "#資源再生",
                "在 CoreProtect 紀錄中，此插件所造成的改動要顯示成哪個名稱。",
                "演示圖片: https://media.discordapp.net/attachments/934304177134370847/1018496146441764954/AddText_09-11-08.12.27.png",
                "The username shown in CoreProtect logs for changes made by this plugin.");

        changed |= def(c, "storage.method", "yaml",
                "儲存待重生區塊的方式，可選 yaml（本地）、sqlite（本地）、mysql（遠端，需配置下方 MySQL 連線資訊）。",
                "Where to store the pending chunks: 'yaml' (local), 'sqlite' (local) or 'mysql' (remote).",
                "The options below only apply to 'mysql'.");

        changed |= def(c, "storage.database-name", "naturerevive",
                "MySQL 資料庫名稱。",
                "The database name used for creating tables and storing data in MySQL.");

        changed |= def(c, "storage.table-name", "locations",
                "MySQL 資料表名稱。",
                "The table name used for storing data in the MySQL server.");

        changed |= def(c, "storage.database-hostname", "127.0.0.1",
                "MySQL 的 IP 或網域。",
                "The IP or domain used for connecting to the MySQL server.");

        changed |= def(c, "storage.database-port", 3306,
                "MySQL 的連接埠。",
                "The port used for connecting to the MySQL server.");

        changed |= def(c, "storage.database-username", "root",
                "MySQL 的使用者名稱。",
                "The username used for connecting to the MySQL server.");

        changed |= def(c, "storage.database-password", "20480727",
                "MySQL 的密碼。",
                "The password used for connecting to the MySQL server.");

        changed |= def(c, "storage.jdbc-connection-string", "jdbc:mysql://{database_ip}:{database_port}/{database_name}",
                "連接 MySQL 所使用的 JDBC 字串，{database_ip}、{database_port}、{database_name} 為佔位符，",
                "只要上方欄位填寫正確，執行時會自動帶入。",
                "The JDBC connection string used for connecting to the MySQL server.",
                "{database_ip}, {database_port} and {database_name} are placeholders filled in at runtime",
                "from the options above.");

        return changed;
    }

    static boolean migrateLegacyKeys(YamlFile c) {
        boolean changed = false;

        for (String[] entry : LEGACY_KEYS) {
            Object value = c.get(entry[0]);
            if (value == null) continue;

            c.set(entry[1], value);
            c.set(entry[0], null);
            changed = true;
        }

        for (String key : LEGACY_REMOVED_KEYS) {
            if (c.get(key) == null) continue;

            c.set(key, null);
            changed = true;
        }

        return changed;
    }

    private static boolean def(YamlFile c, String path, Object value, String... comment) {
        boolean absent = c.get(path) == null;

        if (absent) c.set(path, value);

        c.setComment(path, String.join(System.lineSeparator(), comment));

        return absent;
    }

    public void reloadConfig() throws IOException {
        this.configuration = new YamlFile(file);
        this.configuration.createOrLoadWithComments();

        debug = configuration.getBoolean("debug", false);
        language = configuration.getString("language", "en_US");

        ttlDuration = parseDuration(configuration.getString("regeneration.regenerate-after", "7d"));
        spawnTimer = configuration.getString("regeneration.regenerate-at", "00:00:00-23:59:59");
        regenOffsetDuration = parseDuration(configuration.getString("regeneration.offset-max-duration", "0d"));
        regenerationStrategy = configuration.getString("regeneration.strategy", "passive");
        regenerationEngine = configuration.getString("regeneration.engine", "bukkit");
        minTPSCountForRegeneration = configuration.getDouble("regeneration.min-tps", 16.0);
        maxPlayersCountForRegeneration = configuration.getInt("regeneration.max-players", 40);
        chunkRegenerateRadiusOnAverageApplied = configuration.getInt("regeneration.check-chunk-radius", 2);
        suppressNearbyChunkCount = configuration.getInt("regeneration.track-nearby-n-chunks", 0);

        taskPerProcess = configuration.getInt("performance.regenerate-n-chunks-per-time", 1);
        queuePerNTick = configuration.getInt("performance.regenerate-chunks-per-n-ticks", 200);
        checkChunkTTLTick = configuration.getInt("performance.check-expires-chunks-every-n-ticks", 200);
        dataSaveTime = configuration.getInt("performance.save-per-n-ticks", 1200);
        blockPutPerTick = configuration.getInt("performance.block-process-batch-size", 1024);
        blockPutActionPerNTick = configuration.getInt("performance.block-process-per-n-ticks", 10);
        blockProcessingTick = configuration.getInt("performance.flag-blocks-per-n-ticks", 10);
        blockProcessingAmountPerProcessing = configuration.getInt("performance.flag-n-blocks-per-time", 200);
        sqlProcessingCount = configuration.getInt("performance.sql-process-batch-size", 500);

        ignoredWorld = configuration.getStringList("world-list.blacklist");
        allowedWorld = configuration.getStringList("world-list.whitelist");
        ignoredBiomes = configuration.getStringList("biome-list.blacklist");

        residenceStrictCheck = configuration.getBoolean("lands.residence-strict", false);
        griefPreventionStrictCheck = configuration.getBoolean("lands.griefprevention-strict", false);
        griefDefenderStrictCheck = configuration.getBoolean("lands.griefdefender-strict", false);

        enableOreObfuscation = configuration.getBoolean("ore-obfuscation.enable", false);
        saferOreObfuscation = configuration.getBoolean("ore-obfuscation.safer", true);

        adaptiveLootChestReplacement = configuration.getBoolean("loot-chest.adaptive-replacement", false);

        maxElytraPerDay = configuration.getInt("elytra.max-per-day", 10);
        elytraExceedLimitOffsetDuration = parseDuration(configuration.getString("elytra.exceed-limit-offset-duration", "1d"));

        inPlaceRegenerateEntities = configuration.getBoolean("inplace.regenerate-entities", true);
        inPlaceClearChunkPersistentData = configuration.getBoolean("inplace.clear-chunk-persistent-data", true);
        inPlaceClearEntitiesBeforeRegeneration = configuration.getBoolean("inplace.clear-entities", false);
        inPlaceClearNearbyEntitiesBeforeRegeneration = configuration.getBoolean("inplace.clear-nearby-entities", false);
        inPlaceClearNearbyDroppedItemsBeforeRegeneration = configuration.getBoolean("inplace.clear-nearby-dropped-items", false);

        coreProtectLogging = configuration.getBoolean("coreprotect.logging", false);
        coreProtectUserName = configuration.getString("coreprotect.username", "#資源再生");

        databaseName = configuration.getString("storage.database-name", "naturerevive");
        databaseTableName = configuration.getString("storage.table-name", "locations");
        databaseIp = configuration.getString("storage.database-hostname", "127.0.0.1");
        databasePort = configuration.getInt("storage.database-port", 3306);
        databaseUsername = configuration.getString("storage.database-username", "root");
        databasePassword = configuration.getString("storage.database-password", "20480727");
        jdbcConnectionString = configuration.getString("storage.jdbc-connection-string", "jdbc:mysql://{database_ip}:{database_port}/{database_name}");

        if (NatureRevivePlugin.databaseConfig != null) {
            try {
                NatureRevivePlugin.databaseConfig.save();
                NatureRevivePlugin.databaseConfig.close();

                NatureRevivePlugin.databaseConfig = determineDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveRegenerationEngine(String engine) {
        configuration.set("regeneration.engine", engine);
        try {
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long parseDuration(String duration) {
        String target =
                Pattern.compile("\\d+d\\s").matcher(duration).find() ?
                        ("P" + duration.substring(0, duration.indexOf(" ")) + "T" + duration.substring(duration.indexOf(" "))) :
                        Pattern.compile("\\d+d").matcher(duration).find() ?
                                ("P" + duration) :
                                ("PT" + duration);

        target = target.replace(" ", "").toUpperCase(Locale.ROOT);

        return Duration.parse(target).toMillis();
    }

    public DatabaseConfig determineDatabase() {
        String databaseType = configuration.getString("storage.method", "yaml");

        switch (databaseType.toLowerCase()) {
            case "sqlite":
                return new SQLiteDatabaseAdapter();
            case "mysql":
                return new MySQLDatabaseAdapter();
            case "yaml":
            default:
                return new YamlDatabaseAdapter();
        }
    }

    public boolean isCurrentTimeAllowForRSC() {
        if ("-1".equals(spawnTimer)) return true;
        String[] timeParts = spawnTimer.split("-");
        LocalTime timeRangeStart = LocalTime.parse(timeParts[0]);
        LocalTime timeRangeEnd = timeParts[1].length() == 5
                ? LocalTime.parse(timeParts[1]).withSecond(59)
                : LocalTime.parse(timeParts[1]);
        LocalTime currentTime = LocalTime.now();
        return !currentTime.isBefore(timeRangeStart) && !currentTime.isAfter(timeRangeEnd);
    }
}
