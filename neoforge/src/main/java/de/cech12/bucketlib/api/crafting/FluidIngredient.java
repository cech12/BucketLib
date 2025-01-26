package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FluidIngredient implements ICustomIngredient {

    protected final Fluid fluid;
    protected final TagKey<Fluid> tag;
    private Holder<Item>[] matchingStacks;

    private FluidIngredient(Fluid fluid, TagKey<Fluid> tag) {
        this.fluid = fluid;
        this.tag = tag;
    }

    public FluidIngredient(Optional<ResourceLocation> fluidOptional, Optional<TagKey<Fluid>> tagOptional) {
        this(fluidOptional.map(BuiltInRegistries.FLUID::get).filter(Optional::isPresent).map(itemReference -> itemReference.get().value()).orElse(null), tagOptional.orElse(null));
    }

    public FluidIngredient(Fluid fluid) {
        this(fluid, null);
    }

    public FluidIngredient(TagKey<Fluid> tag) {
        this((Fluid)null, tag);
    }

    private boolean isFluidCorrect(Fluid fluid) {
        return fluid != null && (
                (this.fluid != null && fluid.isSame(this.fluid))
                        || (this.tag != null && fluid.defaultFluidState().is(this.tag))
        );
    }

    @Override
    public boolean test(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        ItemStack container = itemStack.copyWithCount(1);
        Optional<FluidStack> drainedFluidOptional = FluidUtil.getFluidHandler(container)
                .map(fluidHandler -> fluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE));
        if (drainedFluidOptional.isPresent() && !drainedFluidOptional.get().isEmpty()) {
            FluidStack drainedFluid = drainedFluidOptional.get();
            return this.isFluidCorrect(drainedFluid.getFluid()) && drainedFluid.getAmount() == FluidType.BUCKET_VOLUME;
        }
        return false;
    }

    @Override
    @Nonnull
    public Stream<Holder<Item>> items() {
        if (this.matchingStacks == null) {
            ArrayList<Holder<Item>> stacks = new ArrayList<>();
            List<Fluid> fluids = new ArrayList<>();
            if (this.tag != null) {
                BuiltInRegistries.FLUID.getTagOrEmpty(this.tag).forEach(fluid -> fluids.add(fluid.value()));
            } else if (this.fluid != null) {
                fluids.add(this.fluid);
            }
            for (Fluid fluid : fluids) {
                //vanilla bucket
                Item bucketItem = fluid.getBucket();
                if (!(bucketItem instanceof BucketItem) || Services.BUCKET.getFluidOfBucketItem((BucketItem) bucketItem) != fluid) {
                    continue; //skip fluids that have no vanilla bucket
                }
                stacks.add(Holder.direct(bucketItem));
                //bucket lib buckets
                BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                    if (universalBucketItem.canHoldFluid(fluid)) {
                        //stacks.add(BucketLibUtil.addFluid(new ItemStack(universalBucketItem), fluid));
                        stacks.add(Holder.direct(universalBucketItem));
                    }
                });
            }
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

    public static final MapCodec<FluidIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(i -> Optional.of(BuiltInRegistries.FLUID.getKey(i.fluid))),
                    TagKey.codec(BuiltInRegistries.FLUID.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, FluidIngredient::new)
    );

    public static final IngredientType<FluidIngredient> TYPE = new IngredientType<>(CODEC);

}
