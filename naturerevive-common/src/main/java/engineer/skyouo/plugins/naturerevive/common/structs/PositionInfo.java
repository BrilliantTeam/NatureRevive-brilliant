package engineer.skyouo.plugins.naturerevive.common.structs;

public class PositionInfo {
    protected String worldName;
    protected int x;
    protected int z;

    protected long ttl;

    public PositionInfo(String worldName, int x, int z, long ttl) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
        this.ttl = ttl;
    }

    public boolean isOverTTL() {
        return isOverTTL(System.currentTimeMillis());
    }

    public boolean isOverTTL(long now) {
        return now > ttl;
    }
}
