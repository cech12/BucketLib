package de.cech12.bucketlib.api;

import de.cech12.bucketlib.BucketLibMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class BucketLibApi {

    public static void registerBucket(RegisterCapabilitiesEvent event, ResourceLocation bucket) {
        BucketLibMod.processRegistration(event, bucket);
    }

}
