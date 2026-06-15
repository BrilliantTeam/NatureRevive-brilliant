package engineer.skyouo.plugins.naturerevive.spigot.webhook;

import org.bukkit.Location;

public final class FlaggedLocation {
    private final Location location;
    private final FlagReason reason;

    public FlaggedLocation(Location location, FlagReason reason) {
        this.location = location;
        this.reason = reason;
    }

    public Location getLocation() {
        return location;
    }

    public FlagReason getReason() {
        return reason;
    }
}
