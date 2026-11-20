package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibComponents;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.ItemStackUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.Set;

/**
 * Content of a bucket (fluid, milk, entity, block) via BucketLibUtil, how it is stored and the item name.
 */
public final class ContentTests {

    private ContentTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("content/empty_bucket", ContentTests::emptyBucket);
        tests.add("content/empty_stack_has_no_content", ContentTests::emptyStackHasNoContent);
        tests.add("content/fluid", ContentTests::fluid);
        tests.add("content/fluid_that_cannot_be_held_is_not_added", ContentTests::fluidThatCannotBeHeldIsNotAdded);
        tests.add("content/milk", ContentTests::milk);
        tests.add("content/entity", ContentTests::entity);
        tests.add("content/block", ContentTests::block);
        tests.add("content/storage_format", ContentTests::storageFormat);
        tests.add("content/unknown_content_ids", ContentTests::unknownContentIds);
        tests.add("content/invalid_content_ids", ContentTests::invalidContentIds);
        tests.add("content/item_names", ContentTests::itemNames);
        tests.add("content/item_instances", ContentTests::itemInstances);
        tests.add("content/removing_one_content_keeps_the_other", ContentTests::removingOneContentKeepsTheOther);
        tests.add("content/other_items_are_not_changed", ContentTests::otherItemsAreNotChanged);
    }

    /** An empty bucket contains nothing. */
    public static void emptyBucket(GameTestHelper helper) {
        ItemStack stack = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        helper.assertTrue(BucketLibUtil.isEmpty(stack), "new bucket is not empty");
        helper.assertFalse(BucketLibUtil.containsFluid(stack) || BucketLibUtil.containsMilk(stack) || BucketLibUtil.containsEntityType(stack)
                || BucketLibUtil.containsBlock(stack) || BucketLibUtil.containsContent(stack), "new bucket contains something");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(stack), Fluids.EMPTY, "fluid of a new bucket");
        helper.assertTrue(BucketLibUtil.getEntityType(stack) == null && BucketLibUtil.getBlock(stack) == null && BucketLibUtil.getContent(stack) == null, "content of a new bucket");
        helper.assertTrue(stack.getComponentsPatch().isEmpty(), "new bucket stores components: " + stack.getComponentsPatch());
        helper.succeed();
    }

    /** The content checks of BucketLibUtil can be used for empty stacks (e.g. empty inventory slots). */
    public static void emptyStackHasNoContent(GameTestHelper helper) {
        helper.assertFalse(BucketLibUtil.containsMilk(ItemStack.EMPTY) || BucketLibUtil.containsEntityType(ItemStack.EMPTY) || BucketLibUtil.containsBlock(ItemStack.EMPTY), "empty stack has content");
        KnownIssues.run(helper, "KI-16 BucketLibUtil throws for empty stacks on NeoForge", Set.of(Platforms.NEOFORGE), () -> {
            helper.assertFalse(TestUtils.noException(helper, "containsFluid of an empty stack", () -> BucketLibUtil.containsFluid(ItemStack.EMPTY)), "empty stack contains a fluid");
            helper.assertTrue(TestUtils.noException(helper, "isEmpty of an empty stack", () -> BucketLibUtil.isEmpty(ItemStack.EMPTY)), "empty stack is not empty");
        });
    }

    /** A fluid can be added and removed, the original stack is never changed. */
    public static void fluid(GameTestHelper helper) {
        ItemStack empty = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        ItemStack water = BucketLibUtil.addFluid(empty, Fluids.WATER);
        helper.assertTrue(BucketLibUtil.isEmpty(empty), "addFluid changed the original stack");
        helper.assertTrue(BucketLibUtil.containsFluid(water) && !BucketLibUtil.isEmpty(water), "water was not added");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(water), Fluids.WATER, "fluid");
        TestUtils.assertEquals(helper, Services.FLUID.getContainedFluid(water), Fluids.WATER, "contained fluid of the fluid helper");
        helper.assertFalse(BucketLibUtil.containsMilk(water) || BucketLibUtil.containsBlock(water) || BucketLibUtil.containsEntityType(water), "water bucket contains more than water");
        ItemStack removed = BucketLibUtil.removeFluid(water, helper.getLevel(), null);
        helper.assertTrue(BucketLibUtil.containsFluid(water), "removeFluid changed the original stack");
        helper.assertTrue(BucketLibUtil.isEmpty(removed) && removed.is(TestBuckets.TEST_BUCKET.item()), "water was not removed: " + TestUtils.describe(removed));
        // adding "no fluid" changes nothing
        ItemStack noFluid = BucketLibUtil.addFluid(empty, Fluids.EMPTY);
        helper.assertTrue(noFluid != empty && BucketLibUtil.isEmpty(noFluid), "adding the empty fluid changed the bucket");
        // lava
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(BucketLibUtil.addFluid(empty, Fluids.LAVA)), Fluids.LAVA, "fluid");
        helper.succeed();
    }

    /** A fluid that the bucket cannot hold is not added, the result is an unchanged copy. */
    public static void fluidThatCannotBeHeldIsNotAdded(GameTestHelper helper) {
        ItemStack lava = BucketLibUtil.addFluid(TestUtils.bucket(TestBuckets.TEMPERATURE_BUCKET), Fluids.LAVA);
        helper.assertTrue(BucketLibUtil.isEmpty(lava), "temperature bucket contains lava: " + TestUtils.describe(lava));
        ItemStack water = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        ItemStack waterAndLava = BucketLibUtil.addFluid(water, Fluids.LAVA);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(waterAndLava), Fluids.WATER, "fluid after adding lava to a water bucket");
        helper.succeed();
    }

    /**
     * Milk is stored as content "minecraft:milk" (and on NeoForge with enabled milk fluid also as fluid). Milk is no
     * block.
     */
    public static void milk(GameTestHelper helper) {
        ItemStack milk = TestUtils.withMilk(TestBuckets.TEST_BUCKET);
        helper.assertTrue(BucketLibUtil.containsMilk(milk) && !BucketLibUtil.isEmpty(milk), "milk was not added");
        TestUtils.assertEquals(helper, BucketLibUtil.getContent(milk), BucketLibUtil.MILK_LOCATION, "content of a milk bucket");
        helper.assertFalse(BucketLibUtil.containsBlock(milk), "milk is a block");
        helper.assertTrue(BucketLibUtil.getBlock(milk) == null, "milk bucket has a block");
        TestUtils.assertEquals(helper, BucketLibUtil.containsFluid(milk), Services.FLUID.hasMilkFluid(), "milk bucket contains a fluid");
        if (Services.FLUID.hasMilkFluid()) {
            TestUtils.assertEquals(helper, BucketLibUtil.getFluid(milk), Services.FLUID.getMilkFluid(), "fluid of a milk bucket");
        }
        ItemStack removed = BucketLibUtil.removeMilk(milk, helper.getLevel(), null);
        helper.assertTrue(BucketLibUtil.isEmpty(removed), "milk was not removed: " + TestUtils.describe(removed));
        // removing a block from a milk bucket does not remove the milk
        helper.assertTrue(BucketLibUtil.containsMilk(BucketLibUtil.removeBlock(milk, helper.getLevel(), null, false)), "removeBlock removed the milk");
        helper.succeed();
    }

    /** An entity (with its fluid) can be added and removed. */
    public static void entity(GameTestHelper helper) {
        ItemStack salmon = TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON);
        helper.assertTrue(BucketLibUtil.containsEntityType(salmon) && !BucketLibUtil.isEmpty(salmon), "salmon was not added");
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(salmon), EntityTypes.SALMON, "entity type");
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityTypeString(salmon), "minecraft:salmon", "entity type string");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(salmon), Fluids.WATER, "fluid of a salmon bucket");
        ItemStack water = BucketLibUtil.removeEntityData(salmon, helper.getLevel(), null, false);
        helper.assertTrue(BucketLibUtil.containsEntityType(salmon), "removeEntityData changed the original stack");
        helper.assertFalse(BucketLibUtil.containsEntityType(water), "entity was not removed");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(water), Fluids.WATER, "fluid after removing the entity");
        // without level
        ItemStack waterWithoutLevel = BucketLibUtil.removeEntityData(salmon, null, null, false);
        helper.assertFalse(BucketLibUtil.containsEntityType(waterWithoutLevel), "entity was not removed without level");
        // entity without fluid
        ItemStack sulfurCube = TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE);
        helper.assertTrue(BucketLibUtil.containsEntityType(sulfurCube) && !BucketLibUtil.containsFluid(sulfurCube), "sulfur cube bucket is wrong");
        helper.assertTrue(BucketLibUtil.isEmpty(BucketLibUtil.removeEntityData(sulfurCube, helper.getLevel(), null, false)), "sulfur cube was not removed");
        helper.succeed();
    }

    /** A block (powder snow) can be added and removed. */
    public static void block(GameTestHelper helper) {
        ItemStack powderSnow = TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW);
        helper.assertTrue(BucketLibUtil.containsBlock(powderSnow) && !BucketLibUtil.isEmpty(powderSnow), "powder snow was not added");
        TestUtils.assertEquals(helper, BucketLibUtil.getBlock(powderSnow), Blocks.POWDER_SNOW, "block");
        TestUtils.assertEquals(helper, BucketLibUtil.getContentString(powderSnow), "minecraft:powder_snow", "content string");
        helper.assertFalse(BucketLibUtil.containsMilk(powderSnow) || BucketLibUtil.containsFluid(powderSnow), "powder snow bucket contains milk or a fluid");
        ItemStack removed = BucketLibUtil.removeBlock(powderSnow, helper.getLevel(), null, false);
        helper.assertTrue(BucketLibUtil.isEmpty(removed), "powder snow was not removed");
        helper.assertTrue(removed.getComponentsPatch().isEmpty(), "removed bucket still stores components: " + removed.getComponentsPatch());
        helper.succeed();
    }

    /**
     * Storage format of the contents, which must stay compatible with buckets in existing worlds: the custom data
     * component bucketlib:bucket_content with "BucketContent" (block or milk id) and "EntityType" (entity type id).
     */
    public static void storageFormat(GameTestHelper helper) {
        TestUtils.assertEquals(helper, BucketLib.BUCKET_CONTENT_TAG, "BucketContent", "content tag name");
        TestUtils.assertEquals(helper, BucketLib.ENTITY_TYPE_TAG, "EntityType", "entity type tag name");
        ItemStack stack = TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE);
        CompoundTag tag = stack.getOrDefault(BucketLibComponents.BUCKET_CONTENT, CustomData.EMPTY).copyTag();
        TestUtils.assertEquals(helper, tag.getString("EntityType").orElse(null), "minecraft:sulfur_cube", "stored entity type");
        // a bucket written by an older version (only the custom data) is read correctly
        CompoundTag old = new CompoundTag();
        old.putString("BucketContent", "minecraft:powder_snow");
        ItemStack oldStack = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        oldStack.set(BucketLibComponents.BUCKET_CONTENT, CustomData.of(old));
        TestUtils.assertEquals(helper, BucketLibUtil.getBlock(oldStack), Blocks.POWDER_SNOW, "block of a stored bucket");
        // removing the last content removes the whole component
        ItemStack removed = BucketLibUtil.removeBlock(oldStack, null, null, false);
        helper.assertFalse(removed.has(BucketLibComponents.BUCKET_CONTENT), "empty bucket content component is not removed");
        helper.succeed();
    }

    /** Contents with ids that are not registered (e.g. of removed mods) are handled as missing, not as crash. */
    public static void unknownContentIds(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString("BucketContent", "bucketlibtest:not_existing_block");
        tag.putString("EntityType", "bucketlibtest:not_existing_entity");
        ItemStack stack = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        stack.set(BucketLibComponents.BUCKET_CONTENT, CustomData.of(tag));
        helper.assertTrue(BucketLibUtil.getEntityType(stack) == null, "unknown entity type is found");
        helper.assertTrue(BucketLibUtil.getBlock(stack) == null, "unknown block is found");
        Component name = TestUtils.noException(helper, "name of a bucket with unknown content", () -> stack.getHoverName());
        helper.assertTrue(name.getString().contains("?") || name.getContents() instanceof TranslatableContents, "name of a bucket with an unknown entity: " + name);
        helper.succeed();
    }

    /** Contents with invalid ids (e.g. written by commands) do not crash the game, they are handled as missing. */
    public static void invalidContentIds(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString("BucketContent", "Not A Valid Id!");
        ItemStack stack = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        stack.set(BucketLibComponents.BUCKET_CONTENT, CustomData.of(tag));
        KnownIssues.run(helper, "KI-2 invalid content ids crash", Platforms.ALL, () -> {
            helper.assertTrue(TestUtils.noException(helper, "reading the block of invalid content", () -> BucketLibUtil.getBlock(stack)) == null, "invalid block id is found");
            TestUtils.noException(helper, "name of a bucket with invalid content", () -> stack.getHoverName());
        });
    }

    private static void assertName(GameTestHelper helper, ItemStack stack, String expectedKey, Object expectedArgument) {
        Component name = stack.getHoverName();
        helper.assertTrue(name.getContents() instanceof TranslatableContents, "name of " + TestUtils.describe(stack) + " is not translatable: " + name);
        TranslatableContents contents = (TranslatableContents) name.getContents();
        TestUtils.assertEquals(helper, contents.getKey(), expectedKey, "translation key of " + TestUtils.describe(stack));
        if (expectedArgument == null) {
            TestUtils.assertEquals(helper, contents.getArgs().length, 0, "translation arguments of " + TestUtils.describe(stack));
        } else {
            TestUtils.assertEquals(helper, contents.getArgs().length, 1, "translation arguments of " + TestUtils.describe(stack));
            TestUtils.assertEquals(helper, contents.getArgs()[0], expectedArgument, "translation argument of " + TestUtils.describe(stack));
        }
    }

    /**
     * The item name depends on the content: "item.mod.bucket" (empty), "...filled" with the fluid, block or milk
     * name, "...entity" with the entity name.
     */
    public static void itemNames(GameTestHelper helper) {
        String key = TestBuckets.TEST_BUCKET.item().getDescriptionId();
        TestUtils.assertEquals(helper, key, "item.bucketlibtest.test_bucket", "description id");
        assertName(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET), key, null);
        assertName(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), key + UniversalBucketItem.DESCRIPTION_FILLED_SUFFIX, Services.FLUID.getFluidDescription(Fluids.WATER));
        assertName(helper, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW), key + UniversalBucketItem.DESCRIPTION_FILLED_SUFFIX, Blocks.POWDER_SNOW.getName());
        assertName(helper, TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON), key + UniversalBucketItem.DESCRIPTION_ENTITY_SUFFIX, EntityTypes.SALMON.getDescription());
        if (Services.FLUID.hasMilkFluid()) {
            assertName(helper, TestUtils.withMilk(TestBuckets.TEST_BUCKET), key + UniversalBucketItem.DESCRIPTION_FILLED_SUFFIX, Services.FLUID.getFluidDescription(Services.FLUID.getMilkFluid()));
        } else {
            assertName(helper, TestUtils.withMilk(TestBuckets.TEST_BUCKET), key + UniversalBucketItem.DESCRIPTION_FILLED_SUFFIX, Component.translatable(Services.PLATFORM.getMilkTranslationKey()));
        }
        // a custom name (anvil) wins
        ItemStack named = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("My Bucket"));
        TestUtils.assertEquals(helper, named.getHoverName().getString(), "My Bucket", "custom name");
        helper.succeed();
    }

    /** The content tags are independent: removing the entity of a bucket that also has a block keeps the block. */
    public static void removingOneContentKeepsTheOther(GameTestHelper helper) {
        ItemStack both = BucketLibUtil.addEntityType(TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW), EntityTypes.SULFUR_CUBE);
        ItemStack blockOnly = BucketLibUtil.removeEntityData(both, helper.getLevel(), null, false);
        TestUtils.assertEquals(helper, BucketLibUtil.getBlock(blockOnly), Blocks.POWDER_SNOW, "block after removing the entity");
        helper.assertFalse(BucketLibUtil.containsEntityType(blockOnly), "entity was not removed");
        ItemStack entityOnly = BucketLibUtil.removeBlock(both, helper.getLevel(), null, false);
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(entityOnly), EntityTypes.SULFUR_CUBE, "entity after removing the block");
        helper.assertFalse(BucketLibUtil.containsBlock(entityOnly), "block was not removed");
        helper.succeed();
    }

    /** The fluid helper of the loader does not change items that are no BucketLib buckets. */
    public static void otherItemsAreNotChanged(GameTestHelper helper) {
        ItemStack stone = new ItemStack(net.minecraft.world.item.Items.STONE);
        TestUtils.assertEquals(helper, Services.FLUID.getContainedFluid(stone), Fluids.EMPTY, "fluid of stone");
        helper.assertTrue(ItemStack.isSameItemSameComponents(Services.FLUID.addFluid(stone, Fluids.WATER), stone), "adding water changed stone");
        helper.assertTrue(ItemStack.isSameItemSameComponents(Services.FLUID.removeFluid(stone, helper.getLevel(), null), stone), "removing a fluid changed stone");
        helper.succeed();
    }

    /**
     * BucketLibUtil works with item stacks and item stack templates (ItemInstance), e.g. for crafting remainders.
     */
    public static void itemInstances(GameTestHelper helper) {
        ItemStack water = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        ItemStackTemplate template = ItemStackTemplate.fromNonEmptyStack(water);
        TestUtils.assertEquals(helper, BucketLibUtil.getItem(template), TestBuckets.TEST_BUCKET.item(), "item of a template");
        TestUtils.assertEquals(helper, BucketLibUtil.getItem(water), TestBuckets.TEST_BUCKET.item(), "item of a stack");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(template), Fluids.WATER, "fluid of a template");
        helper.assertTrue(BucketLibUtil.getItemStackTemplate(template) == template, "template of a template is not the same");
        helper.assertTrue(ItemStack.isSameItemSameComponents(BucketLibUtil.getItemStack(template), water), "stack of a template");
        helper.assertTrue(BucketLibUtil.getItemStack(water) == water, "stack of a stack is not the same");
        helper.assertTrue(ItemStack.isSameItemSameComponents(BucketLibUtil.getItemStackTemplate(water).create(), water), "template of a stack");
        helper.assertFalse(BucketLibUtil.isEmpty(template), "water template is empty");
        // ItemStackUtil
        helper.assertTrue(new ItemStackUtil() != null, "ItemStackUtil cannot be created");
        helper.assertTrue(ItemStackUtil.copyStackWithSize(water, 0).isEmpty(), "copy with size 0 is not empty");
        ItemStack copy = ItemStackUtil.copyStackWithSize(TestUtils.bucket(TestBuckets.TEST_BUCKET), 5);
        TestUtils.assertEquals(helper, copy.getCount(), 5, "count of a copy");
        helper.succeed();
    }

}
