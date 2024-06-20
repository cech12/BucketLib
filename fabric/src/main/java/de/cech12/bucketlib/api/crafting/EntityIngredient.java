package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EntityIngredient implements CustomIngredient {

    protected final EntityType<?> entityType;
    protected final TagKey<EntityType<?>> tag;
    private List<ItemStack> matchingStacks;

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
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
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
    public List<ItemStack> getMatchingStacks() {
        if (this.matchingStacks == null) {
            this.matchingStacks = new ArrayList<>();
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
                this.matchingStacks.add(new ItemStack(bucketEntity.bucketItem()));
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
                        this.matchingStacks.add(filledBucket);
                    }
                });
            }
        }
        return this.matchingStacks;
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements CustomIngredientSerializer<EntityIngredient> {

        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = BucketLib.id("entity");

        public static final MapCodec<EntityIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
                builder.group(
                        ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(i -> Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(i.entityType))),
                        TagKey.codec(BuiltInRegistries.ENTITY_TYPE.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
                ).apply(builder, EntityIngredient::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, EntityIngredient> PACKET_CODEC = StreamCodec.of(
                EntityIngredient.Serializer::write,
                EntityIngredient.Serializer::read);

        private Serializer() {}

        @Override
        public ResourceLocation getIdentifier() {
            return NAME;
        }

        @Override
        public MapCodec<EntityIngredient> getCodec(boolean allowEmpty) {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EntityIngredient> getPacketCodec() {
            return PACKET_CODEC;
        }

        @Nonnull
        private static EntityIngredient read(RegistryFriendlyByteBuf buffer) {
            String entity = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(tagId));
                return new EntityIngredient(tag);
            }
            if (entity.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a entity ingredient with no entity or tag.");
            }
            return new EntityIngredient(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entity)));
        }

        private static void write(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull EntityIngredient ingredient) {
            buffer.writeUtf(ingredient.entityType != null ? Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(ingredient.entityType)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    }

}
