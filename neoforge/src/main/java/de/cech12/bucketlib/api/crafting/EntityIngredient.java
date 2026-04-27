package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class EntityIngredient implements ICustomIngredient {

    protected final EntityType<?> entityType;
    protected final TagKey<EntityType<?>> tag;
    private Holder<Item>[] matchingStacks;

    private EntityIngredient(EntityType<?> entityType, TagKey<EntityType<?>> tag) {
        this.entityType = entityType;
        this.tag = tag;
    }

    public EntityIngredient(Optional<Identifier> blockOptional, Optional<TagKey<EntityType<?>>> tagOptional) {
        this(blockOptional.map(BuiltInRegistries.ENTITY_TYPE::get).filter(Optional::isPresent).map(itemReference -> itemReference.get().value()).orElse(null), tagOptional.orElse(null));
    }

    public EntityIngredient(EntityType<?> entityType) {
        this(entityType, null);
    }

    public EntityIngredient(TagKey<EntityType<?>> tag) {
        this(null, tag);
    }

    @Override
    public boolean test(@NotNull ItemStack itemStack) {
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
            bucketEntities = RegistryUtil.getBucketEntities().stream().filter(bucketBlock -> bucketBlock.entityType().builtInRegistryHolder().is(this.tag)).toList();
        }
        if (bucketEntities.isEmpty()) {
            return false;
        }
        RegistryUtil.BucketEntity bucketEntity = bucketEntities.getFirst();
        if (itemStack.getItem() == bucketEntity.bucketItem()) {
            return true;
        }
        return BucketLibUtil.getEntityType(itemStack) == bucketEntity.entityType();
    }

    @Override
    @NotNull
    public Stream<Holder<Item>> items() {
        if (this.matchingStacks == null) {
            ArrayList<Holder<Item>> stacks = new ArrayList<>();
            List<EntityType<?>> entityTypes = new ArrayList<>();
            Optional<HolderSet.Named<EntityType<?>>> entityTag = Optional.empty();
            if (this.tag != null) {
                BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(this.tag).forEach(fluid -> entityTypes.add(fluid.value()));
            } else if (this.entityType != null) {
                entityTypes.add(this.entityType);
            }
            List<RegistryUtil.BucketEntity> bucketEntities = RegistryUtil.getBucketEntities().stream().filter(bucketEntity -> entityTypes.contains(bucketEntity.entityType())).toList();
            //vanilla buckets
            for (RegistryUtil.BucketEntity bucketEntity : bucketEntities) {
                stacks.add(Holder.direct(bucketEntity.bucketItem()));
            }
            //bucket lib buckets
            for (RegistryUtil.BucketEntity bucketEntity : bucketEntities) {
                BucketLibMod.getRegisteredBuckets().forEach(bucket -> {
                    if (bucket.canHoldFluid(bucketEntity.fluid()) && bucket.canHoldEntity(bucketEntity.entityType())) {
                        /*
                        ItemStack filledBucket = new ItemStack(bucket);
                        if (bucketEntity.fluid() != Fluids.EMPTY) {
                            filledBucket = BucketLibUtil.addFluid(filledBucket, bucketEntity.fluid());
                        }
                        filledBucket = BucketLibUtil.addEntityType(filledBucket, bucketEntity.entityType());
                         */
                        stacks.add(Holder.direct(bucket));
                    }
                });
            }
            this.matchingStacks = stacks.toArray(new Holder[0]);
        }
        return Stream.of(this.matchingStacks);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    @NotNull
    public IngredientType<?> getType() {
        return TYPE;
    }

    public static final MapCodec<EntityIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Identifier.CODEC.optionalFieldOf("entity").forGetter(i -> Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(i.entityType))),
                    TagKey.codec(BuiltInRegistries.ENTITY_TYPE.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, EntityIngredient::new)
    );

    public static final IngredientType<EntityIngredient> TYPE = new IngredientType<>(CODEC);

}
