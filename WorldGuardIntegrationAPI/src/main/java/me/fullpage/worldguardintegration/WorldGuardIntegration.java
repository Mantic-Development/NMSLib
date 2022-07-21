package me.fullpage.worldguardintegration;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public interface WorldGuardIntegration {

    boolean isSupported();

    List<String> getRegionsAt(Location location);

    boolean canBuildAt(Player player, Location location);

    List<Player> getPlayersInRegion(String region);

    List<Player> getPlayersInRegion(World world, String region);

}
