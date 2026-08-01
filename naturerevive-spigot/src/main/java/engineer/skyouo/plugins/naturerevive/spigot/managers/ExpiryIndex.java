package engineer.skyouo.plugins.naturerevive.spigot.managers;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Function;

public final class ExpiryIndex {

    private ExpiryIndex() {
    }

    private record Entry(BukkitPositionInfo pos, long ttl) {
    }

    private static final PriorityBlockingQueue<Entry> HEAP =
            new PriorityBlockingQueue<>(1024, Comparator.comparingLong(Entry::ttl));

    static Function<BukkitPositionInfo, BukkitPositionInfo> liveLookup =
            positionInfo -> NatureRevivePlugin.databaseConfig.get(positionInfo);

    public static void add(BukkitPositionInfo positionInfo) {
        if (positionInfo == null) return;
        HEAP.add(new Entry(positionInfo, positionInfo.getTTL()));
    }

    public static void seed(Collection<BukkitPositionInfo> all) {
        HEAP.clear();
        if (all == null) return;
        for (BukkitPositionInfo positionInfo : all) {
            add(positionInfo);
        }
    }

    public static int size() {
        return HEAP.size();
    }

    public static void clear() {
        HEAP.clear();
    }

    public static List<BukkitPositionInfo> drainExpired(long now, int max) {
        List<BukkitPositionInfo> out = new ArrayList<>();

        int steps = 0;
        int budget = Math.max(max, 16) * 4;

        while (out.size() < max && steps++ < budget) {
            Entry head = HEAP.peek();
            if (head == null || head.ttl() > now) break;

            Entry popped = HEAP.poll();
            if (popped == null) break;

            BukkitPositionInfo live = liveLookup.apply(popped.pos());
            if (live == null) continue;

            if (live.getTTL() != popped.ttl()) {
                HEAP.add(new Entry(live, live.getTTL()));
                continue;
            }

            out.add(live);
        }

        return out;
    }
}
