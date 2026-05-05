package de.cech12.bucketlib.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class BucketFillingShapelessRecipe extends ShapelessRecipe {

    private static final MapCodec<BucketFillingShapelessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    CommonInfo.MAP_CODEC.forGetter((recipe) -> recipe.commonInfo),
                    CraftingBookInfo.MAP_CODEC.forGetter((recipe) -> recipe.bookInfo),
                    Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter((o) -> o.ingredients),
                    BucketFillingType.CODEC.fieldOf("filling_type").forGetter((recipe) -> recipe.fillingType),
                    RegistryUtil.FLUID_CODEC.optionalFieldOf("fluid").forGetter(recipe -> Optional.of(recipe.fluid)),
                    RegistryUtil.BLOCK_CODEC.optionalFieldOf("block").forGetter(recipe -> Optional.of(recipe.block)),
                    RegistryUtil.ENTITY_TYPE_CODEC.optionalFieldOf("entity").forGetter(recipe -> Optional.of(recipe.entityType))
            ).apply(instance, BucketFillingShapelessRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BucketFillingShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            CommonInfo.STREAM_CODEC, (recipe) -> recipe.commonInfo,
            CraftingBookInfo.STREAM_CODEC, (recipe) -> recipe.bookInfo,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), (o) -> o.ingredients,
            BucketFillingType.STREAM_CODEC, (recipe) -> recipe.fillingType,
            RegistryUtil.STREAM_FLUID_CODEC, (recipe) -> recipe.fluid,
            RegistryUtil.STREAM_BLOCK_CODEC, (recipe) -> recipe.block,
            RegistryUtil.STREAM_ENTITY_TYPE_CODEC, (recipe) -> recipe.entityType,
            BucketFillingShapelessRecipe::createFromNetwork
    );
    public static final RecipeSerializer<BucketFillingShapelessRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Recipe.CommonInfo commonInfo;
    private final CraftingRecipe.CraftingBookInfo bookInfo;
    private final List<Ingredient> ingredients;
    private final BucketFillingType fillingType;
    private final Holder<Fluid> fluid;
    private final Holder<Block> block;
    private final Holder<EntityType<?>> entityType;

    private static BucketFillingShapelessRecipe createFromNetwork(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, List<Ingredient> ingredients, BucketFillingType fillingType, Holder<Fluid> fluid, Holder<Block> block, Holder<EntityType<?>> entityType) {
        return new BucketFillingShapelessRecipe(commonInfo, bookInfo, ingredients, fillingType,
                fluid != null ? Optional.of(fluid) : Optional.empty(),
                block != null ? Optional.of(block) : Optional.empty(),
                entityType != null ? Optional.of(entityType) : Optional.empty());
    }

    public BucketFillingShapelessRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, List<Ingredient> ingredients, BucketFillingType fillingType, Optional<Holder<Fluid>> fluid, Optional<Holder<Block>> block, Optional<Holder<EntityType<?>>> entityType) {
        super(commonInfo, bookInfo, ItemStackTemplate.fromNonEmptyStack(getAssembledBucket(fillingType, fluid.orElse(null), block.orElse(null), entityType.orElse(null), ingredients.stream().map(ingredient -> ingredient.items().map(itemHolder -> new ItemStack(itemHolder.value())).toList()).flatMap(List::stream).toList())), ingredients);
        this.commonInfo = commonInfo;
        this.bookInfo = bookInfo;
        this.ingredients = ingredients;
        this.fillingType = fillingType;
        this.fluid = fluid.orElse(null);
        this.block = block.orElse(null);
        this.entityType = entityType.orElse(null);
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

    private static ItemStack getAssembledBucket(BucketFillingType fillingType, Holder<Fluid> fluid, Holder<Block> block, Holder<EntityType<?>> entityType, List<ItemStack> itemStacks) {
        ItemStack bucket = getAffectedBucket(itemStacks);
        if (bucket.getItem() instanceof UniversalBucketItem universalBucketItem) {
            if (fillingType == BucketFillingType.BLOCK && universalBucketItem.canHoldBlock(block.value())) {
                return BucketLibUtil.addBlock(bucket, block.value());
            } else if (fillingType == BucketFillingType.ENTITY && universalBucketItem.canHoldEntity(entityType.value()) && (fluid == null || universalBucketItem.canHoldFluid(fluid.value()))) {
                if (fluid != null) {
                    bucket = BucketLibUtil.addFluid(bucket, fluid.value());
                }
                return BucketLibUtil.addEntityType(bucket, entityType.value());
            } else if (fillingType == BucketFillingType.FLUID && universalBucketItem.canHoldFluid(fluid.value())) {
                return BucketLibUtil.addFluid(bucket, fluid.value());
            } else if (fillingType == BucketFillingType.MILK && universalBucketItem.canMilkEntities()) {
                return BucketLibUtil.addMilk(bucket);
            }
        }
        throw new IllegalStateException("BucketFillingShapelessRecipe was used for a non UniversalBucketItem. This is not supported!");
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        ItemStack bucket = getAffectedBucket(input.items());
        if (bucket == ItemStack.EMPTY) {
            return false;
        }
        UniversalBucketItem universalBucketItem = ((UniversalBucketItem)bucket.getItem());
        return super.matches(input, level)
                && (this.fillingType != BucketFillingType.BLOCK || universalBucketItem.canHoldBlock(this.block.value()))
                && (this.fillingType != BucketFillingType.ENTITY || (universalBucketItem.canHoldEntity(this.entityType.value()) && (this.fluid == null || universalBucketItem.canHoldFluid(this.fluid.value()))))
                && (this.fillingType != BucketFillingType.FLUID || universalBucketItem.canHoldFluid(this.fluid.value()))
                && (this.fillingType != BucketFillingType.MILK || universalBucketItem.canMilkEntities());
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    @NotNull
    public ItemStack assemble(CraftingInput input) {
        return getAssembledBucket(this.fillingType, this.fluid, this.block, this.entityType, input.items());
    }

    @Override
    @NotNull
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (Object) SERIALIZER;
    }

}
