package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class EntityIngredient implements ICustomIngredient {

    protected final EntityType<?> entityType;
    protected final TagKey<EntityType<?>> tag;
    private ItemStack[] matchingStacks;

    private EntityIngredient(EntityType<?> entityType, TagKey<EntityType<?>> tag) {
        this.entityType = entityType;
        this.tag = tag;
    }

    public EntityIngredient(Optional<ResourceLocation> blockOptional, Optional<TagKey<EntityType<?>>> tagOptional) {
        this(blockOptional.map(BuiltInRegistries.ENTITY_TYPE::get).orElse(null), tagOptional.orElse(null));
    }

    public EntityIngredient(EntityType<?> entityType) {
        this(entityType, null);
    }

    public EntityIngredient(TagKey<EntityType<?>> tag) {
        this(null, tag);
    }

    @Override
    public boolean test(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        List<RegistryUtil.BucketEntity> bucketEntities;
        if (this.entityType != null) {
            RegistryUtil.BucketEntity bucketEntity = RegistryUtil.getBucketEntity(this.entityType);
            if (bucketEntity == null) {
                return false;
            }
            bucketEntities = List.of(bucketEntity);
        } else {
            bucketEntities = RegistryUtil.getBucketEntities().stream().filter(bucketBlock -> bucketBlock.entityType().is(this.tag)).toList();
        }
        for (RegistryUtil.BucketEntity bucketEntity : bucketEntities) {
            if (itemStack.getItem() == bucketEntity.bucketItem()) {
                return true;
            }
            return BucketLibUtil.getEntityType(itemStack) == bucketEntity.entityType();
        }
        return false;
    }

    @Override
    @Nonnull
    public Stream<ItemStack> getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            List<EntityType<?>> entityTypes = new ArrayList<>();
            Optional<HolderSet.Named<EntityType<?>>> entityTag = Optional.empty();
            if (this.tag != null) {
                entityTag = BuiltInRegistries.ENTITY_TYPE.getTag(this.tag);
            }
            if (entityTag.isPresent()) {
                entityTag.get().forEach(fluid -> entityTypes.add(fluid.value()));
            } else if (this.entityType != null) {
                entityTypes.add(this.entityType);
            }
            List<RegistryUtil.BucketEntity> bucketEntities = RegistryUtil.getBucketEntities().stream().filter(bucketEntity -> entityTypes.contains(bucketEntity.entityType())).toList();
            //vanilla buckets
            for (RegistryUtil.BucketEntity bucketEntity : bucketEntities) {
                stacks.add(new ItemStack(bucketEntity.bucketItem()));
            }
            //bucket lib buckets
            for (RegistryUtil.BucketEntity bucketEntity : bucketEntities) {
                BucketLibMod.getRegisteredBuckets().forEach(bucket -> {
                    if (bucket.canHoldFluid(bucketEntity.fluid()) && bucket.canHoldEntity(bucketEntity.entityType())) {
                        ItemStack filledBucket = new ItemStack(bucket);
                        if (bucketEntity.fluid() != Fluids.EMPTY) {
                            filledBucket = BucketLibUtil.addFluid(filledBucket, bucketEntity.fluid());
                        }
                        filledBucket = BucketLibUtil.addEntityType(filledBucket, bucketEntity.entityType());
                        stacks.add(filledBucket);
                    }
                });
            }
            this.matchingStacks = stacks.toArray(new ItemStack[0]);
        }
        return Stream.of(this.matchingStacks);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    @Nonnull
    public IngredientType<?> getType() {
        return TYPE;
    }

    public static final MapCodec<EntityIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(i -> Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(i.entityType))),
                    TagKey.codec(BuiltInRegistries.ENTITY_TYPE.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, EntityIngredient::new)
    );

    public static final IngredientType<EntityIngredient> TYPE = new IngredientType<>(CODEC);

}
