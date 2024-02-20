package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.services.IRegistryHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;

public class ForgeRegistryHelper implements IRegistryHelper {
    @Override
    public List<UniversalBucketItem> getRegisteredBuckets() {
        return BucketLibMod.getRegisteredBuckets();
    }

    @Override
    public EntityType<?> getEntityType(ResourceLocation location) {
        return ForgeRegistries.ENTITY_TYPES.getValue(location);
    }

    @Override
    public ResourceLocation getEntityTypeLocation(EntityType<?> entityType) {
        return Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType));
    }

    @Override
    public Block getBlock(ResourceLocation location) {
        return ForgeRegistries.BLOCKS.getValue(location);
    }

    @Override
    public ResourceLocation getBlockLocation(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block);
    }

    @Override
    public Iterable<Item> getAllItems() {
        return ForgeRegistries.ITEMS;
    }

    @Override
    public Iterable<Fluid> getAllFluids() {
        return ForgeRegistries.FLUIDS;
    }

    @Override
    public Fluid getFluid(ResourceLocation location) {
        return ForgeRegistries.FLUIDS.getValue(location);
    }

    @Override
    public ResourceLocation getFluidLocation(Fluid fluid) {
        return ForgeRegistries.FLUIDS.getKey(fluid);
    }
}
