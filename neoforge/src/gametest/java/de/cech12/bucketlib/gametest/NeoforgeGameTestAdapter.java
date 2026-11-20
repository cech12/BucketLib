package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.platform.NeoforgeConfigHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * NeoForge specific operations of the common game tests (see {@link IGameTestPlatform}). Only part of the gametest
 * source set. The test mod itself is {@link NeoforgeTestMod}.
 */
public final class NeoforgeGameTestAdapter implements IGameTestPlatform {

    @Override
    public boolean setInfinityEnchantmentEnabled(boolean enabled) {
        boolean previous = NeoforgeConfigHelper.INFINITY_ENCHANTMENT_ENABLED.get();
        NeoforgeConfigHelper.INFINITY_ENCHANTMENT_ENABLED.set(enabled);
        return previous;
    }

    @Override
    public boolean defaultInfinityEnchantmentEnabled() {
        return NeoforgeConfigHelper.INFINITY_ENCHANTMENT_ENABLED.getDefault();
    }

    @Override
    public boolean canEnchant(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        return stack.supportsEnchantment(enchantment);
    }

    @Override
    public String customIngredientTypeKey() {
        return "neoforge:ingredient_type";
    }

    @Override
    public net.minecraft.world.item.crafting.Ingredient networkRoundTrip(net.minecraft.world.item.crafting.Ingredient ingredient, net.minecraft.core.RegistryAccess registryAccess) {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registryAccess, net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        net.minecraft.world.item.crafting.Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
        return net.minecraft.world.item.crafting.Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
    }

    private static ResourceHandler<FluidResource> handler(ItemAccess access) {
        return access.getCapability(Capabilities.Fluid.ITEM);
    }

    @Override
    public FluidContent getFluidContent(ItemStack stack) {
        if (stack.isEmpty()) {
            return FluidContent.EMPTY;
        }
        ResourceHandler<FluidResource> handler = handler(ItemAccess.forStack(stack.copy()));
        if (handler != null) {
            for (int i = 0; i < handler.size(); i++) {
                if (!handler.getResource(i).isEmpty()) {
                    return new FluidContent(handler.getResource(i).getFluid(), handler.getAmountAsInt(i));
                }
            }
        }
        return FluidContent.EMPTY;
    }

    @Override
    public boolean hasFluidStorage(ItemStack stack) {
        return !stack.isEmpty() && handler(ItemAccess.forStack(stack.copy())) != null;
    }

    @Override
    public FluidTransfer insertFluid(ItemStack stack, Fluid fluid, int millibuckets) {
        ItemStack copy = stack.copy();
        ResourceHandler<FluidResource> handler = handler(ItemAccess.forStack(copy));
        if (handler == null) {
            return new FluidTransfer(0, copy);
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(FluidResource.of(fluid), millibuckets, transaction);
            transaction.commit();
            return new FluidTransfer(inserted, copy);
        }
    }

    @Override
    public FluidTransfer extractFluid(ItemStack stack, Fluid fluid, int millibuckets) {
        ItemStack copy = stack.copy();
        ResourceHandler<FluidResource> handler = handler(ItemAccess.forStack(copy));
        if (handler == null) {
            return new FluidTransfer(0, copy);
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(FluidResource.of(fluid), millibuckets, transaction);
            transaction.commit();
            return new FluidTransfer(extracted, copy);
        }
    }

}
