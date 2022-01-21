package cech12.bucketlib.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EntityUtil {

    private static List<BucketEntity> bucketEntities;

    private EntityUtil() {}

    private static void readRegistry() {
        bucketEntities = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof MobBucketItem bucket) {
                try {
                    Method method = bucket.getClass().getDeclaredMethod("getFishType");
                    method.setAccessible(true);
                    EntityType<?> entityType = (EntityType<?>) method.invoke(bucket);
                    if (entityType != null) {
                        bucketEntities.add(new BucketEntity(entityType, bucket.getFluid()));
                    }
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}
            }
        }
    }

    public static List<BucketEntity> getBucketEntities() {
        if (bucketEntities == null) {
            readRegistry();
        }
        return bucketEntities;
    }

    public static class BucketEntity {
        private final EntityType<?> entityType;
        private final Fluid fluid;

        private BucketEntity(EntityType<?> entityType, Fluid fluid) {
            this.entityType = entityType;
            this.fluid = fluid;
        }

        public EntityType<?> getEntityType() {
            return entityType;
        }

        public Fluid getFluid() {
            return fluid;
        }
    }

}
