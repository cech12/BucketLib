package de.cech12.bucketlib.api;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class BucketLibApi {

    private static final Logger LOGGER = LogManager.getLogger(BucketLibApi.class);

    public static void registerBucket(ResourceLocation bucketLocation) {
        Optional<Item> bucketItem = BuiltInRegistries.ITEM.getOptional(bucketLocation);
        if (bucketItem.isEmpty()) {
            LOGGER.info("Bucket could not be registered. The given ResourceLocation \"{}\" does not match any registered item in Forge registry.", bucketLocation);
            return;
        }
        if (bucketItem.get() instanceof UniversalBucketItem bucket) {
            registerBucket(bucket);
        } else {
            LOGGER.info("Bucket could not be registered. The item \"{}\" is not a {}.", bucketLocation, UniversalBucketItem.class.getName());
        }
    }

    private static void registerBucket(UniversalBucketItem bucket) {
        BucketLibMod.addBucket(bucket);
        addItemToTab(bucket);
    }

    private static void addItemToTab(UniversalBucketItem bucket) {
        ItemGroupEvents.modifyEntriesEvent(bucket.getCreativeTab()).register(content -> {
            ItemStack emptyBucket = new ItemStack(bucket);
            //add empty bucket
            content.accept(emptyBucket);
            //add fluid buckets
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid == Fluids.EMPTY) {
                    continue;
                }
                //Fabric has no common milk fluid
                if (bucket.canHoldFluid(fluid)) {
                    content.accept(BucketLibUtil.addFluid(emptyBucket, fluid));
                }
            }
            //add milk bucket
            content.accept(BucketLibUtil.addMilk(emptyBucket));
            //add entity buckets
            for (RegistryUtil.BucketEntity bucketEntity : RegistryUtil.getBucketEntities()) {
                if (bucket.canHoldEntity(bucketEntity.entityType()) && bucket.canHoldFluid(bucketEntity.fluid())) {
                    ItemStack filledBucket = BucketLibUtil.addFluid(emptyBucket, bucketEntity.fluid());
                    filledBucket = BucketLibUtil.addEntityType(filledBucket, bucketEntity.entityType());
                    content.accept(filledBucket);
                }
            }
            //add block buckets
            for (RegistryUtil.BucketBlock bucketBlock : RegistryUtil.getBucketBlocks()) {
                if (bucket.canHoldBlock(bucketBlock.block())) {
                    content.accept(BucketLibUtil.addBlock(emptyBucket, bucketBlock.block()));
                }
            }
        });
    }

}
