package de.cech12.bucketlibtest;

import de.cech12.bucketlib.api.BucketLibApi;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Collections;
import java.util.List;

public class BucketLibTestMod implements ModInitializer {

    public static final String MOD_ID = "bucketlibtest";

    public static final TagKey<Fluid> WATER_TAG = TagKey.create(Registries.FLUID, location("water"));

    private static final Identifier TEST_BUCKET = location("test_bucket");
    private static final Identifier TEMPERATURE_BUCKET = location("temperature_bucket");
    private static final Identifier WATER_ALLOWING_BUCKET = location("water_allowing_bucket");
    private static final Identifier WATER_ALLOWING_TAG_BUCKET = location("water_allowing_tag_bucket");
    private static final Identifier WATER_DENYING_BUCKET = location("water_denying_bucket");
    private static final Identifier WATER_DENYING_TAG_BUCKET = location("water_denying_tag_bucket");
    private static final Identifier CRACKING_BUCKET = location("cracking_bucket");
    private static final Identifier COLORED_BUCKET = location("colored_bucket");
    private static final Identifier ANTI_MILK_BUCKET = location("anti_milk_bucket");
    private static final Identifier NO_ENTITIES_BUCKET = location("no_entities_bucket");
    private static final Identifier ANTI_SALMON_BUCKET = location("anti_salmon_bucket");
    private static final Identifier ONLY_PUFFER_BUCKET = location("only_puffer_bucket");
    private static final Identifier NO_BLOCKS_BUCKET = location("no_blocks_bucket");
    private static final Identifier DURABILITY_BUCKET = location("durability_bucket");
    private static final Identifier BURNING_BUCKET = location("burning_bucket");
    private static final Identifier FREEZING_BUCKET = location("freezing_bucket");

    private static final List<Identifier> BUCKETS = List.of(
            TEST_BUCKET, TEMPERATURE_BUCKET, WATER_ALLOWING_BUCKET, WATER_ALLOWING_TAG_BUCKET, WATER_DENYING_BUCKET,
            WATER_DENYING_TAG_BUCKET, CRACKING_BUCKET, COLORED_BUCKET, ANTI_MILK_BUCKET, NO_ENTITIES_BUCKET, ANTI_SALMON_BUCKET,
            ONLY_PUFFER_BUCKET, NO_BLOCKS_BUCKET, DURABILITY_BUCKET, BURNING_BUCKET, FREEZING_BUCKET
    );

    static {
        registerBucket(TEST_BUCKET, new UniversalBucketItem.Properties());
        registerBucket(TEMPERATURE_BUCKET, new UniversalBucketItem.Properties().maxTemperature(1000));
        registerBucket(WATER_ALLOWING_BUCKET, new UniversalBucketItem.Properties().allowedFluids(Collections.singletonList(Fluids.WATER)));
        registerBucket(WATER_ALLOWING_TAG_BUCKET, new UniversalBucketItem.Properties().allowedFluids(WATER_TAG));
        registerBucket(WATER_DENYING_BUCKET, new UniversalBucketItem.Properties().deniedFluids(Collections.singletonList(Fluids.WATER)));
        registerBucket(WATER_DENYING_TAG_BUCKET, new UniversalBucketItem.Properties().deniedFluids(WATER_TAG));
        registerBucket(CRACKING_BUCKET, new UniversalBucketItem.Properties().upperCrackingTemperature(1000));
        registerBucket(COLORED_BUCKET, new UniversalBucketItem.Properties());
        registerBucket(ANTI_MILK_BUCKET, new UniversalBucketItem.Properties().disableMilking());
        registerBucket(NO_ENTITIES_BUCKET, new UniversalBucketItem.Properties().disableEntityObtaining());
        registerBucket(ANTI_SALMON_BUCKET, new UniversalBucketItem.Properties().deniedEntities(Collections.singletonList(EntityTypes.SALMON)));
        registerBucket(ONLY_PUFFER_BUCKET, new UniversalBucketItem.Properties().allowedEntities(Collections.singletonList(EntityTypes.PUFFERFISH)));
        registerBucket(NO_BLOCKS_BUCKET, new UniversalBucketItem.Properties().disableBlockObtaining());
        registerBucket(DURABILITY_BUCKET, new UniversalBucketItem.Properties().durability(5));
        registerBucket(BURNING_BUCKET, new UniversalBucketItem.Properties().durability(20).burningTemperature(1000).burningBlocks(Collections.singletonList(Blocks.POWDER_SNOW)));
        registerBucket(FREEZING_BUCKET, new UniversalBucketItem.Properties().durability(20).freezingTemperature(500).freezingBlocks(Collections.singletonList(Blocks.POWDER_SNOW)));
    }

    private static void registerBucket(Identifier location, UniversalBucketItem.Properties properties) {
        Registry.register(BuiltInRegistries.ITEM, location, new UniversalBucketItem(ResourceKey.create(BuiltInRegistries.ITEM.key(), location), properties));
    }

    @Override
    public void onInitialize() {
        BUCKETS.forEach(BucketLibApi::registerBucket);
    }

    private static Identifier location(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

}
