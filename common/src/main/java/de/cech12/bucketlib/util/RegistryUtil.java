package de.cech12.bucketlib.util;

import de.cech12.bucketlib.platform.Services;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

public class RegistryUtil {

    private static List<BucketBlock> bucketBlocks;
    private static List<BucketEntity> bucketEntities;

    private RegistryUtil() {}

    private static void readRegistry() {
        bucketBlocks = new ArrayList<>();
        bucketEntities = new ArrayList<>();
        Level level = Services.PLATFORM.getCurrentLevel();
        for (Item item : Services.REGISTRY.getAllItems()) {
            if (item instanceof SolidBucketItem bucket) {
                bucketBlocks.add(new BucketBlock(bucket.getBlock(), bucket));
            }
            if (item instanceof MobBucketItem bucket) {
                EntityType<?> entityType = BucketLibUtil.getEntityTypeOfMobBucketItem(bucket);
                if (entityType != null && level != null && entityType.create(level) instanceof Bucketable) {
                    bucketEntities.add(new BucketEntity(entityType, BucketLibUtil.getFluidOfBucketItem(bucket), bucket));
                }
            }
        }
    }

    public static List<BucketBlock> getBucketBlocks() {
        if (bucketBlocks == null) {
            readRegistry();
        }
        return bucketBlocks;
    }

    public static BucketBlock getBucketBlock(Block block) {
        for (BucketBlock bucketBlock : getBucketBlocks()) {
            if (bucketBlock.block() == block) {
                return bucketBlock;
            }
        }
        return null;
    }

    public static List<BucketEntity> getBucketEntities() {
        if (bucketEntities == null) {
            readRegistry();
        }
        return bucketEntities;
    }

    public record BucketBlock(Block block, SolidBucketItem bucketItem) {}

    public record BucketEntity(EntityType<?> entityType, Fluid fluid, MobBucketItem bucketItem) {}

}
