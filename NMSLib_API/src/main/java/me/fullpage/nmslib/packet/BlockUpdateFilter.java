package me.fullpage.nmslib.packet;

import java.util.UUID;

/**
 * Rewrites outbound block updates on the network thread. Implementations must only read thread-safe state.
 */
@FunctionalInterface
public interface BlockUpdateFilter {
    /**
     * Returns a replacement state id, or a negative value to retain the original.
     */
    int replace(UUID player, UUID world, int x, int y, int z, int stateId);
}
