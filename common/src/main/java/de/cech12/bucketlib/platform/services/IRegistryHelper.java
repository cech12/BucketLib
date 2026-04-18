package de.cech12.bucketlib.platform.services;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Objects;

/**
 * Common registry helper service interface.
 */
public interface IRegistryHelper {

    List<UniversalBucketItem> getRegisteredBuckets();

    default ResourceLocation getItemLocation(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    default EntityType<?> getEntityType(ResourceLocation location) {
        return BuiltInRegistries.ENTITY_TYPE.get(location).map(Holder.Reference::value).orElse(null);
    }

    default ResourceLocation getEntityTypeLocation(EntityType<?> entityType) {
        return Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    default Block getBlock(ResourceLocation location) {
        return BuiltInRegistries.BLOCK.get(location).map(Holder.Reference::value).orElse(null);
    }

    default ResourceLocation getBlockLocation(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    default Iterable<Item> getAllItems() {
        return BuiltInRegistries.ITEM;
    }

    default Iterable<Fluid> getAllFluids() {
        return BuiltInRegistries.FLUID;
    }

    default Fluid getFluid(ResourceLocation location) {
        return BuiltInRegistries.FLUID.get(location).map(Holder.Reference::value).orElse(null);
    }

    default ResourceLocation getFluidLocation(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid);
    }

}
