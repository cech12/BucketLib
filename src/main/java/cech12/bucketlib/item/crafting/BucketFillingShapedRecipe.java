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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BucketFillingShapedRecipe extends ShapedRecipe {

    private final CraftingBookCategory category;
    private final List<Ingredient> ingredients;
    private final BucketFillingType fillingType;
    private final Fluid fluid;
    private final Block block;
    private final EntityType<?> entityType;

    public BucketFillingShapedRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, BucketFillingType fillingType, Fluid fluid, Block block, EntityType<?> entityType) {
        super(id, group, category, width, height, ingredients, getAssembledBucket(fillingType, fluid, block, entityType, ingredients.stream().map(ingredient -> Arrays.stream(ingredient.getItems()).toList()).flatMap(List::stream).toList()));
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

    public static class Serializer implements RecipeSerializer<BucketFillingShapedRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        public Serializer() {
        }

        @Nonnull
        public BucketFillingShapedRecipe fromJson(@Nonnull ResourceLocation id, @Nonnull JsonObject json) {
            String s = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            try {
                Method keyFromJsonMethod;
                Method shrinkMethod;
                Method patternFromJsonMethod;
                Method dissolvePatternMethod;
                try {
                    keyFromJsonMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "keyFromJson", JsonObject.class);
                    shrinkMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "shrink", String[].class);
                    patternFromJsonMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "patternFromJson", JsonArray.class);
                    dissolvePatternMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "dissolvePattern", String[].class , Map.class, int.class, int.class);
                } catch (ObfuscationReflectionHelper.UnableToFindMethodException ex) {
                    //fallback for Forge
                    keyFromJsonMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "m_44210_", JsonObject.class);
                    shrinkMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "m_44186_", String[].class);
                    patternFromJsonMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "m_44196_", JsonArray.class);
                    dissolvePatternMethod = ObfuscationReflectionHelper.findMethod(ShapedRecipe.class, "m_44202_", String[].class , Map.class, int.class, int.class);
                }

                Map<String, Ingredient> map = (Map<String, Ingredient>) keyFromJsonMethod.invoke(null, GsonHelper.getAsJsonObject(json, "key"));
                String[] pattern = (String[]) patternFromJsonMethod.invoke(null, GsonHelper.getAsJsonArray(json, "pattern"));
                String[] shrinkedPattern = (String[]) shrinkMethod.invoke(null, (Object) pattern);
                int width = shrinkedPattern[0].length();
                int height = shrinkedPattern.length;
                NonNullList<Ingredient> ingredients = (NonNullList<Ingredient>) dissolvePatternMethod.invoke(null, shrinkedPattern, map, width, height);

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
                return new BucketFillingShapedRecipe(id, s, category, width, height, ingredients, fillingType, fluid, block, entityType);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        public BucketFillingShapedRecipe fromNetwork(@Nonnull ResourceLocation id, FriendlyByteBuf buffer) {
            int width = buffer.readVarInt();
            int height = buffer.readVarInt();
            String s = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> recipeItems = NonNullList.withSize(width * height, Ingredient.EMPTY);
            recipeItems.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
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
            return new BucketFillingShapedRecipe(id, s, category, width, height, recipeItems, fillingType, fluid, block, entityType);
        }

        public void toNetwork(FriendlyByteBuf buffer, BucketFillingShapedRecipe recipe) {
            buffer.writeVarInt(recipe.getWidth());
            buffer.writeVarInt(recipe.getHeight());
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category);
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
