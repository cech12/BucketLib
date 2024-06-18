package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.stream.Stream;

public class MilkIngredient implements ICustomIngredient {

    private ItemStack[] matchingStacks;

    public MilkIngredient() {
    }

    @Override
    public boolean test(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.getItem() == Items.MILK_BUCKET) {
            return true;
        }
        return BucketLibUtil.containsMilk(itemStack.copy());
    }

    @Override
    @Nonnull
    public Stream<ItemStack> getItems() {
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
        return Stream.of(this.matchingStacks);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    @Nonnull
    public IngredientType<?> getType() {
        return TYPE;
    }

    public static final MapCodec<MilkIngredient> CODEC = MapCodec.of(Encoder.empty(), Decoder.unit(new MilkIngredient()));

    public static final IngredientType<MilkIngredient> TYPE = new IngredientType<>(CODEC);

}
