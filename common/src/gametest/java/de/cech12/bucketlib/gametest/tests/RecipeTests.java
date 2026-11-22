package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.item.crafting.BucketFillingType;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crafting with BucketLib buckets: crafting remainders (the empty bucket stays), the cake recipe of BucketLib, recipes of
 * data packs with BucketLib ingredients, the bucket filling recipe types, dyeing and repairing.
 */
public final class RecipeTests {

    private RecipeTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("recipe/crafting_remainder_is_the_empty_bucket", RecipeTests::craftingRemainderIsTheEmptyBucket);
        tests.add("recipe/cracked_buckets_have_no_crafting_remainder", RecipeTests::crackedBucketsHaveNoCraftingRemainder);
        tests.add("recipe/cake_with_milk_buckets", RecipeTests::cakeWithMilkBuckets);
        tests.add("recipe/cake_recipe_is_unlocked_by_eggs", RecipeTests::cakeRecipeIsUnlockedByEggs);
        tests.add("recipe/data_pack_recipes_with_bucketlib_ingredients", RecipeTests::dataPackRecipesWithBucketLibIngredients);
        tests.add("recipe/bucket_filling_shapeless", RecipeTests::bucketFillingShapeless);
        tests.add("recipe/bucket_filling_shaped", RecipeTests::bucketFillingShaped);
        tests.add("recipe/bucket_filling_respects_the_properties", RecipeTests::bucketFillingRespectsTheProperties);
        tests.add("recipe/bucket_filling_with_several_buckets", RecipeTests::bucketFillingWithSeveralBuckets);
        tests.add("recipe/dyeing_keeps_the_content", RecipeTests::dyeingKeepsTheContent);
        tests.add("recipe/repairing_combines_durability", RecipeTests::repairingCombinesDurability);
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> recipe(GameTestHelper helper, Identifier id) {
        var recipe = helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id));
        helper.assertTrue(recipe.isPresent(), "recipe " + id + " is missing");
        return (Recipe<CraftingInput>) recipe.get().value();
    }

    /** crafting remainder like the crafting grid uses it (loader specific remainder hooks included) */
    private static ItemStack remainder(ItemStack stack) {
        return CraftingRecipe.defaultCraftingReminder(CraftingInput.of(1, 1, List.of(stack))).getFirst();
    }

    /** Crafting with a filled bucket leaves the empty bucket (like vanilla buckets), the empty bucket has none. */
    public static void craftingRemainderIsTheEmptyBucket(GameTestHelper helper) {
        for (ItemStack stack : List.of(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), TestUtils.withMilk(TestBuckets.TEST_BUCKET),
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON), TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE),
                TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW))) {
            ItemStack remainder = remainder(stack);
            helper.assertTrue(remainder.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(remainder), "crafting remainder of " + TestUtils.describe(stack) + " is " + TestUtils.describe(remainder));
            helper.assertTrue(remainder.getComponentsPatch().isEmpty(), "crafting remainder of " + TestUtils.describe(stack) + " keeps components: " + remainder.getComponentsPatch());
        }
        helper.assertTrue(remainder(TestUtils.bucket(TestBuckets.TEST_BUCKET)).isEmpty(), "empty bucket has a crafting remainder");
        // durability is used
        ItemStack durability = remainder(TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER));
        TestUtils.assertEquals(helper, durability.getDamageValue(), 1, "damage of the crafting remainder");
        ItemStack powderSnow = remainder(TestUtils.withBlock(TestBuckets.DURABILITY_BUCKET, Blocks.POWDER_SNOW));
        TestUtils.assertEquals(helper, powderSnow.getDamageValue(), 1, "damage of the crafting remainder of a powder snow bucket");
        ItemStack salmon = remainder(TestUtils.withEntity(TestBuckets.DURABILITY_BUCKET, Fluids.WATER, EntityTypes.SALMON));
        TestUtils.assertEquals(helper, salmon.getDamageValue(), 1, "damage of the crafting remainder of a salmon bucket");
        helper.succeed();
    }

    /** A bucket that cracks when emptied is used up when crafting. */
    public static void crackedBucketsHaveNoCraftingRemainder(GameTestHelper helper) {
        ItemStack lava = TestUtils.withFluid(TestBuckets.CRACKING_BUCKET, Fluids.LAVA);
        helper.assertTrue(remainder(lava).isEmpty(), "cracked bucket has a crafting remainder: " + remainder(lava));
        helper.assertFalse(remainder(TestUtils.withFluid(TestBuckets.CRACKING_BUCKET, Fluids.WATER)).isEmpty(), "not cracked bucket has no crafting remainder");
        helper.succeed();
    }

    private static CraftingInput cakeInput(ItemStack milk) {
        ItemStack sugar = new ItemStack(Items.SUGAR);
        ItemStack wheat = new ItemStack(Items.WHEAT);
        return CraftingInput.of(3, 3, List.of(milk.copy(), milk.copy(), milk.copy(), sugar, new ItemStack(Items.EGG), sugar.copy(), wheat, wheat.copy(), wheat.copy()));
    }

    /** BucketLib adds a cake recipe that accepts every milk bucket (bucketlib:milk ingredient) and leaves empty buckets. */
    public static void cakeWithMilkBuckets(GameTestHelper helper) {
        Recipe<CraftingInput> recipe = recipe(helper, BucketLib.id("cake"));
        CraftingInput input = cakeInput(TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "cake recipe does not accept BucketLib milk buckets");
        helper.assertTrue(recipe.assemble(input).is(Items.CAKE), "cake recipe does not craft a cake");
        helper.assertTrue(recipe.matches(cakeInput(new ItemStack(Items.MILK_BUCKET)), helper.getLevel()), "cake recipe does not accept vanilla milk buckets");
        helper.assertFalse(recipe.matches(cakeInput(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)), helper.getLevel()), "cake recipe accepts water buckets");
        var remaining = ((CraftingRecipe) recipe).getRemainingItems(input);
        for (int slot = 0; slot < 3; slot++) {
            helper.assertTrue(remaining.get(slot).is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(remaining.get(slot)), "remaining item of the milk bucket in slot " + slot + ": " + remaining.get(slot));
        }
        // the recipe lookup finds a cake recipe
        var found = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(found.isPresent() && found.get().value().assemble(input).is(Items.CAKE), "no cake recipe is found for BucketLib milk buckets");
        helper.succeed();
    }

    /** The recipe advancement unlocks the cake recipe of BucketLib with an egg, like the vanilla cake recipe. */
    public static void cakeRecipeIsUnlockedByEggs(GameTestHelper helper) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(BucketLib.id("recipes/food/cake"));
        helper.assertTrue(advancement != null, "recipe advancement is missing");
        helper.assertTrue(advancement.value().rewards().recipes().contains(ResourceKey.create(Registries.RECIPE, BucketLib.id("cake"))), "recipe advancement does not unlock the cake recipe");
        helper.assertTrue(advancement.value().criteria().containsKey("has_egg"), "recipe advancement has no has_egg criterion");
        helper.assertTrue(advancement.value().requirements().test("has_egg"::equals), "recipe advancement is not completed by has_egg alone");
        helper.succeed();
    }

    /** Recipes of the test data pack with the BucketLib ingredients, with BucketLib buckets and their remainders. */
    public static void dataPackRecipesWithBucketLibIngredients(GameTestHelper helper) {
        ItemStack water = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        ItemStack lava = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA);
        record Case(String recipe, CraftingInput input, net.minecraft.world.item.Item result) {}
        List<Case> cases = List.of(
                new Case("fluidrecipe", CraftingInput.of(2, 1, List.of(water, lava)), Items.OBSIDIAN),
                new Case("milkrecipe", CraftingInput.of(2, 1, List.of(TestUtils.withMilk(TestBuckets.TEST_BUCKET), new ItemStack(Items.SUGAR))), Items.CAKE),
                new Case("entityrecipe", CraftingInput.of(1, 1, List.of(TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON))), Items.SALMON),
                new Case("blockrecipe", CraftingInput.of(1, 1, List.of(TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW))), Items.SNOWBALL),
                new Case("emptybucketrecipe", CraftingInput.of(1, 1, List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET))), Items.GRAVEL),
                new Case("emptybucketrecipe_shaped", CraftingInput.of(2, 1, List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET), TestUtils.bucket(TestBuckets.TEST_BUCKET))), Items.GRAVEL));
        for (Case testCase : cases) {
            Recipe<CraftingInput> recipe = recipe(helper, TestBuckets.id(testCase.recipe()));
            helper.assertTrue(recipe.matches(testCase.input(), helper.getLevel()), testCase.recipe() + " does not match");
            helper.assertTrue(recipe.assemble(testCase.input()).is(testCase.result()), testCase.recipe() + " does not craft " + testCase.result());
            var remaining = ((CraftingRecipe) recipe).getRemainingItems(testCase.input());
            for (int slot = 0; slot < testCase.input().size(); slot++) {
                ItemStack in = testCase.input().getItem(slot);
                if (in.is(TestBuckets.TEST_BUCKET.item())) {
                    ItemStack out = remaining.get(slot);
                    boolean expectedRemainder = !BucketLibUtil.isEmpty(in);
                    TestUtils.assertEquals(helper, out.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(out), expectedRemainder, testCase.recipe() + " leaves an empty bucket in slot " + slot + " (" + out + ")");
                }
            }
        }
        helper.succeed();
    }

    private static Recipe<?> fillingRecipe(GameTestHelper helper, boolean shaped, String ingredient, BucketFillingType type, String extraField, String extraValue) {
        String json;
        String extra = extraField != null ? ",\"" + extraField + "\":\"" + extraValue + "\"" : "";
        if (shaped) {
            json = "{\"type\":\"bucketlib:bucket_filling_shaped\",\"category\":\"misc\",\"pattern\":[\"BS\"],\"key\":{\"B\":" + ingredient
                    + ",\"S\":\"minecraft:stick\"},\"filling_type\":\"" + type.getSerializedName() + "\"" + extra + "}";
        } else {
            json = "{\"type\":\"bucketlib:bucket_filling_shapeless\",\"category\":\"misc\",\"ingredients\":[" + ingredient
                    + ",\"minecraft:stick\"],\"filling_type\":\"" + type.getSerializedName() + "\"" + extra + "}";
        }
        return TestUtils.noException(helper, "decoding " + json, () -> TestUtils.decode(helper, Recipe.CODEC, TestUtils.json(json)).value());
    }

    @SuppressWarnings("unchecked")
    private static void assertFilling(GameTestHelper helper, Recipe<?> anyRecipe, List<ItemStack> grid, int width, ItemStack expected) {
        Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) anyRecipe;
        CraftingInput input = CraftingInput.of(width, grid.size() / width, grid);
        if (expected == null) {
            helper.assertFalse(recipe.matches(input, helper.getLevel()), "bucket filling recipe matches " + grid);
            return;
        }
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "bucket filling recipe does not match " + grid);
        ItemStack result = recipe.assemble(input);
        helper.assertTrue(ItemStack.isSameItemSameComponents(result, expected), "bucket filling result is " + TestUtils.describe(result) + " instead of " + TestUtils.describe(expected));
        TestUtils.assertEquals(helper, result.getCount(), 1, "count of the bucket filling result");
    }

    private static final String TEST_BUCKET = "\"bucketlibtest:test_bucket\"";

    /** The shapeless bucket filling recipe fills an empty bucket with a fluid, milk, an entity or a block. */
    public static void bucketFillingShapeless(GameTestHelper helper) {
        ItemStack empty = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        ItemStack stick = new ItemStack(Items.STICK);
        assertFilling(helper, fillingRecipe(helper, false, TEST_BUCKET, BucketFillingType.FLUID, "fluid", "minecraft:water"), List.of(empty, stick), 2, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        assertFilling(helper, fillingRecipe(helper, false, TEST_BUCKET, BucketFillingType.MILK, null, null), List.of(stick, empty), 2, TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        assertFilling(helper, fillingRecipe(helper, false, TEST_BUCKET, BucketFillingType.BLOCK, "block", "minecraft:powder_snow"), List.of(empty, stick), 2, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW));
        assertFilling(helper, fillingRecipe(helper, false, TEST_BUCKET, BucketFillingType.ENTITY, "entity", "minecraft:sulfur_cube"), List.of(empty, stick), 2, TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE));
        // a filled bucket is not filled again
        assertFilling(helper, fillingRecipe(helper, false, TEST_BUCKET, BucketFillingType.FLUID, "fluid", "minecraft:water"), List.of(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), stick), 2, null);
        // without bucket
        assertFilling(helper, fillingRecipe(helper, false, TEST_BUCKET, BucketFillingType.FLUID, "fluid", "minecraft:water"), List.of(stick, stick), 2, null);
        helper.succeed();
    }

    /** The shaped bucket filling recipe fills an empty bucket, also with an entity and its fluid. */
    public static void bucketFillingShaped(GameTestHelper helper) {
        ItemStack empty = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        ItemStack stick = new ItemStack(Items.STICK);
        assertFilling(helper, fillingRecipe(helper, true, TEST_BUCKET, BucketFillingType.FLUID, "fluid", "minecraft:lava"), List.of(empty, stick), 2, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA));
        assertFilling(helper, fillingRecipe(helper, true, TEST_BUCKET, BucketFillingType.MILK, null, null), List.of(empty, stick), 2, TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        assertFilling(helper, fillingRecipe(helper, true, TEST_BUCKET, BucketFillingType.BLOCK, "block", "minecraft:powder_snow"), List.of(empty, stick), 2, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW));
        Recipe<?> salmon = TestUtils.noException(helper, "decoding a shaped entity filling recipe", () -> TestUtils.decode(helper, Recipe.CODEC, TestUtils.json(
                "{\"type\":\"bucketlib:bucket_filling_shaped\",\"category\":\"misc\",\"pattern\":[\"BS\"],\"key\":{\"B\":" + TEST_BUCKET
                        + ",\"S\":\"minecraft:stick\"},\"filling_type\":\"entity\",\"entity\":\"minecraft:salmon\",\"fluid\":\"minecraft:water\"}")).value());
        assertFilling(helper, salmon, List.of(empty, stick), 2, TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON));
        // wrong shape (horizontally mirrored patterns match like vanilla shaped recipes, vertical ones do not)
        assertFilling(helper, fillingRecipe(helper, true, TEST_BUCKET, BucketFillingType.FLUID, "fluid", "minecraft:lava"), List.of(empty, stick), 1, null);
        // without empty bucket: no match, and the result of a not matching input is empty
        @SuppressWarnings("unchecked")
        Recipe<CraftingInput> lava = (Recipe<CraftingInput>) fillingRecipe(helper, true, "[" + TEST_BUCKET + ",\"bucketlibtest:temperature_bucket\"]", BucketFillingType.FLUID, "fluid", "minecraft:lava");
        CraftingInput noBucket = CraftingInput.of(2, 1, List.of(stick, stick));
        helper.assertFalse(lava.matches(noBucket, helper.getLevel()), "shaped bucket filling recipe matches two sticks");
        helper.assertTrue(lava.assemble(CraftingInput.of(2, 1, List.of(TestUtils.bucket(TestBuckets.TEMPERATURE_BUCKET), stick))).isEmpty(), "shaped bucket filling recipe fills a bucket that cannot hold lava");
        helper.assertTrue(lava.assemble(noBucket).isEmpty(), "shaped bucket filling recipe crafts without bucket");
        helper.succeed();
    }

    /** Bucket filling recipes only match buckets that can hold the content. */
    public static void bucketFillingRespectsTheProperties(GameTestHelper helper) {
        ItemStack stick = new ItemStack(Items.STICK);
        Recipe<?> lava = fillingRecipe(helper, false, "[" + TEST_BUCKET + ",\"bucketlibtest:temperature_bucket\"]", BucketFillingType.FLUID, "fluid", "minecraft:lava");
        assertFilling(helper, lava, List.of(TestUtils.bucket(TestBuckets.TEMPERATURE_BUCKET), stick), 2, null);
        assertFilling(helper, lava, List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET), stick), 2, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA));
        Recipe<?> milk = fillingRecipe(helper, false, "[" + TEST_BUCKET + ",\"bucketlibtest:anti_milk_bucket\"]", BucketFillingType.MILK, null, null);
        assertFilling(helper, milk, List.of(TestUtils.bucket(TestBuckets.ANTI_MILK_BUCKET), stick), 2, null);
        Recipe<?> block = fillingRecipe(helper, true, "[" + TEST_BUCKET + ",\"bucketlibtest:no_blocks_bucket\"]", BucketFillingType.BLOCK, "block", "minecraft:powder_snow");
        assertFilling(helper, block, List.of(TestUtils.bucket(TestBuckets.NO_BLOCKS_BUCKET), stick), 2, null);
        Recipe<?> entity = fillingRecipe(helper, true, "[" + TEST_BUCKET + ",\"bucketlibtest:no_entities_bucket\"]", BucketFillingType.ENTITY, "entity", "minecraft:sulfur_cube");
        assertFilling(helper, entity, List.of(TestUtils.bucket(TestBuckets.NO_ENTITIES_BUCKET), stick), 2, null);
        helper.succeed();
    }

    /**
     * A bucket filling recipe whose ingredient lists several buckets loads and works, even if the first listed bucket
     * cannot hold the content.
     */
    public static void bucketFillingWithSeveralBuckets(GameTestHelper helper) {
        ItemStack stick = new ItemStack(Items.STICK);
        String ingredient = "[\"bucketlibtest:temperature_bucket\"," + TEST_BUCKET + "]";
        String shapeless = "{\"type\":\"bucketlib:bucket_filling_shapeless\",\"category\":\"misc\",\"ingredients\":[" + ingredient + ",\"minecraft:stick\"],\"filling_type\":\"fluid\",\"fluid\":\"minecraft:lava\"}";
        String shaped = "{\"type\":\"bucketlib:bucket_filling_shaped\",\"category\":\"misc\",\"pattern\":[\"BS\"],\"key\":{\"B\":" + ingredient + ",\"S\":\"minecraft:stick\"},\"filling_type\":\"fluid\",\"fluid\":\"minecraft:lava\"}";
        for (String json : List.of(shapeless, shaped)) {
            Recipe<?> recipe = TestUtils.noException(helper, "decoding " + json, () -> TestUtils.decode(helper, Recipe.CODEC, TestUtils.json(json)).value());
            assertFilling(helper, recipe, List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET), stick), 2, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA));
        }
        helper.succeed();
    }

    /** Dyeing a dyeable bucket keeps its content and does not leave a second bucket as remainder. */
    public static void dyeingKeepsTheContent(GameTestHelper helper) {
        ItemStack bucket = TestUtils.withFluid(TestBuckets.COLORED_BUCKET, Fluids.WATER);
        CraftingInput input = CraftingInput.of(2, 1, List.of(bucket, new ItemStack(Items.DYE.red())));
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "no dye recipe found for the colored bucket");
        ItemStack result = recipe.get().value().assemble(input);
        helper.assertTrue(result.is(TestBuckets.COLORED_BUCKET.item()) && result.has(DataComponents.DYED_COLOR), "colored bucket was not dyed: " + TestUtils.describe(result));
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(result), Fluids.WATER, "fluid of the dyed bucket");
        var remaining = recipe.get().value().getRemainingItems(input);
        helper.assertTrue(remaining.get(0).isEmpty(), "dyeing leaves a bucket as remainder: " + remaining.get(0));
        helper.succeed();
    }

    /**
     * Two damaged buckets can be combined like tools (remaining durability + 5%). The result is a copy of the bucket
     * without stored max damage (so config changes still apply) and without a second bucket as remainder.
     */
    public static void repairingCombinesDurability(GameTestHelper helper) {
        ItemStack first = TestUtils.bucket(TestBuckets.DURABILITY_BUCKET);
        first.setDamageValue(3);
        ItemStack second = TestUtils.bucket(TestBuckets.DURABILITY_BUCKET);
        second.setDamageValue(3);
        CraftingInput input = CraftingInput.of(2, 1, List.of(first, second));
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "no repair recipe found for two damaged buckets");
        ItemStack result = recipe.get().value().assemble(input);
        int max = 5;
        int expectedDamage = Math.max(max - ((max - 3) + (max - 3) + max * 5 / 100), 0);
        helper.assertTrue(result.is(TestBuckets.DURABILITY_BUCKET.item()), "repair result is " + result);
        TestUtils.assertEquals(helper, result.getDamageValue(), expectedDamage, "damage of the repaired bucket");
        helper.assertFalse(result.getComponentsPatch().split().added().has(DataComponents.MAX_DAMAGE), "repaired bucket stores the max damage");
        var remaining = recipe.get().value().getRemainingItems(input);
        helper.assertTrue(remaining.stream().allMatch(ItemStack::isEmpty), "repairing leaves a remainder: " + remaining);
        // fully repaired buckets equal new buckets
        ItemStack slightly = TestUtils.bucket(TestBuckets.DURABILITY_BUCKET);
        slightly.setDamageValue(1);
        ItemStack slightly2 = TestUtils.bucket(TestBuckets.DURABILITY_BUCKET);
        slightly2.setDamageValue(1);
        ItemStack repaired = recipe.get().value().assemble(CraftingInput.of(2, 1, List.of(slightly, slightly2)));
        helper.assertTrue(repaired.getComponentsPatch().isEmpty(), "fully repaired bucket stores components: " + repaired.getComponentsPatch());
        helper.succeed();
    }

}
