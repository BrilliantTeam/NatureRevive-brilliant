package engineer.skyouo.plugins.naturerevive.spigot.webhook;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import org.bukkit.Chunk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WebhookManager {
    private WebhookManager() {
    }

    private static final int MAX_EMBEDS_PER_MESSAGE = 10;
    private static final int MAX_MESSAGES_PER_FLUSH = 5;
    private static final int MAX_QUEUE_SIZE = 10000;

    private static final int DISCORD_CONTENT_LIMIT = 2000;
    private static final int DISCORD_EMBED_TOTAL_LIMIT = 6000;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter REGEN_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    // Discord rate-limits each webhook to roughly 5 requests / 2 seconds, so messages are
    // dispatched one at a time with this minimum gap rather than fired concurrently.
    private static final long MIN_SEND_INTERVAL_MS = 750;
    // Used when a 429 response carries no parseable Retry-After.
    private static final long DEFAULT_RETRY_MS = 2000;
    // How many times a single message is re-tried after a 429 before being dropped.
    private static final int MAX_SEND_RETRIES = 5;
    // Upper bound on queued outbound messages; oldest are dropped beyond this.
    private static final int MAX_OUTBOX = 500;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    private static final Queue<PendingEvent> recordQueue = new ConcurrentLinkedQueue<>();
    private static final Queue<PendingEvent> updateQueue = new ConcurrentLinkedQueue<>();
    private static final Queue<PendingEvent> regenQueue = new ConcurrentLinkedQueue<>();

    // Built Discord messages awaiting paced delivery, drained by a single self-driving pump.
    private static final Deque<Outbound> outbox = new ConcurrentLinkedDeque<>();
    private static final AtomicBoolean PUMPING = new AtomicBoolean(false);

    public static void notifyFlag(BukkitPositionInfo info, FlagReason reason, boolean isNew) {
        WebhookConfig config = NatureRevivePlugin.webhookConfig;
        if (config == null || !config.enabled) return;

        WebhookConfig.EventSetting setting = isNew ? config.record : config.update;
        if (setting == null || !setting.enabled) return;

        String regenTime = REGEN_TIME_FORMAT.format(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(info.getTTL()), ZoneId.systemDefault()));

        PendingEvent event = new PendingEvent(
                info.getWorldName(), info.getX(), info.getZ(),
                reason == null ? "" : reason.getDisplayName(),
                regenTime,
                TIME_FORMAT.format(LocalDateTime.now()));

        offer(isNew ? recordQueue : updateQueue, event);
    }

    public static void notifyRegen(Chunk chunk, LocalDateTime dateTime) {
        WebhookConfig config = NatureRevivePlugin.webhookConfig;
        if (config == null || !config.enabled) return;
        if (config.regenerate == null || !config.regenerate.enabled) return;

        PendingEvent event = new PendingEvent(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ(),
                "", "", TIME_FORMAT.format(dateTime == null ? LocalDateTime.now() : dateTime));

        offer(regenQueue, event);
    }

    private static void offer(Queue<PendingEvent> queue, PendingEvent event) {
        if (queue.size() >= MAX_QUEUE_SIZE) {
            queue.poll();
        }
        queue.add(event);
    }

    public static void flush() {
        WebhookConfig config = NatureRevivePlugin.webhookConfig;
        if (config == null || !config.enabled || config.url == null || config.url.isBlank()) {
            recordQueue.clear();
            updateQueue.clear();
            regenQueue.clear();
            return;
        }

        flushQueue(config, config.record, recordQueue);
        flushQueue(config, config.update, updateQueue);
        flushQueue(config, config.regenerate, regenQueue);
    }

    private static void flushQueue(WebhookConfig config, WebhookConfig.EventSetting setting, Queue<PendingEvent> queue) {
        if (setting == null || !setting.enabled || queue.isEmpty()) return;

        int perMessage = config.maxPerMessage <= 0 ? 10 : config.maxPerMessage;
        if (setting.useEmbed) {
            perMessage = Math.min(perMessage, MAX_EMBEDS_PER_MESSAGE);
        }

        int messages = 0;
        while (!queue.isEmpty() && messages < MAX_MESSAGES_PER_FLUSH) {
            List<PendingEvent> batch = new ArrayList<>();
            for (int i = 0; i < perMessage && !queue.isEmpty(); i++) {
                PendingEvent event = queue.poll();
                if (event != null) batch.add(event);
            }
            if (batch.isEmpty()) break;

            send(config, setting, batch);
            messages++;
        }
    }

    private static void send(WebhookConfig config, WebhookConfig.EventSetting setting, List<PendingEvent> batch) {
        JsonObject root = new JsonObject();
        if (notBlank(config.username)) root.addProperty("username", config.username);
        if (notBlank(config.avatarUrl)) root.addProperty("avatar_url", config.avatarUrl);

        boolean overflow;
        if (setting.useEmbed) {
            JsonArray embeds = new JsonArray();
            int totalChars = 0;
            for (PendingEvent event : batch) {
                embeds.add(buildEmbed(setting, event, batch.size()));
                totalChars += embedTextLength(setting, event, batch.size());
            }
            overflow = totalChars > DISCORD_EMBED_TOTAL_LIMIT;
            if (!overflow) root.add("embeds", embeds);
        } else {
            StringBuilder content = new StringBuilder();
            for (PendingEvent event : batch) {
                if (content.length() > 0) content.append('\n');
                content.append(replace(setting.content, event, batch.size()));
            }
            overflow = content.length() > DISCORD_CONTENT_LIMIT;
            if (!overflow) root.addProperty("content", content.toString());
        }

        if (overflow) {
            sendAsFile(config, setting, batch);
        } else {
            enqueueOutbound(new Outbound(config.url, GSON.toJson(root), null, null, 0));
        }
    }

    private static void sendAsFile(WebhookConfig config, WebhookConfig.EventSetting setting, List<PendingEvent> batch) {
        StringBuilder text = new StringBuilder();
        for (PendingEvent event : batch) {
            if (text.length() > 0) text.append('\n');
            text.append(renderLine(setting, event, batch.size()));
        }

        JsonObject root = new JsonObject();
        if (notBlank(config.username)) root.addProperty("username", config.username);
        if (notBlank(config.avatarUrl)) root.addProperty("avatar_url", config.avatarUrl);
        root.addProperty("content", Lang.get("webhook.attachment-notice", batch.size()));

        String fileName = "NatureRevive-" + FILE_TIME_FORMAT.format(LocalDateTime.now()) + ".txt";
        enqueueOutbound(new Outbound(config.url, GSON.toJson(root), fileName, text.toString(), 0));
    }

    private static int embedTextLength(WebhookConfig.EventSetting setting, PendingEvent event, int count) {
        int len = 0;
        if (notBlank(setting.embedTitle)) len += replace(setting.embedTitle, event, count).length();
        if (notBlank(setting.embedDescription)) len += replace(setting.embedDescription, event, count).length();
        if (setting.embedFields != null) {
            for (WebhookConfig.EmbedField field : setting.embedFields) {
                len += replace(field.name, event, count).length();
                len += replace(field.value, event, count).length();
            }
        }
        if (notBlank(setting.embedFooter)) len += replace(setting.embedFooter, event, count).length();
        return len;
    }

    private static String renderLine(WebhookConfig.EventSetting setting, PendingEvent event, int count) {
        if (notBlank(setting.content)) return replace(setting.content, event, count);

        StringBuilder sb = new StringBuilder();
        if (notBlank(setting.embedTitle)) sb.append(replace(setting.embedTitle, event, count));
        if (setting.embedFields != null) {
            for (WebhookConfig.EmbedField field : setting.embedFields) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(replace(field.name, event, count)).append(": ").append(replace(field.value, event, count));
            }
        }
        if (notBlank(setting.embedFooter)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(replace(setting.embedFooter, event, count));
        }
        return sb.toString();
    }

    private static JsonObject buildEmbed(WebhookConfig.EventSetting setting, PendingEvent event, int count) {
        JsonObject embed = new JsonObject();

        if (notBlank(setting.embedTitle)) embed.addProperty("title", replace(setting.embedTitle, event, count));
        if (notBlank(setting.embedDescription))
            embed.addProperty("description", replace(setting.embedDescription, event, count));

        Integer color = parseColor(setting.embedColor);
        if (color != null) embed.addProperty("color", color);

        if (setting.embedFields != null && !setting.embedFields.isEmpty()) {
            JsonArray fields = new JsonArray();
            for (WebhookConfig.EmbedField field : setting.embedFields) {
                JsonObject node = new JsonObject();
                node.addProperty("name", replace(field.name, event, count));
                node.addProperty("value", replace(field.value, event, count));
                node.addProperty("inline", field.inline);
                fields.add(node);
            }
            embed.add("fields", fields);
        }

        if (notBlank(setting.embedFooter)) {
            JsonObject footer = new JsonObject();
            footer.addProperty("text", replace(setting.embedFooter, event, count));
            embed.add("footer", footer);
        }

        return embed;
    }

    private static String replace(String template, PendingEvent event, int count) {
        if (template == null) return "";

        int blockX = event.chunkX << 4;
        int blockZ = event.chunkZ << 4;

        return template
                .replace("%world%", resolveWorld(event.world))
                .replace("%chunk_x%", String.valueOf(event.chunkX))
                .replace("%chunk_z%", String.valueOf(event.chunkZ))
                .replace("%block_x%", String.valueOf(blockX))
                .replace("%block_z%", String.valueOf(blockZ))
                .replace("%block_x_end%", String.valueOf(blockX + 15))
                .replace("%block_z_end%", String.valueOf(blockZ + 15))
                .replace("%reason%", event.reason == null ? "" : event.reason)
                .replace("%regen_time%", event.regenTime == null ? "" : event.regenTime)
                .replace("%time%", event.time == null ? "" : event.time)
                .replace("%count%", String.valueOf(count));
    }

    private static String resolveWorld(String worldName) {
        if (worldName == null) return "";
        String key = "webhook.world." + worldName;
        String translated = Lang.get(key);
        return translated.equals(key) ? worldName : translated;
    }

    private static Integer parseColor(String hex) {
        if (!notBlank(hex)) return null;
        String value = hex.trim();
        if (value.startsWith("#")) value = value.substring(1);
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void enqueueOutbound(Outbound msg) {
        if (outbox.size() >= MAX_OUTBOX) outbox.pollFirst();
        outbox.addLast(msg);
        ensurePump();
    }

    /** Starts the delivery pump unless another thread already owns it. */
    private static void ensurePump() {
        if (PUMPING.compareAndSet(false, true)) {
            pumpNext();
        }
    }

    /** Sends one queued message, then re-schedules itself after the appropriate delay. */
    private static void pumpNext() {
        Outbound msg = outbox.pollFirst();
        if (msg == null) {
            PUMPING.set(false);
            // A message may have been enqueued between poll and release; reclaim the pump if so.
            if (!outbox.isEmpty() && PUMPING.compareAndSet(false, true)) {
                pumpNext();
            }
            return;
        }

        HttpRequest request = buildRequest(msg);
        if (request == null) {
            schedulePump(MIN_SEND_INTERVAL_MS); // build already logged the failure; skip this one
            return;
        }

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    long delay = MIN_SEND_INTERVAL_MS;
                    if (error == null && response != null && response.statusCode() == 429) {
                        delay = Math.max(parseRetryAfterMs(response), MIN_SEND_INTERVAL_MS);
                        if (msg.attempts + 1 < MAX_SEND_RETRIES) {
                            outbox.addFirst(msg.retry()); // re-send the same message once the limit clears
                        } else {
                            NatureReviveComponentLogger.debug(Lang.get("console.webhook-rate-limited"));
                        }
                    } else {
                        handleResponse(response, error);
                    }
                    schedulePump(delay);
                });
    }

    private static void schedulePump(long delayMs) {
        CompletableFuture.runAsync(WebhookManager::pumpNext,
                CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS));
    }

    private static HttpRequest buildRequest(Outbound msg) {
        try {
            if (msg.fileName == null) {
                return HttpRequest.newBuilder()
                        .uri(URI.create(msg.url))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(msg.json, StandardCharsets.UTF_8))
                        .build();
            }

            String boundary = "NatureReviveBoundary" + System.nanoTime();
            byte[] body = buildMultipartBody(boundary, msg.json, msg.fileName, msg.fileText);
            return HttpRequest.newBuilder()
                    .uri(URI.create(msg.url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
        } catch (Exception ex) {
            NatureReviveComponentLogger.debug(Lang.get("console.webhook-send-failed", ex.getMessage()));
            return null;
        }
    }

    private static byte[] buildMultipartBody(String boundary, String payloadJson, String fileName, String fileText)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Disposition: form-data; name=\"payload_json\"\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: application/json\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(payloadJson.getBytes(StandardCharsets.UTF_8));
        baos.write(("\r\n--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"files[0]\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: text/plain; charset=utf-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(fileText.getBytes(StandardCharsets.UTF_8));
        baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    /** Reads Discord's retry delay, preferring the JSON body's fractional seconds over the header. */
    private static long parseRetryAfterMs(HttpResponse<String> response) {
        try {
            JsonObject body = GSON.fromJson(response.body(), JsonObject.class);
            if (body != null && body.has("retry_after")) {
                return (long) Math.ceil(body.get("retry_after").getAsDouble() * 1000);
            }
        } catch (Exception ignored) {
            // fall through to header
        }
        return response.headers().firstValue("retry-after")
                .map(value -> {
                    try {
                        return (long) Math.ceil(Double.parseDouble(value) * 1000);
                    } catch (NumberFormatException ex) {
                        return DEFAULT_RETRY_MS;
                    }
                })
                .orElse(DEFAULT_RETRY_MS);
    }

    private static void handleResponse(HttpResponse<String> response, Throwable error) {
        if (error != null) {
            NatureReviveComponentLogger.debug(Lang.get("console.webhook-send-failed", error.getMessage()));
        } else if (response.statusCode() == 429) {
            NatureReviveComponentLogger.debug(Lang.get("console.webhook-rate-limited"));
        } else if (response.statusCode() >= 300) {
            NatureReviveComponentLogger.debug(Lang.get("console.webhook-send-failed", "HTTP " + response.statusCode()));
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static final class PendingEvent {
        final String world;
        final int chunkX;
        final int chunkZ;
        final String reason;
        final String regenTime;
        final String time;

        PendingEvent(String world, int chunkX, int chunkZ, String reason, String regenTime, String time) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.reason = reason;
            this.regenTime = regenTime;
            this.time = time;
        }
    }

    private static final class Outbound {
        final String url;
        final String json;
        final String fileName;
        final String fileText;
        final int attempts;

        Outbound(String url, String json, String fileName, String fileText, int attempts) {
            this.url = url;
            this.json = json;
            this.fileName = fileName;
            this.fileText = fileText;
            this.attempts = attempts;
        }

        Outbound retry() {
            return new Outbound(url, json, fileName, fileText, attempts + 1);
        }
    }
}
