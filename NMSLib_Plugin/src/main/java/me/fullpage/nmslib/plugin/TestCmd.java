package me.fullpage.nmslib.plugin;

import me.fullpage.manticlib.command.ManticCommand;

public class TestCmd extends ManticCommand {
    public TestCmd() {
        super("test", "nmslib.admin");
    }

    @Override
    public void run() {

    /*    Location location = player.getLocation();
        //
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        List<String> text = Txt.list("&d&l+1", "&c&odamage");
        for (int i = -1; ++i < text.size(); ) {
            EntityArmorStand entityArmorStand = new EntityArmorStand(craftWorld.getHandle(), location.getX(), location.getY(), location.getZ());

            entityArmorStand.setInvisible(true);
            entityArmorStand.setCustomNameVisible(true);
            entityArmorStand.setCustomName(Txt.parse(text.get(i)));
            entityArmorStand.inactiveTick();

            entityArmorStand.has

            PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving(entityArmorStand);
            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(entityArmorStand.getId(), entityArmorStand.getDataWatcher(), true);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutSpawnEntityLiving);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutEntityMetadata);
            location = location.subtract(0, 0.4, 0);

            // remove entity packet 10 seconds later
            Bukkit.getScheduler().runTaskLaterAsynchronously(JavaPlugin.getProvidingPlugin(TestCmd.class), () -> {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(entityArmorStand.getId()));

            }, 200L);*/
    }

}
