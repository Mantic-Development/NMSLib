package me.fullpage.nmslib.v26_2;

import me.fullpage.nmslib.packet.PacketEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

final class ClientEntity implements PacketEntity {

    private final Entity entity;
    private final AttributeMap attributes;
    private final ClientboundRemoveEntitiesPacket remove;
    private Vec3 sentMotion;
    private double x, y, z;
    private float yaw, pitch;
    private long sentX, sentY, sentZ;
    private boolean moved, resync;

    ClientEntity(Entity entity, double x, double y, double z, float yaw, float pitch) {
        this.entity = entity;
        attributes = entity instanceof LivingEntity living ? living.getAttributes() : null;
        remove = new ClientboundRemoveEntitiesPacket(entity.getId());
        sentMotion = entity.getDeltaMovement();
        setLocation(x, y, z, yaw, pitch);
        moved = false;
        sentX = encode(x);
        sentY = encode(y);
        sentZ = encode(z);
    }

    @Override
    public int getEntityId() {
        return entity.getId();
    }

    @Override
    public org.bukkit.entity.Entity getEntity() {
        return entity.getBukkitEntity();
    }

    @Override
    public void setLocation(double x, double y, double z, float yaw, float pitch) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch))
            throw new IllegalArgumentException("Entity location must be finite");
        if (x == this.x && y == this.y && z == this.z && yaw == this.yaw && pitch == this.pitch) return;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        moved = true;
    }

    @Override
    public void spawn(Collection<? extends Player> viewers) {
        if (viewers.isEmpty()) return;
        ClientboundBundlePacket packet = spawnPacket();
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null && handle.level() == entity.level()) handle.connection.send(packet);
        }
    }

    @Override
    public void flush(Collection<? extends Player> viewers) {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(2);
        changes(packets);
        send(viewers, packets);
    }

    @Override
    public void playEvent(Collection<? extends Player> viewers, byte event) {
        ClientboundEntityEventPacket packet = new ClientboundEntityEventPacket(entity, event);
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null) handle.connection.send(packet);
        }
    }

    @Override
    public void destroy(Collection<? extends Player> viewers) {
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null) handle.connection.send(remove);
        }
    }

    static void flush(Collection<? extends Player> viewers, Collection<? extends PacketEntity> entities) {
        // Called every tick per animation and most ticks change nothing, so the list is made only when something did.
        List<Packet<? super ClientGamePacketListener>> packets = null;
        for (PacketEntity entity : entities) {
            ClientEntity client = (ClientEntity) entity;
            if (!client.changed()) continue;
            if (packets == null) packets = new ArrayList<>(entities.size() * 2);
            client.changes(packets);
        }
        if (packets != null) send(viewers, packets);
    }

    ClientboundBundlePacket spawnPacket() {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(3);
        // Like vanilla Projectile: the owner's id, which the client skips while the projectile leaves it.
        int owner = entity instanceof Projectile projectile && projectile.getOwner() instanceof Entity shooter ? shooter.getId() : 0;
        packets.add(new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), x, y, z, pitch, yaw, entity.getType(), owner, entity.getDeltaMovement(), yaw));
        List<SynchedEntityData.DataValue<?>> data = entity.getEntityData().getNonDefaultValues();
        if (data != null) packets.add(new ClientboundSetEntityDataPacket(entity.getId(), data));
        if (attributes != null) {
            Collection<AttributeInstance> syncable = attributes.getSyncableAttributes();
            if (!syncable.isEmpty()) packets.add(new ClientboundUpdateAttributesPacket(entity.getId(), syncable));
        }
        // Viewers who already have the entity hold the last flushed position, so a relative move would split them from this viewer.
        if (moved) resync = true;
        return new ClientboundBundlePacket(packets);
    }

    void changes(List<Packet<? super ClientGamePacketListener>> out) {
        List<SynchedEntityData.DataValue<?>> data = entity.getEntityData().packDirty();
        if (data != null) out.add(new ClientboundSetEntityDataPacket(entity.getId(), data));
        if (attributes != null) {
            Set<AttributeInstance> dirty = attributes.getAttributesToSync();
            if (!dirty.isEmpty()) {
                out.add(new ClientboundUpdateAttributesPacket(entity.getId(), dirty));
                dirty.clear();
            }
        }
        Vec3 motion = entity.getDeltaMovement();
        if (!motion.equals(sentMotion)) {
            out.add(new ClientboundSetEntityMotionPacket(entity.getId(), motion));
            sentMotion = motion;
        }
        if (moved) out.add(movePacket());
        moved = false;
    }

    private boolean changed() {
        return moved || entity.getEntityData().isDirty() || attributes != null && !attributes.getAttributesToSync().isEmpty()
                || !entity.getDeltaMovement().equals(sentMotion);
    }

    private static void send(Collection<? extends Player> viewers, List<Packet<? super ClientGamePacketListener>> packets) {
        int size = packets.size();
        if (size == 0 || viewers.isEmpty()) return;
        if (size > 1) {
            List<Packet<? super ClientGamePacketListener>> bundles = new ArrayList<>(1 + size / BlockChanges.BUNDLE_LIMIT);
            for (int from = 0; from < size; from += BlockChanges.BUNDLE_LIMIT)
                bundles.add(new ClientboundBundlePacket(packets.subList(from, Math.min(size, from + BlockChanges.BUNDLE_LIMIT))));
            packets = bundles;
        }
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null) for (Packet<? super ClientGamePacketListener> packet : packets) handle.connection.send(packet);
        }
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
        return absolute ? new ClientboundEntityPositionSyncPacket(entity.getId(), new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch), false)
                : new ClientboundMoveEntityPacket.PosRot(entity.getId(), (short) dx, (short) dy, (short) dz, Mth.packDegrees(yaw), Mth.packDegrees(pitch), false);
    }

    // Same quantisation as VecDeltaCodec, so relative moves land where the client's own codec puts them.
    private static long encode(double value) {
        return Math.round(value * 4096.0);
    }
}
