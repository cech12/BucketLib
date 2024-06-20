package de.cech12.bucketlib.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BucketFillingShapelessRecipe extends ShapelessRecipe {

    private final CraftingBookCategory category;
    private final NonNullList<Ingredient> ingredients;
    private final BucketFillingType fillingType;
    private final Fluid fluid;
    private final Block block;
    private final EntityType<?> entityType;

    public BucketFillingShapelessRecipe(String group, CraftingBookCategory category, NonNullList<Ingredient> ingredients, BucketFillingType fillingType, Optional<Fluid> fluid, Optional<Block> block, Optional<EntityType<?>> entityType) {
        super(group, category, getAssembledBucket(fillingType, fluid.orElse(null), block.orElse(null), entityType.orElse(null), ingredients.stream().map(ingredient -> Arrays.stream(ingredient.getItems()).toList()).flatMap(List::stream).toList()), ingredients);
        this.category = category;
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
    public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) {
        ItemStack bucket = getAffectedBucket(input.items());
        if (bucket == ItemStack.EMPTY) {
            return false;
        }
        UniversalBucketItem universalBucketItem = ((UniversalBucketItem)bucket.getItem());
        return super.matches(input, level)
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
    public ItemStack assemble(CraftingInput input, @Nonnull HolderLookup.Provider provider) {
        return getAssembledBucket(this.fillingType, this.fluid, this.block, this.entityType, input.items());
    }

    @Override
    @Nonnull
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<BucketFillingShapelessRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<BucketFillingShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec((record) ->
                record.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((recipe) -> recipe.category),
                        Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(
                                ($$0x) -> {
                                    Ingredient[] $$1 = $$0x.stream().filter(($$0xx) -> !$$0xx.isEmpty()).toArray(Ingredient[]::new);
                                    if ($$1.length == 0) {
                                        return DataResult.error(() -> "No ingredients for shapeless recipe");
                                    } else {
                                        return $$1.length > 9 ? DataResult.error(() -> "Too many ingredients for shapeless recipe") : DataResult.success(NonNullList.of(Ingredient.EMPTY, $$1));
                                    }}, DataResult::success).forGetter((recipe) -> recipe.ingredients),
                        BucketFillingType.CODEC.fieldOf("filling_type").forGetter((recipe) -> recipe.fillingType),
                        RegistryUtil.FLUID_CODEC.optionalFieldOf("fluid").forGetter(recipe -> Optional.of(recipe.fluid)),
                        RegistryUtil.BLOCK_CODEC.optionalFieldOf("block").forGetter(recipe -> Optional.of(recipe.block)),
                        RegistryUtil.ENTITY_TYPE_CODEC.optionalFieldOf("entity").forGetter(recipe -> Optional.of(recipe.entityType))
                ).apply(record, BucketFillingShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BucketFillingShapelessRecipe> STREAM_CODEC = StreamCodec.of(
                BucketFillingShapelessRecipe.Serializer::toNetwork,
                BucketFillingShapelessRecipe.Serializer::fromNetwork);

        private Serializer() {
        }

        @Override
        @Nonnull
        public MapCodec<BucketFillingShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        @Nonnull
        public StreamCodec<RegistryFriendlyByteBuf, BucketFillingShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        @Nonnull
        private static BucketFillingShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            int i = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(i, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            BucketFillingType fillingType = BucketFillingType.valueOf(buf.readUtf());
            Optional<Fluid> fluid = Optional.empty();
            Optional<Block> block = Optional.empty();
            Optional<EntityType<?>> entityType = Optional.empty();
            if (fillingType == BucketFillingType.BLOCK) {
                block = Optional.of(Services.REGISTRY.getBlock(buf.readResourceLocation()));
            } else if (fillingType == BucketFillingType.ENTITY) {
                entityType = Optional.of(Services.REGISTRY.getEntityType(buf.readResourceLocation()));
                fluid = Optional.of(Services.REGISTRY.getFluid(buf.readResourceLocation()));
                if (fluid.get() == Fluids.EMPTY) {
                    fluid = Optional.empty();
                }
            } else if (fillingType == BucketFillingType.FLUID) {
                fluid = Optional.of(Services.REGISTRY.getFluid(buf.readResourceLocation()));
            }
            return new BucketFillingShapelessRecipe(group, category, ingredients, fillingType, fluid, block, entityType);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, BucketFillingShapelessRecipe recipe) {
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            buf.writeEnum(recipe.fillingType);
            if (recipe.fillingType == BucketFillingType.BLOCK) {
                buf.writeResourceLocation(Objects.requireNonNull(Services.REGISTRY.getBlockLocation(recipe.block)));
            } else if (recipe.fillingType == BucketFillingType.ENTITY) {
                buf.writeResourceLocation(Objects.requireNonNull(Services.REGISTRY.getEntityTypeLocation(recipe.entityType)));
                buf.writeResourceLocation(Objects.requireNonNull(Services.REGISTRY.getFluidLocation((recipe.fluid != null ? recipe.fluid : Fluids.EMPTY))));
            } else if (recipe.fillingType == BucketFillingType.FLUID) {
                buf.writeResourceLocation(Services.REGISTRY.getFluidLocation(recipe.fluid));
            }
        }
    }

}
