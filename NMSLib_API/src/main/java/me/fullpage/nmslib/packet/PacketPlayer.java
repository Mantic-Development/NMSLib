package me.fullpage.nmslib.packet;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.UUID;

/**
 * A client-only player: no server entity or ServerPlayer exists, so nothing ticks, tracks, collides, saves or appears in
 * the server's player list. Same model as PacketArmorStand: setters only record state and skip values that did not change;
 * spawn sends the full current state to new viewers (an unlisted tab-list entry carrying the name and skin, then the entity,
 * its metadata, head rotation and equipment, as one bundle); flush sends what changed since the last flush (moves as
 * relative moves, a teleport when a move is too long) to the current viewers, building each packet once for all of them.
 * Server thread only. Clients drop it when they change worlds: spawn it again for them.
 */
public interface PacketPlayer {

    /** Allocated from the server's entity id counter, so it never collides with a real entity. */
    int getEntityId();

    /** The profile id sent to clients: random (version 4), never an online player's id, so skins and tab entries cannot clash. */
    UUID getUniqueId();

    /** Profile name shown above the head, at most 16 characters. */
    String getName();

    /** Feet position, body yaw and pitch in degrees. Throws IllegalArgumentException for non-finite values. */
    void setLocation(double x, double y, double z, float yaw, float pitch);

    /** Head yaw in degrees; follows the body yaw until set. */
    void setHeadYaw(float headYaw);

    void setOnGround(boolean onGround);

    void setPose(Pose pose);

    /** Shared flags: crouching (0x02), sprinting (0x08), swimming (0x10), gliding (0x80). */
    void setSneaking(boolean sneaking);

    void setSprinting(boolean sprinting);

    void setSwimming(boolean swimming);

    void setGliding(boolean gliding);

    void setGlowing(boolean glowing);

    /** Null or air clears the slot. The item is copied. */
    void setEquipment(EquipmentSlot slot, ItemStack item);

    /** Queues an arm swing, sent with the next flush. */
    void swingArm(boolean mainHand);

    /** Shows the current state to viewers in the player's world; others are skipped. */
    void spawn(Collection<? extends Player> viewers);

    /** Sends the changes since the last flush to the viewers and clears them, also when viewers is empty. */
    void flush(Collection<? extends Player> viewers);

    /** Removes the entity and its tab-list entry from the viewers' clients; the handle keeps its state and can spawn again. */
    void destroy(Collection<? extends Player> viewers);

    /** Entity poses a replay needs (the 1.13 API this module compiles against has no Pose enum). */
    enum Pose { STANDING, SNEAKING, SWIMMING, FALL_FLYING, SLEEPING, DYING }

    /** Signed textures property as Mojang's session server returns it; null value means the default skin. */
    final class Skin {
        private final String value;
        private final String signature;

        public Skin(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }

        /** Base64 textures JSON. */
        public String getValue() {
            return value;
        }

        /** Base64 signature, or null when unsigned (clients then ignore the skin). */
        public String getSignature() {
            return signature;
        }
    }
}
