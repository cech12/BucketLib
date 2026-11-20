package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

import java.util.List;
import java.util.function.Supplier;

/**
 * Buckets of the test mod "bucketlibtest". Every bucket uses another combination of {@link UniversalBucketItem.Properties},
 * like a mod that uses BucketLib would do. The loader adapters register all of them as items and at BucketLib
 * (BucketLibApi.registerBucket). They are also used for manual tests in the client runs.
 * <p>
 * Adding a bucket: add it to {@link #ALL}, add an item model definition
 * ({@code assets/bucketlibtest/items/<name>.json}) and a translation (checked by the resource tests).
 */
public final class TestBuckets {

    /** namespace (mod id) of the test mod */
    public static final String NAMESPACE = "bucketlibtest";

    /** fluid tag bucketlibtest:water (contains water) */
    public static final TagKey<Fluid> WATER_TAG = TagKey.create(Registries.FLUID, id("water"));
    /** entity type tag bucketlibtest:salmon (contains salmon) */
    public static final TagKey<EntityType<?>> SALMON_TAG = TagKey.create(Registries.ENTITY_TYPE, id("salmon"));
    /** entity type tag bucketlibtest:pufferfish (contains pufferfish) */
    public static final TagKey<EntityType<?>> PUFFERFISH_TAG = TagKey.create(Registries.ENTITY_TYPE, id("pufferfish"));
    /** block tag bucketlibtest:powder_snow (contains powder snow) */
    public static final TagKey<Block> POWDER_SNOW_TAG = TagKey.create(Registries.BLOCK, id("powder_snow"));
    /** creative tab "Ingredients" (the vanilla constant is not public) */
    public static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients"));
    /** context int provider bucketlibtest:cooking/test_fuel (constant 200 ticks) */
    public static final ResourceKey<ContextIntProvider> TEST_FUEL = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, id("cooking/test_fuel"));

    /**
     * Values of the config options of {@link #CONFIG_BUCKET}. A test may change them, but must restore the defaults
     * in the same tick (tests of one batch run in parallel).
     */
    public static final class Config {
        public static final int DEFAULT_DURABILITY = 3;
        public static int durability = DEFAULT_DURABILITY;
        public static int maxTemperature = 1000;
        public static int minTemperature = 0;
        public static int upperCrackingTemperature = 2000;
        public static int lowerCrackingTemperature = -1000;
        public static int burningTemperature = 2000;
        public static int freezingTemperature = -1000;
        public static boolean milking = true;
        public static boolean entityObtaining = true;
        public static boolean blockObtaining = true;

        private Config() {}

        /** Restores the default values. */
        public static void reset() {
            durability = DEFAULT_DURABILITY;
            maxTemperature = 1000;
            minTemperature = 0;
            upperCrackingTemperature = 2000;
            lowerCrackingTemperature = -1000;
            burningTemperature = 2000;
            freezingTemperature = -1000;
            milking = true;
            entityObtaining = true;
            blockObtaining = true;
        }
    }

    /**
     * @param name item name in the namespace {@link #NAMESPACE}
     * @param properties creates the bucket properties
     */
    public record Definition(String name, Supplier<UniversalBucketItem.Properties> properties) {
        /** @return item id */
        public Identifier id() {
            return TestBuckets.id(this.name);
        }

        /** @return a new bucket item with these properties (only for registration) */
        public UniversalBucketItem create() {
            return new UniversalBucketItem(ResourceKey.create(Registries.ITEM, this.id()), this.properties.get());
        }

        /** @return the registered bucket item */
        public UniversalBucketItem item() {
            Item item = BuiltInRegistries.ITEM.getValue(this.id());
            if (!(item instanceof UniversalBucketItem bucket)) {
                throw new IllegalStateException("test bucket " + this.id() + " is not registered");
            }
            return bucket;
        }
    }

    private static Definition bucket(String name, Supplier<UniversalBucketItem.Properties> properties) {
        return new Definition(name, properties);
    }

    /** default properties */
    public static final Definition TEST_BUCKET = bucket("test_bucket", UniversalBucketItem.Properties::new);
    /** fluids up to 1000 (water, not lava) */
    public static final Definition TEMPERATURE_BUCKET = bucket("temperature_bucket", () -> new UniversalBucketItem.Properties().maxTemperature(1000));
    /** fluids from 500 (lava, not water) */
    public static final Definition MIN_TEMPERATURE_BUCKET = bucket("min_temperature_bucket", () -> new UniversalBucketItem.Properties().minTemperature(500));
    /** only water (list) */
    public static final Definition WATER_ALLOWING_BUCKET = bucket("water_allowing_bucket", () -> new UniversalBucketItem.Properties().allowedFluids(List.of(Fluids.WATER)));
    /** only water (tag) */
    public static final Definition WATER_ALLOWING_TAG_BUCKET = bucket("water_allowing_tag_bucket", () -> new UniversalBucketItem.Properties().allowedFluids(WATER_TAG));
    /** everything except water (list) */
    public static final Definition WATER_DENYING_BUCKET = bucket("water_denying_bucket", () -> new UniversalBucketItem.Properties().deniedFluids(List.of(Fluids.WATER)));
    /** everything except water (tag) */
    public static final Definition WATER_DENYING_TAG_BUCKET = bucket("water_denying_tag_bucket", () -> new UniversalBucketItem.Properties().deniedFluids(WATER_TAG));
    /** breaks when it is emptied after holding a fluid of 1000 or more (lava) */
    public static final Definition CRACKING_BUCKET = bucket("cracking_bucket", () -> new UniversalBucketItem.Properties().upperCrackingTemperature(1000));
    /** breaks when it is emptied after holding a fluid of 500 or less (water) */
    public static final Definition LOWER_CRACKING_BUCKET = bucket("lower_cracking_bucket", () -> new UniversalBucketItem.Properties().lowerCrackingTemperature(500));
    /** breaks when it is emptied after holding water (list) */
    public static final Definition CRACKING_FLUIDS_BUCKET = bucket("cracking_fluids_bucket", () -> new UniversalBucketItem.Properties().crackingFluids(List.of(Fluids.WATER)));
    /** breaks when it is emptied after holding water (tag) */
    public static final Definition CRACKING_FLUIDS_TAG_BUCKET = bucket("cracking_fluids_tag_bucket", () -> new UniversalBucketItem.Properties().crackingFluids(WATER_TAG));
    /** dyeable (in #minecraft:dyeable) */
    public static final Definition COLORED_BUCKET = bucket("colored_bucket", UniversalBucketItem.Properties::new);
    /** cannot milk */
    public static final Definition ANTI_MILK_BUCKET = bucket("anti_milk_bucket", () -> new UniversalBucketItem.Properties().disableMilking());
    /** cannot hold entities */
    public static final Definition NO_ENTITIES_BUCKET = bucket("no_entities_bucket", () -> new UniversalBucketItem.Properties().disableEntityObtaining());
    /** every entity except salmon (list) */
    public static final Definition ANTI_SALMON_BUCKET = bucket("anti_salmon_bucket", () -> new UniversalBucketItem.Properties().deniedEntities(List.of(EntityTypes.SALMON)));
    /** every entity except salmon (tag) */
    public static final Definition ANTI_SALMON_TAG_BUCKET = bucket("anti_salmon_tag_bucket", () -> new UniversalBucketItem.Properties().deniedEntities(SALMON_TAG));
    /** only pufferfish (list) */
    public static final Definition ONLY_PUFFER_BUCKET = bucket("only_puffer_bucket", () -> new UniversalBucketItem.Properties().allowedEntities(List.of(EntityTypes.PUFFERFISH)));
    /** only pufferfish (tag) */
    public static final Definition ONLY_PUFFER_TAG_BUCKET = bucket("only_puffer_tag_bucket", () -> new UniversalBucketItem.Properties().allowedEntities(PUFFERFISH_TAG));
    /** cannot hold blocks */
    public static final Definition NO_BLOCKS_BUCKET = bucket("no_blocks_bucket", () -> new UniversalBucketItem.Properties().disableBlockObtaining());
    /** every block except powder snow (list) */
    public static final Definition ANTI_POWDER_SNOW_BUCKET = bucket("anti_powder_snow_bucket", () -> new UniversalBucketItem.Properties().deniedBlocks(List.of(Blocks.POWDER_SNOW)));
    /** every block except powder snow (tag) */
    public static final Definition ANTI_POWDER_SNOW_TAG_BUCKET = bucket("anti_powder_snow_tag_bucket", () -> new UniversalBucketItem.Properties().deniedBlocks(POWDER_SNOW_TAG));
    /** only powder snow (list) */
    public static final Definition ONLY_POWDER_SNOW_BUCKET = bucket("only_powder_snow_bucket", () -> new UniversalBucketItem.Properties().allowedBlocks(List.of(Blocks.POWDER_SNOW)));
    /** only stone, so no bucketable block at all (tag variant: only #bucketlibtest:powder_snow) */
    public static final Definition ONLY_STONE_BUCKET = bucket("only_stone_bucket", () -> new UniversalBucketItem.Properties().allowedBlocks(List.of(Blocks.STONE)));
    /** only powder snow (tag) */
    public static final Definition ONLY_POWDER_SNOW_TAG_BUCKET = bucket("only_powder_snow_tag_bucket", () -> new UniversalBucketItem.Properties().allowedBlocks(POWDER_SNOW_TAG));
    /** durability 5 (in #minecraft:enchantable/durability) */
    public static final Definition DURABILITY_BUCKET = bucket("durability_bucket", () -> new UniversalBucketItem.Properties().durability(5));
    /** burns its holder with fluids of 1000 or more and with powder snow (list) */
    public static final Definition BURNING_BUCKET = bucket("burning_bucket", () -> new UniversalBucketItem.Properties().durability(20).burningTemperature(1000).burningBlocks(List.of(Blocks.POWDER_SNOW)));
    /** freezes its holder with fluids of 500 or less and with powder snow (list) */
    public static final Definition FREEZING_BUCKET = bucket("freezing_bucket", () -> new UniversalBucketItem.Properties().durability(20).freezingTemperature(500).freezingBlocks(List.of(Blocks.POWDER_SNOW)));
    /** burns its holder with water (fluid list) and with powder snow (block tag) */
    public static final Definition BURNING_FLUIDS_BUCKET = bucket("burning_fluids_bucket", () -> new UniversalBucketItem.Properties().durability(20).burningFluids(List.of(Fluids.WATER)).burningBlocks(POWDER_SNOW_TAG));
    /** burns its holder with water (fluid tag) */
    public static final Definition BURNING_FLUIDS_TAG_BUCKET = bucket("burning_fluids_tag_bucket", () -> new UniversalBucketItem.Properties().durability(20).burningFluids(WATER_TAG));
    /** freezes its holder with water (fluid list) and with powder snow (block tag) */
    public static final Definition FREEZING_FLUIDS_BUCKET = bucket("freezing_fluids_bucket", () -> new UniversalBucketItem.Properties().durability(20).freezingFluids(List.of(Fluids.WATER)).freezingBlocks(POWDER_SNOW_TAG));
    /** freezes its holder with water (fluid tag) */
    public static final Definition FREEZING_FLUIDS_TAG_BUCKET = bucket("freezing_fluids_tag_bucket", () -> new UniversalBucketItem.Properties().durability(20).freezingFluids(WATER_TAG));
    /** every property that can be a config option is read from {@link Config} */
    public static final Definition CONFIG_BUCKET = bucket("config_bucket", () -> new UniversalBucketItem.Properties()
            .durability(() -> Config.durability)
            .maxTemperature(() -> Config.maxTemperature)
            .minTemperature(() -> Config.minTemperature)
            .upperCrackingTemperature(() -> Config.upperCrackingTemperature)
            .lowerCrackingTemperature(() -> Config.lowerCrackingTemperature)
            .burningTemperature(() -> Config.burningTemperature)
            .freezingTemperature(() -> Config.freezingTemperature)
            .milking(() -> Config.milking)
            .entityObtaining(() -> Config.entityObtaining)
            .blockObtaining(() -> Config.blockObtaining));
    /** empty buckets stack to 64 */
    public static final Definition STACKING_BUCKET = bucket("stacking_bucket", () -> new UniversalBucketItem.Properties().stacksTo(64));
    /** empty buckets do not stack */
    public static final Definition SINGLE_BUCKET = bucket("single_bucket", () -> new UniversalBucketItem.Properties().stacksTo(1));
    /** listed in the creative tab "Ingredients" instead of "Tools & Utilities" */
    public static final Definition TAB_BUCKET = bucket("tab_bucket", () -> new UniversalBucketItem.Properties().tab(INGREDIENTS_TAB));
    /** empty bucket is a furnace fuel ({@link #TEST_FUEL}, 200 ticks) */
    public static final Definition FUEL_BUCKET = bucket("fuel_bucket", () -> new UniversalBucketItem.Properties().cookingFuel(TEST_FUEL));

    /** all test buckets in registration order */
    public static final List<Definition> ALL = List.of(
            TEST_BUCKET, TEMPERATURE_BUCKET, MIN_TEMPERATURE_BUCKET, WATER_ALLOWING_BUCKET, WATER_ALLOWING_TAG_BUCKET,
            WATER_DENYING_BUCKET, WATER_DENYING_TAG_BUCKET, CRACKING_BUCKET, LOWER_CRACKING_BUCKET, CRACKING_FLUIDS_BUCKET,
            CRACKING_FLUIDS_TAG_BUCKET, COLORED_BUCKET, ANTI_MILK_BUCKET, NO_ENTITIES_BUCKET, ANTI_SALMON_BUCKET,
            ANTI_SALMON_TAG_BUCKET, ONLY_PUFFER_BUCKET, ONLY_PUFFER_TAG_BUCKET, NO_BLOCKS_BUCKET, ANTI_POWDER_SNOW_BUCKET,
            ANTI_POWDER_SNOW_TAG_BUCKET, ONLY_POWDER_SNOW_BUCKET, ONLY_STONE_BUCKET, ONLY_POWDER_SNOW_TAG_BUCKET,
            DURABILITY_BUCKET, BURNING_BUCKET, FREEZING_BUCKET, BURNING_FLUIDS_BUCKET, BURNING_FLUIDS_TAG_BUCKET,
            FREEZING_FLUIDS_BUCKET, FREEZING_FLUIDS_TAG_BUCKET, CONFIG_BUCKET, STACKING_BUCKET, SINGLE_BUCKET, TAB_BUCKET,
            FUEL_BUCKET
    );

    /**
     * An item that is registered, but intentionally not registered at BucketLib (BucketLibApi.registerBucket). The
     * adapters try to register it with an unknown id and as a non bucket item to cover the error handling.
     */
    public static final Identifier UNKNOWN_BUCKET_ID = id("unknown_bucket");

    private TestBuckets() {}

    /**
     * @param path path
     * @return identifier in the namespace of the test mod
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

}
