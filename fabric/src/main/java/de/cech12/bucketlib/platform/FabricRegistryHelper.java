package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.services.IRegistryHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Objects;

/**
 * The registry service implementation for NeoForge.
 */
public class FabricRegistryHelper implements IRegistryHelper {
    @Override
    public List<UniversalBucketItem> getRegisteredBuckets() {
        return BucketLibMod.getRegisteredBuckets();
    }

    @Override
    public EntityType<?> getEntityType(ResourceLocation location) {
        return BuiltInRegistries.ENTITY_TYPE.get(location);
    }

    @Override
    public ResourceLocation getEntityTypeLocation(EntityType<?> entityType) {
        return Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    @Override
    public Block getBlock(ResourceLocation location) {
        return BuiltInRegistries.BLOCK.get(location);
    }

    @Override
    public ResourceLocation getBlockLocation(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    @Override
    public Iterable<Item> getAllItems() {
        return BuiltInRegistries.ITEM;
    }

    @Override
    public Iterable<Fluid> getAllFluids() {
        return BuiltInRegistries.FLUID;
    }

    @Override
    public Fluid getFluid(ResourceLocation location) {
        return BuiltInRegistries.FLUID.get(location);
    }

    @Override
    public ResourceLocation getFluidLocation(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid);
    }
}
