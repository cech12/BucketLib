package de.cech12.bucketlib.platform.services;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * Common registry helper service interface.
 */
public interface IRegistryHelper {

    List<UniversalBucketItem> getRegisteredBuckets();

    EntityType<?> getEntityType(ResourceLocation location);

    ResourceLocation getEntityTypeLocation(EntityType<?> entityType);

    Block getBlock(ResourceLocation location);

    ResourceLocation getBlockLocation(Block block);

    Iterable<Item> getAllItems();

    Iterable<Fluid> getAllFluids();

    Fluid getFluid(ResourceLocation location);

    ResourceLocation getFluidLocation(Fluid fluid);

}
