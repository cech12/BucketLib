package de.cech12.bucketlib.platform.services;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.material.Fluid;

public interface IBucketHelper {

    Fluid getFluidOfBucketItem(BucketItem bucket);

    EntityType<?> getEntityTypeOfMobBucketItem(MobBucketItem mobBucketItem);

}
