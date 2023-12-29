package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.CommonLoader;
import de.cech12.bucketlib.platform.services.IBucketHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.material.Fluid;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The bucket helper implementation for Forge.
 */
public class ForgeBucketHelper implements IBucketHelper {

    @Override
    public Fluid getFluidOfBucketItem(BucketItem bucket) {
        return bucket.getFluid();
    }

    @Override
    public EntityType<?> getEntityTypeOfMobBucketItem(MobBucketItem mobBucketItem) {
        try {
            Method entityTypeMethod = MobBucketItem.class.getDeclaredMethod("getFishType");
            entityTypeMethod.setAccessible(true);
            return  (EntityType<?>) entityTypeMethod.invoke(mobBucketItem);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            CommonLoader.LOG.error("Could not get entity type of MobBucketItem.", ex);
        }
        return null;
    }
}
