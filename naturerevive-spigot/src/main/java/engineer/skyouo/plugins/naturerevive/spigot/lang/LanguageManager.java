package engineer.skyouo.plugins.naturerevive.spigot.lang;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {
    public static final String DEFAULT_LOCALE = "en_US";
    private static final String[] BUNDLED_LOCALES = {"zh_TW", "en_US"};

    private final NatureRevivePlugin plugin;

    private FileConfiguration messages;
    private FileConfiguration fallback;

    public LanguageManager(NatureRevivePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        for (String locale : BUNDLED_LOCALES) {
            File file = new File(langDir, locale + ".yml");
            if (!file.exists()) {
                plugin.saveResource("lang/" + locale + ".yml", false);
            }
        }

        String locale = NatureRevivePlugin.readonlyConfig != null
                ? NatureRevivePlugin.readonlyConfig.language
                : DEFAULT_LOCALE;
        if (locale == null || locale.isBlank()) locale = DEFAULT_LOCALE;

        File localeFile = new File(langDir, locale + ".yml");
        boolean localeFileExists = localeFile.exists();
        if (!localeFileExists) {
            NatureReviveComponentLogger.warning(
                    "Language file lang/" + locale + ".yml was not found, falling back to " + DEFAULT_LOCALE + ".");
            localeFile = new File(langDir, DEFAULT_LOCALE + ".yml");
        }

        messages = YamlConfiguration.loadConfiguration(localeFile);

        fallback = loadFromResource("lang/" + DEFAULT_LOCALE + ".yml");

        if (localeFileExists) {
            FileConfiguration template = loadFromResource("lang/" + locale + ".yml");
            if (template == null) template = fallback;
            writeMissingKeys(localeFile, messages, template);
        }
    }

    public String get(String key) {
        String value = messages != null ? messages.getString(key) : null;
        if (value == null && fallback != null) value = fallback.getString(key);
        return value != null ? value : key;
    }

    public String get(String key, Object... args) {
        String value = get(key);
        if (args == null || args.length == 0) return value;
        try {
            return String.format(value, args);
        } catch (Exception ex) {
            return value;
        }
    }

    private FileConfiguration loadFromResource(String resourcePath) {
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) return null;
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private void writeMissingKeys(File localeFile, FileConfiguration target, FileConfiguration template) {
        if (template == null) return;

        int added = 0;
        for (String key : template.getKeys(true)) {
            if (template.isConfigurationSection(key)) continue; // skip parents, copy only leaf values
            if (!target.contains(key)) {
                target.set(key, template.get(key));
                added++;
            }
        }

        if (added == 0) return;

        try {
            target.save(localeFile);
            NatureReviveComponentLogger.info(
                    "Added " + added + " missing language key(s) to lang/" + localeFile.getName()
                            + " from the bundled template.");
        } catch (IOException e) {
            NatureReviveComponentLogger.warning(
                    "Failed to write missing language keys to lang/" + localeFile.getName() + ".");
            e.printStackTrace();
        }
    }
}
