package me.fullpage.nmslib.packet;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface DigListener {

    /**
     * Called on the player's netty thread and must not touch world state; return true to drop the packet.
     * face is the Direction 3D data value (0 down, 1 up, 2 north, 3 south, 4 west, 5 east).
     */
    boolean onDig(Player player, DigAction action, int x, int y, int z, int face, int sequence);
}
