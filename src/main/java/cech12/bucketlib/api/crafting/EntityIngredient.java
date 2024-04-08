package cech12.bucketlib.api.crafting;

import cech12.bucketlib.BucketLib;
import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.util.BucketLibUtil;
import cech12.bucketlib.util.RegistryUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EntityIngredient extends Ingredient {

    protected final EntityType<?> entityType;
    protected final TagKey<EntityType<?>> tag;
    private ItemStack[] matchingStacks;

    private EntityIngredient(EntityType<?> entityType, TagKey<EntityType<?>> tag) {
        super(Stream.of());
        this.entityType = entityType;
        this.tag = tag;
    }

    public EntityIngredient(EntityType<?> entityType) {
        this(entityType, null);
    }

    public EntityIngredient(TagKey<EntityType<?>> tag) {
        this(null, tag);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty() || itemStack.getCount() > 1) { //TODO count cannot be limited!
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
                BucketLib.getRegisteredBuckets().forEach(bucket -> {
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
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Nonnull
    public JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", Serializer.NAME.toString());
        if (this.entityType != null) {
            jsonObject.addProperty("entity", Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(this.entityType)).toString());
        }
        if (this.tag != null) {
            jsonObject.addProperty("tag", this.tag.location().toString());
        }
        return jsonObject;
    }

    public static final class Serializer implements IIngredientSerializer<EntityIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLibApi.MOD_ID, "entity");

        private Serializer() {}

        @Nonnull
        @Override
        public EntityIngredient parse(@Nonnull FriendlyByteBuf buffer) {
            String entity = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<EntityType<?>> tag = TagKey.create(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), new ResourceLocation(tagId));
                return new EntityIngredient(tag);
            }
            if (entity.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a entity ingredient with no entity or tag.");
            }
            return new EntityIngredient(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entity)));
        }

        @Nonnull
        @Override
        public EntityIngredient parse(@Nonnull JsonObject json) {
            if (json.has("tag")) {
                ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
                TagKey<EntityType<?>> tag = TagKey.create(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), tagId);
                return new EntityIngredient(tag);
            } else {
                ResourceLocation entity = new ResourceLocation(json.get("entity").getAsString());
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(entity)) {
                    throw new JsonSyntaxException("Unknown entity type: " + entity);
                }
                return new EntityIngredient(ForgeRegistries.ENTITY_TYPES.getValue(entity));
            }
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull EntityIngredient ingredient) {
            buffer.writeUtf(ingredient.entityType != null ? Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(ingredient.entityType)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    }
}
