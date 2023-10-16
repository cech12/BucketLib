package cech12.bucketlib.item.crafting;

import cech12.bucketlib.api.item.UniversalBucketItem;
import cech12.bucketlib.util.ColorUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BucketDyeingRecipe extends CustomRecipe {

    public BucketDyeingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Nullable
    private Pair<ItemStack, List<DyeItem>> getBucketAndDyes(@Nonnull CraftingContainer inv) {
        ItemStack bucket = ItemStack.EMPTY;
        List<DyeItem> dyeItems = Lists.newArrayList();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (item instanceof UniversalBucketItem) {
                if (!bucket.isEmpty() || !((UniversalBucketItem) item).isDyeable()) {
                    return null;
                }
                bucket = stack;
            } else if (!(item instanceof DyeItem)) {
                return null;
            } else {
                dyeItems.add((DyeItem)item);
            }
        }
        if (bucket.isEmpty() || dyeItems.isEmpty()) {
            return null;
        }
        return new Pair<>(bucket, dyeItems);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(@Nonnull CraftingContainer inv, @Nonnull Level worldIn) {
        Pair<ItemStack, List<DyeItem>> bucketAndDyes = getBucketAndDyes(inv);
        return bucketAndDyes != null;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    @Nonnull
    public ItemStack assemble(@Nonnull CraftingContainer inv, RegistryAccess registryAccess) {
        Pair<ItemStack, List<DyeItem>> bucketAndDyes = getBucketAndDyes(inv);
        if (bucketAndDyes != null) {
            return ColorUtil.dyeItem(bucketAndDyes.getFirst(), bucketAndDyes.getSecond());
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        //override it to avoid remaining container items
        return NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    @Nonnull
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer extends SimpleCraftingRecipeSerializer<BucketDyeingRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        public Serializer() {
            super(BucketDyeingRecipe::new);
        }
    }

}
