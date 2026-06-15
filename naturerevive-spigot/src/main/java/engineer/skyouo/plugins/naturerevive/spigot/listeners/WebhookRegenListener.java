package engineer.skyouo.plugins.naturerevive.spigot.listeners;

import engineer.skyouo.plugins.naturerevive.spigot.events.ChunkRegenEvent;
import engineer.skyouo.plugins.naturerevive.spigot.webhook.WebhookManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class WebhookRegenListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkRegen(ChunkRegenEvent event) {
        WebhookManager.notifyRegen(event.getChunk(), event.getDateTime());
    }
}
