package engineer.skyouo.plugins.naturerevive.spigot.webhook;

import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;

public enum FlagReason {
    BLOCK_BREAK("webhook.reason.block-break"),
    BLOCK_PLACE("webhook.reason.block-place"),
    BLOCK_EXPLODE("webhook.reason.block-explode"),
    ENTITY_EXPLODE("webhook.reason.entity-explode"),
    ENTITY_DEATH("webhook.reason.entity-death"),
    BREW("webhook.reason.brew"),
    FURNACE_BURN("webhook.reason.furnace-burn"),
    BLOCK_GROW("webhook.reason.block-grow"),
    BLOCK_REDSTONE("webhook.reason.block-redstone"),
    PLAYER_INTERACT("webhook.reason.player-interact"),
    MANUAL("webhook.reason.manual");

    private final String langKey;

    FlagReason(String langKey) {
        this.langKey = langKey;
    }

    public String getLangKey() {
        return langKey;
    }

    public String getDisplayName() {
        return Lang.get(langKey);
    }
}
