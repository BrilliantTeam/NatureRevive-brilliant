package engineer.skyouo.plugins.naturerevive.common;

import org.bukkit.Bukkit;

public class VersionUtil {
    private static Boolean isPaperCache;
    public static boolean isPaper() {
        if (isPaperCache != null)
            return isPaperCache;

        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaperCache = true;
            return true;
        } catch (Exception e) {
            isPaperCache = false;
            return false;
        }
    }

    public static int[] getVersion() {
        int[] version = {0, 0, 0};
        String[] splited = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        for (int i = 0; i < splited.length && i < version.length; i++) {
            try {
                version[i] = Integer.parseInt(splited[i]);
            } catch (NumberFormatException e) {
                break;
            }
        }

        return version;
    }

    public static boolean isVersionMinorThan(int major) {
        return getVersion()[1] > major;
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        int[] version = getVersion();
        if (version[0] != major) return version[0] > major;
        if (version[1] != minor) return version[1] > minor;
        return version[2] >= patch;
    }
}