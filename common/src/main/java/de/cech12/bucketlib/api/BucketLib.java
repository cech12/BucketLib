package de.cech12.bucketlib.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Class that contains all common constants.
 */
public class BucketLib {

    /** mod id */
    public static final String MOD_ID = "bucketlib";
    /** mod name*/
    public static final String MOD_NAME = "BucketLib";

    private BucketLib() {}

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

}