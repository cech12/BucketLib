package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibApi;
import de.cech12.bucketlib.api.BucketLibComponents;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.crafting.BlockIngredient;
import de.cech12.bucketlib.api.crafting.EmptyIngredient;
import de.cech12.bucketlib.api.crafting.EntityIngredient;
import de.cech12.bucketlib.api.crafting.FluidIngredient;
import de.cech12.bucketlib.api.crafting.MilkIngredient;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.item.FluidStorageData;
import de.cech12.bucketlib.item.UniversalBucketDispenseBehaviour;
import de.cech12.bucketlib.item.UniversalBucketFluidStorage;
import de.cech12.bucketlib.item.crafting.BucketFillingShapedRecipe;
import de.cech12.bucketlib.item.crafting.BucketFillingShapelessRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BucketLibMod implements ModInitializer {

    public static ServerLevel SERVER_LEVEL;

    public static DataComponentType<FluidStorageData> STORAGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BucketLibComponents.FLUID_LOCATION,
            new DataComponentType.Builder<FluidStorageData>().persistent(FluidStorageData.CODEC).networkSynchronized(FluidStorageData.STREAM_CODEC).build()
    );

    static {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BucketLibComponents.BUCKET_CONTENT_LOCATION, BucketLibComponents.BUCKET_CONTENT);

        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, BucketLib.id("bucket_filling_shaped"), BucketFillingShapedRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, BucketLib.id("bucket_filling_shapeless"), BucketFillingShapelessRecipe.SERIALIZER);
        CustomIngredientSerializer.register(BlockIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(EmptyIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(EntityIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(FluidIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(MilkIngredient.Serializer.INSTANCE);
    }

    //TODO remove test bucket
    public static final Identifier TEST_BUCKET_ID = BucketLib.id("test_bucket");
    public static final Item TEST_BUCKET = Registry.register(BuiltInRegistries.ITEM, TEST_BUCKET_ID, new UniversalBucketItem(
            ResourceKey.create(BuiltInRegistries.ITEM.key(), TEST_BUCKET_ID),
            new UniversalBucketItem.Properties()));

    private static final List<UniversalBucketItem> BUCKETS = new ArrayList<>();

    public BucketLibMod() {
        //remember server level to have an easy getter
        ServerTickEvents.END_SERVER_TICK.register(server -> SERVER_LEVEL = server.getLevel(Level.OVERWORLD));
    }

    @Override
    public void onInitialize() {
        CommonLoader.init();
        //Ensure that the tags are initialized
        BucketLibTags.init();


        //TODO remove!!
        //register bucket
        BucketLibApi.registerBucket(TEST_BUCKET_ID);
    }

    public static void addBucket(UniversalBucketItem bucket) {
        BUCKETS.add(bucket);
        //register dispense behaviour
        DispenserBlock.registerBehavior(bucket, UniversalBucketDispenseBehaviour.getInstance());
        // Register bucket storage
        FluidStorage.ITEM.registerForItems((stack, context) -> new UniversalBucketFluidStorage(context), bucket);
    }

    public static List<UniversalBucketItem> getRegisteredBuckets() {
        return Collections.unmodifiableList(BUCKETS);
    }

}
