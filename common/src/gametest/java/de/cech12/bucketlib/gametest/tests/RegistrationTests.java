package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibComponents;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.GameTestPlatform;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.item.crafting.BucketFillingShapedRecipe;
import de.cech12.bucketlib.item.crafting.BucketFillingShapelessRecipe;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Registration of BucketLib (components, recipe serializers, ingredient types, buckets via BucketLibApi) and the
 * registry helpers (RegistryUtil).
 */
public final class RegistrationTests {

    private RegistrationTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("registration/data_components_are_registered", RegistrationTests::dataComponentsAreRegistered);
        tests.add("registration/recipe_serializers_are_registered", RegistrationTests::recipeSerializersAreRegistered);
        tests.add("registration/ingredient_types_are_registered", RegistrationTests::ingredientTypesAreRegistered);
        tests.add("registration/buckets_have_a_fluid_storage", RegistrationTests::bucketsHaveAFluidStorage);
        tests.add("registration/api_ignores_invalid_buckets", RegistrationTests::apiIgnoresInvalidBuckets);
        tests.add("registration/tags_are_defined", RegistrationTests::tagsAreDefined);
        tests.add("registration/vanilla_bucket_blocks_and_entities_are_found", RegistrationTests::vanillaBucketBlocksAndEntitiesAreFound);
        tests.add("registration/registry_and_fluid_helpers", RegistrationTests::registryAndFluidHelpers);
        tests.add("registration/buckets_are_in_their_creative_tab", RegistrationTests::bucketsAreInTheirCreativeTab);
        tests.add("registration/creative_tab_only_lists_possible_contents", RegistrationTests::creativeTabOnlyListsPossibleContents);
    }

    /** The registry and fluid helpers of the loader (also used by mods that use BucketLib) answer like the registries. */
    public static void registryAndFluidHelpers(GameTestHelper helper) {
        var registry = Services.REGISTRY;
        TestUtils.assertEquals(helper, registry.getItemLocation(Items.WATER_BUCKET), Identifier.withDefaultNamespace("water_bucket"), "item id");
        TestUtils.assertEquals(helper, registry.getFluid(Identifier.withDefaultNamespace("lava")), Fluids.LAVA, "fluid of an id");
        TestUtils.assertEquals(helper, registry.getFluid(TestBuckets.id("not_existing")), null, "fluid of an unknown id");
        TestUtils.assertEquals(helper, registry.getFluidLocation(Fluids.WATER), Identifier.withDefaultNamespace("water"), "fluid id");
        TestUtils.assertEquals(helper, registry.getEntityType(Identifier.withDefaultNamespace("salmon")), EntityTypes.SALMON, "entity type of an id");
        TestUtils.assertEquals(helper, registry.getBlock(Identifier.withDefaultNamespace("powder_snow")), Blocks.POWDER_SNOW, "block of an id");
        boolean waterFound = false;
        for (Fluid fluid : registry.getAllFluids()) {
            waterFound |= fluid == Fluids.WATER;
        }
        helper.assertTrue(waterFound, "water is not in all fluids");
        TestUtils.assertEquals(helper, Services.FLUID.getFluidLightLevel(Fluids.LAVA), 15, "light level of lava");
        TestUtils.assertEquals(helper, Services.FLUID.getFluidLightLevel(Fluids.WATER), 0, "light level of water");
        helper.assertFalse(Services.FLUID.isFluidLighterThanAir(Fluids.WATER), "water is lighter than air");
        helper.assertTrue(de.cech12.bucketlib.platform.services.IConfigHelper.INFINITY_ENCHANTMENT_ENABLED_DESCRIPTION.contains(BucketLibTags.Fluids.INFINITY_ENCHANTABLE.location().toString()), "config description does not name the tag");
        helper.succeed();
    }

    /** The data components bucketlib:bucket_content and bucketlib:fluid (fluid storage of the loader) exist. */
    public static void dataComponentsAreRegistered(GameTestHelper helper) {
        TestUtils.assertEquals(helper, BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(BucketLibComponents.BUCKET_CONTENT_LOCATION), BucketLibComponents.BUCKET_CONTENT, "registered bucket content component");
        helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(BucketLibComponents.FLUID_LOCATION), "fluid component " + BucketLibComponents.FLUID_LOCATION + " is not registered");
        TestUtils.assertEquals(helper, BucketLibComponents.BUCKET_CONTENT_LOCATION, BucketLib.id("bucket_content"), "bucket content component id");
        helper.succeed();
    }

    /** The recipe serializers bucketlib:bucket_filling_shaped and bucketlib:bucket_filling_shapeless exist. */
    public static void recipeSerializersAreRegistered(GameTestHelper helper) {
        TestUtils.assertEquals(helper, BuiltInRegistries.RECIPE_SERIALIZER.getValue(BucketLib.id("bucket_filling_shaped")), BucketFillingShapedRecipe.SERIALIZER, "shaped bucket filling serializer");
        TestUtils.assertEquals(helper, BuiltInRegistries.RECIPE_SERIALIZER.getValue(BucketLib.id("bucket_filling_shapeless")), BucketFillingShapelessRecipe.SERIALIZER, "shapeless bucket filling serializer");
        helper.succeed();
    }

    /** All custom ingredient types (block, empty, entity, fluid, milk) can be used in data packs. */
    public static void ingredientTypesAreRegistered(GameTestHelper helper) {
        Map<String, Map<String, String>> ingredients = Map.of(
                "block", Map.of("block", "minecraft:powder_snow"),
                "empty", Map.of(),
                "entity", Map.of("entity", "minecraft:salmon"),
                "fluid", Map.of("fluid", "minecraft:water"),
                "milk", Map.of());
        ingredients.forEach((type, fields) -> TestUtils.noException(helper, "decoding ingredient bucketlib:" + type, () -> TestUtils.ingredient(helper, type, fields)));
        helper.succeed();
    }

    /** Every bucket registered with BucketLibApi.registerBucket has a fluid storage (Fabric) / capability (NeoForge). */
    public static void bucketsHaveAFluidStorage(GameTestHelper helper) {
        for (TestBuckets.Definition bucket : TestBuckets.ALL) {
            helper.assertTrue(GameTestPlatform.INSTANCE.hasFluidStorage(TestUtils.bucket(bucket)), bucket.id() + " has no fluid storage");
        }
        helper.assertFalse(GameTestPlatform.INSTANCE.hasFluidStorage(new ItemStack(Items.STONE)), "stone has a fluid storage");
        helper.succeed();
    }

    /**
     * BucketLibApi.registerBucket ignores (and logs) ids of unknown items and of items that are no UniversalBucketItem
     * (the adapters try both), so only the test buckets are registered.
     */
    public static void apiIgnoresInvalidBuckets(GameTestHelper helper) {
        List<UniversalBucketItem> registered = Services.REGISTRY.getRegisteredBuckets();
        TestUtils.assertEquals(helper, registered.size(), TestBuckets.ALL.size(), "number of registered buckets");
        helper.assertFalse(BuiltInRegistries.ITEM.containsKey(TestBuckets.UNKNOWN_BUCKET_ID), "test setup broken: unknown bucket exists");
        helper.assertTrue(registered.stream().noneMatch(bucket -> BuiltInRegistries.ITEM.getKey(bucket).equals(Identifier.withDefaultNamespace("stone"))), "stone is registered as bucket");
        boolean unmodifiable = false;
        try {
            registered.add(TestBuckets.TEST_BUCKET.item());
        } catch (UnsupportedOperationException e) {
            unmodifiable = true;
        }
        helper.assertTrue(unmodifiable, "the list of registered buckets can be modified");
        helper.succeed();
    }

    /** The tags of BucketLib have the documented ids and default contents. */
    public static void tagsAreDefined(GameTestHelper helper) {
        BucketLibTags.init();
        TestUtils.assertEquals(helper, BucketLibTags.EntityTypes.MILKABLE.location(), BucketLib.id("milkable"), "milkable tag");
        TestUtils.assertEquals(helper, BucketLibTags.Fluids.INFINITY_ENCHANTABLE.location(), BucketLib.id("infinity_enchantable"), "infinity enchantable tag");
        TestUtils.assertEquals(helper, BucketLibTags.Fluids.NO_FLIPPING.location(), BucketLib.id("no_flipping"), "no flipping tag");
        for (EntityType<?> type : List.of(EntityTypes.COW, EntityTypes.GOAT, EntityTypes.MOOSHROOM)) {
            helper.assertTrue(type.builtInRegistryHolder().is(BucketLibTags.EntityTypes.MILKABLE), type + " is not milkable");
        }
        helper.assertFalse(EntityTypes.PIG.builtInRegistryHolder().is(BucketLibTags.EntityTypes.MILKABLE), "pig is milkable");
        helper.assertTrue(Fluids.WATER.defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE), "water is not infinity enchantable");
        helper.assertFalse(Fluids.LAVA.defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE), "lava is infinity enchantable");
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.FLUID).get(BucketLibTags.Fluids.NO_FLIPPING).isPresent(), "no flipping tag does not exist");
        helper.succeed();
    }

    /**
     * RegistryUtil finds every vanilla bucket that holds a block (powder snow) or an entity (fish, axolotl, tadpole,
     * sulfur cube) together with its fluid. These lists are the base of picking up blocks and entities and of the
     * creative tab entries.
     */
    public static void vanillaBucketBlocksAndEntitiesAreFound(GameTestHelper helper) {
        var powderSnow = RegistryUtil.getBucketBlock(Blocks.POWDER_SNOW);
        helper.assertTrue(powderSnow != null && powderSnow.bucketItem() == Items.POWDER_SNOW_BUCKET, "powder snow bucket not found: " + powderSnow);
        helper.assertTrue(RegistryUtil.getBucketBlock(Blocks.STONE) == null, "stone is a bucket block");
        Map<EntityType<?>, Fluid> expected = Map.of(
                EntityTypes.PUFFERFISH, Fluids.WATER, EntityTypes.SALMON, Fluids.WATER, EntityTypes.COD, Fluids.WATER,
                EntityTypes.TROPICAL_FISH, Fluids.WATER, EntityTypes.AXOLOTL, Fluids.WATER, EntityTypes.TADPOLE, Fluids.WATER,
                EntityTypes.SULFUR_CUBE, Fluids.EMPTY);
        expected.forEach((type, fluid) -> {
            var bucketEntity = RegistryUtil.getBucketEntity(type);
            helper.assertTrue(bucketEntity != null, "bucket of " + type + " not found");
            TestUtils.assertEquals(helper, bucketEntity.fluid(), fluid, "fluid of the bucket of " + type);
        });
        helper.assertTrue(RegistryUtil.getBucketEntity(EntityTypes.PIG) == null, "pig is a bucket entity");
        long vanillaEntities = RegistryUtil.getBucketEntities().stream().filter(e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.entityType()).getNamespace().equals("minecraft")).count();
        TestUtils.assertEquals(helper, (int) vanillaEntities, expected.size(), "number of vanilla bucket entities " + RegistryUtil.getBucketEntities());
        helper.succeed();
    }

    private static Collection<ItemStack> tabItems(GameTestHelper helper, net.minecraft.resources.ResourceKey<CreativeModeTab> tab) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tab).getDisplayItems();
    }

    private static boolean containsSame(Collection<ItemStack> items, ItemStack stack) {
        return items.stream().anyMatch(item -> ItemStack.isSameItemSameComponents(item, stack));
    }

    /**
     * Every bucket is listed in its creative tab (default "Tools & Utilities", or the tab of the properties) with all
     * contents it can hold: empty, every fluid, milk, every entity and every block.
     */
    public static void bucketsAreInTheirCreativeTab(GameTestHelper helper) {
        if (TestUtils.creativeTabsBrokenByKnownIssue19()) {
            TestUtils.assertCreativeTabsBrokenByKnownIssue19(helper);
            return;
        }
        Collection<ItemStack> tools = tabItems(helper, Services.PLATFORM.getToolsAndUtilitiesTab());
        Collection<ItemStack> ingredients = tabItems(helper, TestBuckets.INGREDIENTS_TAB);
        TestUtils.assertEquals(helper, TestBuckets.TEST_BUCKET.item().getCreativeTab(), Services.PLATFORM.getToolsAndUtilitiesTab(), "default creative tab");
        TestUtils.assertEquals(helper, TestBuckets.TAB_BUCKET.item().getCreativeTab(), TestBuckets.INGREDIENTS_TAB, "creative tab of the properties");
        List<ItemStack> expected = new ArrayList<>(List.of(
                TestUtils.bucket(TestBuckets.TEST_BUCKET),
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER),
                TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA),
                TestUtils.withMilk(TestBuckets.TEST_BUCKET),
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON),
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE),
                TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW)));
        for (ItemStack stack : expected) {
            helper.assertTrue(containsSame(tools, stack), "Tools & Utilities does not contain " + TestUtils.describe(stack));
        }
        helper.assertTrue(containsSame(ingredients, TestUtils.bucket(TestBuckets.TAB_BUCKET)), "Ingredients does not contain the tab bucket");
        helper.assertFalse(containsSame(tools, TestUtils.bucket(TestBuckets.TAB_BUCKET)), "Tools & Utilities contains the tab bucket");
        // the temperature bucket cannot hold lava, so there is no lava variant
        helper.assertFalse(tools.stream().anyMatch(stack -> stack.is(TestBuckets.TEMPERATURE_BUCKET.item()) && BucketLibUtil.getFluid(stack) == Fluids.LAVA), "Tools & Utilities contains a lava temperature bucket");
        helper.assertFalse(tools.stream().anyMatch(stack -> stack.is(TestBuckets.NO_ENTITIES_BUCKET.item()) && BucketLibUtil.containsEntityType(stack)), "Tools & Utilities contains a no entities bucket with an entity");
        helper.assertFalse(tools.stream().anyMatch(stack -> stack.is(TestBuckets.NO_BLOCKS_BUCKET.item()) && BucketLibUtil.containsBlock(stack)), "Tools & Utilities contains a no blocks bucket with a block");
        helper.succeed();
    }

    /** Buckets that cannot be milked do not show a milk bucket in the creative tab (it cannot be filled in survival). */
    public static void creativeTabOnlyListsPossibleContents(GameTestHelper helper) {
        if (TestUtils.creativeTabsBrokenByKnownIssue19()) {
            TestUtils.assertCreativeTabsBrokenByKnownIssue19(helper);
            return;
        }
        Collection<ItemStack> tools = tabItems(helper, Services.PLATFORM.getToolsAndUtilitiesTab());
        helper.assertTrue(containsSame(tools, TestUtils.bucket(TestBuckets.ANTI_MILK_BUCKET)), "Tools & Utilities does not contain the anti milk bucket");
        KnownIssues.run(helper, "KI-1 creative tab lists milk for buckets that cannot be milked", Platforms.ALL, () ->
                helper.assertFalse(tools.stream().anyMatch(stack -> stack.is(TestBuckets.ANTI_MILK_BUCKET.item()) && BucketLibUtil.containsMilk(stack)),
                        "Tools & Utilities contains an anti milk bucket with milk"));
    }

}
