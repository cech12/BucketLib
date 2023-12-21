package de.cech12.bucketlib.platform.services;

import de.cech12.bucketlib.api.BucketLibTags;

/**
 * Common configuration helper service interface.
 */
public interface IConfigHelper {

    /** Default value of the infinity enchantment enabled option */
    boolean INFINITY_ENCHANTMENT_ENABLED_DEFAULT = false;
    /** Config description of the infinity enchantment enabled option */
    String INFINITY_ENCHANTMENT_ENABLED_DESCRIPTION = "Whether or not Infinity enchantment for modded buckets filled with fluids of the tag \"" + BucketLibTags.Fluids.INFINITY_ENCHANTABLE.location() + "\" should be enabled.";

    /**
     * Initialization method for the Service implementations.
     */
    void init();

    /**
     * Gets the configured infinity enchantment enabled option value.
     *
     * @return configured infinity enchantment enabled option value
     */
    boolean isInfinityEnchantmentEnabled();

}