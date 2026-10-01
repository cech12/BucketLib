package de.cech12.bucketlib.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
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

    public static final String BUCKET_CONTENT_TAG = "BucketContent";
    public static final String ENTITY_TYPE_TAG = "EntityType";

    public static final ResourceKey<ContextIntProvider> DEFAULT_COOKING_FUEL = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, id("cooking/time_bucketlib"));

    private BucketLib() {}

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

}