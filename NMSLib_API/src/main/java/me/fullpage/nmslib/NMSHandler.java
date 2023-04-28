package me.fullpage.nmslib;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;

public interface NMSHandler {

    void sendActionBar(Player player, String message);

    void sendJsonMessage(Player player, String json);

}
