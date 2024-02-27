package cech12.bucketlib.item.crafting;

import cech12.bucketlib.api.item.UniversalBucketItem;
import cech12.bucketlib.util.BucketLibUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BucketFillingShapelessRecipe extends ShapelessRecipe {

    private final CraftingBookCategory category;
    private final List<Ingredient> ingredients;
    private final BucketFillingType fillingType;
    private final Fluid fluid;
    private final Block block;
    private final EntityType<?> entityType;

    public BucketFillingShapelessRecipe(ResourceLocation id, String group, CraftingBookCategory category, NonNullList<Ingredient> ingredients, BucketFillingType fillingType, Fluid fluid, Block block, EntityType<?> entityType) {
        super(id, group, category, getAssembledBucket(fillingType, fluid, block, entityType, ingredients.stream().map(ingredient -> Arrays.stream(ingredient.getItems()).toList()).flatMap(List::stream).toList()), ingredients);
        this.category = category;
        this.ingredients = ingredients;
        this.fillingType = fillingType;
        this.fluid = fluid;
        this.block = block;
        this.entityType = entityType;
    }

    private static ItemStack getAffectedBucket(List<ItemStack> itemStacks) {
        for (ItemStack stack : itemStacks) {
            if (stack.getItem() instanceof UniversalBucketItem && BucketLibUtil.isEmpty(stack)) {
                ItemStack bucket = stack.copy();
                bucket.setCount(1);
                return bucket;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getAssembledBucket(BucketFillingType fillingType, Fluid fluid, Block block, EntityType<?> entityType, List<ItemStack> itemStacks) {
        ItemStack bucket = getAffectedBucket(itemStacks);
        if (bucket.getItem() instanceof UniversalBucketItem universalBucketItem) {
            if (fillingType == BucketFillingType.BLOCK && universalBucketItem.canHoldBlock(block)) {
                return BucketLibUtil.addBlock(bucket, block);
            } else if (fillingType == BucketFillingType.ENTITY && universalBucketItem.canHoldEntity(entityType) && (fluid == null || universalBucketItem.canHoldFluid(fluid))) {
                if (fluid != null) {
                    bucket = BucketLibUtil.addFluid(bucket, fluid);
                }
                return BucketLibUtil.addEntityType(bucket, entityType);
            } else if (fillingType == BucketFillingType.FLUID && universalBucketItem.canHoldFluid(fluid)) {
                return BucketLibUtil.addFluid(bucket, fluid);
            } else if (fillingType == BucketFillingType.MILK && universalBucketItem.canMilkEntities()) {
                return BucketLibUtil.addMilk(bucket);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(@Nonnull CraftingContainer inv, @Nonnull Level level) {
        ItemStack bucket = getAffectedBucket(inv.getItems());
        if (bucket == ItemStack.EMPTY) {
            return false;
        }
        UniversalBucketItem universalBucketItem = ((UniversalBucketItem)bucket.getItem());
        return super.matches(inv, level)
                && (this.fillingType != BucketFillingType.BLOCK || universalBucketItem.canHoldBlock(this.block))
                && (this.fillingType != BucketFillingType.ENTITY || (universalBucketItem.canHoldEntity(this.entityType) && (this.fluid == null || universalBucketItem.canHoldFluid(this.fluid))))
                && (this.fillingType != BucketFillingType.FLUID || universalBucketItem.canHoldFluid(this.fluid))
                && (this.fillingType != BucketFillingType.MILK || universalBucketItem.canMilkEntities());
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    @Nonnull
    public ItemStack assemble(@Nonnull CraftingContainer inv, @Nonnull RegistryAccess registryAccess) {
        return getAssembledBucket(this.fillingType, this.fluid, this.block, this.entityType, inv.getItems());
    }

    @Override
    @Nonnull
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<BucketFillingShapelessRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        public Serializer() {
        }

        @Nonnull
        public BucketFillingShapelessRecipe fromJson(@Nonnull ResourceLocation id, @Nonnull JsonObject json) {
            String s = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory craftingbookcategory = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            NonNullList<Ingredient> nonnulllist = itemsFromJson(GsonHelper.getAsJsonArray(json, "ingredients"));
            if (nonnulllist.isEmpty()) {
                throw new JsonParseException("No ingredients for bucket filling shapeless recipe");
            } else if (nonnulllist.size() > 9) {
                throw new JsonParseException("Too many ingredients for bucket filling shapeless recipe. The maximum is 9.");
            }
            BucketFillingType fillingType = BucketFillingType.CODEC.byName(GsonHelper.getAsString(json, "filling_type", null), BucketFillingType.FLUID);
            Fluid fluid = null;
            Block block = null;
            EntityType<?> entityType = null;
            if (fillingType == BucketFillingType.BLOCK) {
                if (!json.has("block")) throw new JsonParseException("Cannot create a bucket filling recipe of fillingType \"block\" without a defined block.");
                block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(json.get("block").getAsString()));
            } else if (fillingType == BucketFillingType.ENTITY) {
                if (!json.has("entity")) throw new JsonParseException("Cannot create a bucket filling recipe of fillingType \"entity\" without a defined entity.");
                entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(json.get("entity").getAsString()));
                fluid = json.has("fluid") ? ForgeRegistries.FLUIDS.getValue(new ResourceLocation(json.get("fluid").getAsString())) : null;
            } else if (fillingType == BucketFillingType.FLUID) {
                if (!json.has("fluid")) throw new JsonParseException("Cannot create a bucket filling recipe of fillingType \"fluid\" without a defined fluid.");
                fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(json.get("fluid").getAsString()));
            } else if (fillingType != BucketFillingType.MILK) {
                throw new JsonParseException("Cannot create a bucket filling recipe of fillingType \"" + fillingType + "\".");
            }
            return new BucketFillingShapelessRecipe(id, s, craftingbookcategory, nonnulllist, fillingType, fluid, block, entityType);
        }

        private static NonNullList<Ingredient> itemsFromJson(JsonArray p_44276_) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();

            for(int i = 0; i < p_44276_.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(p_44276_.get(i), false);
                nonnulllist.add(ingredient);
            }

            return nonnulllist;
        }

        public BucketFillingShapelessRecipe fromNetwork(@Nonnull ResourceLocation id, FriendlyByteBuf buffer) {
            String s = buffer.readUtf();
            CraftingBookCategory craftingbookcategory = buffer.readEnum(CraftingBookCategory.class);
            int i = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(i, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
            BucketFillingType fillingType = buffer.readEnum(BucketFillingType.class);
            Fluid fluid = null;
            Block block = null;
            EntityType<?> entityType = null;
            if (fillingType == BucketFillingType.BLOCK) {
                block = ForgeRegistries.BLOCKS.getValue(buffer.readResourceLocation());
            } else if (fillingType == BucketFillingType.ENTITY) {
                entityType = ForgeRegistries.ENTITY_TYPES.getValue(buffer.readResourceLocation());
                fluid = ForgeRegistries.FLUIDS.getValue(buffer.readResourceLocation());
                if (fluid == Fluids.EMPTY) {
                    fluid = null;
                }
            } else if (fillingType == BucketFillingType.FLUID) {
                fluid = ForgeRegistries.FLUIDS.getValue(buffer.readResourceLocation());
            }
            return new BucketFillingShapelessRecipe(id, s, craftingbookcategory, ingredients, fillingType, fluid, block, entityType);
        }

        public void toNetwork(FriendlyByteBuf buffer, BucketFillingShapelessRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category);
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeEnum(recipe.fillingType);
            if (recipe.fillingType == BucketFillingType.BLOCK) {
                buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(recipe.block)));
            } else if (recipe.fillingType == BucketFillingType.ENTITY) {
                buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(recipe.entityType)));
                buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(recipe.fluid != null ? recipe.fluid : Fluids.EMPTY)));
            } else if (recipe.fillingType == BucketFillingType.FLUID) {
                buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(recipe.fluid)));
            }
        }
    }


}
