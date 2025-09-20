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

    public final int CONFIG_VERSION = 18;

    public boolean debug;

    public boolean residenceStrictCheck;

    public boolean griefPreventionStrictCheck;

    public boolean griefDefenderStrictCheck;

    public boolean saferOreObfuscation;

    public boolean coreProtectLogging;

    public boolean adaptiveLootChestReplacement;

    public boolean enableOreObfuscation;

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

    public String reloadSuccessMessage;

    public String reloadFailureMessage;

    public String stopChunkRegenerationMessage;

    public String startChunkRegenerationMessage;

    public String forceRegenFailedDueRegenStopMessage;

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

    public ReadonlyConfig() throws IOException {
        new File("plugins/NatureRevive").mkdirs();

        file.createNewFile();

        this.configuration = new YamlFile(file);

        this.configuration.createOrLoadWithComments();

        if (configuration.get("config-version") == null) {
            configuration.set("debug", false);
            configuration.setComment("debug", convertListStringToString(Arrays.asList("除錯模式",
                    "Debug mode, displaying verbose messages for debugging purposes.",
                    "You could disable this if you're not in development environments.")));

            configuration.set("ttl-duration", "7d");
            configuration.setComment("ttl-duration", convertListStringToString(Arrays.asList(
                    "一個區塊會在多久之後過期，可選 d , h , m , s 作為時間單位",
                    "The time-to-live duration of chunks need to be regenerated.",
                    "You can use d (as N Days) / h (as N hours) / m (as N minutes) / s (as N seconds), for instance \"1d3h\" means set",
                    "the TTL of out-dated chunks to 1 day 3 minutes, after expired, the chunk will be regenerated in next queue process period.")));

            configuration.set("task-process-per-tick", 1);
            configuration.setComment("task-process-per-tick", convertListStringToString(Arrays.asList("每次再生可以再生幾個區塊",
                    "How many chunk(s) to regenerate per queue process period.")
            ));

            configuration.set("queue-process-per-n-tick", 200);
            configuration.setComment("queue-process-per-n-tick",
                    convertListStringToString(Arrays.asList("每 n 個 tick 處理一次區塊再生 (1 tick = 50ms)",
                            "Invoking the queue processing function every n tick(s).")
                    ));

            configuration.set("check-chunk-ttl-per-n-tick", 200);
            configuration.setComment("check-chunk-ttl-per-n-tick",
                    convertListStringToString(Arrays.asList("每 n 個 tick 檢查一次過期的區塊 (過期的區塊 = 需要被再生的區塊)",
                            "Checking the expired chunks every n tick(s)")
                    ));

            configuration.set("data-save-time-tick", 3600);
            configuration.setComment("data-save-time-tick",
                    convertListStringToString(Arrays.asList("每 n 個 tick 將過期的區塊儲存至本地資料庫 (過期的區塊 = 需要被再生的區塊)",
                            "Saving the chunks not over TTL to the local file every n tick(s).")
                    ));

            configuration.set("residence-strict-check", false);
            configuration.setComment("residence-strict-check", convertListStringToString(Arrays.asList("是否啟用 再生含有領地的區塊，但是不再生領地範圍內的方塊 功能",
                    "演示影片: https://www.youtube.com/watch?v=OOm7FVhG7fk&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=5",
                    "Whether to enable the experimental function that if the expired chunk has residences in it, put all blocks in residences to new chunk instead of skipping chunk.",
                    "Demo: https://www.youtube.com/watch?v=OOm7FVhG7fk&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=5")));

            configuration.set("griefprevention-strict-check", false);
            configuration.setComment("griefprevention-strict-check", convertListStringToString(Arrays.asList("是否啟用 再生含有GP領地的區塊，但是不再生GP領地範圍內的方塊 功能",
                    "演示影片: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=7",
                    "Whether to enable the experimental function that if the expired chunk has GriefPrevention in it, put all blocks in GriefPrevention's claims to new chunk instead of skipping chunk.",
                    "Demo: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=7")));

            configuration.set("griefdefender-strict-check", false);
            configuration.setComment("griefdefender-strict-check", convertListStringToString(Arrays.asList("是否啟用 再生含有GD領地的區塊，但是不再生GD領地範圍內的方塊 功能",
                    "演示影片: https://www.youtube.com/watch?v=euKrueUrD_4&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=9",
                    "Whether to enable the experimental function that if the expired chunk has GriefDefender in it, put all blocks in GriefDefender's claims to new chunk instead of skipping chunk.",
                    "Demo: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=9")));

            configuration.set("coreprotect-logging-enable", true);
            configuration.setComment("coreprotect-logging-enable", convertListStringToString(Arrays.asList("是否啟用 CoreProtect 的紀錄功能.",
                    "Whether or not to enable the CoreProtect logging integration.")));

            configuration.set("coreprotect-log-username", "#資源再生");
            configuration.setComment("coreprotect-log-username", convertListStringToString(Arrays.asList("在 CoreProtect 紀錄中，有關此插件相關改動的顯示名稱",
                    "演示圖片: https://media.discordapp.net/attachments/934304177134370847/1018496146441764954/AddText_09-11-08.12.27.png",
                    "The username to use in CoreProtect logging.",
                    "Demo: https://media.discordapp.net/attachments/934304177134370847/1018496146441764954/AddText_09-11-08.12.27.png")));

            configuration.set("messages.reload-success-message", "&a成功重載插件配置檔!");
            configuration.setComment("messages.reload-success-message", convertListStringToString(Arrays.asList("當插件配置檔重載成功時，向指令執行者傳送的訊息",
                    "The message to be sent on plugin's configuration is reloaded successfully.")));

            configuration.set("messages.reload-failure-message", "&c插件配置檔重載失敗, 請查看後台以獲取詳細記錄.");
            configuration.setComment("messages.reload-failure-message", convertListStringToString(Arrays.asList("當插件配置檔重載失敗時，向指令執行者傳送的訊息.",
                    "The message to be sent on plugin's configuration is failed to reload.")));

            configuration.set("messages.stop-regeneration", "&e關閉區塊重生系統成功, 倘若想要再次開啟, 請執行 /naturerevive resume");
            configuration.setComment("messages.stop-regeneration", convertListStringToString(Arrays.asList("當區塊重生系統被關閉時，向指令執行者傳送的訊息.",
                    "The message to be sent on plugin's chunk regeneration system was turned off.")));

            configuration.set("messages.start-regeneration", "&a開啟區塊重生系統成功, 倘若想要再次關閉, 請執行 /naturerevive pause");
            configuration.setComment("messages.start-regeneration", convertListStringToString(Arrays.asList("當區塊重生系統被重新開啟時，向指令執行者傳送的訊息.",
                    "The message to be sent on plugin's chunk regeneration system was turned on again.")));

            configuration.set("messages.force-regen-fail-due-to-regeneration-stop", "&c無法在區塊重生系統關閉時強制重生區塊!");
            configuration.setComment("messages.force-regen-fail-due-to-regeneration-stop", convertListStringToString(Arrays.asList("當區塊重生系統被關閉時, 執行強制重生系統會回傳的訊息.",
                    "The message to be sent on forcing regenerate command was invoked but the regeneration system was paused.")));

            configuration.set("config-version", CONFIG_VERSION);
            configuration.setComment("config-version", convertListStringToString(Arrays.asList("配置檔案版本，請不要更改此數值！", "Config version, DO NOT CHANGE IT MANUALLY AS IT MIGHT OVERWRITE ENTIRE CONFIGURATION.")));

            configuration.set("enable-ore-obfuscation", false);
            configuration.setComment("enable-ore-obfuscation", convertListStringToString(Arrays.asList(
                    "此選項將開啟礦物替換，將礦物的所有位置進行調換，避免玩家得知種子碼後，透過記錄礦物位置訊息以重複獲取礦物之行為。",
                    "This option enable the ore 'obfuscation' system, once a chunk was regenerated, we will shuffle the ore's location to prevent players from logging ore's location."
            )));

            configuration.set("safer-ore-obfuscation", true);
            configuration.setComment("safer-ore-obfuscation", convertListStringToString(Arrays.asList(
                    "該選項將限制礦物混淆系統強制於高度 40 以下替換礦物 (僅主世界)，並且將會嚴格限制替換的方塊種類為 石頭 / 深板岩 (主世界) 或 地獄石 (地獄)",
                    "當礦物混淆產生的礦物錯位時，再考慮開啟此選項，開啟此選項後 y > 40 的區域將不會生成任何礦物",
                    "This option will force ore obfuscation system to replace ore block to under y = 40 in overworld, and will limit the target block to stone / deepslate (overworld) or netherrack (nether) to prevent odd behavior.",
                    "Please only consider to enable it when the obfuscated ores are glitched (like spawning ores at ground, spawning ores above water etc.), when this feature is on, the region y > 40 will NOT generate any ores.")
            ));

            configuration.set("adaptive-loot-chest-replacement", false);
            configuration.setComment("adaptive-loot-chest-replacement", convertListStringToString(Arrays.asList(
                    "改變生成寶藏箱時的策略, 當偵測到寶藏箱時, 僅更新其種籽碼, 而不進行物品填充.",
                    "當您遇到每次打開寶藏箱會再生物品的情況時, 請打開該設置.",
                    "This option will prevent plugin from filling the loot chesting when detected the loot chest",
                    "It should be turned on when the loot chest is filled unexpectedly.")
            ));

            configuration.set("max-elytra-per-day", 10);
            configuration.setComment("max-elytra-per-day", convertListStringToString(Arrays.asList(
                    "限制每日重生鞘翅的最大數量，將於本地時間 00:00 重置。",
                    "This option will set a limit for max elytra regenerated, the limit will be reset on 00:00 A.M. local time.")
            ));

            configuration.set("regen-offset-max-duration", "0d");
            configuration.setComment("regen-offset-max-duration", convertListStringToString(Arrays.asList(
                    "重生時，隨機額外添加重生時長，避免玩家於定期於重生時間搜刮物資，格式同 ttl-duration。",
                    "This option will add an offset to regen time to prevent player from logging regen time to get elytra or lootable chest,",
                    "the format is same as the ttl-duration options")
            ));

            configuration.set("elytra-exceed-limit-offset-duration", "1d");
            configuration.setComment("elytra-exceed-limit-offset-duration", convertListStringToString(Arrays.asList(
                    "當鞘翅重生數量超過設定值時，偏移所設置的時間值。",
                    "This option will set a time offset on elytra regen task, when the elytra regen reach the limit.")
            ));

            configuration.set("suppress-chunk-refresh-radius", 0);
            configuration.setComment("suppress-chunk-refresh-radius", convertListStringToString(Arrays.asList(
                    "每當某區塊發生變更進而重置區塊重生時間時，一同重置周圍 n 個區塊的時間。",
                    "How many nearby chunks' expiration times should be updates once any chunk is active.")
            ));

            configuration.set("block-put-per-tick", 1024);
            configuration.setComment("block-put-per-tick", convertListStringToString(Arrays.asList(
                    "每次區域放置的保留方塊數量，倘若無特殊情況，請保持在默認值",
                    "How many blocks to put for residences/structures reserved action.",
                    "Please leave it as it is if your server does not have a bunch of structures/residences in a chunk."
            )));

            configuration.set("block-put-action-per-n-tick", 3);
            configuration.setComment("block-put-action-per-n-tick", convertListStringToString(Arrays.asList(
                            "每 n 個 tick 檢查是否有需要保留的方塊等待放置, 倘若該數值被設置的過久的話玩家將可能見到終界折躍門方塊突然消失, 又再次出現.",
                            "Check whether the queue has blocks need to put every n tick(s), if the value is set too high, player in the center of the end might see the end gateway suddenly vanished then appeared."
                    )
            ));

            configuration.set("min-tps-for-regenerate-chunk", 16.0);
            configuration.setComment("min-tps-for-regenerate-chunk", convertListStringToString(Arrays.asList(
                    "重生區塊的最低 TPS 數值, 倘若低於該數值, 區塊重生將會被擱置.",
                    "The minimum TPS for regenerating expired chunks, if server's tps is lower than this value, the regeneration task will be stopped."
            )));

            configuration.set("max-players-for-regenerate-chunk", 40);
            configuration.setComment("max-players-for-regenerate-chunk", convertListStringToString(Arrays.asList(
                    "重生區塊的最高玩家上限, 倘若玩家數高於該數值, 區塊重生將會被擱置.",
                    "The maximum players count for regenerating expired chunks, if server's players count is greater than this value, the regeneration task will be stopped."
            )));
            configuration.set("spawn-timer", "00:00-23:59");
            configuration.setComment("spawn-timer", convertListStringToString(Arrays.asList(
                    "限定進行重生區塊的時間, 區塊重生僅會在指定時間內進行生成(通常設定為半夜).",
                    "格式為24小時制，0不可省略 (xx:xx-xx:xx), 預設為凌晨0:00至上午7:00",
                    "Limit the time of chunk regenerate (mostly at midnight) to prevent player",
                    "experiencing from major performance degraded."
            )));

            configuration.set("blacklist-worlds", Arrays.asList("世界 1", "World 2"));
            configuration.setComment("blacklist-worlds", convertListStringToString(Arrays.asList(
                    "該列表內的世界將會被重生系統忽略, 並將不會再生.",
                    "The list of ignored world that will be skipped by regeneration system."
            )));

            configuration.set("whitelist-worlds", List.of());
            configuration.setComment("whitelist-worlds", convertListStringToString(Arrays.asList(
                    "倘若該列表內有填寫任何世界名稱，NatureRevive 將只會重生該名單內的世界，",
                    "倘若列表內的世界同樣在 blacklist-worlds 內，那麼將會被忽略。",
                    "The list of allowed world that will be skipped by regeneration system,",
                    "if the list is empty, all worlds will be allow to perform regen task",
                    "if any world in list is also in blacklist-worlds, the world will not perform task."
            )));

            configuration.set("blacklist-biomes", Arrays.asList("naturerevive"));
            configuration.setComment("blacklist-biomes", convertListStringToString(Arrays.asList(
                    "該列表內的生態域將會被重生系統忽略, 並將不會再生.",
                    "The list of ignored biomes that will be skipped by regeneration system."
            )));

            configuration.set("regeneration-strategy", "passive");
            configuration.setComment("regeneration-strategy", convertListStringToString(Arrays.asList(
                    "控制區塊的生成策略以及激進程度, 可選 aggressive (激進), passive (緩和), average (均衡)",
                    "當選擇 aggressive 時, 插件將會主動載入重生過期的區塊, 該方法可以有效清空所有過期, 但較為消耗資源.",
                    "當選擇 average 時, 插件會定期檢查玩家周圍八格 (可以透過 average-chunk-radius 調整) 的區塊是否過期, 該方法對人數均衡的伺服器較為友善.",
                    "當選擇 passive 時, 插件將不會主動加載過期的區塊, 並將等到玩家主動加載區塊時才會進行重生,",
                    "該方法可以避免區塊重生所造成的 TPS 跌幅, 但會有部分未被玩家探索的區塊長時間未重生.",
                    "The option to determine the plugin's regeneration management strategy, valid options are 'aggressive', 'passive' and 'average'",
                    "When aggressive is chosen, the plugin will load and regenerate expired chunks periodically, this method can regenerate all chunks that is expired, but the performance cost will much higher.",
                    "When average is chosen, the plugin will check all players' neighboring chunks whether or not is expired, if it is, the neighboring chunks will be queued to be regenerated.",
                    "When passive is chosen, the plugin will only regenrate chunk on player visited, this method will reduce performance cost but not all the expired chunks will be regenerated."
            )));

            configuration.set("regeneration-engine", "bukkit");
            configuration.setComment("regeneration-engine", convertListStringToString(Arrays.asList(
                    "變更生成區塊時所指定的引擎，如 bukkit 為原版引擎，生成速度較快，但會導致版本不兼容出現的區塊斷層，",
                    "fawe 則將調用 FastAsyncWorldEdit 的方式，生成速度較慢，但可保持區塊間地形兼容。",
                    "使用 FAWE 方法須安裝 FastAsyncWorldEdit。",
                    "Change the method of regeneration, e.g. bukkit using original method, which is rapid but cause",
                    "major chunk problem (e.g. terrain break), fawe use FastAsyncWorldEdit implementation,",
                    "which have slower speed but guarantee terrain, you have to install FastAsyncWorldEdit in order to",
                    "use fawe method."
            )));

            configuration.set("average-chunk-radius", 2);
            configuration.setComment("average-chunk-radius",
                    convertListStringToString(Arrays.asList("調整 regeneration-strategy 為 average 時, 插件重生區塊的範圍。",
                            "Config the radius of regenerating chunk when regeneration-strategy is set to 'average'.",
                            "Formula: f(x) = (2x - 1) ^ 2 - 1",
                            "(2x - 1) ^ 2 is the radius, and we reduce 1 to except the chunk where player located.")
                    ));

            configuration.set("block-queue-process-per-n-tick", 10);
            configuration.setComment("block-queue-process-per-n-tick",
                    convertListStringToString(Arrays.asList("每 n 個 tick 處理一次事件影響之區塊計算 (1 tick = 50ms)",
                            "Proceeding the chunks flagging function every n tick(s).")
                    ));

            configuration.set("block-queue-process-per-time", 200);
            configuration.setComment("block-queue-process-per-time", convertListStringToString(Arrays.asList("每次可以處理幾個被事件影響的方塊.",
                    "How many block(s) to calculate per chunk flagging process period.")
            ));

            configuration.set("sql-processing-count", 50);
            configuration.setComment("sql-processing-count", convertListStringToString(Arrays.asList("每次執行可以執行多少個 SQL 指令.",
                    "How many queries to execute per execution period.")
            ));

            configuration.set("storage.method", "yaml");
            configuration.setComment("storage.method", convertListStringToString(Arrays.asList(
                    "選擇儲存待更新區塊的資料庫類型, 可選擇 yaml (本地), sqlite (本地), mysql (遠端, 需配置 MySQL 伺服器)",
                    "Choosing the database type of storing chunks not reaching ttl, the available option is 'yaml' and 'sqlite."
            )));

            configuration.set("storage.database-name", "naturerevive");
            configuration.setComment("storage.database-name", convertListStringToString(Arrays.asList(
                    "應在 MySQL 使用的資料庫名稱.",
                    "The database name used for creating tables and storing data in MySQL."
            )));

            configuration.set("storage.table-name", "locations");
            configuration.setComment("storage.table-name", convertListStringToString(Arrays.asList(
                    "連接至 MySQL 所用的資料表名稱.",
                    "The table's name used for storing data in MySQL server."
            )));

            configuration.set("storage.database-domain-or-ip", "127.0.0.1");
            configuration.setComment("storage.database-domain-or-ip", convertListStringToString(Arrays.asList(
                    "連接至 MySQL 所用的 IP 或域名.",
                    "The IP or domain used for connecting to MySQL server."
            )));

            configuration.set("storage.database-port", 3306);
            configuration.setComment("storage.database-port", convertListStringToString(Arrays.asList(
                    "連接至 MySQL 所用的端口名稱.",
                    "The port used for connecting to MySQL server."
            )));

            configuration.set("storage.database-username", "root");
            configuration.setComment("storage.database-name", convertListStringToString(Arrays.asList(
                    "應在 MySQL 使用的資料庫名稱.",
                    "The username used for connecting to MySQL server."
            )));

            configuration.set("storage.database-password", "20480727");
            configuration.setComment("storage.database-password", convertListStringToString(Arrays.asList(
                    "應在 MySQL 使用的資料庫密碼.",
                    "The password used for connecting to MySQL server."
            )));

            configuration.set("storage.jdbc-connection-string", "jdbc:mysql://{database_ip}:{database_port}/{database_name}");
            configuration.setComment("storage.jdbc-connection-string", convertListStringToString(Arrays.asList(
                    "連接至 MySQL 時所使用的 JDBC 參數, {database_ip} 表示資料庫 IP 的佔位符, {database_port} 表示資料庫端口的佔位符, {database_name} 表示資料庫名稱的佔位符.",
                    "倘若 database-ip-and-domain 等欄位有被正確填寫的話, 將會自動帶入佔位符.",
                    "The JDBC connection string used for connecting to MySQL server, {database_ip} standing for the port of MySQL server to connect,",
                    "{database_port} standing for the port of MySQL server to connect, {database_name} standing for the database name used to create tables and storing data.",
                    "Once the database config was filled up correctly, the placeholders will be automatically filled at runtime."
            )));

            try {
                configuration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            updateConfigurations(configuration.getInt("config-version"));
        }

        reloadConfig();
    }

    private void updateConfigurations(int version) {
        switch (version) {
            case 1:
                configuration.set("griefprevention-strict-check", false);
                configuration.setComment("griefprevention-strict-check", convertListStringToString(Arrays.asList("是否啟用 再生含有GP領地的區塊，但是不再生GP領地範圍內的方塊 功能",
                        "演示影片: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=7",
                        "Whether to enable the experimental function that if the expired chunk has GriefPrevention in it, put all blocks in GriefPrevention's claims to new chunk instead of skipping chunk.",
                        "Demo: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=7")));
            case 2:
                configuration.set("block-put-per-tick", 1024);
                configuration.setComment("block-put-per-tick", convertListStringToString(Arrays.asList(
                        "每次區域放置的保留方塊數量，倘若無特殊情況，請保持在默認值",
                        "How many blocks to put for residences/structures reserved action.",
                        "Please leave it as it is if your server does not have a bunch of structures/residences in a chunk."
                )));

                configuration.set("block-put-action-per-n-tick", 10);
                configuration.setComment("block-put-action-per-n-tick", convertListStringToString(Arrays.asList(
                                "每 n 個 tick 檢查是否有需要保留的方塊等待放置, 倘若該數值被設置的過久的話玩家將可能見到終界折躍門方塊突然消失, 又再次出現.",
                                "Check whether the queue has blocks need to put every n tick(s), if the value is set too high, player in the center of the end might see the end gateway suddenly vanished then appeared."
                        )
                ));
            case 3:
                configuration.set("storage.method", "yaml");
                configuration.setComment("storage.method", convertListStringToString(Arrays.asList(
                        "選擇儲存待更新區塊的資料庫類型, 可選擇 yaml (本地), sqlite (本地), mysql (遠端, 需配置 MySQL 伺服器)",
                        "Choosing the database type of storing chunks not reaching ttl, the available option is 'yaml' and 'sqlite."
                )));
            case 4:
                configuration.set("storage.database-name", "naturerevive");
                configuration.setComment("storage.database-name", convertListStringToString(Arrays.asList(
                        "應在 MySQL 使用的資料庫名稱.",
                        "The database name used for creating tables and storing data in MySQL."
                )));

                configuration.set("storage.database-domain-or-ip", "127.0.0.1");
                configuration.setComment("storage.database-domain-or-ip", convertListStringToString(Arrays.asList(
                        "連接至 MySQL 所用的 IP 或域名.",
                        "The IP or domain used for connecting to MySQL server."
                )));

                configuration.set("storage.database-port", 3306);
                configuration.setComment("storage.database-port", convertListStringToString(Arrays.asList(
                        "連接至 MySQL 所用的端口名稱.",
                        "The port used for connecting to MySQL server."
                )));

                configuration.set("storage.database-username", "root");
                configuration.setComment("storage.database-name", convertListStringToString(Arrays.asList(
                        "應在 MySQL 使用的資料庫名稱.",
                        "The username used for connecting to MySQL server."
                )));

                configuration.set("storage.database-password", "20480727");
                configuration.setComment("storage.database-password", convertListStringToString(Arrays.asList(
                        "應在 MySQL 使用的資料庫密碼.",
                        "The password used for connecting to MySQL server."
                )));

                configuration.set("storage.jdbc-connection-string", "jdbc:mysql://{database_ip}:{database_port}/{database_name}");
                configuration.setComment("storage.jdbc-connection-string", convertListStringToString(Arrays.asList(
                        "連接至 MySQL 時所使用的 JDBC 參數, {database_ip} 表示資料庫 IP 的佔位符, {database_port} 表示資料庫端口的佔位符, {database_name} 表示資料庫名稱的佔位符.",
                        "倘若 database-ip-and-domain 等欄位有被正確填寫的話, 將會自動帶入佔位符.",
                        "The JDBC connection string used for connecting to MySQL server, {database_ip} standing for the port of MySQL server to connect,",
                        "{database_port} standing for the port of MySQL server to connect, {database_name} standing for the database name used to create tables and storing data.",
                        "Once the database config was filled up correctly, the placeholders will be automatically filled at runtime."
                )));
            case 5:
                configuration.set("min-tps-for-regenerate-chunk", 16.0);
                configuration.setComment("min-tps-for-regenerate-chunk", convertListStringToString(Arrays.asList(
                        "重生區塊的最低 TPS 數值, 倘若低於該數值, 區塊重生將會被擱置.",
                        "The minimum TPS for regenerating expired chunks, if server's tps is lower than this value, the regeneration task will be stopped."
                )));

                configuration.set("max-players-for-regenerate-chunk", 40);
                configuration.setComment("max-players-for-regenerate-chunk", convertListStringToString(Arrays.asList(
                        "重生區塊的最高玩家上限, 倘若玩家數高於該數值, 區塊重生將會被擱置.",
                        "The maximum players count for regenerating expired chunks, if server's players count is greater than this value, the regeneration task will be stopped."
                )));
            case 6:
                configuration.set("messages.stop-regeneration", "&e關閉區塊重生系統成功, 倘若想要再次開啟, 請重新執行該指令!");
                configuration.setComment("messages.stop-regeneration", convertListStringToString(Arrays.asList("當區塊重生系統被關閉時，向指令執行者傳送的訊息.",
                        "The message to be sent on plugin's chunk regeneration system was turned off.")));

                configuration.set("messages.start-regeneration", "&a開啟區塊重生系統成功, 倘若想要再次關閉, 請重新執行該指令!");
                configuration.setComment("messages.start-regeneration", convertListStringToString(Arrays.asList("當區塊重生系統被重新開啟時，向指令執行者傳送的訊息.",
                        "The message to be sent on plugin's chunk regeneration system was turned on again.")));

                configuration.set("blacklist-worlds", Arrays.asList("世界 1", "World 2"));
                configuration.setComment("blacklist-worlds", convertListStringToString(Arrays.asList(
                        "該列表內的世界將會被重生系統忽略, 並將不會再生.",
                        "The list of ignored world that will be skipped by regeneration system."
                )));

                configuration.set("messages.force-regen-fail-due-to-regeneration-stop", "&c無法在區塊重生系統關閉時強制重生區塊!");
                configuration.setComment("messages.force-regen-fail-due-to-regeneration-stop", convertListStringToString(Arrays.asList("當區塊重生系統被關閉時, 執行強制重生系統會回傳的訊息.",
                        "The message to be sent on forcing regenerate command was invoked but the regeneration system was paused.")));
            case 7:
                configuration.set("storage.table-name", "locations");
                configuration.setComment("storage.table-name", convertListStringToString(Arrays.asList(
                        "連接至 MySQL 所用的資料表名稱.",
                        "The table's name used for storing data in MySQL server."
                )));
            case 8:
                configuration.set("griefdefender-strict-check", false);
                configuration.setComment("griefdefender-strict-check", convertListStringToString(Arrays.asList("是否啟用 再生含有GD領地的區塊，但是不再生GD領地範圍內的方塊 功能",
                        "演示影片: https://www.youtube.com/watch?v=euKrueUrD_4&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=9",
                        "Whether to enable the experimental function that if the expired chunk has GriefDefender in it, put all blocks in GriefDefender's claims to new chunk instead of skipping chunk.",
                        "Demo: https://www.youtube.com/watch?v=41RAkj97fJY&list=PLiqb-2W5wSDFvBwnNJCtt_O-kIem40iDG&index=9")));

                configuration.set("regeneration-strategy", "aggressive");
                configuration.setComment("regeneration-strategy", convertListStringToString(Arrays.asList(
                        "控制區塊的生成策略以及激進程度, 可選 aggressive (激進), passive (緩和), average (均衡)",
                        "當選擇 aggressive 時, 插件將會主動載入重生過期的區塊, 該方法可以有效清空所有過期, 但較為消耗資源.",
                        "當選擇 average 時, 插件會定期檢查玩家周圍八格的區塊是否過期, 該方法對人數均衡的伺服器較為友善.",
                        "當選擇 passive 時, 插件將不會主動加載過期的區塊, 並將等到玩家主動加載區塊時才會進行重生,",
                        "該方法可以避免區塊重生所造成的 TPS 跌幅, 但會有部分未被玩家探索的區塊長時間未重生.",
                        "The option to determine the plugin's regeneration management strategy, valid options are 'aggressive', 'passive' and 'average'",
                        "When aggressive is chosen, the plugin will load and regenerate expired chunks periodically, this method can regenerate all chunks that is expired, but the performance cost will much higher.",
                        "When average is chosen, the plugin will check all players' neighboring chunks whether or not is expired, if it is, the neighboring chunks will be queued to be regenerated.",
                        "When passive is chosen, the plugin will only regenrate chunk on player visited, this method will reduce performance cost but not all the expired chunks will be regenerated."
                )));
            case 9:
                configuration.set("block-explosion-queue-process-per-n-tick", 10);
                configuration.setComment("block-explosion-queue-process-per-n-tick",
                        convertListStringToString(Arrays.asList("每 n 個 tick 處理一次爆炸影響之區塊計算 (1 tick = 50ms)",
                                "Proceeding the block/entity explosions affected chunks calculation function every n tick(s).")
                        ));

                configuration.set("block-explosion-queue-process-per-time", 200);
                configuration.setComment("block-explosion-queue-process-per-time", convertListStringToString(Arrays.asList("每次可以處理幾個被爆炸範圍影響的方塊.",
                        "How many block(s) to calculate per explosion process period.")
                ));
            case 10:
                configuration.set("block-queue-process-per-n-tick", configuration.getInt("block-explosion-queue-process-per-n-tick"));
                configuration.set("block-queue-process-per-time", configuration.getInt("block-explosion-queue-process-per-time"));

                configuration.set("block-explosion-queue-process-per-n-tick", null);
                configuration.set("block-explosion-queue-process-per-time", null);
            case 11:
                configuration.set("sql-processing-tick", 3);
                configuration.setComment("sql-processing-tick", convertListStringToString(Arrays.asList("每幾個 tick 可以對資料庫進行增刪查改的動作.",
                        "請盡量將該數值設置的低一點, 否則資料庫有可能會毀損.",
                        "How many tick(s) to execute SQL query (like insert, update, delete).",
                        "Please set it lower than 5 to prevent sql-cache sync error.")
                ));

                configuration.set("sql-processing-count", 50);
                configuration.setComment("sql-processing-count", convertListStringToString(Arrays.asList("每次執行可以執行多少個 SQL 指令.",
                        "How many queries to execute per execution period.")
                ));

                configuration.set("coreprotect-logging-enable", false);
                configuration.setComment("coreprotect-logging-enable", convertListStringToString(Arrays.asList("是否啟用 CoreProtect 的紀錄功能.",
                        "Whether or not to enable the CoreProtect logging integration.")));
            case 12:
                configuration.set("adaptive-loot-chest-replacement", false);
                configuration.setComment("adaptive-loot-chest-replacement", convertListStringToString(Arrays.asList(
                        "改變生成寶藏箱時的策略, 當偵測到寶藏箱時, 僅更新其種子, 而不進行物品填充.",
                        "當您遇到每次打開寶藏箱會再生物品的情況時, 請打開該設置.",
                        "This option will prevent plugin from filling the loot chesting when detected the loot chest",
                        "It should be turned on when the loot chest is filled unexpectedly.")
                ));
            case 13:
                configuration.setComment("regeneration-strategy", convertListStringToString(Arrays.asList(
                        "控制區塊的生成策略以及激進程度, 可選 aggressive (激進), passive (緩和), average (均衡)",
                        "當選擇 aggressive 時, 插件將會主動載入重生過期的區塊, 該方法可以有效清空所有過期, 但較為消耗資源.",
                        "當選擇 average 時, 插件會定期檢查玩家周圍八格 (可以透過 average-chunk-radius 調整) 的區塊是否過期, 該方法對人數均衡的伺服器較為友善.",
                        "當選擇 passive 時, 插件將不會主動加載過期的區塊, 並將等到玩家主動加載區塊時才會進行重生,",
                        "該方法可以避免區塊重生所造成的 TPS 跌幅, 但會有部分未被玩家探索的區塊長時間未重生.",
                        "The option to determine the plugin's regeneration management strategy, valid options are 'aggressive', 'passive' and 'average'",
                        "When aggressive is chosen, the plugin will load and regenerate expired chunks periodically, this method can regenerate all chunks that is expired, but the performance cost will much higher.",
                        "When average is chosen, the plugin will check all players' neighboring chunks whether or not is expired, if it is, the neighboring chunks will be queued to be regenerated.",
                        "When passive is chosen, the plugin will only regenerate chunk on player visited, this method will reduce performance cost but not all the expired chunks will be regenerated."
                )));

                configuration.set("average-chunk-radius", 2);
                configuration.setComment("average-chunk-radius",
                        convertListStringToString(Arrays.asList("調整 regeneration-strategy 為 average 時, 插件重生區塊的範圍。",
                                "Config the radius of regenerating chunk when regeneration-strategy is set to 'average'.",
                                "Formula: f(x) = (2x - 1) ^ 2 - 1",
                                "(2x - 1) ^ 2 is the radius, and we reduce 1 to except the chunk where player located.")
                        ));
            case 14:
                configuration.set("suppress-chunk-refresh-radius", 0);
                configuration.setComment("suppress-chunk-refresh-radius", convertListStringToString(Arrays.asList(
                        "每當某區塊發生變更進而重置區塊重生時間時，一同重置周圍 n 個區塊的時間。",
                        "How many nearby chunks' expiration times should be updates once any chunk is active.")

                ));
                configuration.set("spawn-timer", "00:00-23:59");
                configuration.setComment("spawn-timer", convertListStringToString(Arrays.asList(
                        "限定進行重生區塊的時間, 區塊重生僅會在指定時間內進行生成(通常設定為半夜).",
                        "格式為24小時制，0不可省略 (xx:xx-xx:xx), 預設為凌晨0:00至上午7:00",
                        "Limit the time of chunk regenerate (mostly at midnight) to prevent player",
                        "experiencing from major performance degraded."

                )));
            case 15: // major update
                configuration.set("regeneration-engine", "bukkit");
                configuration.setComment("regeneration-engine", convertListStringToString(Arrays.asList(
                        "變更生成區塊時所指定的引擎，如 bukkit 為原版引擎，生成速度較快，但會導致版本不兼容出現的區塊斷層，",
                        "fawe 則將調用 FastAsyncWorldEdit 的方式，生成速度較慢，但可保持區塊間地形兼容。",
                        "使用 FAWE 方法須安裝 FastAsyncWorldEdit。",
                        "Change the method of regeneration, e.g. bukkit using original method, which is rapid but cause",
                        "major chunk problem (e.g. terrain break), fawe use FastAsyncWorldEdit implementation,",
                        "which have slower speed but guarantee terrain, you have to install FastAsyncWorldEdit in order to",
                        "use fawe method."
                )));

                configuration.set("max-elytra-per-day", 10);
                configuration.setComment("max-elytra-per-day", convertListStringToString(Arrays.asList(
                        "限制每日重生鞘翅的最大數量，將於本地時間 00:00 重置。",
                        "This option will set a limit for max elytra regenerated, the limit will be reset on 00:00 A.M. local time.")
                ));

                configuration.set("regen-offset-max-duration", "0d");
                configuration.setComment("regen-offset-max-duration", convertListStringToString(Arrays.asList(
                        "重生時，隨機額外添加重生時長，避免玩家於定期於重生時間搜刮物資，格式同 ttl-duration。",
                        "This option will add an offset to regen time to prevent player from logging regen time to get elytra or lootable chest,",
                        "the format is same as the ttl-duration options")
                ));

                configuration.set("elytra-exceed-limit-offset-duration", "1d");
                configuration.setComment("elytra-exceed-limit-offset-duration", convertListStringToString(Arrays.asList(
                        "當鞘翅重生數量超過設定值時，偏移所設置的時間值。",
                        "This option will set a time offset on elytra regen task, when the elytra regen reach the limit.")
                ));

                configuration.set("regeneration-strategy", "passive");
                configuration.set("coreprotect-logging-enable", false);

                configuration.set("enable-ore-obfuscation", false);
                configuration.setComment("enable-ore-obfuscation", convertListStringToString(Arrays.asList(
                        "此選項將開啟礦物替換，將礦物的所有位置進行調換，避免玩家得知種子碼後，透過記錄礦物位置訊息以重複獲取礦物之行為。",
                        "This option enable the ore 'obfuscation' system, once a chunk was regenerated, we will shuffle the ore's location to prevent players from logging ore's location."
                )));

                configuration.set("blacklist-biomes", Arrays.asList("naturerevive"));
                configuration.setComment("blacklist-biomes", convertListStringToString(Arrays.asList(
                        "該列表內的生態域將會被重生系統忽略, 並將不會再生.",
                        "The list of ignored biomes that will be skipped by regeneration system."
                )));

                configuration.remove("sql-processing-tick");
                configuration.set("sql-processing-count", 500);
            case 16:
                configuration.set("messages.stop-regeneration", "&e關閉區塊重生系統成功, 倘若想要再次開啟, 請執行 /naturerevive resume");
                configuration.setComment("messages.stop-regeneration", convertListStringToString(Arrays.asList("當區塊重生系統被關閉時，向指令執行者傳送的訊息.",
                        "The message to be sent on plugin's chunk regeneration system was turned off.")));

                configuration.set("messages.start-regeneration", "&a開啟區塊重生系統成功, 倘若想要再次關閉, 請執行 /naturerevive pause");
                configuration.setComment("messages.start-regeneration", convertListStringToString(Arrays.asList("當區塊重生系統被重新開啟時，向指令執行者傳送的訊息.",
                        "The message to be sent on plugin's chunk regeneration system was turned on again.")));
            case 17:
                configuration.set("whitelist-worlds", List.of());
                configuration.setComment("whitelist-worlds", convertListStringToString(Arrays.asList(
                        "倘若該列表內有填寫任何世界名稱，NatureRevive 將只會重生該名單內的世界，",
                        "倘若列表內的世界同樣在 blacklist-worlds 內，那麼將會被忽略。",
                        "The list of allowed world that will be skipped by regeneration system,",
                        "if the list is empty, all worlds will be allow to perform regen task",
                        "if any world in list is also in blacklist-worlds, the world will not perform task."
                )));

                configuration.set("coreprotect-logging-enable", true);
            default:
                configuration.set("config-version", CONFIG_VERSION);
                try {
                    configuration.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public void reloadConfig() throws IOException {
        this.configuration = YamlFile.loadConfiguration(file);

        debug = configuration.getBoolean("debug", false);
        residenceStrictCheck = configuration.getBoolean("residence-strict-check", false); // new options: lands.strict
        griefPreventionStrictCheck = configuration.getBoolean("griefprevention-strict-check", false); // new options: lands.strict
        griefDefenderStrictCheck = configuration.getBoolean("griefdefender-strict-check", false); // new options: lands.strict

        saferOreObfuscation = configuration.getBoolean("safer-ore-obfuscation", false); // ore-obfuscation.safer
        adaptiveLootChestReplacement = configuration.getBoolean("adaptive-loot-chest-replacement", false); // loot-chest.enable-prefill inverse
        suppressNearbyChunkCount = configuration.getInt("suppress-chunk-refresh-radius", 0); // regenerate-track-nearby-n-chunks
        coreProtectLogging = configuration.getBoolean("coreprotect-logging-enable", true); // new-options: logging.disable
        enableOreObfuscation = configuration.getBoolean("enable-ore-obfuscation", false); // ore-obfuscation.enable

        taskPerProcess = configuration.getInt("task-process-per-tick", 1); // regenerate-n-chunks-per-time
        queuePerNTick = configuration.getInt("queue-process-per-n-tick", 200); // regenerate-chunks-per-n-ticks
        checkChunkTTLTick = configuration.getInt("check-chunk-ttl-per-n-tick", 200); // check-expires-chunks-every-n-ticks
        dataSaveTime = configuration.getInt("data-save-time-tick", 3600); // save-per-n-ticks
        blockPutPerTick = configuration.getInt("block-put-per-tick", 1024); // block-process-batch-size
        blockPutActionPerNTick = configuration.getInt("block-put-action-per-n-tick", 10); // block-process-per-n-ticks
        minTPSCountForRegeneration = configuration.getDouble("min-tps-for-regenerate-chunk", 16.0);
        maxPlayersCountForRegeneration = configuration.getInt("max-players-for-regenerate-chunk", 40);
        regenerationStrategy = configuration.getString("regeneration-strategy", "passive"); // strategy
        regenerationEngine = configuration.getString("regeneration-engine", "bukkit"); // engine
        blockProcessingTick = configuration.getInt("block-queue-process-per-n-tick", 10); // regenerate-affected-blocks-flag-per-n-tick
        blockProcessingAmountPerProcessing = configuration.getInt("block-queue-process-per-time", 200); // regenerate-affected-n-blocks-flag-per-tick
        chunkRegenerateRadiusOnAverageApplied = configuration.getInt("average-chunk-radius", 2); // check-chunk-radius
        maxElytraPerDay = configuration.getInt("max-elytra-per-day", 10); // todo: Structure part

        ttlDuration = parseDuration(configuration.getString("ttl-duration", "7d")); // regenerate-after
        regenOffsetDuration = parseDuration(configuration.getString("regen-offset-max-duration", "0d"));
        elytraExceedLimitOffsetDuration = parseDuration(configuration.getString("elytra-exceed-limit-offset-duration", "1d")); // todo: Structure part
        coreProtectUserName = configuration.getString("coreprotect-log-username", "#資源再生"); // coreprotect-username
        reloadSuccessMessage = configuration.getString("messages.reload-success-message", "&a成功重載插件配置檔!"); // same
        reloadFailureMessage = configuration.getString("messages.reload-failure-message", "&c插件配置檔重載失敗, 請查看後台以獲取詳細記錄."); // same
        stopChunkRegenerationMessage = configuration.getString("messages.stop-regeneration", "&e關閉區塊重生系統成功, 倘若想要再次關閉, 請執行 /naturerevive resume"); // same
        startChunkRegenerationMessage = configuration.getString("messages.start-regeneration", "&a開啟區塊重生系統成功, 倘若想要再次關閉, 請執行 /naturerevive pause"); // same
        forceRegenFailedDueRegenStopMessage = configuration.getString("messages.force-regen-fail-due-to-regeneration-stop", "&c無法在區塊重生系統關閉時強制重生區塊!"); // same

        ignoredWorld = configuration.getStringList("blacklist-worlds"); // new options: world-list
        allowedWorld = configuration.getStringList("whitelist-worlds"); // new options: world-list
        ignoredBiomes = configuration.getStringList("blacklist-biomes"); // new options: biomes-list

        databaseName = configuration.getString("storage.database-name", "naturerevive"); // same
        databaseTableName = configuration.getString("storage.table-name", "locations"); // same
        databaseIp = configuration.getString("storage.database-domain-or-ip", "127.0.0.1"); // database-hostname
        databasePort = configuration.getInt("storage.database-port", 3306); // same
        databaseUsername = configuration.getString("storage.database-username", "root"); // same
        databasePassword = configuration.getString("storage.database-password", "20480727"); // same
        jdbcConnectionString = configuration.getString("storage.jdbc-connection-string", "jdbc:mysql://{database_ip}:{database_port}/{database_name}"); // same
        spawnTimer = configuration.getString("spawn-timer","00:00-23:59"); // regenerate-at

        sqlProcessingCount = configuration.getInt("sql-processing-count", 500); // sql-process-batch-size
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

    private String convertListStringToString(List<String> comments) {
        return String.join(System.lineSeparator(), comments);
    }

    //判定當前系統時間是否在表定範圍內
    public boolean isCurrentTimeAllowForRSC(){
        String[] timeParts = spawnTimer.split("-");
        LocalTime timeRangeStart = LocalTime.parse(timeParts[0]); // 設定起始時間
        LocalTime timeRangeEnd =  LocalTime.parse(timeParts[1]); // 設定結束時間
        LocalTime currentTime = LocalTime.now();
        //判定是否可以汪...是否藉於時間內 (r: T/F)
        return currentTime.isAfter(timeRangeStart) && currentTime.isBefore(timeRangeEnd);
    }
}

