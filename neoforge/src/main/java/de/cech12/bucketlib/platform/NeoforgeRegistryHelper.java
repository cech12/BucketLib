package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.services.IRegistryHelper;

import java.util.List;

/**
 * The registry service implementation for NeoForge.
 */
public class NeoforgeRegistryHelper implements IRegistryHelper {
    @Override
    public List<UniversalBucketItem> getRegisteredBuckets() {
        return BucketLibMod.getRegisteredBuckets();
    }
}
