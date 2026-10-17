package me.fullpage.nmslib.packet;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.Collection;
import java.util.UUID;

/**
 * A client-only armor stand: no server entity exists, so nothing ticks, tracks, collides or saves it.
 * Setters only record state and skip values that did not change. spawn sends the full current state to new viewers;
 * flush sends what changed since the last flush (moves as relative moves) to the current viewers, building each packet
 * once for all of them. Packets read no world state; the handle is not thread-safe, so use it from the server thread.
 * Clients forget it when its chunk is resent or unloaded and on world changes: spawn it again for them.
 * Clicks on it arrive as Paper's PlayerUseUnknownEntityEvent carrying getEntityId(), without a reach check.
 */
public interface PacketArmorStand {

    /** Allocated from the server's entity id counter, so it never collides with a real entity. */
    int getEntityId();

    UUID getUniqueId();

    /** Moves the stand; armor stands face their yaw. Throws IllegalArgumentException for non-finite values. */
    void setLocation(double x, double y, double z, float yaw);

    /** Legacy section-sign text; null or empty clears it. */
    void setCustomName(String name);

    void setCustomNameVisible(boolean visible);

    void setGlowing(boolean glowing);

    void setInvisible(boolean invisible);

    void setSmall(boolean small);

    void setArms(boolean arms);

    void setBasePlate(boolean basePlate);

    /** Poses are in radians, like ArmorStand's. */
    void setHeadPose(EulerAngle pose);

    void setBodyPose(EulerAngle pose);

    void setLeftArmPose(EulerAngle pose);

    void setRightArmPose(EulerAngle pose);

    void setLeftLegPose(EulerAngle pose);

    void setRightLegPose(EulerAngle pose);

    /** Null or air clears the slot. The item is copied. */
    void setEquipment(EquipmentSlot slot, ItemStack item);

    /** Shows the current state, as one bundle, to viewers in the stand's world; others are skipped. */
    void spawn(Collection<? extends Player> viewers);

    /** Sends the changes since the last flush to the viewers and clears them, also when viewers is empty. */
    void flush(Collection<? extends Player> viewers);

    /** Removes the stand from the viewers' clients; the handle keeps its state and can spawn again. */
    void destroy(Collection<? extends Player> viewers);
}
