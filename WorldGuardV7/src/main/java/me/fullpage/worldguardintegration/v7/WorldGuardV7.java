package me.fullpage.worldguardintegration.v7;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.fullpage.worldguardintegration.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class WorldGuardV7 implements WorldGuardIntegration {

    @Override
    public boolean isSupported() {
        Class<?> aClass;
        try {
            aClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        } catch (ClassNotFoundException e) {
            aClass = null;
        }
        return aClass != null;
    }

    @Override
    public List<String> getRegionsAt(Location location) {

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regions = container.get(new BukkitWorld(location.getWorld()));
        if (regions == null) {
            return new ArrayList<>();
        }

        final ApplicableRegionSet ars = regions.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));

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
        World w = location.getWorld();
        if (w == null) return false;
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(w);
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (!WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, world)) {
            return query.testState(loc, localPlayer, Flags.BUILD);
        }
        return true;
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

        WorldGuard instance = WorldGuard.getInstance();
        HashMap<String, ProtectedRegion> cache = new HashMap<>();

        for (World w : Bukkit.getWorlds()) {
            if (w == null) {
                continue;
            }
            RegionManager regionManager = instance.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));
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

            BlockVector3 blockVector3 = BukkitAdapter.asBlockVector(location);
            if (blockVector3 == null) {
                continue;
            }

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

        WorldGuard instance = WorldGuard.getInstance();

        RegionManager regionManager = instance.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
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

            BlockVector3 blockVector3 = BukkitAdapter.asBlockVector(location);
            if (blockVector3 == null) {
                continue;
            }

            if (!region.contains(blockVector3)) {
                continue;
            }

            players.add(player);

        }
        return players;
    }
}
