package me.fullpage.nmslib.v26_2;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import me.fullpage.nmslib.packet.PacketPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class ClientPlayer implements PacketPlayer {

    // Entity keeps these private; ClientPlayerTest pins them.
    static final int SHARED_FLAGS_ID = ClientArmorStand.SHARED_FLAGS_ID, POSE_ID = 6;
    static final int SNEAK_BIT = 1, SPRINT_BIT = 3, SWIM_BIT = 4, GLOWING_BIT = ClientArmorStand.GLOWING_BIT, GLIDE_BIT = 7;
    static final byte ALL_SKIN_LAYERS = 0x7F;
    private static final int SHARED = 1, POSE = 1 << 1;

    private final ServerLevel level;
    private final int id;
    private final UUID uuid = UUID.randomUUID();
    private final GameProfile profile;
    private final ClientboundRemoveEntitiesPacket remove;
    private final ClientboundPlayerInfoRemovePacket removeInfo;
    private final ClientboundAnimatePacket swingMain, swingOff;
    private double x, y, z;
    private float yaw, pitch, headYaw;
    private boolean headYawSet, onGround;
    private long sentX, sentY, sentZ;
    private byte sentHead;
    private boolean moved, resync, swungMain, swungOff;
    private byte sharedFlags;
    private net.minecraft.world.entity.Pose pose = net.minecraft.world.entity.Pose.STANDING;
    private final ItemStack[] equipment = new ItemStack[EquipmentSlot.VALUES.size()];
    private int dirtyData, setData, dirtySlots, setSlots;

    ClientPlayer(ServerLevel level, int id, String name, Skin skin, double x, double y, double z, float yaw, float pitch) {
        if (name == null || name.isEmpty() || !StringUtil.isValidPlayerName(name))
            throw new IllegalArgumentException("Packet player name must be 1-16 characters without spaces or control characters");
        this.level = level;
        this.id = id;
        profile = new GameProfile(uuid, name, skin == null || skin.getValue() == null ? PropertyMap.EMPTY
                : new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", skin.getValue(), skin.getSignature()))));
        remove = new ClientboundRemoveEntitiesPacket(id);
        removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(uuid));
        swingMain = animate(id, ClientboundAnimatePacket.SWING_MAIN_HAND);
        swingOff = animate(id, ClientboundAnimatePacket.SWING_OFF_HAND);
        Arrays.fill(equipment, ItemStack.EMPTY);
        setLocation(x, y, z, yaw, pitch);
        moved = false;
        sentX = encode(x);
        sentY = encode(y);
        sentZ = encode(z);
        sentHead = Mth.packDegrees(yaw);
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
    public String getName() {
        return profile.name();
    }

    @Override
    public void setLocation(double x, double y, double z, float yaw, float pitch) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch))
            throw new IllegalArgumentException("Packet player location must be finite");
        if (x == this.x && y == this.y && z == this.z && yaw == this.yaw && pitch == this.pitch) return;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        moved = true;
    }

    @Override
    public void setHeadYaw(float headYaw) {
        if (!Float.isFinite(headYaw)) throw new IllegalArgumentException("Packet player head yaw must be finite");
        this.headYaw = headYaw;
        headYawSet = true;
    }

    @Override
    public void setOnGround(boolean onGround) {
        if (onGround == this.onGround) return;
        this.onGround = onGround;
        moved = true;
    }

    @Override
    public void setPose(Pose pose) {
        net.minecraft.world.entity.Pose next = switch (Objects.requireNonNull(pose, "pose")) {
            case STANDING -> net.minecraft.world.entity.Pose.STANDING;
            case SNEAKING -> net.minecraft.world.entity.Pose.CROUCHING;
            case SWIMMING -> net.minecraft.world.entity.Pose.SWIMMING;
            case FALL_FLYING -> net.minecraft.world.entity.Pose.FALL_FLYING;
            case SLEEPING -> net.minecraft.world.entity.Pose.SLEEPING;
            case DYING -> net.minecraft.world.entity.Pose.DYING;
        };
        if (next == this.pose) return;
        this.pose = next;
        mark(POSE);
    }

    @Override
    public void setSneaking(boolean sneaking) {
        sharedFlag(SNEAK_BIT, sneaking);
    }

    @Override
    public void setSprinting(boolean sprinting) {
        sharedFlag(SPRINT_BIT, sprinting);
    }

    @Override
    public void setSwimming(boolean swimming) {
        sharedFlag(SWIM_BIT, swimming);
    }

    @Override
    public void setGliding(boolean gliding) {
        sharedFlag(GLIDE_BIT, gliding);
    }

    @Override
    public void setGlowing(boolean glowing) {
        sharedFlag(GLOWING_BIT, glowing);
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
    public void swingArm(boolean mainHand) {
        if (mainHand) swungMain = true;
        else swungOff = true;
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
            if (handle.connection == null) continue;
            handle.connection.send(remove);
            handle.connection.send(removeInfo);
        }
    }

    ClientboundBundlePacket spawnPacket() {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(4);
        // The client drops a player entity whose profile it has not been sent, so the unlisted tab entry goes first.
        packets.add(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                new ClientboundPlayerInfoUpdatePacket.Entry(uuid, profile, false, 0, GameType.DEFAULT_MODE, null, true, 0, null)));
        packets.add(new ClientboundAddEntityPacket(id, uuid, x, y, z, pitch, yaw, EntityTypes.PLAYER, 0, Vec3.ZERO, head()));
        packets.add(new ClientboundSetEntityDataPacket(id, values(setData, true)));
        if (setSlots != 0) packets.add(equipmentPacket(setSlots));
        // Viewers who already have the player hold the last flushed position, so a relative move would split them from this viewer.
        if (moved) resync = true;
        return new ClientboundBundlePacket(packets);
    }

    List<Packet<ClientGamePacketListener>> changes() {
        List<Packet<ClientGamePacketListener>> packets = new ArrayList<>(4);
        if (dirtySlots != 0) packets.add(equipmentPacket(dirtySlots));
        if (dirtyData != 0) packets.add(new ClientboundSetEntityDataPacket(id, values(dirtyData, false)));
        if (moved) packets.add(movePacket());
        byte head = Mth.packDegrees(head());
        if (head != sentHead) {
            sentHead = head;
            packets.add(rotateHead(id, head));
        }
        if (swungMain) packets.add(swingMain);
        if (swungOff) packets.add(swingOff);
        dirtySlots = dirtyData = 0;
        moved = swungMain = swungOff = false;
        return packets;
    }

    private float head() {
        return headYawSet ? headYaw : yaw;
    }

    private Packet<ClientGamePacketListener> movePacket() {
        long nextX = encode(x), nextY = encode(y), nextZ = encode(z);
        long dx = nextX - sentX, dy = nextY - sentY, dz = nextZ - sentZ;
        sentX = nextX;
        sentY = nextY;
        sentZ = nextZ;
        boolean absolute = resync || dx != (short) dx || dy != (short) dy || dz != (short) dz;
        resync = false;
        byte packedYaw = Mth.packDegrees(yaw), packedPitch = Mth.packDegrees(pitch);
        // Not a teleport packet: the client only rebases its relative-move decoding on position sync, like vanilla ServerEntity.
        if (absolute) return new ClientboundEntityPositionSyncPacket(id, new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch), onGround);
        if ((dx | dy | dz) == 0) return new ClientboundMoveEntityPacket.Rot(id, packedYaw, packedPitch, onGround);
        return new ClientboundMoveEntityPacket.PosRot(id, (short) dx, (short) dy, (short) dz, packedYaw, packedPitch, onGround);
    }

    private List<SynchedEntityData.DataValue<?>> values(int mask, boolean spawn) {
        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>(3);
        if ((mask & SHARED) != 0) values.add(new SynchedEntityData.DataValue<>(SHARED_FLAGS_ID, EntityDataSerializers.BYTE, sharedFlags));
        if ((mask & POSE) != 0) values.add(new SynchedEntityData.DataValue<>(POSE_ID, EntityDataSerializers.POSE, pose));
        // Players default to no outer skin layers (hat, jacket, sleeves, trousers).
        if (spawn) values.add(SynchedEntityData.DataValue.create(Avatar.DATA_PLAYER_MODE_CUSTOMISATION, ALL_SKIN_LAYERS));
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

    private void mark(int field) {
        dirtyData |= field;
        setData |= field;
    }

    // Both packets only have Entity constructors, so they are decoded from their wire form.
    private static ClientboundAnimatePacket animate(int id, int action) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(6));
        buf.writeVarInt(id);
        buf.writeByte(action);
        return ClientboundAnimatePacket.STREAM_CODEC.decode(buf);
    }

    private static ClientboundRotateHeadPacket rotateHead(int id, byte head) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(6));
        buf.writeVarInt(id);
        buf.writeByte(head);
        return ClientboundRotateHeadPacket.STREAM_CODEC.decode(buf);
    }

    // Same quantisation as VecDeltaCodec, so relative moves land where the client's own codec puts them.
    private static long encode(double value) {
        return Math.round(value * 4096.0);
    }
}
