package de.cech12.bucketlib.api;

import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that contains all common constants.
 */
public class BucketLib {

    public static final Logger LOG = LogManager.getLogger(BucketLib.class);

    /** mod id */
    public static final String MOD_ID = "bucketlib";
    /** mod name*/
    public static final String MOD_NAME = "BucketLib";

    private BucketLib() {}

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

}