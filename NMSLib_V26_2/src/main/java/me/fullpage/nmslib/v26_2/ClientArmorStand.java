package me.fullpage.nmslib.v26_2;

import com.mojang.datafixers.util.Pair;
import me.fullpage.nmslib.packet.PacketArmorStand;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class ClientArmorStand implements PacketArmorStand {

    // Entity keeps these accessors private; ClientArmorStandTest pins the ids.
    static final int SHARED_FLAGS_ID = 0, CUSTOM_NAME_ID = 2, CUSTOM_NAME_VISIBLE_ID = 3, GLOWING_BIT = 6;
    private static final int SHARED = 1, NAME = 1 << 1, NAME_VISIBLE = 1 << 2, CLIENT = 1 << 3, FIRST_POSE = 4;
    private static final List<EntityDataAccessor<Rotations>> POSES = List.of(ArmorStand.DATA_HEAD_POSE, ArmorStand.DATA_BODY_POSE,
            ArmorStand.DATA_LEFT_ARM_POSE, ArmorStand.DATA_RIGHT_ARM_POSE, ArmorStand.DATA_LEFT_LEG_POSE, ArmorStand.DATA_RIGHT_LEG_POSE);
    private static final Rotations[] DEFAULT_POSES = {ArmorStand.DEFAULT_HEAD_POSE, ArmorStand.DEFAULT_BODY_POSE, ArmorStand.DEFAULT_LEFT_ARM_POSE,
            ArmorStand.DEFAULT_RIGHT_ARM_POSE, ArmorStand.DEFAULT_LEFT_LEG_POSE, ArmorStand.DEFAULT_RIGHT_LEG_POSE};

    private final ServerLevel level;
    private final int id;
    private final UUID uuid = UUID.randomUUID();
    private final ClientboundRemoveEntitiesPacket remove;
    private double x, y, z;
    private float yaw;
    private long sentX, sentY, sentZ;
    private boolean moved, resync;
    private byte sharedFlags, clientFlags;
    private String nameText;
    private Component name;
    private boolean nameVisible;
    private final Rotations[] poses = DEFAULT_POSES.clone();
    private final ItemStack[] equipment = new ItemStack[EquipmentSlot.VALUES.size()];
    private int dirtyData, setData, dirtySlots, setSlots;

    ClientArmorStand(ServerLevel level, int id, double x, double y, double z, float yaw) {
        this.level = level;
        this.id = id;
        remove = new ClientboundRemoveEntitiesPacket(id);
        Arrays.fill(equipment, ItemStack.EMPTY);
        setLocation(x, y, z, yaw);
        moved = false;
        sentX = encode(x);
        sentY = encode(y);
        sentZ = encode(z);
    }

    @Override
    public int getEntityId() {
        return id;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public void setLocation(double x, double y, double z, float yaw) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw))
            throw new IllegalArgumentException("Armor stand location must be finite");
        if (x == this.x && y == this.y && z == this.z && yaw == this.yaw) return;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        moved = true;
    }

    @Override
    public void setCustomName(String text) {
        if (Objects.equals(text, nameText)) return;
        nameText = text;
        name = CraftChatMessage.fromStringOrNull(text);
        mark(NAME);
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        if (visible == nameVisible) return;
        nameVisible = visible;
        mark(NAME_VISIBLE);
    }

    @Override
    public void setGlowing(boolean glowing) {
        sharedFlag(GLOWING_BIT, glowing);
    }

    @Override
    public void setInvisible(boolean invisible) {
        sharedFlag(Entity.FLAG_INVISIBLE, invisible);
    }

    @Override
    public void setSmall(boolean small) {
        clientFlag(ArmorStand.CLIENT_FLAG_SMALL, small);
    }

    @Override
    public void setArms(boolean arms) {
        clientFlag(ArmorStand.CLIENT_FLAG_SHOW_ARMS, arms);
    }

    @Override
    public void setBasePlate(boolean basePlate) {
        clientFlag(ArmorStand.CLIENT_FLAG_NO_BASEPLATE, !basePlate);
    }

    @Override
    public void setHeadPose(EulerAngle pose) {
        pose(0, pose);
    }

    @Override
    public void setBodyPose(EulerAngle pose) {
        pose(1, pose);
    }

    @Override
    public void setLeftArmPose(EulerAngle pose) {
        pose(2, pose);
    }

    @Override
    public void setRightArmPose(EulerAngle pose) {
        pose(3, pose);
    }

    @Override
    public void setLeftLegPose(EulerAngle pose) {
        pose(4, pose);
    }

    @Override
    public void setRightLegPose(EulerAngle pose) {
        pose(5, pose);
    }

    @Override
    public void setEquipment(org.bukkit.inventory.EquipmentSlot slot, org.bukkit.inventory.ItemStack item) {
        int index = CraftEquipmentSlot.getNMS(Objects.requireNonNull(slot, "slot")).ordinal();
        ItemStack next = CraftItemStack.asNMSCopy(item);
        if (ItemStack.matches(equipment[index], next)) return;
        equipment[index] = next;
        dirtySlots |= 1 << index;
        setSlots |= 1 << index;
    }

    @Override
    public void spawn(Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;
        ClientboundBundlePacket packet = spawnPacket();
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null && handle.level() == level) handle.connection.send(packet);
        }
    }

    @Override
    public void flush(Collection<? extends Player> viewers) {
        List<Packet<ClientGamePacketListener>> packets = changes();
        if (packets.isEmpty()) return;
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null) for (Packet<ClientGamePacketListener> packet : packets) handle.connection.send(packet);
        }
    }

    @Override
    public void destroy(Collection<? extends Player> viewers) {
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null) handle.connection.send(remove);
        }
    }

    ClientboundBundlePacket spawnPacket() {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(3);
        packets.add(new ClientboundAddEntityPacket(id, uuid, x, y, z, 0, yaw, EntityTypes.ARMOR_STAND, 0, Vec3.ZERO, yaw));
        if (setData != 0) packets.add(new ClientboundSetEntityDataPacket(id, values(setData)));
        if (setSlots != 0) packets.add(equipmentPacket(setSlots));
        // Viewers who already have the stand hold the last flushed position, so a relative move would split them from this viewer.
        if (moved) resync = true;
        return new ClientboundBundlePacket(packets);
    }

    List<Packet<ClientGamePacketListener>> changes() {
        List<Packet<ClientGamePacketListener>> packets = new ArrayList<>(3);
        if (dirtySlots != 0) packets.add(equipmentPacket(dirtySlots));
        if (dirtyData != 0) packets.add(new ClientboundSetEntityDataPacket(id, values(dirtyData)));
        if (moved) packets.add(movePacket());
        dirtySlots = dirtyData = 0;
        moved = false;
        return packets;
    }

    private Packet<ClientGamePacketListener> movePacket() {
        long nextX = encode(x), nextY = encode(y), nextZ = encode(z);
        long dx = nextX - sentX, dy = nextY - sentY, dz = nextZ - sentZ;
        sentX = nextX;
        sentY = nextY;
        sentZ = nextZ;
        boolean absolute = resync || dx != (short) dx || dy != (short) dy || dz != (short) dz;
        resync = false;
        // Not a teleport packet: the client only rebases its relative-move decoding on position sync, like vanilla ServerEntity.
        return absolute ? new ClientboundEntityPositionSyncPacket(id, new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, 0), false)
                : new ClientboundMoveEntityPacket.PosRot(id, (short) dx, (short) dy, (short) dz, Mth.packDegrees(yaw), (byte) 0, false);
    }

    private List<SynchedEntityData.DataValue<?>> values(int mask) {
        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>(Integer.bitCount(mask));
        if ((mask & SHARED) != 0) values.add(new SynchedEntityData.DataValue<>(SHARED_FLAGS_ID, EntityDataSerializers.BYTE, sharedFlags));
        if ((mask & NAME) != 0) values.add(new SynchedEntityData.DataValue<>(CUSTOM_NAME_ID, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.ofNullable(name)));
        if ((mask & NAME_VISIBLE) != 0) values.add(new SynchedEntityData.DataValue<>(CUSTOM_NAME_VISIBLE_ID, EntityDataSerializers.BOOLEAN, nameVisible));
        if ((mask & CLIENT) != 0) values.add(SynchedEntityData.DataValue.create(ArmorStand.DATA_CLIENT_FLAGS, clientFlags));
        for (int part = 0; part < POSES.size(); part++)
            if ((mask & 1 << (FIRST_POSE + part)) != 0) values.add(SynchedEntityData.DataValue.create(POSES.get(part), poses[part]));
        return values;
    }

    private ClientboundSetEquipmentPacket equipmentPacket(int mask) {
        List<Pair<EquipmentSlot, ItemStack>> slots = new ArrayList<>(Integer.bitCount(mask));
        for (EquipmentSlot slot : EquipmentSlot.VALUES) if ((mask & 1 << slot.ordinal()) != 0) slots.add(Pair.of(slot, equipment[slot.ordinal()]));
        return new ClientboundSetEquipmentPacket(id, slots);
    }

    private void sharedFlag(int bit, boolean on) {
        byte next = (byte) (on ? sharedFlags | 1 << bit : sharedFlags & ~(1 << bit));
        if (next == sharedFlags) return;
        sharedFlags = next;
        mark(SHARED);
    }

    private void clientFlag(int flag, boolean on) {
        byte next = (byte) (on ? clientFlags | flag : clientFlags & ~flag);
        if (next == clientFlags) return;
        clientFlags = next;
        mark(CLIENT);
    }

    private void pose(int part, EulerAngle angle) {
        Rotations next = new Rotations((float) Math.toDegrees(angle.getX()), (float) Math.toDegrees(angle.getY()), (float) Math.toDegrees(angle.getZ()));
        if (next.equals(poses[part])) return;
        poses[part] = next;
        mark(1 << (FIRST_POSE + part));
    }

    private void mark(int field) {
        dirtyData |= field;
        setData |= field;
    }

    // Same quantisation as VecDeltaCodec, so relative moves land where the client's own codec puts them.
    private static long encode(double value) {
        return Math.round(value * 4096.0);
    }
}
