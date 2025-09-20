package engineer.skyouo.plugins.naturerevive.spigot.util;

import engineer.skyouo.plugins.naturerevive.common.INMSWrapper;
import engineer.skyouo.plugins.naturerevive.common.VersionUtil;
import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import org.bukkit.Bukkit;

public class Util {
    private final static String nmsWrapperPrefix = "engineer.skyouo.plugins.naturerevive.spigot.nms.";

    public static INMSWrapper getNMSWrapper() {
        INMSWrapper wrapper = getNMSWrapperInternal();

        if (wrapper == null) {
            NatureReviveComponentLogger.warning("由於 NatureRevive 尚未對該 Paper 版本: %s 原生支援，", Bukkit.getBukkitVersion());
            NatureReviveComponentLogger.warning("將嘗試使用兼容層 NMSHandlerCompat，該模式下，有可能出現錯誤或性能下降。");

            return (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandlerCompat");
        } else {
            return wrapper;
        }
    }

    public static INMSWrapper getNMSWrapperInternal() {
        int[] versions = VersionUtil.getVersion();

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
                    : versions[2] == 3 ? (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_3") :
                        (INMSWrapper) getClassAndInit(nmsWrapperPrefix + "NMSHandler1_21_4");
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
