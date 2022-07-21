package me.fullpage.worldguardintegrationplugin;

import me.fullpage.worldguardintegration.WorldGuardIntegration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class WorldGuardIntegrationPlugin extends JavaPlugin implements WorldGuardIntegration {

    @Override
    public void onEnable() {
        // Plugin startup logic
        WorldGuardIntegrationLib.init(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean isSupported() {
        return WorldGuardIntegrationLib.getLib().isSupported();
    }

    @Override
    public List<String> getRegionsAt(Location location) {
        return WorldGuardIntegrationLib.getLib().getRegionsAt(location);
    }

    @Override
    public boolean canBuildAt(Player player, Location location) {
        return WorldGuardIntegrationLib.getLib().canBuildAt(player, location);
    }

    @Override
    public List<Player> getPlayersInRegion(String region) {
        return WorldGuardIntegrationLib.getLib().getPlayersInRegion(region);
    }

    @Override
    public List<Player> getPlayersInRegion(World world, String region) {
        return WorldGuardIntegrationLib.getLib().getPlayersInRegion(world, region);
    }
}
