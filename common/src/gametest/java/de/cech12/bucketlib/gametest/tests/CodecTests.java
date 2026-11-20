package de.cech12.bucketlib.gametest.tests;

import com.google.gson.JsonElement;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.GameTestPlatform;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.item.crafting.BucketFillingType;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Map;

/**
 * Saving and syncing: buckets in saved worlds and in network packets, ingredients and bucket filling recipes in data
 * packs (json) and in network packets.
 */
public final class CodecTests {

    private CodecTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("codec/buckets_are_saved_and_loaded", CodecTests::bucketsAreSavedAndLoaded);
        tests.add("codec/buckets_are_synced", CodecTests::bucketsAreSynced);
        tests.add("codec/ingredients_json_round_trip", CodecTests::ingredientsJsonRoundTrip);
        tests.add("codec/tag_ingredients_json_round_trip", CodecTests::tagIngredientsJsonRoundTrip);
        tests.add("codec/ingredients_network_round_trip", CodecTests::ingredientsNetworkRoundTrip);
        tests.add("codec/bucket_filling_recipes_json_round_trip", CodecTests::bucketFillingRecipesJsonRoundTrip);
        tests.add("codec/bucket_filling_recipes_network_round_trip", CodecTests::bucketFillingRecipesNetworkRoundTrip);
        tests.add("codec/bucket_filling_types", CodecTests::bucketFillingTypes);
        tests.add("codec/content_codecs_reject_empty_values", CodecTests::contentCodecsRejectEmptyValues);
    }

    private static List<ItemStack> buckets() {
        ItemStack damaged = TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER);
        damaged.setDamageValue(2);
        return List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET), TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER),
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), TestUtils.withMilk(TestBuckets.TEST_BUCKET),
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON), TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW),
                damaged);
    }

    private static void assertSameBucket(GameTestHelper helper, ItemStack actual, ItemStack expected, String action) {
        helper.assertTrue(ItemStack.isSameItemSameComponents(actual, expected), action + " changed " + TestUtils.describe(expected) + " to " + TestUtils.describe(actual));
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(actual), BucketLibUtil.getFluid(expected), "fluid after " + action);
        TestUtils.assertEquals(helper, BucketLibUtil.containsMilk(actual), BucketLibUtil.containsMilk(expected), "milk after " + action);
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(actual), BucketLibUtil.getEntityType(expected), "entity after " + action);
        TestUtils.assertEquals(helper, BucketLibUtil.getBlock(actual), BucketLibUtil.getBlock(expected), "block after " + action);
        TestUtils.assertEquals(helper, actual.getDamageValue(), expected.getDamageValue(), "damage after " + action);
    }

    /** All contents survive saving and loading (world save, persistent component codecs). */
    public static void bucketsAreSavedAndLoaded(GameTestHelper helper) {
        for (ItemStack stack : buckets()) {
            JsonElement json = TestUtils.encode(helper, ItemStack.CODEC, stack);
            ItemStack loaded = TestUtils.decode(helper, ItemStack.CODEC, json);
            assertSameBucket(helper, loaded, stack, "saving and loading");
        }
        helper.succeed();
    }

    /** All contents survive sending to the client (network component codecs). */
    public static void bucketsAreSynced(GameTestHelper helper) {
        for (ItemStack stack : buckets()) {
            RegistryFriendlyByteBuf buffer = RegistryFriendlyByteBuf.decorator(helper.getLevel().registryAccess()).apply(Unpooled.buffer());
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            ItemStack synced = ItemStack.STREAM_CODEC.decode(buffer);
            assertSameBucket(helper, synced, stack, "syncing");
        }
        helper.succeed();
    }

    private static Ingredient jsonRoundTrip(GameTestHelper helper, Ingredient ingredient) {
        JsonElement json = TestUtils.noException(helper, "encoding " + ingredient, () -> TestUtils.encode(helper, Ingredient.CODEC, ingredient));
        return TestUtils.noException(helper, "decoding " + json, () -> TestUtils.decode(helper, Ingredient.CODEC, json));
    }

    private static void assertSameMatches(GameTestHelper helper, Ingredient actual, Ingredient expected, String name) {
        List<ItemStack> stacks = List.of(new ItemStack(Items.BUCKET), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.SALMON_BUCKET), new ItemStack(Items.PUFFERFISH_BUCKET), new ItemStack(Items.POWDER_SNOW_BUCKET),
                TestUtils.bucket(TestBuckets.TEST_BUCKET), TestUtils.bucket(TestBuckets.COLORED_BUCKET), TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER),
                TestUtils.withMilk(TestBuckets.TEST_BUCKET), TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.PUFFERFISH),
                TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW));
        for (ItemStack stack : stacks) {
            boolean matches = TestUtils.noException(helper, "testing " + name, () -> actual.test(stack));
            TestUtils.assertEquals(helper, matches, expected.test(stack), name + " accepts " + TestUtils.describe(stack));
        }
    }

    /** Ingredients with an id (fluid, entity, block, item) and milk keep their behavior after encoding and decoding. */
    public static void ingredientsJsonRoundTrip(GameTestHelper helper) {
        Map<String, Map<String, String>> ingredients = Map.of(
                "fluid", Map.of("fluid", "minecraft:water"),
                "milk", Map.of(),
                "entity", Map.of("entity", "minecraft:pufferfish"),
                "block", Map.of("block", "minecraft:powder_snow"));
        ingredients.forEach((type, fields) -> {
            Ingredient ingredient = TestUtils.ingredient(helper, type, fields);
            assertSameMatches(helper, jsonRoundTrip(helper, ingredient), ingredient, "json round trip of " + type + " " + fields);
        });
        Ingredient emptyItem = TestUtils.ingredient(helper, "empty", Map.of("item", "bucketlibtest:test_bucket"));
        assertSameMatches(helper, jsonRoundTrip(helper, emptyItem), emptyItem, "json round trip of empty (item)");
        Ingredient fluidTag = TestUtils.ingredient(helper, "fluid", Map.of("tag", "minecraft:water"));
        assertSameMatches(helper, jsonRoundTrip(helper, fluidTag), fluidTag, "json round trip of fluid (tag)");
        helper.succeed();
    }

    /**
     * Ingredients without id (tag ingredients, empty ingredient without item and tag) keep their behavior after encoding
     * and decoding (e.g. for data generation, recipe viewers or synced recipe displays).
     */
    public static void tagIngredientsJsonRoundTrip(GameTestHelper helper) {
        Ingredient entityTag = TestUtils.ingredient(helper, "entity", Map.of("tag", "bucketlibtest:pufferfish"));
        Ingredient blockTag = TestUtils.ingredient(helper, "block", Map.of("tag", "bucketlibtest:powder_snow"));
        Ingredient emptyTag = TestUtils.ingredient(helper, "empty", Map.of("tag", "minecraft:dyeable"));
        Ingredient empty = TestUtils.ingredient(helper, "empty", Map.of());
        assertSameMatches(helper, jsonRoundTrip(helper, emptyTag), emptyTag, "json round trip of empty (tag)");
        KnownIssues.run(helper, "KI-10 json codec of ingredients writes the default registry entry for missing ids", Platforms.ALL, () -> {
            assertSameMatches(helper, jsonRoundTrip(helper, entityTag), entityTag, "json round trip of entity (tag)");
            assertSameMatches(helper, jsonRoundTrip(helper, blockTag), blockTag, "json round trip of block (tag)");
            assertSameMatches(helper, jsonRoundTrip(helper, empty), empty, "json round trip of empty");
        });
    }

    private static void assertNetworkRoundTrip(GameTestHelper helper, List<Map.Entry<String, Map<String, String>>> ingredients) {
        for (var entry : ingredients) {
            Ingredient ingredient = TestUtils.ingredient(helper, entry.getKey(), entry.getValue());
            Ingredient synced = TestUtils.noException(helper, "syncing " + entry, () -> GameTestPlatform.INSTANCE.networkRoundTrip(ingredient, helper.getLevel().registryAccess()));
            assertSameMatches(helper, synced, ingredient, "network round trip of " + entry);
        }
    }

    /**
     * All ingredient variants keep their behavior after sending them to a client. Fabric uses the packet codecs of the
     * ingredient serializers, NeoForge the json codec (so KI-10 also affects the network on NeoForge).
     */
    public static void ingredientsNetworkRoundTrip(GameTestHelper helper) {
        assertNetworkRoundTrip(helper, List.of(
                Map.entry("fluid", Map.of("fluid", "minecraft:water")), Map.entry("fluid", Map.of("tag", "minecraft:water")),
                Map.entry("milk", Map.of()), Map.entry("entity", Map.of("entity", "minecraft:pufferfish")),
                Map.entry("block", Map.of("block", "minecraft:powder_snow")), Map.entry("empty", Map.of("item", "bucketlibtest:test_bucket")),
                Map.entry("empty", Map.of("tag", "minecraft:dyeable"))));
        KnownIssues.run(helper, "KI-10 json codec of ingredients writes the default registry entry for missing ids", java.util.Set.of(Platforms.NEOFORGE), () ->
                assertNetworkRoundTrip(helper, List.of(Map.entry("entity", Map.of("tag", "bucketlibtest:pufferfish")),
                        Map.entry("block", Map.of("tag", "bucketlibtest:powder_snow")), Map.entry("empty", Map.of()))));
    }

    private static final String SHAPED = "{\"type\":\"bucketlib:bucket_filling_shaped\",\"category\":\"misc\",\"pattern\":[\"BS\"],\"key\":{\"B\":\"bucketlibtest:test_bucket\",\"S\":\"minecraft:stick\"},\"filling_type\":\"fluid\",\"fluid\":\"minecraft:water\"}";
    private static final String SHAPELESS = "{\"type\":\"bucketlib:bucket_filling_shapeless\",\"category\":\"misc\",\"ingredients\":[\"bucketlibtest:test_bucket\",\"minecraft:stick\"],\"filling_type\":\"fluid\",\"fluid\":\"minecraft:water\"}";

    @SuppressWarnings("unchecked")
    private static void assertFillsWater(GameTestHelper helper, Recipe<?> anyRecipe, String action) {
        Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) anyRecipe;
        CraftingInput input = CraftingInput.of(2, 1, List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET), new ItemStack(Items.STICK)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "recipe does not match after " + action);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(recipe.assemble(input)), Fluids.WATER, "fluid of the result after " + action);
    }

    /** Bucket filling recipes can be written as json again (e.g. by data generators) and read back. */
    public static void bucketFillingRecipesJsonRoundTrip(GameTestHelper helper) {
        Recipe<?> shaped = TestUtils.decode(helper, Recipe.DIRECT_CODEC, TestUtils.json(SHAPED));
        assertFillsWater(helper, TestUtils.decode(helper, Recipe.DIRECT_CODEC, TestUtils.noException(helper, "encoding a shaped bucket filling recipe", () -> TestUtils.encode(helper, Recipe.DIRECT_CODEC, shaped))), "json round trip (shaped)");
        Recipe<?> shapeless = TestUtils.decode(helper, Recipe.DIRECT_CODEC, TestUtils.json(SHAPELESS));
        KnownIssues.run(helper, "KI-11 bucket filling recipes cannot be encoded if not all of fluid, block and entity are set", Platforms.ALL, () ->
                assertFillsWater(helper, TestUtils.decode(helper, Recipe.DIRECT_CODEC, TestUtils.noException(helper, "encoding a shapeless bucket filling recipe", () -> TestUtils.encode(helper, Recipe.DIRECT_CODEC, shapeless))), "json round trip (shapeless)"));
    }

    private static Recipe<?> networkRoundTrip(GameTestHelper helper, Recipe<?> recipe) {
        RegistryFriendlyByteBuf buffer = RegistryFriendlyByteBuf.decorator(helper.getLevel().registryAccess()).apply(Unpooled.buffer());
        Recipe.STREAM_CODEC.encode(buffer, recipe);
        return Recipe.STREAM_CODEC.decode(buffer);
    }

    /** Bucket filling recipes can be sent to clients (e.g. for recipe viewers on loaders that sync recipes). */
    public static void bucketFillingRecipesNetworkRoundTrip(GameTestHelper helper) {
        Recipe<?> shaped = TestUtils.decode(helper, Recipe.DIRECT_CODEC, TestUtils.json(SHAPED));
        Recipe<?> shapeless = TestUtils.decode(helper, Recipe.DIRECT_CODEC, TestUtils.json(SHAPELESS));
        KnownIssues.run(helper, "KI-11 bucket filling recipes cannot be encoded if not all of fluid, block and entity are set", Platforms.ALL, () -> {
            assertFillsWater(helper, TestUtils.noException(helper, "syncing a shaped bucket filling recipe", () -> networkRoundTrip(helper, shaped)), "network round trip (shaped)");
            assertFillsWater(helper, TestUtils.noException(helper, "syncing a shapeless bucket filling recipe", () -> networkRoundTrip(helper, shapeless)), "network round trip (shapeless)");
        });
    }

    /** The filling types have stable names and ids (json and network). */
    public static void bucketFillingTypes(GameTestHelper helper) {
        Map<BucketFillingType, String> names = Map.of(BucketFillingType.BLOCK, "block", BucketFillingType.ENTITY, "entity", BucketFillingType.FLUID, "fluid", BucketFillingType.MILK, "milk");
        names.forEach((type, name) -> {
            TestUtils.assertEquals(helper, type.getSerializedName(), name, "name of " + type);
            TestUtils.assertEquals(helper, TestUtils.decode(helper, BucketFillingType.CODEC, TestUtils.json("\"" + name + "\"")), type, "decoded " + name);
            TestUtils.assertEquals(helper, BucketFillingType.BY_ID.apply(type.getId()), type, "type of id " + type.getId());
            var buffer = Unpooled.buffer();
            BucketFillingType.STREAM_CODEC.encode(buffer, type);
            TestUtils.assertEquals(helper, BucketFillingType.STREAM_CODEC.decode(buffer), type, "synced " + type);
        });
        TestUtils.assertEquals(helper, BucketFillingType.BLOCK.getId(), 0, "id of block");
        TestUtils.assertEquals(helper, BucketFillingType.MILK.getId(), 3, "id of milk");
        helper.succeed();
    }

    /** The fluid and block fields of bucket filling recipes do not accept "nothing" (empty fluid, air). */
    public static void contentCodecsRejectEmptyValues(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        helper.assertTrue(RegistryUtil.FLUID_CODEC.parse(ops, TestUtils.json("\"minecraft:empty\"")).isError(), "fluid codec accepts the empty fluid");
        helper.assertTrue(RegistryUtil.FLUID_CODEC.parse(ops, TestUtils.json("\"minecraft:water\"")).isSuccess(), "fluid codec does not accept water");
        helper.assertTrue(RegistryUtil.BLOCK_CODEC.parse(ops, TestUtils.json("\"minecraft:air\"")).isError(), "block codec accepts air");
        helper.assertTrue(RegistryUtil.BLOCK_CODEC.parse(ops, TestUtils.json("\"minecraft:powder_snow\"")).isSuccess(), "block codec does not accept powder snow");
        helper.assertTrue(RegistryUtil.ENTITY_TYPE_CODEC.parse(ops, TestUtils.json("\"minecraft:salmon\"")).isSuccess(), "entity codec does not accept salmon");
        TestUtils.assertEquals(helper, BuiltInRegistries.FLUID.getKey(Fluids.WATER).toString(), "minecraft:water", "test setup broken: water id");
        helper.succeed();
    }

}
