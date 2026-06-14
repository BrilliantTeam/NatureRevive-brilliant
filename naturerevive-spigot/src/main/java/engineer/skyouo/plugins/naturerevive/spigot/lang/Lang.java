package engineer.skyouo.plugins.naturerevive.spigot.lang;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;

public final class Lang {
    private Lang() {
    }

    public static String get(String key, Object... args) {
        LanguageManager manager = NatureRevivePlugin.languageManager;
        if (manager == null) return key;
        return manager.get(key, args);
    }
}
