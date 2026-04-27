package de.cech12.bucketlib.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class BucketFillingShapedRecipe extends ShapedRecipe {

    public static final MapCodec<BucketFillingShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(
            CommonInfo.MAP_CODEC.forGetter((recipe) -> recipe.commonInfo),
            CraftingBookInfo.MAP_CODEC.forGetter((recipe) -> recipe.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter((recipe) -> recipe.pattern),
            BucketFillingType.CODEC.fieldOf("filling_type").forGetter((recipe) -> recipe.fillingType),
            RegistryUtil.FLUID_CODEC.optionalFieldOf("fluid").forGetter(recipe -> recipe.fluid != null ? Optional.of(recipe.fluid) : Optional.empty()),
            RegistryUtil.BLOCK_CODEC.optionalFieldOf("block").forGetter(recipe -> recipe.block != null ? Optional.of(recipe.block) : Optional.empty()),
            RegistryUtil.ENTITY_TYPE_CODEC.optionalFieldOf("entity").forGetter(recipe -> recipe.entityType != null ? Optional.of(recipe.entityType) : Optional.empty()))
            .apply(i, BucketFillingShapedRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BucketFillingShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            CommonInfo.STREAM_CODEC, (recipe) -> recipe.commonInfo,
            CraftingBookInfo.STREAM_CODEC, (recipe) -> recipe.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, (recipe) -> recipe.pattern,
            BucketFillingType.STREAM_CODEC, (recipe) -> recipe.fillingType,
            RegistryUtil.STREAM_FLUID_CODEC, (recipe) -> recipe.fluid,
            RegistryUtil.STREAM_BLOCK_CODEC, (recipe) -> recipe.block,
            RegistryUtil.STREAM_ENTITY_TYPE_CODEC, (recipe) -> recipe.entityType,
            BucketFillingShapedRecipe::createFromNetwork
    );
    public static final RecipeSerializer<BucketFillingShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Recipe.CommonInfo commonInfo;
    private final CraftingRecipe.CraftingBookInfo bookInfo;
    private final ShapedRecipePattern pattern;
    private final BucketFillingType fillingType;
    private final Holder<Fluid> fluid;
    private final Holder<Block> block;
    private final Holder<EntityType<?>> entityType;

    private static BucketFillingShapedRecipe createFromNetwork(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, BucketFillingType fillingType, Holder<Fluid> fluid, Holder<Block> block, Holder<EntityType<?>> entityType) {
        return new BucketFillingShapedRecipe(commonInfo, bookInfo, pattern, fillingType,
                fluid != null ? Optional.of(fluid) : Optional.empty(),
                block != null ? Optional.of(block) : Optional.empty(),
                entityType != null ? Optional.of(entityType) : Optional.empty());
    }

    public BucketFillingShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, BucketFillingType fillingType, Optional<Holder<Fluid>> fluid, Optional<Holder<Block>> block, Optional<Holder<EntityType<?>>> entityType) {
        super(commonInfo, bookInfo, pattern, ItemStackTemplate.fromNonEmptyStack(getAssembledBucket(fillingType, fluid.orElse(null), block.orElse(null), entityType.orElse(null), pattern.ingredients().stream().filter(Optional::isPresent).map(ingredient -> ingredient.get().items().map(itemHolder -> new ItemStack(itemHolder.value())).toList()).flatMap(List::stream).toList())));
        this.commonInfo = commonInfo;
        this.bookInfo = bookInfo;
        this.pattern = pattern;
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
        return ItemStack.EMPTY;
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
    public ItemStack assemble(@NotNull CraftingInput input) {
        return getAssembledBucket(this.fillingType, this.fluid, this.block, this.entityType, input.items());
    }

    @Override
    @NotNull
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (Object) SERIALIZER;
    }
}
