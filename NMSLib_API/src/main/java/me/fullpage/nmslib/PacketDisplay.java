package me.fullpage.nmslib;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.*;
import java.util.*;

/**
 * A detached client-only model. Prepare updates once and send them to one or many viewers on the server thread.
 * No entity is added to a world. Keep the model until all viewers have received its removal update.
 */
public final class PacketDisplay {
    private static Backend backend;
    private static String failure;
    private final Backend transport;
    private final Entity entity;
    private final Object handle;
    private final World world;
    private final boolean item, modern;
    private Update spawn;
    private boolean spawnDirty;
    private final Update remove, unchanged;
    private Location location;
    private String text;
    private ItemStack stack;

    private PacketDisplay(Backend transport, Location location, String text, ItemStack stack) throws ReflectiveOperationException {
        this.transport = transport; this.world = location.getWorld(); this.item = stack != null;
        Class<?> kind = transport.displayClass(item);
        modern = kind != ArmorStand.class;
        Object created = transport.create.invoke(world, nativeLocation(location), kind);
        entity = created instanceof Entity ? (Entity) created : (Entity) transport.bukkitEntity.invoke(created);
        handle = entity.getClass().getMethod("getHandle").invoke(entity);
        unchanged = new Update(world, Collections.emptyList());
        this.location = location.clone();
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(item);
            try { ArmorStand.class.getMethod("setMarker", boolean.class).invoke(stand, true); }
            catch (NoSuchMethodException ignored) { }
            if (item) stand.getEquipment().setHelmet(stack.clone());
            else { stand.setCustomName(text); stand.setCustomNameVisible(true); }
        } else if (item) {
            kind.getMethod("setItemStack", ItemStack.class).invoke(entity, stack.clone());
            try { kind.getMethod("setTeleportDuration", int.class).invoke(entity, 2); }
            catch (NoSuchMethodException ignored) { }
        } else {
            kind.getMethod("setText", String.class).invoke(entity, text);
            Class<?> billboard = Class.forName("org.bukkit.entity.Display$Billboard");
            kind.getMethod("setBillboard", billboard).invoke(entity, Enum.valueOf((Class) billboard, "CENTER"));
            kind.getMethod("setDefaultBackground", boolean.class).invoke(entity, false);
            kind.getMethod("setShadowed", boolean.class).invoke(entity, true);
        }
        this.text = text; this.stack = stack == null ? null : stack.clone();
        List<Object> packets = new ArrayList<>();
        packets.add(transport.spawn(handle, entity, nativeLocation(location), modern));
        packets.add(transport.metadata(handle, entity.getEntityId(), true));
        if (item && !modern) packets.add(transport.equipment(entity.getEntityId(), stack));
        spawn = new Update(world, packets);
        remove = new Update(world, Collections.singletonList(transport.destroy.newInstance((Object) new int[]{entity.getEntityId()})));
    }

    /**
     * Returns null when this server cannot prepare the model. getSupportFailure() explains an unsupported mapping.
     */
    public static PacketDisplay item(Location location, ItemStack item) { return create(location, null, Objects.requireNonNull(item, "item").clone()); }
    public static PacketDisplay text(Location location, String text) { return create(location, Objects.requireNonNull(text, "text"), null); }

    private static PacketDisplay create(Location location, String text, ItemStack item) {
        requireThread(); validate(location);
        if (failure != null) return null;
        try {
            if (backend == null) backend = new Backend(location.getWorld());
            return new PacketDisplay(backend, location, text, item);
        } catch (ReflectiveOperationException | LinkageError error) {
            failure = error.toString(); return null;
        }
    }

    public static String getSupportFailure() { return failure; }
    public int getEntityId() { return entity.getEntityId(); }
    public Update spawn() {
        requireThread();
        if (spawnDirty) try {
            List<Object> packets = new ArrayList<>();
            packets.add(transport.spawn(handle, entity, nativeLocation(location), modern));
            packets.add(transport.metadata(handle, getEntityId(), true));
            if (item && !modern) packets.add(transport.equipment(getEntityId(), stack));
            spawn = new Update(world, packets); spawnDirty = false;
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot prepare display spawn", error); }
        return spawn;
    }
    public Update remove() { requireThread(); return remove; }

    /**
     * Moves the existing ID without respawning. Locations must stay in the model's world.
     */
    public Update move(Location destination) {
        requireThread(); validate(destination);
        if (destination.getWorld() != world) throw new IllegalArgumentException("Display updates cannot change worlds");
        if (location.equals(destination)) return unchanged;
        try {
            Location point = nativeLocation(destination);
            transport.position.invoke(handle, point.getX(), point.getY(), point.getZ(), point.getYaw(), point.getPitch());
            location = destination.clone();
            spawnDirty = true;
            return new Update(world, Collections.singletonList(transport.teleport(handle, getEntityId())));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot move packet display", error); }
    }

    public Update text(String value) {
        requireThread(); Objects.requireNonNull(value, "text");
        if (item) throw new IllegalStateException("This is an item model");
        if (value.equals(text)) return unchanged;
        try {
            if (modern) transport.displayClass(false).getMethod("setText", String.class).invoke(entity, value);
            else entity.setCustomName(value);
            text = value;
            spawnDirty = true;
            return new Update(world, Collections.singletonList(transport.metadata(handle, getEntityId(), false)));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot update packet text", error); }
    }

    public Update item(ItemStack value) {
        requireThread(); Objects.requireNonNull(value, "item");
        if (!item) throw new IllegalStateException("This is a text model");
        if (value.equals(stack)) return unchanged;
        try {
            if (modern) transport.displayClass(true).getMethod("setItemStack", ItemStack.class).invoke(entity, value.clone());
            else ((ArmorStand) entity).getEquipment().setHelmet(value.clone());
            stack = value.clone();
            spawnDirty = true;
            return new Update(world, Collections.singletonList(modern ? transport.metadata(handle, getEntityId(), false) : transport.equipment(getEntityId(), value)));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot update packet item", error); }
    }

    private Location nativeLocation(Location point) { return modern ? point.clone() : point.clone().subtract(0, item ? 1.2 : 0.5, 0); }

    /**
     * Immutable prepared packets, reusable across viewers. Packet data never depends on a viewer's state.
     */
    public static final class Update {
        private final World world;
        private final List<Object> packets;
        private Update(World world, List<Object> packets) { this.world = world; this.packets = Collections.unmodifiableList(new ArrayList<>(packets)); }
        public int packetCount() { return packets.size(); }
    }

    /**
     * Sends one ordered group to this viewer. Modern servers use bundle packets; old servers send the same ordered packets.
     * Offline viewers and updates for a different world are skipped. Groups are split below the protocol's bundle limit.
     */
    public static void send(Player viewer, Collection<Update> updates) {
        requireThread(); Objects.requireNonNull(updates, "updates");
        if (viewer == null || !viewer.isOnline() || backend == null) return;
        List<Object> packets = new ArrayList<>();
        for (Update update : updates) if (update != null && update.world == viewer.getWorld()) packets.addAll(update.packets);
        if (packets.isEmpty()) return;
        try { backend.send(viewer, packets); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot send packet display updates", error); }
    }

    private static void requireThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Packet displays must be used on the server thread"); }
    private static void validate(Location location) {
        Objects.requireNonNull(location, "location"); Objects.requireNonNull(location.getWorld(), "world");
        if (!Double.isFinite(location.getX()) || !Double.isFinite(location.getY()) || !Double.isFinite(location.getZ())
                || !Float.isFinite(location.getYaw()) || !Float.isFinite(location.getPitch())) throw new IllegalArgumentException("Display location must be finite");
    }

    private static final class Backend {
        final String legacy;
        final Class<?> nativeEntity;
        final Method create, bukkitEntity, position, watcher, playerHandle;
        final Constructor<?> spawnLiving, spawnModern, metadata, destroy, teleport, bundle;
        final Method nonDefault, dirty, positionSnapshot;
        final Field connection;
        final Method send;
        final Class<?> itemDisplay, textDisplay;
        final Constructor<?> equipment;
        final Method itemCopy, pair;
        final Object head;
        final Constructor<?> legacyWatcher;
        final Method legacyValues, legacyRegister, legacyIndex;

        Backend(World world) throws ReflectiveOperationException {
            String craft = Bukkit.getServer().getClass().getPackage().getName();
            legacy = "net.minecraft.server." + craft.substring(craft.lastIndexOf('.') + 1) + ".";
            nativeEntity = type("net.minecraft.world.entity.Entity", legacy + "Entity");
            create = world.getClass().getMethod("createEntity", Location.class, Class.class);
            bukkitEntity = nativeEntity.getMethod("getBukkitEntity");
            position = method(nativeEntity, new String[]{"absSnapTo", "absMoveTo", "moveTo", "setPositionRotation", "setLocation", "a", "b"}, double.class, double.class, double.class, float.class, float.class);
            watcher = returning(nativeEntity, "SynchedEntityData", "DataWatcher");
            Class<?> data = watcher.getReturnType();
            Class<?> watchable = optional(legacy + "DataWatcher$WatchableObject");
            legacyWatcher = watchable == null ? null : data.getConstructor(nativeEntity);
            legacyValues = watchable == null ? null : data.getMethod("c");
            legacyRegister = watchable == null ? null : data.getMethod("a", int.class, Object.class);
            legacyIndex = watchable == null ? null : watchable.getMethod("a");
            Class<?> meta = type("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket", "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata", legacy + "PacketPlayOutEntityMetadata");
            Constructor<?> modernMeta = constructor(meta, int.class, List.class);
            metadata = modernMeta == null ? meta.getConstructor(int.class, data, boolean.class) : modernMeta;
            nonDefault = modernMeta == null ? null : method(data, new String[]{"getNonDefaultValues", "c"});
            dirty = modernMeta == null ? null : method(data, new String[]{"packDirty", "b"});
            Class<?> living = optional("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving", legacy + "PacketPlayOutSpawnEntityLiving");
            spawnLiving = living == null ? null : unary(living, nativeEntity);
            Class<?> add = optional("net.minecraft.network.protocol.game.ClientboundAddEntityPacket", "net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity", legacy + "PacketPlayOutSpawnEntity");
            Constructor<?> addModern = null;
            if (add != null) for (Constructor<?> candidate : add.getConstructors()) if (candidate.getParameterCount() == 11) addModern = candidate;
            spawnModern = addModern == null && add != null ? unary(add, nativeEntity) : addModern;
            Class<?> remove = type("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket", "net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy", legacy + "PacketPlayOutEntityDestroy");
            destroy = remove.getConstructor(int[].class);
            Class<?> tp = type("net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket", "net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport", legacy + "PacketPlayOutEntityTeleport");
            Constructor<?> move = unary(tp, nativeEntity); Method snapshot = null;
            if (move == null) for (Constructor<?> candidate : tp.getConstructors()) if (candidate.getParameterCount() == 4 && candidate.getParameterTypes()[0] == int.class) {
                move = candidate;
                for (Method m : candidate.getParameterTypes()[1].getMethods()) if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1 && m.getParameterTypes()[0] == nativeEntity) snapshot = m;
            }
            if (move == null || move.getParameterCount() == 4 && snapshot == null) throw new NoSuchMethodException("Entity teleport packet");
            teleport = move; positionSnapshot = snapshot;
            Class<?> bundled = optional("net.minecraft.network.protocol.game.ClientboundBundlePacket");
            bundle = bundled == null ? null : bundled.getConstructor(Iterable.class);
            Class<?> player = Class.forName(craft + ".entity.CraftPlayer");
            playerHandle = player.getMethod("getHandle");
            Field connectionField = null;
            for (Class<?> owner = playerHandle.getReturnType(); owner != null; owner = owner.getSuperclass()) for (Field field : owner.getDeclaredFields()) {
                String name = field.getType().getSimpleName();
                if (name.equals("ServerGamePacketListenerImpl") || name.equals("PlayerConnection")) connectionField = field;
            }
            if (connectionField == null) throw new NoSuchFieldException("Player connection");
            connection = connectionField; connection.setAccessible(true);
            Class<?> packet = type("net.minecraft.network.protocol.Packet", legacy + "Packet");
            send = method(connection.getType(), new String[]{"send", "sendPacket", "a"}, packet);
            itemDisplay = optional("org.bukkit.entity.ItemDisplay"); textDisplay = optional("org.bukkit.entity.TextDisplay");
            Class<?> eq = type("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket", "net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment", legacy + "PacketPlayOutEntityEquipment");
            Constructor<?> eqCtor = constructor(eq, int.class, List.class);
            Class<?> slot = optional("net.minecraft.world.entity.EquipmentSlot", "net.minecraft.world.entity.EnumItemSlot", legacy + "EnumItemSlot");
            Object headSlot = null;
            if (slot != null) for (Object value : slot.getEnumConstants()) if (((Enum<?>) value).name().equals("HEAD")) headSlot = value;
            head = headSlot;
            Class<?> nativeItem = type("net.minecraft.world.item.ItemStack", legacy + "ItemStack");
            equipment = eqCtor != null ? eqCtor : eq.getConstructor(int.class, slot == null ? int.class : slot, nativeItem);
            itemCopy = Class.forName(craft + ".inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
            pair = eqCtor == null ? null : Class.forName("com.mojang.datafixers.util.Pair").getMethod("of", Object.class, Object.class);
        }

        Class<?> displayClass(boolean item) { Class<?> type = item ? itemDisplay : textDisplay; return type == null ? ArmorStand.class : type; }
        Object spawn(Object handle, Entity entity, Location point, boolean modern) throws ReflectiveOperationException {
            Constructor<?> ctor = !modern && spawnLiving != null ? spawnLiving : spawnModern;
            if (ctor == null) throw new NoSuchMethodException("Entity spawn packet");
            if (ctor.getParameterCount() == 1) {
                Object packet = ctor.newInstance(handle);
                if (legacyWatcher != null) for (Field field : packet.getClass().getDeclaredFields()) if (field.getType() == watcher.getReturnType()) {
                    field.setAccessible(true); field.set(packet, freezeWatcher(watcher.invoke(handle)));
                }
                return packet;
            }
            Class<?> vec = ctor.getParameterTypes()[9];
            Object zero = vec.getConstructor(double.class, double.class, double.class).newInstance(0, 0, 0);
            Object kind = method(nativeEntity, new String[]{"getType", "getEntityType"}).invoke(handle);
            return ctor.newInstance(entity.getEntityId(), entity.getUniqueId(), point.getX(), point.getY(), point.getZ(), point.getPitch(), point.getYaw(), kind, 0, zero, (double) point.getYaw());
        }
        Object metadata(Object handle, int id, boolean full) throws ReflectiveOperationException {
            Object values = watcher.invoke(handle);
            if (nonDefault == null) return metadata.newInstance(id, freezeWatcher(values), true);
            // Dirty values include defaults (cleared text/items); full state is reused for initial spawn.
            List<?> changed = (List<?>) (full ? nonDefault : dirty).invoke(values);
            if (changed == null || changed.isEmpty()) changed = (List<?>) nonDefault.invoke(values);
            return metadata.newInstance(id, changed == null ? Collections.emptyList() : new ArrayList<>(changed));
        }
        Object freezeWatcher(Object original) throws ReflectiveOperationException {
            if (legacyWatcher == null) return original;
            Object copy = legacyWatcher.newInstance(new Object[]{null});
            for (Object entry : (List<?>) legacyValues.invoke(original)) {
                Object value = entry.getClass().getMethod("b").invoke(entry);
                legacyRegister.invoke(copy, legacyIndex.invoke(entry), value);
            }
            return copy;
        }
        Object teleport(Object handle, int id) throws ReflectiveOperationException {
            return positionSnapshot == null ? teleport.newInstance(handle) : teleport.newInstance(id, positionSnapshot.invoke(null, handle), Collections.emptySet(), false);
        }
        Object equipment(int id, ItemStack item) throws ReflectiveOperationException {
            Object copy = itemCopy.invoke(null, item.clone());
            return pair == null ? equipment.newInstance(id, head == null ? 4 : head, copy)
                    : equipment.newInstance(id, Collections.singletonList(pair.invoke(null, head, copy)));
        }
        void send(Player viewer, List<Object> packets) throws ReflectiveOperationException {
            Object target = connection.get(playerHandle.invoke(viewer));
            if (bundle == null) for (Object packet : packets) send.invoke(target, packet);
            else for (int offset = 0; offset < packets.size(); offset += 1024) send.invoke(target, bundle.newInstance(new ArrayList<>(packets.subList(offset, Math.min(offset + 1024, packets.size())))));
        }
    }

    private static Class<?> type(String... names) throws ClassNotFoundException { Class<?> type = optional(names); if (type == null) throw new ClassNotFoundException(Arrays.toString(names)); return type; }
    private static Class<?> optional(String... names) { for (String name : names) try { return Class.forName(name); } catch (ClassNotFoundException ignored) { } return null; }
    private static Constructor<?> constructor(Class<?> type, Class<?>... args) { try { return type.getConstructor(args); } catch (NoSuchMethodException ignored) { return null; } }
    private static Constructor<?> unary(Class<?> type, Class<?> entity) { for (Constructor<?> ctor : type.getConstructors()) if (ctor.getParameterCount() == 1 && entity.isAssignableFrom(ctor.getParameterTypes()[0])) return ctor; return null; }
    private static Method method(Class<?> type, String[] names, Class<?>... args) throws NoSuchMethodException {
        for (String name : names) try { return type.getMethod(name, args); } catch (NoSuchMethodException ignored) { }
        throw new NoSuchMethodException(type.getName() + Arrays.toString(names));
    }
    private static Method returning(Class<?> type, String... names) throws NoSuchMethodException {
        for (Method method : type.getMethods()) if (method.getParameterCount() == 0) for (String name : names) if (method.getReturnType().getSimpleName().equals(name)) return method;
        throw new NoSuchMethodException("Entity metadata watcher");
    }
}
