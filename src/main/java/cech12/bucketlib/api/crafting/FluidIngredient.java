package cech12.bucketlib.api.crafting;

import cech12.bucketlib.BucketLib;
import cech12.bucketlib.api.BucketLibApi;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FluidIngredient extends Ingredient {

    protected final Fluid fluid;
    protected final Tag<Fluid> tag;
    private ItemStack[] matchingStacks;

    private FluidIngredient(Fluid fluid, Tag<Fluid> tag) {
        super(Stream.of());
        this.fluid = fluid;
        this.tag = tag;
    }

    public FluidIngredient(Fluid fluid) {
        this(fluid, null);
    }

    public FluidIngredient(Tag<Fluid> tag) {
        this(null, tag);
    }

    private boolean isFluidCorrect(Fluid fluid) {
        return fluid != null && (
                (this.fluid != null && fluid.isSame(this.fluid))
                        || (this.tag != null && fluid.is(this.tag))
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
                List<Fluid> fluids = this.tag != null ? this.tag.getValues() : Collections.singletonList(this.fluid);
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
            jsonObject.addProperty("fluid", this.fluid.getRegistryName().toString());
        }
        if (this.tag != null) {
            jsonObject.addProperty("tag", SerializationTags.getInstance().getIdOrThrow(Registry.FLUID_REGISTRY, this.tag, () -> new IllegalStateException("Unknown fluid tag: " + this.tag)).toString());
        }
        return jsonObject;
    }

    public static final class Serializer implements IIngredientSerializer<FluidIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLibApi.MOD_ID, "fluid");

        private Serializer() {}

        @NotNull
        @Override
        public FluidIngredient parse(FriendlyByteBuf buffer) {
            String fluid = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                Tag<Fluid> tag = SerializationTags.getInstance().getOrEmpty(Registry.FLUID_REGISTRY).getTag(new ResourceLocation(tagId));
                return new FluidIngredient(tag);
            }
            return new FluidIngredient(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluid)));
        }

        @NotNull
        @Override
        public FluidIngredient parse(@Nonnull JsonObject json) {
            if (json.has("tag")) {
                ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
                Tag<Fluid> tag = SerializationTags.getInstance().getOrEmpty(Registry.FLUID_REGISTRY).getTag(tagId);
                if (tag == null) {
                    throw new JsonSyntaxException("Unknown fluid tag: " + tagId);
                }
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
        public void write(FriendlyByteBuf buffer, FluidIngredient ingredient) {
            buffer.writeUtf(ingredient.fluid != null ? ingredient.fluid.toString() : "");
            buffer.writeUtf(ingredient.tag != null ? SerializationTags.getInstance().getIdOrThrow(Registry.FLUID_REGISTRY, ingredient.tag, () -> new IllegalStateException("Unknown fluid tag: " + ingredient.tag)).toString() : "");
        }
    }
}