package me.fullpage.nmslib.packet;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface UseItemOnListener {

    /** Netty thread; must not touch world state; return true to drop the packet. face = Direction 3D data value. */
    boolean onUseItemOn(Player player, int x, int y, int z, int face, int sequence, boolean mainHand);
}
