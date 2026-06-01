package engineer.skyouo.plugins.naturerevive.spigot.util;

import org.bukkit.World;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * On Folia, ServerLevel holds a per-thread RegionizedWorldData (stored as a field
 * on TickThreadRunner, NOT a ThreadLocal). FAWE reads tile entities during copyToWorld()
 * from an async thread, which has no region context.
 *
 * For misteln-folia: the underlying server was patched to null-check getCurrentWorldData()
 * in LevelChunk.getBlockEntity(), so FAWE works without injection.
 *
 * For standard Folia: this utility attempts to find and inject RegionizedWorldData via
 * a static ThreadLocal (if one exists) as a best-effort compatibility layer.
 */
public class FoliaRegionContext {

    @SuppressWarnings("rawtypes")
    private static volatile ThreadLocal worldDataTL = null;
    private static volatile boolean initialized = false;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized void initFromWorld(World world) {
        if (initialized) return;
        initialized = true;
        try {
            Object handle = world.getClass().getMethod("getHandle").invoke(world);
            Class<?> cls = handle.getClass();
            while (cls != null && worldDataTL == null) {
                for (Field f : cls.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    if (!ThreadLocal.class.isAssignableFrom(f.getType())) continue;
                    try { f.setAccessible(true); } catch (Throwable ignored) { continue; }
                    ThreadLocal tl = (ThreadLocal) f.get(null);
                    if (tl == null) continue;
                    Object value = tl.get();
                    if (value != null && value.getClass().getName().contains("RegionizedWorldData")) {
                        worldDataTL = tl;
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        } catch (Throwable ignored) {}
    }

    public static Object capture(World world) {
        if (!initialized) initFromWorld(world);
        if (worldDataTL == null) return null;
        return worldDataTL.get();
    }

    @SuppressWarnings("unchecked")
    public static void inject(Object data) {
        if (worldDataTL == null || data == null) return;
        worldDataTL.set(data);
    }

    public static void clear() {
        if (worldDataTL != null)
            worldDataTL.remove();
    }
}
