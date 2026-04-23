package de.cech12.bucketlib.api;

import de.cech12.bucketlib.BucketLibMod;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class BucketLibApi {

    public static void registerBucket(RegisterCapabilitiesEvent event, Identifier bucket) {
        BucketLibMod.processRegistration(event, bucket);
    }

}
