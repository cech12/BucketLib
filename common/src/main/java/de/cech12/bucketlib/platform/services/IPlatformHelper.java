package de.cech12.bucketlib.platform.services;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Common platform helper service interface.
 */
public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Gets the current level.
     *
     * @return the current level.
     */
    Level getCurrentLevel();

    /**
     * Gets the TOOLS_AND_UTILITIES creative tab.
     *
     * @return TOOLS_AND_UTILITIES creative tab.
     */
    ResourceKey<CreativeModeTab> getToolsAndUtilitiesTab();

    /**
     * Gets the translation key for milk.
     *
     * @return translation key for milk
     */
    String getMilkTranslationKey();

    /**
     * Gets the burn time of an ItemStack.
     *
     * @return burn time of an ItemStack
     */
    int getBurnTime(ItemStack stack, RecipeType<?> recipeType);

}