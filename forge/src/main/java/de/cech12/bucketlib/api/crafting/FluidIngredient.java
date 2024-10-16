package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class FluidIngredient extends AbstractIngredient {

    protected final Fluid fluid;
    protected final TagKey<Fluid> tag;
    private ItemStack[] matchingStacks;

    private FluidIngredient(Fluid fluid, TagKey<Fluid> tag) {
        super(Stream.of());
        this.fluid = fluid;
        this.tag = tag;
    }

    public FluidIngredient(Optional<ResourceLocation> fluidOptional, Optional<TagKey<Fluid>> tagOptional) {
        this(fluidOptional.map(ForgeRegistries.FLUIDS::getValue).orElse(null), tagOptional.orElse(null));
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
            ITagManager<Fluid> fluidTags = ForgeRegistries.FLUIDS.tags();
            if (this.tag != null && fluidTags != null) {
                fluidTags.getTag(this.tag).forEach(fluids::add);
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

    @Override
    protected void invalidate() {
        this.matchingStacks = null;
    }

    @Override
    @Nonnull
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    public static final IIngredientSerializer<FluidIngredient> SERIALIZER = new IIngredientSerializer<>() {

        private static final MapCodec<FluidIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
                builder.group(
                        ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(i -> Optional.ofNullable(ForgeRegistries.FLUIDS.getKey(i.fluid))),
                        TagKey.codec(ForgeRegistries.FLUIDS.getRegistryKey()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
                ).apply(builder, FluidIngredient::new)
        );

        @Override
        public MapCodec<? extends FluidIngredient> codec() {
            return CODEC;
        }

        @Override
        public FluidIngredient read(RegistryFriendlyByteBuf buffer) {
            String fluid = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<Fluid> tag = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), new ResourceLocation(tagId));
                return new FluidIngredient(tag);
            }
            if (fluid.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a fluid ingredient with no fluid or tag.");
            }
            return new FluidIngredient(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluid)));
        }

        @Override
        public void write(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull FluidIngredient ingredient) {
            buffer.writeUtf(ingredient.fluid != null ? Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(ingredient.fluid)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    };
}
