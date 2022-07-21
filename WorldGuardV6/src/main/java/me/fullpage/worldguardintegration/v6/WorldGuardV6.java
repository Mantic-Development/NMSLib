package me.fullpage.worldguardintegration.v6;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.fullpage.worldguardintegration.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class WorldGuardV6 implements WorldGuardIntegration {

    @Override
    public boolean isSupported() {
        Class<?> aClass;
        Class<?> aClass2;
        Method method;
        try {
            aClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
            aClass2 = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            method = aClass2.getMethod("getRegionManager", World.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            aClass = null;
            aClass2 = null;
            method = null;
        }
        return aClass != null;
    }

    @Override
    public List<String> getRegionsAt(Location location) {
        final RegionManager rgm = WorldGuardPlugin.inst().getRegionManager(location.getWorld());
        final ApplicableRegionSet ars = rgm.getApplicableRegions(location);

        final List<String> list = new ArrayList<>();
        for (ProtectedRegion region : ars.getRegions()) {
            if (region == null) {
                continue;
            }
            list.add(region.getId());
        }

        return list;
    }

    @Override
    public boolean canBuildAt(Player player, Location location) {
        return WorldGuardPlugin.inst().canBuild(player, location);
    }



    @Override
    public List<Player> getPlayersInRegion(String regionName) {
        List<Player> players = new ArrayList<>();
        if (regionName == null || regionName.isEmpty()) {
            return players;
        }

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return players;
        }

        WorldGuardPlugin instance = WorldGuardPlugin.inst();
        HashMap<String, ProtectedRegion> cache = new HashMap<>();

        for (World w : Bukkit.getWorlds()) {
            if (w == null) {
                continue;
            }
            RegionManager regionManager = instance.getRegionManager(w);
            if (regionManager == null) {
                continue;
            }
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                continue;
            }

            cache.put(w.getName(), region);

        }

        for (Player player : onlinePlayers) {
            if (player == null) {
                continue;
            }

            Location location = player.getLocation();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }
            ProtectedRegion region = cache.get(world.getName());
            if (region == null) {
                continue;
            }

            BlockVector2D blockVector3 = new BlockVector2D(location.getBlockX(), location.getBlockZ());

            if (!region.contains(blockVector3)) {
                continue;
            }

            players.add(player);

        }
        return players;
    }

    @Override
    public List<Player> getPlayersInRegion(World world, String regionName) {
        List<Player> players = new ArrayList<>();
        if (regionName == null || regionName.isEmpty() || world == null) {
            return players;
        }

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return players;
        }

        WorldGuardPlugin instance = WorldGuardPlugin.inst();

        RegionManager regionManager = instance.getRegionManager(world);
        if (regionManager == null) {
            return players;
        }
        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) {
            return players;
        }

        for (Player player : onlinePlayers) {
            if (player == null) {
                continue;
            }

            Location location = player.getLocation();
            World w = location.getWorld();
            if (w == null || !w.getUID().equals(world.getUID())) {
                continue;
            }

            BlockVector2D blockVector3 = new BlockVector2D(location.getBlockX(), location.getBlockZ());

            if (!region.contains(blockVector3)) {
                continue;
            }

            players.add(player);

        }
        return players;
    }

}
