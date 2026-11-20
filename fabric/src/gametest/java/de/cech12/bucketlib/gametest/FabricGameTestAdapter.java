package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.api.BucketLibApi;
import de.cech12.bucketlib.platform.FabricConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Thin Fabric adapter for the common game tests: registers the test buckets (test mod "bucketlibtest"), the test
 * functions and implements the Fabric specific test operations. Only part of the gametest source set.
 */
public final class FabricGameTestAdapter implements ModInitializer, IGameTestPlatform {

    /** droplets per millibucket (Fabric uses 81000 droplets per bucket) */
    private static final long DROPLETS_PER_MILLIBUCKET = FluidConstants.BUCKET / 1000;

    @Override
    public void onInitialize() {
        // test blocks
        Registry.register(BuiltInRegistries.BLOCK, TestBlocks.INCOMPATIBLE_FLUID_SOURCE, TestBlocks.create());
        // test fluids of other mods with their attributes and buckets
        for (TestFluids.Definition definition : TestFluids.REGISTERED) {
            net.minecraft.world.level.material.Fluid fluid = Registry.register(BuiltInRegistries.FLUID, definition.id(), new TestFluids.TestFluid(definition) {});
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.register(fluid, new net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler() {
                @Override
                public int getTemperature(FluidVariant variant) {
                    return definition.temperature();
                }

                @Override
                public int getLightEmission(FluidVariant variant) {
                    return definition.lightLevel();
                }

                @Override
                public boolean isLighterThanAir(FluidVariant variant) {
                    return definition.lighterThanAir();
                }

                @Override
                public net.minecraft.network.chat.Component getName(FluidVariant variant) {
                    return net.minecraft.network.chat.Component.translatable("fluid." + TestBuckets.NAMESPACE + "." + definition.name());
                }
            });
            if (definition.bucketKind() == TestFluids.BucketKind.OWN_BUCKET) {
                Registry.register(BuiltInRegistries.ITEM, definition.bucketId(), definition.createBucket(fluid));
            }
        }
        // test buckets, registered like a mod that uses BucketLib would do it
        for (TestBuckets.Definition bucket : TestBuckets.ALL) {
            Registry.register(BuiltInRegistries.ITEM, bucket.id(), bucket.create());
        }
        for (TestBuckets.Definition bucket : TestBuckets.ALL) {
            BucketLibApi.registerBucket(bucket.id());
        }
        // error handling of the API (logged and ignored): an unknown id and an item that is no UniversalBucketItem
        BucketLibApi.registerBucket(TestBuckets.UNKNOWN_BUCKET_ID);
        BucketLibApi.registerBucket(Identifier.withDefaultNamespace("stone"));
        // game tests
        BucketLibGameTests.registerTestFunctions((id, test) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, id, test));
        BucketLibGameTests.registerEnvironmentTypes((id, codec) -> Registry.register(BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE, id, codec));
    }

    @Override
    public boolean setInfinityEnchantmentEnabled(boolean enabled) {
        FabricConfigHelper config = AutoConfig.getConfigHolder(FabricConfigHelper.class).getConfig();
        boolean previous = config.INFINITY_ENCHANTMENT_ENABLED;
        config.INFINITY_ENCHANTMENT_ENABLED = enabled;
        return previous;
    }

    @Override
    public boolean defaultInfinityEnchantmentEnabled() {
        return new FabricConfigHelper().INFINITY_ENCHANTMENT_ENABLED;
    }

    @Override
    public boolean canEnchant(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        return enchantment.value().canEnchant(stack);
    }

    @Override
    public String customIngredientTypeKey() {
        return "fabric:type";
    }

    @Override
    @SuppressWarnings("unchecked")
    public net.minecraft.world.item.crafting.Ingredient networkRoundTrip(net.minecraft.world.item.crafting.Ingredient ingredient, net.minecraft.core.RegistryAccess registryAccess) {
        // Fabric only sends custom ingredients to clients that support them, so the packet codec of the serializer is used directly
        net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient custom = ingredient.getCustomIngredient();
        if (custom == null) {
            throw new IllegalArgumentException("not a custom ingredient: " + ingredient);
        }
        var codec = (net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient>)
                custom.getSerializer().getStreamCodec();
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registryAccess);
        codec.encode(buffer, custom);
        return codec.decode(buffer).toVanilla();
    }

    /** item context with its own slot, so that the resulting stack can be read after the transaction */
    private static ContainerItemContext context(ItemStack stack) {
        return ContainerItemContext.ofSingleSlot(ContainerStorage.of(new SimpleContainer(stack.copy()), null).getSlot(0));
    }

    private static ItemStack result(ContainerItemContext context) {
        // a used up bucket leaves a blank slot
        return context.getItemVariant().isBlank() ? ItemStack.EMPTY : context.getItemVariant().toStack((int) context.getAmount());
    }

    @Override
    public FluidContent getFluidContent(ItemStack stack) {
        Storage<FluidVariant> storage = context(stack).find(FluidStorage.ITEM);
        if (storage != null) {
            for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
                return new FluidContent(view.getResource().getFluid(), (int) (view.getAmount() / DROPLETS_PER_MILLIBUCKET));
            }
        }
        return FluidContent.EMPTY;
    }

    @Override
    public boolean hasFluidStorage(ItemStack stack) {
        return context(stack).find(FluidStorage.ITEM) != null;
    }

    @Override
    public FluidTransfer insertFluid(ItemStack stack, Fluid fluid, int millibuckets) {
        ContainerItemContext context = context(stack);
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage == null) {
            return new FluidTransfer(0, stack.copy());
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(FluidVariant.of(fluid), millibuckets * DROPLETS_PER_MILLIBUCKET, transaction);
            transaction.commit();
            return new FluidTransfer((int) (inserted / DROPLETS_PER_MILLIBUCKET), result(context));
        }
    }

    @Override
    public FluidTransfer extractFluid(ItemStack stack, Fluid fluid, int millibuckets) {
        ContainerItemContext context = context(stack);
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage == null) {
            return new FluidTransfer(0, stack.copy());
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(FluidVariant.of(fluid), millibuckets * DROPLETS_PER_MILLIBUCKET, transaction);
            transaction.commit();
            return new FluidTransfer((int) (extracted / DROPLETS_PER_MILLIBUCKET), result(context));
        }
    }

}
