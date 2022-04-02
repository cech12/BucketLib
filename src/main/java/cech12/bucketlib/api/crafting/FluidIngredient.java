package cech12.bucketlib.api.crafting;

import cech12.bucketlib.BucketLib;
import cech12.bucketlib.api.BucketLibApi;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
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

public class FluidIngredient extends Ingredient {

    protected final Fluid fluid;
    protected final TagKey<Fluid> tag;
    private ItemStack[] matchingStacks;

    private FluidIngredient(Fluid fluid, TagKey<Fluid> tag) {
        super(Stream.of());
        this.fluid = fluid;
        this.tag = tag;
    }

    public FluidIngredient(Fluid fluid) {
        this(fluid, null);
    }

    public FluidIngredient(TagKey<Fluid> tag) {
        this(null, tag);
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
            return this.isFluidCorrect(drainedFluid.getFluid()) && drainedFluid.getAmount() == FluidAttributes.BUCKET_VOLUME;
        }
        return false;
    }

    @Override
    @Nonnull
    public ItemStack[] getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            BucketLib.getRegisteredBuckets().forEach(universalBucketItem -> {
                ItemStack stack = new ItemStack(universalBucketItem);
                List<Fluid> fluids = new ArrayList<>();
                ITagManager<Fluid> fluidTags = ForgeRegistries.FLUIDS.tags();
                if (this.tag != null && fluidTags != null) {
                    fluidTags.getTag(this.tag).forEach(fluids::add);
                } else if (this.fluid != null) {
                    fluids.add(this.fluid);
                }
                for (Fluid fluid : fluids) {
                    Item bucketItem = fluid.getBucket();
                    if (!(bucketItem instanceof BucketItem) || ((BucketItem) bucketItem).getFluid() != fluid) {
                        continue;
                    }
                    stacks.add(new ItemStack(fluid.getBucket()));
                    FluidStack fluidStack = new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME);
                    FluidUtil.getFluidHandler(stack).ifPresent(fluidHandler -> {
                        int filledAmount = fluidHandler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
                        if (filledAmount == FluidAttributes.BUCKET_VOLUME) {
                            stacks.add(fluidHandler.getContainer());
                        }
                    });
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
    @Nonnull
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Nonnull
    public JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", Serializer.NAME.toString());
        if (this.fluid != null) {
            jsonObject.addProperty("fluid", Objects.requireNonNull(this.fluid.getRegistryName()).toString());
        }
        if (this.tag != null) {
            jsonObject.addProperty("tag", this.tag.location().toString());
        }
        return jsonObject;
    }

    public static final class Serializer implements IIngredientSerializer<FluidIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLibApi.MOD_ID, "fluid");

        private Serializer() {}

        @Nonnull
        @Override
        public FluidIngredient parse(@Nonnull FriendlyByteBuf buffer) {
            String fluid = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<Fluid> tag = TagKey.create(Registry.FLUID_REGISTRY, new ResourceLocation(tagId));
                return new FluidIngredient(tag);
            }
            if (fluid.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a fluid ingredient with no fluid or tag.");
            }
            return new FluidIngredient(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluid)));
        }

        @Nonnull
        @Override
        public FluidIngredient parse(@Nonnull JsonObject json) {
            if (json.has("tag")) {
                ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
                TagKey<Fluid> tag = TagKey.create(Registry.FLUID_REGISTRY, tagId);
                return new FluidIngredient(tag);
            } else {
                ResourceLocation fluid = new ResourceLocation(json.get("fluid").getAsString());
                if (!ForgeRegistries.FLUIDS.containsKey(fluid)) {
                    throw new JsonSyntaxException("Unknown fluid: " + fluid);
                }
                return new FluidIngredient(ForgeRegistries.FLUIDS.getValue(fluid));
            }
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull FluidIngredient ingredient) {
            buffer.writeUtf(ingredient.fluid != null ? Objects.requireNonNull(ingredient.fluid.getRegistryName()).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    }
}
