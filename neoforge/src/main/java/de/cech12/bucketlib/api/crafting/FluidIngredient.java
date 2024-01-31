package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FluidIngredient extends Ingredient {

    protected final Fluid fluid;
    protected final TagKey<Fluid> tag;
    private ItemStack[] matchingStacks;

    private FluidIngredient(Fluid fluid, TagKey<Fluid> tag) {
        super(Stream.of());
        this.fluid = fluid;
        this.tag = tag;
    }

    public FluidIngredient(Optional<ResourceLocation> fluidOptional, Optional<TagKey<Fluid>> tagOptional) {
        this(fluidOptional.map(BuiltInRegistries.FLUID::get).orElse(null), tagOptional.orElse(null));
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
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        ItemStack container = ItemHandlerHelper.copyStackWithSize(itemStack, 1);
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
    public ItemStack[] getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            List<Fluid> fluids = new ArrayList<>();
            Optional<HolderSet.Named<Fluid>> fluidTag = Optional.empty();
            if (this.tag != null) {
                fluidTag = BuiltInRegistries.FLUID.getTag(this.tag);
            }
            if (fluidTag.isPresent()) {
                fluidTag.get().forEach(fluid -> fluids.add(fluid.value()));
            } else if (this.fluid != null) {
                fluids.add(this.fluid);
            }
            for (Fluid fluid : fluids) {
                //vanilla bucket
                Item bucketItem = fluid.getBucket();
                if (!(bucketItem instanceof BucketItem) || ((BucketItem) bucketItem).getFluid() != fluid) {
                    continue; //skip fluids that have no vanilla bucket
                }
                stacks.add(new ItemStack(bucketItem));
                //bucket lib buckets
                BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                    if (universalBucketItem.canHoldFluid(fluid)) {
                        stacks.add(BucketLibUtil.addFluid(new ItemStack(universalBucketItem), fluid));
                    }
                });
            }
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

    public static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(i -> Optional.of(BuiltInRegistries.FLUID.getKey(i.fluid))),
                    TagKey.codec(BuiltInRegistries.FLUID.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, FluidIngredient::new)
    );

    public static final IngredientType<FluidIngredient> TYPE = new IngredientType<>(CODEC);

}
