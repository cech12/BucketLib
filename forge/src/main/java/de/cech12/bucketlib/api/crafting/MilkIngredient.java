package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.stream.Stream;

public class MilkIngredient extends AbstractIngredient {

    private ItemStack[] matchingStacks;

    public MilkIngredient() {
        super(Stream.of());
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.getItem() == Items.MILK_BUCKET) {
            return true;
        }
        return BucketLibUtil.containsMilk(itemStack.copy());
    }

    @Override
    @Nonnull
    public ItemStack[] getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            stacks.add(new ItemStack(Items.MILK_BUCKET));
            BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                if (universalBucketItem.canMilkEntities()) {
                    stacks.add(BucketLibUtil.addMilk(new ItemStack(universalBucketItem)));
                }
            });
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
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    public static final IIngredientSerializer<MilkIngredient> SERIALIZER = new IIngredientSerializer<>() {

        private static final MapCodec<MilkIngredient> CODEC = MapCodec.of(Encoder.empty(), Decoder.unit(new MilkIngredient()));

        @Override
        public MapCodec<? extends MilkIngredient> codec() {
            return CODEC;
        }

        @Override
        public MilkIngredient read(RegistryFriendlyByteBuf buffer) {
            return new MilkIngredient();
        }

        @Override
        public void write(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull MilkIngredient ingredient) {
        }
    };
}
