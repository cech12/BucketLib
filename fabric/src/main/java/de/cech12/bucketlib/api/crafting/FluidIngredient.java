package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FluidIngredient implements CustomIngredient {

    protected final Fluid fluid;
    protected final TagKey<Fluid> tag;
    private List<ItemStack> matchingStacks;

    private FluidIngredient(Fluid fluid, TagKey<Fluid> tag) {
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
        ItemStack container = itemStack.copyWithCount(1);
        Storage<FluidVariant> storage = ContainerItemContext.withConstant(container).find(FluidStorage.ITEM);
        StorageView<FluidVariant> fluidView = null;
        if (storage != null) {
            for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
                fluidView = view;
                break;
            }
        }
        if (fluidView == null) {
            return false;
        }
        return this.isFluidCorrect(fluidView.getResource().getFluid()) && fluidView.getAmount() == FluidConstants.BUCKET;
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        if (this.matchingStacks == null) {
            this.matchingStacks = new ArrayList<>();
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
                if (!(bucketItem instanceof BucketItem) || Services.BUCKET.getFluidOfBucketItem((BucketItem) bucketItem) != fluid) {
                    continue; //skip fluids that have no vanilla bucket
                }
                this.matchingStacks.add(new ItemStack(bucketItem));
                //bucket lib buckets
                BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                    if (universalBucketItem.canHoldFluid(fluid)) {
                        this.matchingStacks.add(BucketLibUtil.addFluid(new ItemStack(universalBucketItem), fluid));
                    }
                });
            }
        }
        return this.matchingStacks;
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(i -> Optional.of(BuiltInRegistries.FLUID.getKey(i.fluid))),
                    TagKey.codec(BuiltInRegistries.FLUID.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, FluidIngredient::new)
    );

    public static final class Serializer implements CustomIngredientSerializer<FluidIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLib.MOD_ID, "fluid");

        private Serializer() {}

        @Override
        public ResourceLocation getIdentifier() {
            return NAME;
        }

        @Override
        public FluidIngredient read(FriendlyByteBuf buffer) {
            String fluid = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<Fluid> tag = TagKey.create(Registries.FLUID, new ResourceLocation(tagId));
                return new FluidIngredient(tag);
            }
            if (fluid.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a fluid ingredient with no fluid or tag.");
            }
            return new FluidIngredient(BuiltInRegistries.FLUID.get(new ResourceLocation(fluid)));
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull FluidIngredient ingredient) {
            buffer.writeUtf(ingredient.fluid != null ? Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(ingredient.fluid)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }

        @Override
        public Codec<FluidIngredient> getCodec(boolean allowEmpty) {
            return CODEC;
        }
    }

}
