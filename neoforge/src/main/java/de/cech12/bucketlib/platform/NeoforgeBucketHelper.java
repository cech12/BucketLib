package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IBucketHelper;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;

/**
 * The bucket helper implementation for NeoForge.
 */
public class NeoforgeBucketHelper implements IBucketHelper {

    @Override
    public Fluid getFluidOfBucketItem(BucketItem bucket) {
        return bucket.content;
    }

}
