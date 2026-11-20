package de.cech12.bucketlib.gametest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Loader specific operations that the game tests need, but that cannot be done with vanilla code alone.
 * Each loader provides an implementation in its gametest source set (see META-INF/services).
 */
public interface IGameTestPlatform {

    /**
     * Changes the "infinity enchantment enabled" config option of BucketLib at runtime, as if a server admin changed
     * the config file.
     *
     * @param enabled new value
     * @return previous value, to restore it afterward
     */
    boolean setInfinityEnchantmentEnabled(boolean enabled);

    /**
     * Reads the default of the "infinity enchantment enabled" option from the config definition of the loader
     * (NeoForge: the config spec, Fabric: the initial value of the config class), not from the current value.
     *
     * @return default of the config definition
     */
    boolean defaultInfinityEnchantmentEnabled();

    /**
     * @return json key that selects a custom ingredient type in recipes ("fabric:type" or "neoforge:ingredient_type")
     */
    String customIngredientTypeKey();

    /**
     * Reads the fluid content of an item through the fluid transfer API of the loader (Fabric Transfer API /
     * NeoForge transfer capability), like another mod (e.g. a tank) would do.
     *
     * @param stack item stack, not changed
     * @return contained fluid and its amount in millibuckets ({@link FluidContent#EMPTY} if there is no fluid storage)
     */
    FluidContent getFluidContent(ItemStack stack);

    /**
     * @param stack item stack, not changed
     * @return true if the loader finds a fluid storage (Fabric) / fluid capability (NeoForge) for the stack
     */
    boolean hasFluidStorage(ItemStack stack);

    /**
     * Fills a fluid into a copy of the stack through the fluid transfer API of the loader.
     *
     * @param stack item stack, not changed
     * @param fluid fluid to insert
     * @param millibuckets offered amount (1000 = one bucket)
     * @return inserted amount in millibuckets and the resulting stack
     */
    FluidTransfer insertFluid(ItemStack stack, Fluid fluid, int millibuckets);

    /**
     * Drains a fluid from a copy of the stack through the fluid transfer API of the loader.
     *
     * @param stack item stack, not changed
     * @param fluid fluid to extract
     * @param millibuckets requested amount (1000 = one bucket)
     * @return extracted amount in millibuckets and the resulting stack
     */
    FluidTransfer extractFluid(ItemStack stack, Fluid fluid, int millibuckets);

    /**
     * Checks like an anvil (or the enchant command) whether the enchantment can be applied to the stack. Fabric uses
     * Enchantment#canEnchant, NeoForge ItemStack#supportsEnchantment (which BucketLib overrides there).
     *
     * @param stack item stack
     * @param enchantment enchantment
     * @return true if the enchantment can be applied
     */
    boolean canEnchant(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment);

    /**
     * Sends a recipe ingredient over the network (encodes and decodes it with the network codec that the loader uses
     * for custom ingredients, as if a client with BucketLib is connected).
     *
     * @param ingredient ingredient
     * @param registryAccess registries
     * @return the decoded ingredient
     */
    net.minecraft.world.item.crafting.Ingredient networkRoundTrip(net.minecraft.world.item.crafting.Ingredient ingredient, net.minecraft.core.RegistryAccess registryAccess);

    /**
     * @param fluid fluid content
     * @param millibuckets amount (1000 = one bucket)
     */
    record FluidContent(Fluid fluid, int millibuckets) {
        /** no fluid */
        public static final FluidContent EMPTY = new FluidContent(Fluids.EMPTY, 0);
    }

    /**
     * @param millibuckets transferred amount (1000 = one bucket)
     * @param result resulting item stack (empty if the item was used up)
     */
    record FluidTransfer(int millibuckets, ItemStack result) {}

}
