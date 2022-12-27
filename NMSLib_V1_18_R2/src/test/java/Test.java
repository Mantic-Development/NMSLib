import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.level.pathfinder.PathEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;

public class Test {

    public static void main(String[] args) {

        World world = Bukkit.getWorlds().get(0);
        Entity entity = world.spawnEntity(new Location(world, 0, 0, 0), EntityType.SKELETON);
        EntityInsentient nmsEntity = (EntityInsentient)((CraftLivingEntity) entity).getHandle();
        final double x = 0;
        final double y = 0;
        final double z = 0;

        NavigationAbstract navigationAbstract = nmsEntity.D();
        PathEntity path = navigationAbstract.a(x, y, z, 1);
        navigationAbstract.a(path, 1);

        // make the above in reflection
        Class<?> navigationAbstractClass = navigationAbstract.getClass();
        try {
            Method method = navigationAbstractClass.getDeclaredMethod("a", PathEntity.class, float.class);
            method.setAccessible(true);
            method.invoke(navigationAbstract, path, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


}
