package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

public class MilkIngredient implements ICustomIngredient {

    private Holder<Item>[] matchingStacks;

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
        ResourceLocation location = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        //Mekansim tanks are not compatible: https://github.com/cech12/BucketLib/issues/55 | https://github.com/mekanism/Mekanism/issues/8335
        if ("mekanism".equals(location.getNamespace()) && itemStack.getCraftingRemainder().isEmpty()) {
            return false;
        }
        if (NeoForgeMod.MILK.isBound()) {
            ItemStack container = itemStack.copyWithCount(1);
            Optional<FluidStack> drainedFluidOptional = FluidUtil.getFluidHandler(container)
                    .map(fluidHandler -> fluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE));
            if (drainedFluidOptional.isPresent() && !drainedFluidOptional.get().isEmpty()) {
                FluidStack drainedFluid = drainedFluidOptional.get();
                return drainedFluid.getFluid() == NeoForgeMod.MILK.get() && drainedFluid.getAmount() == FluidType.BUCKET_VOLUME;
            }
        }
        return BucketLibUtil.containsMilk(itemStack.copy());
    }

    @Override
    @Nonnull
    public Stream<Holder<Item>> items() {
        if (this.matchingStacks == null) {
            ArrayList<Holder<Item>> stacks = new ArrayList<>();
            stacks.add(Holder.direct(Items.MILK_BUCKET));
            BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                if (universalBucketItem.canMilkEntities()) {
                    //stacks.add(BucketLibUtil.addMilk(new ItemStack(universalBucketItem)));
                    stacks.add(Holder.direct(universalBucketItem));
                }
            });
            this.matchingStacks = stacks.toArray(new Holder[0]);
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
