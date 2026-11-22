package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/**
 * Durability: buckets with a durability lose one point each time they are emptied and break when it is used up. The
 * max damage is not stored in the stack, so config changes apply to existing buckets.
 */
public final class DurabilityTests {

    private DurabilityTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("durability/default_bucket_is_unbreakable", DurabilityTests::defaultBucketIsUnbreakable);
        tests.add("durability/durability_is_not_stored", DurabilityTests::durabilityIsNotStored);
        tests.add("durability/config_change_applies_to_existing_buckets", DurabilityTests::configChangeAppliesToExistingBuckets);
        tests.add("durability/emptying_fluid_damages", DurabilityTests::emptyingFluidDamages);
        tests.add("durability/placing_block_damages", DurabilityTests::placingBlockDamages);
        tests.add("durability/releasing_entity_damages", DurabilityTests::releasingEntityDamages);
        tests.add("durability/drinking_milk_damages", DurabilityTests::drinkingMilkDamages);
        tests.add("durability/filling_does_not_damage", DurabilityTests::fillingDoesNotDamage);
        tests.add("durability/breaks_when_used_up", DurabilityTests::breaksWhenUsedUp);
        tests.add("durability/creative_mode_does_not_damage", DurabilityTests::creativeModeDoesNotDamage);
        tests.add("durability/unbreaking_is_supported", DurabilityTests::unbreakingIsSupported);
        tests.add("durability/damage_without_level", DurabilityTests::damageWithoutLevel);
    }

    private static ServerPlayer placing(GameTestHelper helper, GameType gameType, ItemStack stack) {
        ServerPlayer player = TestUtils.player(helper, gameType, stack);
        TestUtils.lookDownAt(helper, player, TestUtils.CENTER.below());
        return player;
    }

    /** Buckets without durability are not damageable and have no max damage, like vanilla buckets. */
    public static void defaultBucketIsUnbreakable(GameTestHelper helper) {
        ItemStack stack = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        helper.assertFalse(stack.isDamageableItem(), "default bucket is damageable");
        helper.assertFalse(stack.has(DataComponents.MAX_DAMAGE), "default bucket has max damage");
        helper.assertTrue(stack.get(DataComponents.MAX_DAMAGE) == null, "default bucket has max damage");
        ServerPlayer player = placing(helper, GameType.SURVIVAL, stack);
        TestUtils.use(helper, player);
        TestUtils.assertEquals(helper, player.getMainHandItem().getDamageValue(), 0, "damage of the default bucket");
        helper.succeed();
    }

    /** The durability is answered from the properties for direct component access, not stored in the stack. */
    public static void durabilityIsNotStored(GameTestHelper helper) {
        ItemStack stack = TestUtils.bucket(TestBuckets.DURABILITY_BUCKET);
        helper.assertTrue(stack.isDamageableItem(), "durability bucket is not damageable");
        TestUtils.assertEquals(helper, stack.getMaxDamage(), 5, "max damage");
        TestUtils.assertEquals(helper, stack.get(DataComponents.MAX_DAMAGE), 5, "max damage component");
        TestUtils.assertEquals(helper, stack.getOrDefault(DataComponents.MAX_DAMAGE, -1), 5, "max damage component with default");
        TestUtils.assertEquals(helper, stack.get(DataComponents.DAMAGE), 0, "damage component");
        helper.assertTrue(stack.has(DataComponents.MAX_DAMAGE) && stack.has(DataComponents.DAMAGE), "durability bucket has no damage components");
        helper.assertTrue(stack.getComponentsPatch().isEmpty(), "durability is stored in the stack: " + stack.getComponentsPatch());
        stack.setDamageValue(2);
        TestUtils.assertEquals(helper, stack.getOrDefault(DataComponents.DAMAGE, -1), 2, "stored damage with default");
        helper.succeed();
    }

    /** Durability config changes apply to existing buckets, 0 makes the bucket unbreakable. */
    public static void configChangeAppliesToExistingBuckets(GameTestHelper helper) {
        ItemStack stack = TestUtils.bucket(TestBuckets.CONFIG_BUCKET);
        try {
            TestUtils.assertEquals(helper, stack.getMaxDamage(), TestBuckets.Config.DEFAULT_DURABILITY, "max damage of the config bucket");
            TestBuckets.Config.durability = 9;
            TestUtils.assertEquals(helper, stack.getMaxDamage(), 9, "max damage after a config change");
            TestBuckets.Config.durability = 0;
            helper.assertFalse(stack.isDamageableItem(), "bucket with durability 0 is damageable");
            helper.assertFalse(stack.has(DataComponents.DAMAGE), "bucket with durability 0 has a damage component");
        } finally {
            TestBuckets.Config.reset();
        }
        helper.succeed();
    }

    public static void emptyingFluidDamages(GameTestHelper helper) {
        ServerPlayer player = placing(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER));
        TestUtils.use(helper, player);
        TestUtils.assertEquals(helper, player.getMainHandItem().getDamageValue(), 1, "damage after emptying water");
        helper.succeed();
    }

    public static void placingBlockDamages(GameTestHelper helper) {
        ServerPlayer player = placing(helper, GameType.SURVIVAL, TestUtils.withBlock(TestBuckets.DURABILITY_BUCKET, Blocks.POWDER_SNOW));
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.POWDER_SNOW, TestUtils.CENTER);
        TestUtils.assertEquals(helper, player.getMainHandItem().getDamageValue(), 1, "damage after placing powder snow");
        helper.succeed();
    }

    /** Releasing an entity with water damages once (not once for the fluid and once for the entity). */
    public static void releasingEntityDamages(GameTestHelper helper) {
        ServerPlayer player = placing(helper, GameType.SURVIVAL, TestUtils.withEntity(TestBuckets.DURABILITY_BUCKET, Fluids.WATER, EntityTypes.SALMON));
        TestUtils.use(helper, player);
        TestUtils.assertEquals(helper, player.getMainHandItem().getDamageValue(), 1, "damage after releasing a salmon with water");
        ServerPlayer player2 = placing(helper, GameType.SURVIVAL, TestUtils.withEntity(TestBuckets.DURABILITY_BUCKET, null, EntityTypes.SULFUR_CUBE));
        TestUtils.use(helper, player2);
        TestUtils.assertEquals(helper, player2.getMainHandItem().getDamageValue(), 1, "damage after releasing a sulfur cube");
        helper.succeed();
    }

    public static void drinkingMilkDamages(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withMilk(TestBuckets.DURABILITY_BUCKET));
        ItemStack result = player.getMainHandItem().finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(BucketLibUtil.isEmpty(result), "bucket is not empty after drinking");
        TestUtils.assertEquals(helper, result.getDamageValue(), 1, "damage after drinking milk");
        helper.succeed();
    }

    public static void fillingDoesNotDamage(GameTestHelper helper) {
        helper.setBlock(TestUtils.CENTER, Blocks.WATER);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.DURABILITY_BUCKET));
        TestUtils.lookDownAt(helper, player, TestUtils.CENTER);
        TestUtils.use(helper, player);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(player.getMainHandItem()), Fluids.WATER, "fluid after filling");
        TestUtils.assertEquals(helper, player.getMainHandItem().getDamageValue(), 0, "damage after filling");
        helper.succeed();
    }

    /** A bucket breaks when its last durability point is used, like a tool. */
    public static void breaksWhenUsedUp(GameTestHelper helper) {
        ItemStack stack = TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER);
        stack.setDamageValue(4);
        ServerPlayer player = placing(helper, GameType.SURVIVAL, stack);
        TestUtils.use(helper, player);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "bucket did not break: " + TestUtils.describe(player.getMainHandItem()));
        TestUtils.assertEquals(helper, player.getStats().getValue(Stats.ITEM_BROKEN.get(TestBuckets.DURABILITY_BUCKET.item())), 1, "broken statistic");
        helper.assertTrue(helper.getBlockState(TestUtils.CENTER).getFluidState().isSource(), "water was not placed with the last durability point");
        helper.succeed();
    }

    public static void creativeModeDoesNotDamage(GameTestHelper helper) {
        ServerPlayer player = placing(helper, GameType.CREATIVE, TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER));
        TestUtils.use(helper, player);
        TestUtils.assertEquals(helper, player.getMainHandItem().getDamageValue(), 0, "damage in creative mode");
        helper.succeed();
    }

    /** Buckets with durability in #minecraft:enchantable/durability accept Unbreaking (documented requirement). */
    public static void unbreakingIsSupported(GameTestHelper helper) {
        var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        helper.assertTrue(de.cech12.bucketlib.gametest.GameTestPlatform.INSTANCE.canEnchant(TestUtils.bucket(TestBuckets.DURABILITY_BUCKET), unbreaking), "durability bucket cannot be enchanted with Unbreaking");
        helper.assertFalse(de.cech12.bucketlib.gametest.GameTestPlatform.INSTANCE.canEnchant(TestUtils.bucket(TestBuckets.TEST_BUCKET), unbreaking), "default bucket can be enchanted with Unbreaking");
        helper.succeed();
    }

    /**
     * Without level (crafting, fluid API of other mods) the bucket is damaged without enchantment effects, and an
     * advancement trigger is fired for a player.
     */
    public static void damageWithoutLevel(GameTestHelper helper) {
        ItemStack stack = TestUtils.bucket(TestBuckets.DURABILITY_BUCKET);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        BucketLibUtil.damageByOne(stack, null, player);
        TestUtils.assertEquals(helper, stack.getDamageValue(), 1, "damage without level");
        stack.setDamageValue(4);
        BucketLibUtil.damageByOne(stack, null, null);
        helper.assertTrue(stack.isEmpty(), "bucket did not break without level");
        ItemStack unbreakable = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        BucketLibUtil.damageByOne(unbreakable, helper.getLevel(), player);
        TestUtils.assertEquals(helper, unbreakable.getDamageValue(), 0, "damage of an unbreakable bucket");
        helper.succeed();
    }

}
