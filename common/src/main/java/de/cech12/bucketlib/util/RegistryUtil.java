package de.cech12.bucketlib.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import de.cech12.bucketlib.mixin.MobBucketItemAccessor;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.resources.ResourceLocation;
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

    public static Codec<Fluid> FLUID_CODEC = Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(Fluid input, DynamicOps<T> ops, T prefix) {
            return ops.mergeToPrimitive(prefix, ops.createString(Services.REGISTRY.getFluidLocation(input).toString()));
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<Fluid, T>> decode(DynamicOps<T> ops, T input) {
            return DataResult.success(Pair.of(Services.REGISTRY.getFluid(ResourceLocation.parse(ops.getStringValue(input).getOrThrow())), ops.empty()));
        }
    });

    public static Codec<Block> BLOCK_CODEC = Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(Block input, DynamicOps<T> ops, T prefix) {
            return ops.mergeToPrimitive(prefix, ops.createString(Services.REGISTRY.getBlockLocation(input).toString()));
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<Block, T>> decode(DynamicOps<T> ops, T input) {
            return DataResult.success(Pair.of(Services.REGISTRY.getBlock(ResourceLocation.parse(ops.getStringValue(input).getOrThrow())), ops.empty()));
        }
    });

    public static Codec<EntityType<?>> ENTITY_TYPE_CODEC = Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(EntityType<?> input, DynamicOps<T> ops, T prefix) {
            return ops.mergeToPrimitive(prefix, ops.createString(Services.REGISTRY.getEntityTypeLocation(input).toString()));
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<EntityType<?>, T>> decode(DynamicOps<T> ops, T input) {
            return DataResult.success(Pair.of(Services.REGISTRY.getEntityType(ResourceLocation.parse(ops.getStringValue(input).getOrThrow())), ops.empty()));
        }
    });

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
                EntityType<?> entityType = ((MobBucketItemAccessor) bucket).bucketlib_getEntityType();
                if (entityType != null && level != null && entityType.create(level) instanceof Bucketable) {
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

    public record BucketBlock(Block block, SolidBucketItem bucketItem) {}

    public record BucketEntity(EntityType<?> entityType, Fluid fluid, MobBucketItem bucketItem) {}

}
