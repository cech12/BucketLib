package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IBucketHelper;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;

/**
 * The bucket helper implementation for Fabric.
 */
public class FabricBucketHelper implements IBucketHelper {

    @Override
    public Fluid getFluidOfBucketItem(BucketItem bucket) {
        return ((BucketItemAccessor) bucket).fabric_getFluid();
    }

}
