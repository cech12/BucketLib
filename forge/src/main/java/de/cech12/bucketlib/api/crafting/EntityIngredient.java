package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class EntityIngredient extends AbstractIngredient {

    protected final EntityType<?> entityType;
    protected final TagKey<EntityType<?>> tag;
    private ItemStack[] matchingStacks;

    private EntityIngredient(EntityType<?> entityType, TagKey<EntityType<?>> tag) {
        super(Stream.of());
        this.entityType = entityType;
        this.tag = tag;
    }

    public EntityIngredient(Optional<ResourceLocation> entityTypeOptional, Optional<TagKey<EntityType<?>>> tagOptional) {
        this(entityTypeOptional.map(ForgeRegistries.ENTITY_TYPES::getValue).orElse(null), tagOptional.orElse(null));
    }

    public EntityIngredient(EntityType<?> entityType) {
        this(entityType, null);
    }

    public EntityIngredient(TagKey<EntityType<?>> tag) {
        this(null, tag);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        Iterable<EntityType<?>> entityTypeIterator;
        if (this.entityType != null) {
            entityTypeIterator = List.of(this.entityType);
        } else {
            entityTypeIterator = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.tags()).getTag(this.tag);
        }
        for (EntityType<?> entityType : entityTypeIterator) {
            RegistryUtil.BucketEntity bucketEntity = RegistryUtil.getBucketEntity(entityType);
            if (bucketEntity != null) {
                if (itemStack.getItem() == bucketEntity.bucketItem()) {
                    return true;
                }
                return BucketLibUtil.getEntityType(itemStack) == entityType;
            }
        }
        return false;
    }

    @Override
    @Nonnull
    public ItemStack[] getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            List<EntityType<?>> entityTypes = new ArrayList<>();
            ITagManager<EntityType<?>> entityTypeTags = ForgeRegistries.ENTITY_TYPES.tags();
            if (this.tag != null && entityTypeTags != null) {
                entityTypeTags.getTag(this.tag).forEach(entityTypes::add);
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
        return this.matchingStacks;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    protected void invalidate() {
        this.matchingStacks = null;
    }

    @Override
    @Nonnull
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    public static final IIngredientSerializer<EntityIngredient> SERIALIZER = new IIngredientSerializer<>() {

        private static final MapCodec<EntityIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
                builder.group(
                        ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(i -> Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getKey(i.entityType))),
                        TagKey.codec(ForgeRegistries.ENTITY_TYPES.getRegistryKey()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
                ).apply(builder, EntityIngredient::new)
        );

        @Override
        public MapCodec<? extends EntityIngredient> codec() {
            return CODEC;
        }

        @Override
        public EntityIngredient read(RegistryFriendlyByteBuf buffer) {
            String block = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<EntityType<?>> tag = TagKey.create(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), new ResourceLocation(tagId));
                return new EntityIngredient(tag);
            }
            if (block.isEmpty()) {
                throw new IllegalArgumentException("Cannot create an entity ingredient with no entity or tag.");
            }
            return new EntityIngredient(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(block)));
        }

        @Override
        public void write(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull EntityIngredient ingredient) {
            buffer.writeUtf(ingredient.entityType != null ? Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(ingredient.entityType)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    };
}
