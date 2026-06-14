package engineer.skyouo.plugins.naturerevive.spigot.integration;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.api.IIntegrationManager;
import engineer.skyouo.plugins.naturerevive.spigot.integration.engine.DefaultEngineIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.integration.engine.FAWEIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.integration.land.GriefDefenderIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.integration.land.GriefPreventionIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.integration.land.ResidenceIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.integration.logging.CoreProtectIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.lang.Lang;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntegrationManager implements IIntegrationManager {
    private static final Set<IDependency> dependencies = new HashSet<>();
    private static final Set<IDependency> builtinDependencies = new HashSet<>();
    static {
        builtinDependencies.add(new CoreProtectIntegration());
        builtinDependencies.add(new ResidenceIntegration());
        builtinDependencies.add(new GriefDefenderIntegration());
        builtinDependencies.add(new GriefPreventionIntegration());
        builtinDependencies.add(new DefaultEngineIntegration());
        builtinDependencies.add(new FAWEIntegration());
    }

    @Override
    public boolean init(Plugin plugin) {
        for (IDependency dependency : builtinDependencies) {
            boolean result = false;
            try {
                result = registerIntegration(plugin, dependency);
            } catch (Exception ignored) {
                unregisterIntegration(plugin, dependency);
            }

            if (result) {
                NatureReviveComponentLogger.info(
                        Lang.get("console.integration-loaded", dependency.getPluginName())
                );

                if (!dependency.getType().equals(IDependency.Type.LAND) && !dependency.isEnabled()) {
                    NatureReviveComponentLogger.info(
                            Lang.get("console.integration-found-not-enabled", dependency.getPluginName())
                    );
                }
            }

            if (!result && dependency.shouldExitOnFatal()) {
                NatureReviveComponentLogger.error(
                        Lang.get("console.integration-required-missing", dependency.getPluginName())
                );

                NatureReviveComponentLogger.warning(Lang.get("console.integration-required-missing-hint"));
                return false;
            }
        }

        // builtinDependencies.clear();
        return true;
    }

    @Override
    public @Nullable IDependency getAvailableDependency(String name) {
        for (IDependency dependency : dependencies) {
            if (dependency.getPluginName().equals(name))
                return dependency;
        }

        return null;
    }

    @Override
    public @Nullable IDependency getAvailableDependency(IDependency.Type type) {
        for (IDependency dependency : dependencies) {
            if (dependency.getType().equals(type))
                return dependency;
        }

        return null;
    }

    @Override
    public List<IDependency> getAvailableDependencies(IDependency.Type type) {
        List<IDependency> result = new ArrayList<>();

        for (IDependency dependency : dependencies) {
            if (dependency.getType().equals(type))
                result.add(dependency);
        }

        return result;
    }

    @Override
    public boolean registerIntegration(Plugin plugin, IDependency dependency) {
        if (dependencies.contains(dependency))
            return false;

        NatureReviveComponentLogger.debug(
                "Plugin %s tried to register integration of %s.", TextColor.fromHexString("#AAAAAA"), plugin.getName(), dependency.getPluginName()
        );

        if (!dependency.load()) {
            NatureReviveComponentLogger.debug(
                    "Plugin %s failed to register integration of %s.", TextColor.fromHexString("#AAAAAA"), plugin.getName(), dependency.getPluginName()
            );

            return false;
        }

        return dependencies.add(dependency);
    }

    @Override
    public boolean unregisterIntegration(Plugin plugin, IDependency dependency) {
        if (!dependencies.contains(dependency))
            return false;

        NatureReviveComponentLogger.debug(
                "Plugin %s tried to unregister integration of %s.", TextColor.fromHexString("#AAAAAA"), plugin.getName(), dependency.getPluginName()
        );

        return dependencies.remove(dependency);
    }

    public void clearDependency() {
        dependencies.clear();
    }
}
