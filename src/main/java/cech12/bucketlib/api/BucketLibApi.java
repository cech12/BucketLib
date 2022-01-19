package cech12.bucketlib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.InterModComms;

public class BucketLibApi {

    public static final String MOD_ID = "bucketlib";

    public static final String REGISTER_BUCKET = "register_bucket";

    public static void registerBucket(ResourceLocation bucket) {
        InterModComms.sendTo(MOD_ID, REGISTER_BUCKET, () -> bucket);
    }

}
