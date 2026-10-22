package me.fullpage.nmslib.v26_2;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.NMSHandler;
import me.fullpage.nmslib.chunk.ChunkSource;
import me.fullpage.nmslib.chunk.VirtualChunk;
import me.fullpage.nmslib.packet.DigListener;
import me.fullpage.nmslib.packet.PacketArmorStand;
import me.fullpage.nmslib.packet.PacketEntity;
import me.fullpage.nmslib.packet.PacketPlayer;
import me.fullpage.nmslib.packet.UseItemOnListener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEnderPearl;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class NMSLib_V26_2 implements NMSHandler {

    public NMSLib_V26_2() {
        //((CraftMagicNumbers) CraftMagicNumbers.INSTANCE).getMappingsVersion();
    }

    private static final net.minecraft.world.level.block.state.BlockState AIR_STATE = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

    @Override
    public boolean breakBlock(Block block, boolean applyPhysics) {
        return setNativeBlock(block, AIR_STATE, applyPhysics);
    }

    @Override
    public boolean setBlock(Block block, org.bukkit.block.data.BlockData data, boolean applyPhysics) {
        java.util.Objects.requireNonNull(data, "data");
        return setNativeBlock(block, ((org.bukkit.craftbukkit.block.data.CraftBlockData) data).getState(), applyPhysics);
    }

    private static boolean setNativeBlock(Block block, net.minecraft.world.level.block.state.BlockState state, boolean applyPhysics) {
        java.util.Objects.requireNonNull(block, "block");
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Block changes must run on the server thread");
        }
        org.bukkit.craftbukkit.block.CraftBlock nativeBlock = (org.bukkit.craftbukkit.block.CraftBlock) block;
        net.minecraft.core.BlockPos position = nativeBlock.getPosition();
        net.minecraft.world.level.LevelAccessor level = nativeBlock.getLevel();
        if (!level.getMinecraftWorld().getChunkSource().isChunkLoaded(position.getX() >> 4, position.getZ() >> 4)) return false;
        // Without physics: client and light updates only; no neighbour/shape updates, onPlace, block entity side effects or POI updates.
        return level.setBlock(position, state, applyPhysics ? net.minecraft.world.level.block.Block.UPDATE_ALL : NO_PHYSICS_FLAGS);
    }

    private static final int NO_PHYSICS_FLAGS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS
            | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
            | net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE
            | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
            | net.minecraft.world.level.block.Block.UPDATE_SKIP_POI;

    static net.minecraft.world.level.block.state.BlockState state(int stateId) {
        net.minecraft.world.level.block.state.BlockState state = net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.byId(stateId);
        if (state == null) throw new IllegalArgumentException("Unknown block state id " + stateId);
        return state;
    }

    @Override
    public boolean supportsPacketListeners() {
        return true;
    }

    @Override
    public AutoCloseable addDigListener(Plugin owner, DigListener listener) {
        return PacketInjector.add(owner, listener);
    }

    @Override
    public AutoCloseable addUseItemOnListener(Plugin owner, UseItemOnListener listener) {
        return PacketInjector.add(owner, listener);
    }

    @Override
    public void removePacketListeners(Plugin owner) {
        PacketInjector.remove(owner);
    }

    @Override
    public boolean supportsVirtualChunks() {
        return true;
    }

    @Override
    public VirtualChunk prepareChunk(org.bukkit.World world, int chunkX, int chunkZ, ChunkSource source) {
        return VirtualChunks.prepare(world, chunkX, chunkZ, source);
    }

    @Override
    public void sendChunk(Player viewer, VirtualChunk chunk) {
        VirtualChunks.send(viewer, chunk);
    }

    @Override
    public void resendChunk(Player viewer, int chunkX, int chunkZ) {
        VirtualChunks.resend(viewer, chunkX, chunkZ);
    }

    @Override
    public void sendTabListAdd(Player viewer, Collection<? extends Player> players) {
        ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
        if (players.isEmpty() || handle.connection == null) return;
        List<ServerPlayer> entries = new ArrayList<>(players.size());
        for (Player player : players) entries.add(((CraftPlayer) player).getHandle());
        handle.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(entries, handle));
    }

    @Override
    public void sendTabListRemove(Player viewer, Collection<UUID> players) {
        ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
        if (players.isEmpty() || handle.connection == null) return;
        handle.connection.send(new ClientboundPlayerInfoRemovePacket(new ArrayList<>(players)));
    }

    @Override
    public void sendTabListUpdate(Collection<? extends Player> viewers, Collection<? extends Player> players) {
        if (viewers.isEmpty() || players.isEmpty()) return;
        List<ServerPlayer> entries = new ArrayList<>(players.size());
        for (Player player : players) entries.add(((CraftPlayer) player).getHandle());
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), entries);
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null) handle.connection.send(packet);
        }
    }

    @Override
    public boolean setItemDisplay(ItemStack item, String name, List<String> lore) {
        return ItemDisplays.set(item, name, lore);
    }

    @Override
    public Object itemNameToken(ItemStack item) {
        return CraftItemStack.unwrap(item).get(net.minecraft.core.component.DataComponents.ITEM_NAME);
    }

    @Override
    public Object itemLoreToken(ItemStack item) {
        return CraftItemStack.unwrap(item).get(net.minecraft.core.component.DataComponents.LORE);
    }

    @Override
    public long dataSlots(PlayerInventory inventory, NamespacedKey key) {
        return InventorySlots.dataSlots(inventory, key);
    }

    @Override
    public int dataSlot(PlayerInventory inventory, NamespacedKey key, String value, int hint) {
        return InventorySlots.dataSlot(inventory, key, value, hint);
    }

    @Override
    public boolean hasStorageRoom(PlayerInventory inventory) {
        return InventorySlots.hasStorageRoom(inventory);
    }

    @Override
    public long addToStorage(PlayerInventory inventory, ItemStack prototype, long amount, int maxStack) {
        return InventorySlots.addToStorage(inventory, prototype, amount, maxStack);
    }

    @Override
    public int blockStateId(BlockData data) {
        return net.minecraft.world.level.block.Block.getId(((org.bukkit.craftbukkit.block.data.CraftBlockData) java.util.Objects.requireNonNull(data, "data")).getState());
    }

    @Override
    public BlockData blockData(int stateId) {
        return state(stateId).asBlockData();
    }

    @Override
    public int dataVersion() {
        return DataUpgrades.current();
    }

    @Override
    public String upgradeBlockData(String blockData, int fromDataVersion) {
        return DataUpgrades.blockData(blockData, fromDataVersion);
    }

    @Override
    public String upgradeBlockEntity(String snbt, int fromDataVersion) {
        return DataUpgrades.blockEntity(snbt, fromDataVersion);
    }

    @Override
    public void sendBlockChanges(Player viewer, long[] positions, int[] stateIds, int count) {
        BlockChanges.send(viewer, positions, stateIds, count);
    }

    @Override
    public void acknowledgeBlockChanges(Player player, int sequence) {
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Block change acknowledgements must run on the server thread");
        }
        net.minecraft.server.network.ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        // A negative sequence would make ackBlockChangesUpTo kick the player.
        if (connection != null && sequence >= 0) connection.ackBlockChangesUpTo(sequence);
    }

    @Override
    public int fillBlocks(org.bukkit.World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                          int[] stateIds, int[] cumulativeWeights, long seed) {
        return BlockFill.fill(world, minX, minY, minZ, maxX, maxY, maxZ, stateIds, cumulativeWeights, seed);
    }

    @Override
    public PacketArmorStand preparePacketArmorStand(Location location) {
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Packet armor stands must be prepared on the server thread");
        }
        net.minecraft.server.level.ServerLevel level = ((CraftWorld) java.util.Objects.requireNonNull(location.getWorld(), "world")).getHandle();
        return new ClientArmorStand(level, level.getNextEntityId(), location.getX(), location.getY(), location.getZ(), location.getYaw());
    }

    @Override
    public boolean supportsPacketPlayers() {
        return true;
    }

    @Override
    public PacketPlayer preparePacketPlayer(Location location, String name, PacketPlayer.Skin skin) {
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Packet players must be prepared on the server thread");
        }
        net.minecraft.server.level.ServerLevel level = ((CraftWorld) java.util.Objects.requireNonNull(location.getWorld(), "world")).getHandle();
        return new ClientPlayer(level, level.getNextEntityId(), name, skin, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    @Override
    public PacketEntity preparePacketEntity(Location location, org.bukkit.entity.EntityType type) {
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Packet entities must be prepared on the server thread");
        }
        net.minecraft.server.level.ServerLevel level = ((CraftWorld) java.util.Objects.requireNonNull(location.getWorld(), "world")).getHandle();
        net.minecraft.world.entity.Entity entity = org.bukkit.craftbukkit.entity.CraftEntityType.bukkitToMinecraft(type)
                .create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (entity == null) throw new IllegalArgumentException(type + " cannot be created in " + location.getWorld().getName());
        return new ClientEntity(entity, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    @Override
    public void flushPacketEntities(Collection<? extends Player> viewers, Collection<? extends PacketEntity> entities) {
        ClientEntity.flush(viewers, entities);
    }

    @Override
    public void sendExplosion(Collection<? extends Player> viewers, double x, double y, double z, float radius, int blockCount) {
        // Empty knockback: a present one pushes the viewer.
        net.minecraft.network.protocol.game.ClientboundExplodePacket packet = new net.minecraft.network.protocol.game.ClientboundExplodePacket(
                new net.minecraft.world.phys.Vec3(x, y, z), radius, blockCount, java.util.Optional.empty(),
                radius >= 2 ? net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER : net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.world.level.Level.DEFAULT_EXPLOSION_BLOCK_PARTICLES);
        for (Player viewer : viewers) {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            if (handle.connection != null) handle.connection.send(packet);
        }
    }

    @Override
    public int spawnPacketHologram(Player player, Location location, String text) {
        if (player == null || location == null || location.getWorld() == null || text == null
                || !player.isOnline() || player.getWorld() != location.getWorld()) {
            return -1;
        }
        net.minecraft.server.level.ServerPlayer nmsPlayer = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
        net.minecraft.world.entity.Display.TextDisplay hologram =
                new net.minecraft.world.entity.Display.TextDisplay(
                        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(
                                net.minecraft.resources.Identifier.withDefaultNamespace("text_display"))
                                .orElseThrow().value(), nmsPlayer.level());
        hologram.setPos(location.getX(), location.getY(), location.getZ());
        org.bukkit.entity.TextDisplay display = (org.bukkit.entity.TextDisplay) hologram.getBukkitEntity();
        display.setText(text);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setDefaultBackground(false);
        display.setShadowed(true);

        int entityId = hologram.getId();
        nmsPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(
                entityId, hologram.getUUID(), location.getX(), location.getY(), location.getZ(),
                0.0f, 0.0f, hologram.getType(), 0,
                net.minecraft.world.phys.Vec3.ZERO, 0.0));
        nmsPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                entityId, hologram.getEntityData().getNonDefaultValues()));
        return entityId;
    }

    @Override
    public void removePacketEntities(Player player, int... entityIds) {
        if (player == null || !player.isOnline() || entityIds == null || entityIds.length == 0) {
            return;
        }
        ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle().connection.send(
                new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(entityIds));
    }

    @Override
    public void sendActionBar(Player player, String message) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        if (handle.connection != null) handle.connection.send(new ClientboundSystemChatPacket(actionBar(message), true));
    }

    private static final String PLAIN_CODES = "0123456789abcdefklor";

    /**
     * The client applies colour and format codes in plain text as fromString styles them, and one literal encodes for a fraction
     * of a styled tree. Anything else after a section sign keeps fromString: hex (§x) only it understands, a stray § pairs
     * differently on the client, and Geyser passes raw §m/§n (Bedrock colours) and upper case codes to Bedrock as written.
     * The narrator speaks a literal's codes aloud; accepted for the encode cost.
     */
    static net.minecraft.network.chat.Component actionBar(String message) {
        return message != null && plainCodes(message) ? net.minecraft.network.chat.Component.literal(message) : CraftChatMessage.fromString(message, true, true)[0];
    }

    private static boolean plainCodes(String text) {
        for (int i = text.indexOf('§'); i >= 0; i = text.indexOf('§', i + 2)) {
            if (i + 1 == text.length() || PLAIN_CODES.indexOf(text.charAt(i + 1)) < 0) return false;
        }
        return true;
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        player.spigot().sendMessage(ComponentSerializer.parse(json));
    }

    @Override
    public boolean isMainHand(PlayerInteractEvent event) {
        return event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 20, 10);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void clearTitle(Player player) {
        player.resetTitle();
    }

    @Override
    public ItemStack getItemInMainHand(Player player) {
        return player == null ? null : player.getInventory().getItemInMainHand();
    }

    @Override
    public ItemStack getItemInUse(Player player) {
        return player == null ? null : player.getActiveItem();
    }

    @Override
    public Enchantment lookupEnchantment(String name, int internalId) {
        for (Enchantment value : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).stream().toArray(Enchantment[]::new)) {
            NamespacedKey key = value.getKey();
            if (key.getKey().equalsIgnoreCase(name) || name.equalsIgnoreCase(key.getNamespace() + ":" + key.getKey()) || name.equalsIgnoreCase("minecraft:" + key.getKey())) {
                return value;
            }
        }
        return null;
    }

    @Override
    public HashMap<EnchantInfo, Enchantment> registerEnchantments(Collection<EnchantInfo> enchantInfos, Plugin plugin) {
        HashMap<EnchantInfo, Enchantment> temp = new HashMap<>();
        for (EnchantInfo enchantInfo : enchantInfos) {
            Enchantment enchantment = registerEnchantment(enchantInfo, plugin);
            temp.put(enchantInfo, enchantment);
        }
        return temp;
    }


    @Override
    public org.bukkit.enchantments.Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, enchantInfo.getName());
        return new ManticApiEnchant(key, enchantInfo);
    }


    @Override
    public Enchantment registerEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        Enchantment enchantment = lookupEnchantment(enchantInfo.getName(), enchantInfo.getInternalId());
        if (enchantment != null) {
            return enchantment;
        }
        EnchantHandler.unfreezeRegistry();

        Enchantment ench = EnchantHandler.registerEnchantment(enchantInfo);
        EnchantHandler.freezeRegistry();
        return ench;
    }

    @Override
    public boolean registerEnchantment(org.bukkit.enchantments.Enchantment enchantment) {
        throw new UnsupportedOperationException("This method is not supported in 1.20.4 and above. Use registerEnchantment(EnchantInfo, Plugin) instead.");

    }


    @Override
    public boolean isRegistered(org.bukkit.enchantments.Enchantment enchantment) {
        for (org.bukkit.enchantments.Enchantment value : Enchantment.values()) {
            if (value.equals(enchantment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRegistered(String name, int internalId) {
        return lookupEnchantment(name, internalId) != null;
    }


    @Override
    public boolean isGrown(Block block, org.bukkit.block.BlockState blockState) {
        if (block == null) {
            return true;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        BlockData blockData = blockState.getBlockData();
        if (blockData instanceof CaveVinesPlant) {
            CaveVinesPlant caveVinesPlant = (CaveVinesPlant) blockData;
            return caveVinesPlant.isBerries();
        }

        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            return ageable.getAge() >= ageable.getMaximumAge();
        }

        return true;
    }

    @Override
    public void setCropToAdult(Block block, org.bukkit.block.BlockState blockState) {
        if (block == null) {
            return;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        BlockData blockData = blockState.getBlockData();
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            ageable.setAge(ageable.getMaximumAge());
            blockState.setBlockData(ageable);
            blockState.update(true);
        }

        if (blockData instanceof CaveVinesPlant) {
            CaveVinesPlant caveVinesPlant = (CaveVinesPlant) blockData;
            caveVinesPlant.setBerries(true);
            blockState.setBlockData(caveVinesPlant);
            blockState.update(true);
        }
    }

    @Override
    public void setCropToBaby(Block block, BlockState blockState) {
        if (block == null) {
            return;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        BlockData blockData = blockState.getBlockData();
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            ageable.setAge(0);
            blockState.setBlockData(ageable);
            blockState.update(true);
        }

        if (blockData instanceof CaveVinesPlant) {
            CaveVinesPlant caveVinesPlant = (CaveVinesPlant) blockData;
            caveVinesPlant.setBerries(false);
            blockState.setBlockData(caveVinesPlant);
            blockState.update(true);
        }

    }

    @Override
    public void moveTo(LivingEntity entity, Location moveTo, float speed) {
        if (entity == null || moveTo == null) {
            return;
        }
        CraftLivingEntity craftEntity = (CraftLivingEntity) entity;
        net.minecraft.world.entity.LivingEntity handle = craftEntity.getHandle();

        if (!(handle instanceof PathfinderMob pathfinderMob)) {
            return;
        }
        pathfinderMob.getNavigation().moveTo(moveTo.getX(), moveTo.getY(), moveTo.getZ(), speed);
    }


    @Override
    public void stopNavigation(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        CraftLivingEntity craftEntity = (CraftLivingEntity) entity;
        net.minecraft.world.entity.LivingEntity handle = craftEntity.getHandle();
        if (!(handle instanceof PathfinderMob pathfinderMob)) {
            return;
        }
        PathNavigation navigation = pathfinderMob.getNavigation();
        navigation.stop();
    }


    @Override
    public void setBiteTime(PlayerFishEvent event, int ticks) {
        try {
            Field hookEntity = event.getClass().getDeclaredField("hookEntity");
            hookEntity.setAccessible(true);
            Object object = hookEntity.get(event);
            CraftEntity craftEntity = (CraftEntity) object;
            FishingHook entityFishingHook = (FishingHook) craftEntity.getHandle();

            Field fishCatchTime = FishingHook.class.getDeclaredField("timeUntilLured"); // Mojang-mapped runtime, no remapping in 26.x
            fishCatchTime.setAccessible(true);
            fishCatchTime.setInt(entityFishingHook, Math.max(15, ticks));
            fishCatchTime.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void simulateRodInteraction(Player player) {
        ServerPlayer p = ((CraftPlayer) player).getHandle();

        InteractionHand hand = getHand(player);
        p.gameMode.useItem(p, p.level(), p.getItemInHand(hand), hand);

        p.swing(hand, true);
    }

    private InteractionHand getHand(Player player) {
        return getItemInMainHand(player).getType().equals(Material.FISHING_ROD) ?
                InteractionHand.MAIN_HAND : player.getInventory().getItemInOffHand().getType().equals(Material.FISHING_ROD) ?
                                            InteractionHand.OFF_HAND : null;
    }

    private boolean isRodInMainHand(Player player) {
        org.bukkit.Material main = player.getInventory().getItemInMainHand().getType();
        return main.name().contains("FISHING_ROD");
    }
    @Override
    public boolean isInWater(Entity entity) {
        return entity.isInWater();
    }

    @Override
    public void setVelocity(Entity entity, Vector vector) {
        entity.setVelocity(vector);
    }

    @Override
    public Vector getVelocity(Entity entity) {
        return entity.getVelocity();
    }


    @Override
    public void setStairDirection(BlockState blockState, BlockFace blockFace, boolean inverted) {
        Stairs stairs = (Stairs) blockState.getBlockData();
        stairs.setFacing(blockFace);
        blockState.setBlockData(stairs);
    }
}
