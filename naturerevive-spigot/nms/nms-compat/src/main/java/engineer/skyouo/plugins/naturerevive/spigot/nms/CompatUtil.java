package engineer.skyouo.plugins.naturerevive.spigot.nms;

import engineer.skyouo.plugins.naturerevive.common.VersionUtil;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

public class CompatUtil {
    private static HashMap<String, Class<?>> cache = new HashMap<>();

    public static String getCraftBukkitClassName(String className) {
        return Bukkit.getServer().getClass().getPackage().getName() + "." + className;
    }

    public static Object invokeFunction(Object object, String name) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return invokeFunction(object, name, new Class[]{}, new Object[]{});
    }

    public static Object invokeFunction(Object object, Class<?> asType, String name) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return invokeFunction(object, asType, name, new Class[]{}, new Object[]{});
    }

    public static Object invokeFunction(Object object, String name, Class<?>[] types, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return invokeFunction(object, object.getClass(), name, types, args);
    }

    public static Object invokeFunction(Object object, Class<?> asType, String name, Class<?>[] types, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return asType.getDeclaredMethod(name, types).invoke(object, args);
    }

    public static Object invokeConstructor(Class<?> type, Class<?>[] types, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return type.getConstructor(types).newInstance(args);
    }

    public static Object getParameter(Object object, String name) throws NoSuchFieldException, IllegalAccessException {
        return object.getClass().getField(name).get(object);
    }

    public static Class<?> getClassFromName(String name) throws ClassNotFoundException {
        if (cache.containsKey(name))
            return cache.get(name);

        Class<?> clazz = Class.forName(name);
        cache.put(name, clazz);

        return clazz;
    }

    public static boolean isRelocated() {
        return VersionUtil.isAtLeast(1, 20, 5);
    }
}
