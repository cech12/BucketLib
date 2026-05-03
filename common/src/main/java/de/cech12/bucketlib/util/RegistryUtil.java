package de.cech12.bucketlib.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import de.cech12.bucketlib.mixin.MobBucketItemAccessor;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

public class RegistryUtil {

    public static final Codec<Holder<Fluid>> FLUID_CODEC = BuiltInRegistries.FLUID.holderByNameCodec().validate((fluid) -> fluid.value().defaultFluidState().is(Fluids.EMPTY) ? DataResult.error(() -> "Fluid must not be fluid:empty") : DataResult.success(fluid));
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Fluid>> STREAM_FLUID_CODEC = ByteBufCodecs.holderRegistry(Registries.FLUID);

    public static final Codec<Holder<Block>> BLOCK_CODEC = BuiltInRegistries.BLOCK.holderByNameCodec().validate((block) -> block.value() == Blocks.AIR ? DataResult.error(() -> "Block must not be block:air") : DataResult.success(block));
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Block>> STREAM_BLOCK_CODEC = ByteBufCodecs.holderRegistry(Registries.BLOCK);

    public static final Codec<Holder<EntityType<?>>> ENTITY_TYPE_CODEC = BuiltInRegistries.ENTITY_TYPE.holderByNameCodec().validate(DataResult::success);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<EntityType<?>>> STREAM_ENTITY_TYPE_CODEC = ByteBufCodecs.holderRegistry(Registries.ENTITY_TYPE);

    private static List<BucketBlock> bucketBlocks;
    private static List<BucketEntity> bucketEntities;
    private static RegistryAccess registryAccess;

    private RegistryUtil() {}

    private static void readRegistry() {
        bucketBlocks = new ArrayList<>();
        bucketEntities = new ArrayList<>();
        for (Item item : Services.REGISTRY.getAllItems()) {
            if (item instanceof SolidBucketItem bucket) {
                if (bucketBlocks.stream().noneMatch(bucketBlock -> bucketBlock.block == bucket.getBlock())) {
                    bucketBlocks.add(new BucketBlock(bucket.getBlock(), bucket));
                }
            }
            if (item instanceof MobBucketItem bucket) {
                EntityType<?> entityType = ((MobBucketItemAccessor) bucket).bucketlib_getEntityType();
                if (entityType != null && bucketEntities.stream().noneMatch(bucketEntity -> bucketEntity.entityType == entityType)) {
                    bucketEntities.add(new BucketEntity(entityType, Services.BUCKET.getFluidOfBucketItem(bucket), bucket));
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

    public static BucketEntity getBucketEntity(EntityType<?> entityType) {
        for (BucketEntity bucketEntity : getBucketEntities()) {
            if (bucketEntity.entityType() == entityType) {
                return bucketEntity;
            }
        }
        return null;
    }

    public static RegistryAccess getRegistryAccess() {
        if (registryAccess == null) {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) {
                throw new IllegalStateException("Could not get registry, registry access is unavailable because the level is currently null");
            }
            registryAccess = level.registryAccess();
        }
        return registryAccess;
    }

    public record BucketBlock(Block block, SolidBucketItem bucketItem) {}

    public record BucketEntity(EntityType<?> entityType, Fluid fluid, MobBucketItem bucketItem) {}

}
