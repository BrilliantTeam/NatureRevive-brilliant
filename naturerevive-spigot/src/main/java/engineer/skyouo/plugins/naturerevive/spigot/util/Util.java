package engineer.skyouo.plugins.naturerevive.spigot.util;

import engineer.skyouo.plugins.naturerevive.common.INMSWrapper;
import engineer.skyouo.plugins.naturerevive.common.VersionUtil;
import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import org.bukkit.Bukkit;

public class Util {
    private final static String nmsWrapperPrefix = "engineer.skyouo.plugins.naturerevive.spigot.nms.";

    public static INMSWrapper getNMSWrapper() {
        INMSWrapper wrapper = getNMSWrapperInternal();

        if (wrapper == null) {
            NatureReviveComponentLogger.warning(Lang.get("console.nms-compat-fallback-1", Bukkit.getBukkitVersion()));
            NatureReviveComponentLogger.warning(Lang.get("console.nms-compat-fallback-2"));

            return (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandlerCompat");
        } else {
            return wrapper;
        }
    }

    public static INMSWrapper getNMSWrapperInternal() {
        int[] versions = VersionUtil.getVersion();

        if (versions[0] == 26 && versions[1] == 1) {
            return (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler26_1_2");
        }

        if (versions[0] == 26 && versions[1] == 2) {
            return (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler26_2");
        }

        switch (versions[1]) {
            case 17:
                return (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_17");
            case 18:
                return (INMSWrapper) (
                        versions[2] == 2 ?
                                getClassAndInit(nmsWrapperPrefix + "NMSHandler1_18_2") :
                                getClassAndInit(nmsWrapperPrefix + "NMSHandler1_18")
                );
            case 19:
                return (INMSWrapper) (
                        versions[2] == 0 ?
                        getClassAndInit(nmsWrapperPrefix + "NMSHandler1_19") :
                        versions[2] == 1 || versions[2] == 2 ?
                        getClassAndInit(nmsWrapperPrefix + "NMSHandler1_19_1") :
                        versions[2] == 3 ?
                        getClassAndInit(nmsWrapperPrefix + "NMSHandler1_19_3") :
                        getClassAndInit(nmsWrapperPrefix + "NMSHandler1_19_4")
                );
            case 20:
                return versions[2] == 2 ?
                    (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_20_2") :
                    versions[2] < 2 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_20_1") :
                    versions[2] < 5 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_20_4") :
                            (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_20_6");
            case 21:
                return versions[2] < 3 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21")
                    : versions[2] == 3 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_3")
                    : versions[2] == 4 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_4")
                    : versions[2] == 5 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_5")
                    : versions[2] <= 8 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_7")
                    : versions[2] <= 10 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_9")
                    : (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_11");
        }

        return null;
    }

    private static Object getClassAndInit(String name) {
        try {
             return Class.forName(name).getConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
