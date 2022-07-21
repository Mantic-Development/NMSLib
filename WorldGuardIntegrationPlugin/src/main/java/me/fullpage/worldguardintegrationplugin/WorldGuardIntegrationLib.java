package me.fullpage.worldguardintegrationplugin;

import me.fullpage.worldguardintegration.WorldGuardIntegration;
import me.fullpage.worldguardintegration.v6.WorldGuardV6;
import me.fullpage.worldguardintegration.v7.WorldGuardV7;
import org.bukkit.plugin.Plugin;

public class WorldGuardIntegrationLib {

    private static WorldGuardIntegration instance;

    public static WorldGuardIntegration init(Plugin plugin) {
        WorldGuardV6 worldGuardV6 = new WorldGuardV6();
        if (worldGuardV6.isSupported()) {
            plugin.getLogger().info("WorldGuard v6 detected, using it.");
            instance = worldGuardV6;
            return instance;
        }

        WorldGuardV7 worldGuardV7 = new WorldGuardV7();
        if (worldGuardV7.isSupported()) {
            plugin.getLogger().info("WorldGuard v7 detected, using it.");
            instance = worldGuardV7;
            return instance;
        }
        return null;
    }

    public static WorldGuardIntegration getLib() {
        if (instance == null) {
            throw new IllegalStateException("WorldGuardIntegrationLib is not initialized");
        }
        return instance;
    }
}
