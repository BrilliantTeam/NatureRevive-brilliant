package engineer.skyouo.plugins.naturerevive.spigot.webhook;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebhookConfig {
    private static final String FILE_NAME = "webhook.yml";

    private final NatureRevivePlugin plugin;

    public boolean enabled;
    public String url;
    public String username;
    public String avatarUrl;
    public int maxPerMessage;

    public EventSetting record;
    public EventSetting update;
    public EventSetting regenerate;

    public WebhookConfig(NatureRevivePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        FileConfiguration template = loadFromResource(FILE_NAME);
        if (template != null) {
            writeMissingKeys(file, config, template);
        }

        enabled = config.getBoolean("enabled", false);
        url = config.getString("url", "");
        username = config.getString("username", "");
        avatarUrl = config.getString("avatar-url", "");
        maxPerMessage = config.getInt("batch.max-per-message", 10);

        record = parseEvent(config.getConfigurationSection("events.record"));
        update = parseEvent(config.getConfigurationSection("events.update"));
        regenerate = parseEvent(config.getConfigurationSection("events.regenerate"));
    }

    private EventSetting parseEvent(ConfigurationSection section) {
        if (section == null) return EventSetting.disabled();

        boolean enabled = section.getBoolean("enabled", true);
        boolean useEmbed = section.getBoolean("use-embed", false);
        String content = section.getString("content", "");

        ConfigurationSection embed = section.getConfigurationSection("embed");
        String title = "";
        String color = "";
        String description = "";
        String footer = "";
        List<EmbedField> fields = new ArrayList<>();

        if (embed != null) {
            title = embed.getString("title", "");
            color = embed.getString("color", "");
            description = embed.getString("description", "");
            footer = embed.getString("footer", "");

            for (Map<?, ?> raw : embed.getMapList("fields")) {
                Object name = raw.get("name");
                Object value = raw.get("value");
                Object inline = raw.get("inline");
                if (name == null && value == null) continue;
                fields.add(new EmbedField(
                        name == null ? "" : String.valueOf(name),
                        value == null ? "" : String.valueOf(value),
                        inline instanceof Boolean ? (Boolean) inline : false));
            }
        }

        return new EventSetting(enabled, useEmbed, content, title, color, description, fields, footer);
    }

    private FileConfiguration loadFromResource(String resourcePath) {
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) return null;
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private void writeMissingKeys(File file, FileConfiguration target, FileConfiguration template) {
        int added = 0;
        for (String key : template.getKeys(true)) {
            if (template.isConfigurationSection(key)) continue;
            if (!target.contains(key)) {
                target.set(key, template.get(key));
                added++;
            }
        }

        if (added == 0) return;

        try {
            target.save(file);
            NatureReviveComponentLogger.info(
                    "Added " + added + " missing webhook key(s) to " + FILE_NAME + " from the bundled template.");
        } catch (IOException e) {
            NatureReviveComponentLogger.warning("Failed to write missing keys to " + FILE_NAME + ".");
            e.printStackTrace();
        }
    }

    public static final class EmbedField {
        public final String name;
        public final String value;
        public final boolean inline;

        public EmbedField(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }

    public static final class EventSetting {
        public final boolean enabled;
        public final boolean useEmbed;
        public final String content;
        public final String embedTitle;
        public final String embedColor;
        public final String embedDescription;
        public final List<EmbedField> embedFields;
        public final String embedFooter;

        public EventSetting(boolean enabled, boolean useEmbed, String content,
                            String embedTitle, String embedColor, String embedDescription,
                            List<EmbedField> embedFields, String embedFooter) {
            this.enabled = enabled;
            this.useEmbed = useEmbed;
            this.content = content;
            this.embedTitle = embedTitle;
            this.embedColor = embedColor;
            this.embedDescription = embedDescription;
            this.embedFields = embedFields;
            this.embedFooter = embedFooter;
        }

        static EventSetting disabled() {
            return new EventSetting(false, true, "", "", "", "", new ArrayList<>(), "");
        }
    }
}
