package engineer.skyouo.plugins.naturerevive.spigot.managers;

import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExpiryIndexSelfCheck {

    private ExpiryIndexSelfCheck() {
    }

    private static final Map<String, BukkitPositionInfo> CACHE = new HashMap<>();

    private static BukkitPositionInfo pos(String world, int x, long ttl) {
        return new BukkitPositionInfo(world, x, 0, ttl);
    }

    private static void mark(BukkitPositionInfo positionInfo) {
        if (CACHE.put(positionInfo.getChunkKey(), positionInfo) == null) {
            ExpiryIndex.add(positionInfo);
        }
    }

    private static void unmark(BukkitPositionInfo positionInfo) {
        CACHE.remove(positionInfo.getChunkKey());
    }

    private static void reschedule(BukkitPositionInfo positionInfo) {
        CACHE.put(positionInfo.getChunkKey(), positionInfo);
        ExpiryIndex.add(positionInfo);
    }

    private static void reset() {
        CACHE.clear();
        ExpiryIndex.clear();
    }

    public static void main(String[] args) {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (!assertionsEnabled) {
            throw new IllegalStateException("Must be run with -ea, otherwise the assertions do nothing.");
        }

        ExpiryIndex.liveLookup = positionInfo -> CACHE.get(positionInfo.getChunkKey());

        onlyExpiredComeOut();
        expiredComeOutInTtlOrder();
        unmarkedAreDiscarded();
        refreshedAreRescheduledNotLost();
        mutatedTtlIsDetected();
        heapDoesNotGrowPerMark();
        maxIsRespectedAndRemainderSurvives();

        System.out.println("ExpiryIndex self-check: all checks passed.");
    }

    private static void onlyExpiredComeOut() {
        reset();
        mark(pos("world", 1, 500));
        mark(pos("world", 2, 1_500));

        List<BukkitPositionInfo> out = ExpiryIndex.drainExpired(1_000, 64);

        assert out.size() == 1 : "expected 1 expired, got " + out.size();
        assert out.get(0).getX() == 1 : "drained the wrong chunk";
        assert ExpiryIndex.size() == 1 : "the unexpired entry must not leave the index";
    }

    private static void expiredComeOutInTtlOrder() {
        reset();
        mark(pos("world", 1, 300));
        mark(pos("world", 2, 100));
        mark(pos("world", 3, 200));

        List<BukkitPositionInfo> out = ExpiryIndex.drainExpired(1_000, 64);

        assert out.size() == 3 : "expected 3, got " + out.size();
        assert out.get(0).getTTL() == 100 && out.get(1).getTTL() == 200 && out.get(2).getTTL() == 300
                : "not drained in TTL order";
    }

    private static void unmarkedAreDiscarded() {
        reset();
        BukkitPositionInfo gone = pos("world", 1, 100);
        mark(gone);
        mark(pos("world", 2, 200));

        unmark(gone);

        List<BukkitPositionInfo> out = ExpiryIndex.drainExpired(1_000, 64);

        assert out.size() == 1 : "unmarked chunk was handed out, got " + out.size();
        assert out.get(0).getX() == 2 : "drained the wrong chunk";
        assert ExpiryIndex.size() == 0 : "index should be empty";
    }

    private static void refreshedAreRescheduledNotLost() {
        reset();
        mark(pos("world", 1, 100));
        mark(pos("world", 1, 5_000));

        List<BukkitPositionInfo> out = ExpiryIndex.drainExpired(1_000, 64);

        assert out.isEmpty() : "TTL was pushed forward, must not be drained yet";
        assert ExpiryIndex.size() == 1 : "current version must stay in the index, size = " + ExpiryIndex.size();

        List<BukkitPositionInfo> later = ExpiryIndex.drainExpired(6_000, 64);

        assert later.size() == 1 : "should be drained once actually expired, got " + later.size();
        assert later.get(0).getTTL() == 5_000 : "drained a stale version, TTL = " + later.get(0).getTTL();
    }

    private static void mutatedTtlIsDetected() {
        reset();
        BukkitPositionInfo mutable = pos("world", 1, 100);
        mark(mutable);

        mutable.setTTL(5_000);

        assert ExpiryIndex.drainExpired(1_000, 64).isEmpty() : "mutated TTL was not detected";

        List<BukkitPositionInfo> later = ExpiryIndex.drainExpired(6_000, 64);
        assert later.size() == 1 : "rescheduled entry was lost after mutation";
        assert later.get(0).getTTL() == 5_000 : "wrong TTL after mutation";
    }

    private static void heapDoesNotGrowPerMark() {
        reset();
        for (int i = 0; i < 1_000; i++) {
            mark(pos("world", 7, 1_000 + i));
        }

        assert ExpiryIndex.size() == 1 : "heap should hold 1 entry after 1000 marks, got " + ExpiryIndex.size();
    }

    private static void maxIsRespectedAndRemainderSurvives() {
        reset();
        for (int i = 0; i < 10; i++) {
            mark(pos("world", i, 100 + i));
        }

        List<BukkitPositionInfo> first = ExpiryIndex.drainExpired(1_000, 4);
        assert first.size() == 4 : "max not honoured, got " + first.size();
        assert ExpiryIndex.size() == 6 : "remainder must stay indexed, got " + ExpiryIndex.size();

        List<BukkitPositionInfo> second = ExpiryIndex.drainExpired(1_000, 64);
        assert second.size() == 6 : "second pass should drain the rest, got " + second.size();
        assert ExpiryIndex.size() == 0 : "index should be empty";

        reschedule(pos("world", 0, 50));
        assert ExpiryIndex.size() == 1 : "reschedule did not reach the index";
        assert ExpiryIndex.drainExpired(1_000, 64).size() == 1 : "rescheduled entry could not be drained again";
    }
}
