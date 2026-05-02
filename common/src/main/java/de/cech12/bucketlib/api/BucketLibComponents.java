package de.cech12.bucketlib.api;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.CustomData;

public class BucketLibComponents {

    public static final Identifier BUCKET_CONTENT_LOCATION = BucketLib.id("bucket_content");
    public static final Identifier FLUID_LOCATION = BucketLib.id("fluid");

    public static final DataComponentType<CustomData> BUCKET_CONTENT = new DataComponentType.Builder<CustomData>().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build();

    private BucketLibComponents() {
        //avoid instantiating this class
    }

}
