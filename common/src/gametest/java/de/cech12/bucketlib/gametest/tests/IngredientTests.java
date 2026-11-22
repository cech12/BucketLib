package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Map;

/**
 * Custom recipe ingredients of BucketLib for data packs (bucketlib:fluid, bucketlib:milk, bucketlib:entity,
 * bucketlib:block, bucketlib:empty). They accept the vanilla bucket and every BucketLib bucket with this content.
 */
public final class IngredientTests {

    private IngredientTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("ingredient/fluid", IngredientTests::fluid);
        tests.add("ingredient/fluid_tag", IngredientTests::fluidTag);
        tests.add("ingredient/fluid_does_not_accept_buckets_with_entities", IngredientTests::fluidDoesNotAcceptBucketsWithEntities);
        tests.add("ingredient/milk", IngredientTests::milk);
        tests.add("ingredient/entity", IngredientTests::entity);
        tests.add("ingredient/entity_tag", IngredientTests::entityTag);
        tests.add("ingredient/block", IngredientTests::block);
        tests.add("ingredient/block_tag", IngredientTests::blockTag);
        tests.add("ingredient/empty", IngredientTests::empty);
        tests.add("ingredient/empty_item", IngredientTests::emptyItem);
        tests.add("ingredient/empty_tag", IngredientTests::emptyTag);
        tests.add("ingredient/unknown_ids_match_nothing", IngredientTests::unknownIdsMatchNothing);
    }

    private static void assertMatches(GameTestHelper helper, Ingredient ingredient, String name, Map<ItemStack, Boolean> expected) {
        expected.forEach((stack, matches) ->
                TestUtils.assertEquals(helper, ingredient.test(stack), matches, name + " accepts " + TestUtils.describe(stack)));
    }

    private static void assertDisplayed(GameTestHelper helper, Ingredient ingredient, String name, List<Item> expected, List<Item> notExpected) {
        List<Item> items = ingredient.items().map(holder -> holder.value()).toList();
        for (Item item : expected) {
            helper.assertTrue(items.contains(item), name + " does not show " + item + ": " + items);
        }
        for (Item item : notExpected) {
            helper.assertFalse(items.contains(item), name + " shows " + item + ": " + items);
        }
    }

    public static void fluid(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "fluid", Map.of("fluid", "minecraft:water"));
        assertMatches(helper, ingredient, "fluid ingredient (water)", Map.of(
                new ItemStack(Items.WATER_BUCKET), true,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), true,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), false,
                TestUtils.bucket(TestBuckets.TEST_BUCKET), false,
                new ItemStack(Items.LAVA_BUCKET), false,
                new ItemStack(Items.STONE), false,
                ItemStack.EMPTY, false));
        assertDisplayed(helper, ingredient, "fluid ingredient (water)", List.of(Items.WATER_BUCKET, TestBuckets.TEST_BUCKET.item(), TestBuckets.WATER_ALLOWING_BUCKET.item()),
                List.of(TestBuckets.WATER_DENYING_BUCKET.item(), TestBuckets.MIN_TEMPERATURE_BUCKET.item()));
        helper.succeed();
    }

    public static void fluidTag(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "fluid", Map.of("tag", "minecraft:lava"));
        assertMatches(helper, ingredient, "fluid ingredient (#lava)", Map.of(
                new ItemStack(Items.LAVA_BUCKET), true,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), true,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), false));
        assertDisplayed(helper, ingredient, "fluid ingredient (#lava)", List.of(Items.LAVA_BUCKET, TestBuckets.TEST_BUCKET.item()), List.of(TestBuckets.TEMPERATURE_BUCKET.item()));
        helper.succeed();
    }

    /**
     * Like with vanilla fish buckets (which are no water buckets for recipes), a bucket with water and a fish is not
     * accepted as water. Otherwise crafting would destroy the fish.
     */
    public static void fluidDoesNotAcceptBucketsWithEntities(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "fluid", Map.of("fluid", "minecraft:water"));
        helper.assertFalse(ingredient.test(new ItemStack(Items.SALMON_BUCKET)), "fluid ingredient accepts a vanilla salmon bucket");
        helper.assertFalse(ingredient.test(TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON)), "fluid ingredient (water) accepts a bucket with water and salmon");
        helper.succeed();
    }

    public static void milk(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "milk", Map.of());
        assertMatches(helper, ingredient, "milk ingredient", Map.of(
                new ItemStack(Items.MILK_BUCKET), true,
                TestUtils.withMilk(TestBuckets.TEST_BUCKET), true,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), false,
                TestUtils.bucket(TestBuckets.TEST_BUCKET), false,
                ItemStack.EMPTY, false));
        assertDisplayed(helper, ingredient, "milk ingredient", List.of(Items.MILK_BUCKET, TestBuckets.TEST_BUCKET.item()), List.of(TestBuckets.ANTI_MILK_BUCKET.item()));
        helper.succeed();
    }

    public static void entity(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "entity", Map.of("entity", "minecraft:salmon"));
        assertMatches(helper, ingredient, "entity ingredient (salmon)", Map.of(
                new ItemStack(Items.SALMON_BUCKET), true,
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON), true,
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.COD), false,
                new ItemStack(Items.COD_BUCKET), false,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), false,
                ItemStack.EMPTY, false));
        assertDisplayed(helper, ingredient, "entity ingredient (salmon)", List.of(Items.SALMON_BUCKET, TestBuckets.TEST_BUCKET.item()),
                List.of(TestBuckets.ANTI_SALMON_BUCKET.item(), TestBuckets.NO_ENTITIES_BUCKET.item()));
        helper.succeed();
    }

    public static void entityTag(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "entity", Map.of("tag", "bucketlibtest:pufferfish"));
        assertMatches(helper, ingredient, "entity ingredient (#pufferfish)", Map.of(
                new ItemStack(Items.PUFFERFISH_BUCKET), true,
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.PUFFERFISH), true,
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON), false));
        assertDisplayed(helper, ingredient, "entity ingredient (#pufferfish)", List.of(Items.PUFFERFISH_BUCKET, TestBuckets.ONLY_PUFFER_BUCKET.item()), List.of(TestBuckets.NO_ENTITIES_BUCKET.item()));
        helper.succeed();
    }

    public static void block(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "block", Map.of("block", "minecraft:powder_snow"));
        assertMatches(helper, ingredient, "block ingredient (powder snow)", Map.of(
                new ItemStack(Items.POWDER_SNOW_BUCKET), true,
                TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW), true,
                TestUtils.bucket(TestBuckets.TEST_BUCKET), false,
                TestUtils.withMilk(TestBuckets.TEST_BUCKET), false,
                ItemStack.EMPTY, false));
        assertDisplayed(helper, ingredient, "block ingredient (powder snow)", List.of(Items.POWDER_SNOW_BUCKET, TestBuckets.TEST_BUCKET.item()), List.of(TestBuckets.NO_BLOCKS_BUCKET.item()));
        // a block without vanilla bucket matches nothing
        Ingredient stone = TestUtils.ingredient(helper, "block", Map.of("block", "minecraft:stone"));
        helper.assertFalse(stone.test(TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW)), "block ingredient (stone) accepts powder snow");
        helper.succeed();
    }

    public static void blockTag(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "block", Map.of("tag", "bucketlibtest:powder_snow"));
        assertMatches(helper, ingredient, "block ingredient (#powder_snow)", Map.of(
                new ItemStack(Items.POWDER_SNOW_BUCKET), true,
                TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW), true,
                TestUtils.bucket(TestBuckets.TEST_BUCKET), false));
        assertDisplayed(helper, ingredient, "block ingredient (#powder_snow)", List.of(Items.POWDER_SNOW_BUCKET, TestBuckets.TEST_BUCKET.item()), List.of(TestBuckets.NO_BLOCKS_BUCKET.item()));
        Ingredient emptyTag = TestUtils.ingredient(helper, "block", Map.of("tag", "minecraft:logs"));
        helper.assertFalse(emptyTag.test(TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW)), "block ingredient (#logs) accepts powder snow");
        helper.succeed();
    }

    /** Without item and tag: every empty bucket (vanilla and BucketLib). */
    public static void empty(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "empty", Map.of());
        assertMatches(helper, ingredient, "empty ingredient", Map.of(
                new ItemStack(Items.BUCKET), true,
                TestUtils.bucket(TestBuckets.TEST_BUCKET), true,
                TestUtils.bucket(TestBuckets.NO_BLOCKS_BUCKET), true,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), false,
                new ItemStack(Items.WATER_BUCKET), false,
                ItemStack.EMPTY, false));
        assertDisplayed(helper, ingredient, "empty ingredient", List.of(Items.BUCKET, TestBuckets.TEST_BUCKET.item(), TestBuckets.NO_BLOCKS_BUCKET.item()), List.of(Items.WATER_BUCKET));
        helper.succeed();
    }

    /** With item: only the empty bucket of this item. */
    public static void emptyItem(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "empty", Map.of("item", "bucketlibtest:test_bucket"));
        assertMatches(helper, ingredient, "empty ingredient (test bucket)", Map.of(
                TestUtils.bucket(TestBuckets.TEST_BUCKET), true,
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), false,
                TestUtils.bucket(TestBuckets.NO_BLOCKS_BUCKET), false,
                new ItemStack(Items.BUCKET), false));
        assertDisplayed(helper, ingredient, "empty ingredient (test bucket)", List.of(TestBuckets.TEST_BUCKET.item()), List.of(Items.BUCKET, TestBuckets.NO_BLOCKS_BUCKET.item()));
        helper.succeed();
    }

    /** With item tag: only the empty buckets of the tag. */
    public static void emptyTag(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "empty", Map.of("tag", "minecraft:dyeable"));
        assertMatches(helper, ingredient, "empty ingredient (#dyeable)", Map.of(
                TestUtils.bucket(TestBuckets.COLORED_BUCKET), true,
                TestUtils.withFluid(TestBuckets.COLORED_BUCKET, Fluids.WATER), false,
                TestUtils.bucket(TestBuckets.TEST_BUCKET), false,
                new ItemStack(Items.BUCKET), false));
        assertDisplayed(helper, ingredient, "empty ingredient (#dyeable)", List.of(), List.of(Items.BUCKET, TestBuckets.TEST_BUCKET.item()));
        assertDisplayed(helper, ingredient, "empty ingredient (#dyeable)", List.of(TestBuckets.COLORED_BUCKET.item()), List.of());
        helper.succeed();
    }

    private static Ingredient unknown(GameTestHelper helper, String type, String field) {
        return TestUtils.noException(helper, "decoding an ingredient with unknown " + type,
                () -> TestUtils.ingredient(helper, type, Map.of(field, "bucketlibtest:not_existing")));
    }

    private static void assertMatchesNothing(GameTestHelper helper, String type, Ingredient ingredient) {
        for (ItemStack stack : List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET), TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER),
                new ItemStack(Items.BUCKET), new ItemStack(Items.SALMON_BUCKET), new ItemStack(Items.POWDER_SNOW_BUCKET))) {
            boolean matches = TestUtils.noException(helper, "testing an ingredient with unknown " + type, () -> ingredient.test(stack));
            helper.assertFalse(matches, "ingredient with unknown " + type + " accepts " + TestUtils.describe(stack));
        }
    }

    /** Ingredients with unknown ids (e.g. of a removed mod) do not crash the recipe matching and match nothing. */
    public static void unknownIdsMatchNothing(GameTestHelper helper) {
        assertMatchesNothing(helper, "fluid", unknown(helper, "fluid", "fluid"));
        Ingredient empty = unknown(helper, "empty", "item");
        Ingredient entity = unknown(helper, "entity", "entity");
        Ingredient block = unknown(helper, "block", "block");
        assertMatchesNothing(helper, "empty", empty);
        assertMatchesNothing(helper, "entity", entity);
        assertMatchesNothing(helper, "block", block);
        helper.succeed();
    }

}
