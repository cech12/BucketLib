package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Optional;
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
        ResourceLocation location = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        //Mekansim tanks are not compatible: https://github.com/cech12/BucketLib/issues/55 | https://github.com/mekanism/Mekanism/issues/8335
        if (location != null && "mekanism".equals(location.getNamespace()) && itemStack.getCraftingRemainingItem().isEmpty()) {
            return false;
        }
        if (ForgeMod.MILK.isPresent()) {
            ItemStack container = ItemHandlerHelper.copyStackWithSize(itemStack, 1);
            Optional<FluidStack> drainedFluidOptional = FluidUtil.getFluidHandler(container)
                    .map(fluidHandler -> fluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE));
            if (drainedFluidOptional.isPresent() && !drainedFluidOptional.get().isEmpty()) {
                FluidStack drainedFluid = drainedFluidOptional.get();
                return drainedFluid.getFluid() == ForgeMod.MILK.get() && drainedFluid.getAmount() == FluidType.BUCKET_VOLUME;
            }
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
