package me.fullpage.nmslib.packet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * A client-only entity of any type: a server entity that is never added to a world, so nothing ticks, tracks, collides or
 * saves it. Change it through getEntity()'s setters (display transformation and interpolation, item, block, text, brightness,
 * glow, pose, attributes such as scale, velocity); its synched data records what changed. spawn sends the full current state
 * to new viewers; flush sends what changed since the last flush (moves as relative moves) to the current viewers, building each
 * packet once for all of them. A keyframe is the setters plus setInterpolationDelay(0), which restarts interpolation even when
 * the delay is unchanged. Clients move projectiles by their velocity themselves (the handle's location stays put) and skip
 * their shooter (setShooter) while leaving it.
 * Server thread only. Clients drop it when they change worlds: spawn it again for them.
 */
public interface PacketEntity {

    /** Allocated from the server's entity id counter, so it never collides with a real entity. */
    int getEntityId();

    /** The entity's Bukkit view, for its setters: it is in no world, so never teleport, remove or add it. */
    Entity getEntity();

    /** Throws IllegalArgumentException for non-finite values. */
    void setLocation(double x, double y, double z, float yaw, float pitch);

    /** Shows the current state, as one bundle, to viewers in the entity's world; others are skipped. */
    void spawn(Collection<? extends Player> viewers);

    /** Sends the changes since the last flush to the viewers and clears them, also when viewers is empty. */
    void flush(Collection<? extends Player> viewers);

    /** Plays a vanilla entity event (e.g. 60, death smoke) on the viewers' clients. */
    void playEvent(Collection<? extends Player> viewers, byte event);

    /** Removes the entity from the viewers' clients; the handle keeps its state and can spawn again. */
    void destroy(Collection<? extends Player> viewers);
}
