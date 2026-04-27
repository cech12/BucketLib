package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.NotNull;

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

    public FluidIngredient(Optional<Identifier> fluidOptional, Optional<TagKey<Fluid>> tagOptional) {
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
    public boolean test(@NotNull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        Identifier location = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        //Mekansim tanks are not compatible: https://github.com/cech12/BucketLib/issues/55 | https://github.com/mekanism/Mekanism/issues/8335
        if ("mekanism".equals(location.getNamespace()) && (itemStack.getCraftingRemainder() == null || itemStack.getCraftingRemainder().count() < 1)) {
            return false;
        }
        ItemStack container = itemStack.copyWithCount(1);
        ItemAccess access = ItemAccess.forStack(container);
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            return this.isFluidCorrect(handler.getResource(0).getFluid()) && handler.getAmountAsInt(0) == FluidType.BUCKET_VOLUME;
        }
        return false;
    }

    @Override
    @NotNull
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
    @NotNull
    public IngredientType<?> getType() {
        return TYPE;
    }

    public static final MapCodec<FluidIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Identifier.CODEC.optionalFieldOf("fluid").forGetter(i -> Optional.of(BuiltInRegistries.FLUID.getKey(i.fluid))),
                    TagKey.codec(BuiltInRegistries.FLUID.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, FluidIngredient::new)
    );

    public static final IngredientType<FluidIngredient> TYPE = new IngredientType<>(CODEC);

}
