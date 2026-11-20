package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.GameTestPlatform;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.platform.services.IConfigHelper;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;


/**
 * Infinity enchantment (config option "infinity enchantment enabled", default false): buckets filled with a fluid of
 * #bucketlib:infinity_enchantable (water) can be enchanted with Infinity and are never emptied.
 * <p>
 * The tests "infinity/enabled/*" run in the test environment "bucketlib:infinity_enabled".
 */
public final class InfinityTests {

    private InfinityTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("infinity/disabled_by_default", InfinityTests::disabledByDefault);
        tests.add("infinity/enabled/water_buckets_are_enchantable", InfinityTests::waterBucketsAreEnchantable);
        tests.add("infinity/enabled/water_is_infinite", InfinityTests::waterIsInfinite);
        tests.add("infinity/enabled/water_is_infinite_for_other_mods", InfinityTests::waterIsInfiniteForOtherMods);
        tests.add("infinity/enabled/no_durability_loss", InfinityTests::noDurabilityLoss);
        tests.add("infinity/enabled/crafting_keeps_the_water", InfinityTests::craftingKeepsTheWater);
        tests.add("infinity/enabled/prevents_cracking", InfinityTests::preventsCracking);
        tests.add("infinity/enabled/entity_buckets_are_not_enchantable", InfinityTests::entityBucketsAreNotEnchantable);
    }

    private static Holder<Enchantment> infinity(GameTestHelper helper) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack) {
        stack.enchant(infinity(helper), 1);
        return stack;
    }

    /** The option is disabled by default: water buckets cannot get Infinity, an Infinity enchantment has no effect. */
    public static void disabledByDefault(GameTestHelper helper) {
        helper.assertFalse(IConfigHelper.INFINITY_ENCHANTMENT_ENABLED_DEFAULT, "documented default");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.defaultInfinityEnchantmentEnabled(), IConfigHelper.INFINITY_ENCHANTMENT_ENABLED_DEFAULT, "default of the config definition");
        helper.assertFalse(Services.CONFIG.isInfinityEnchantmentEnabled(), "infinity enchantment is enabled");
        ItemStack water = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        helper.assertFalse(GameTestPlatform.INSTANCE.canEnchant(water, infinity(helper)), "water bucket can be enchanted with Infinity");
        helper.assertFalse(BucketLibUtil.isInfinityEnchantmentAllowed(water), "infinity is allowed");
        ItemStack enchanted = enchanted(helper, water.copy());
        helper.assertTrue(BucketLibUtil.hasInfinityEnchantment(enchanted), "test setup broken: no Infinity");
        helper.assertFalse(BucketLibUtil.isAffectedByInfinityEnchantment(enchanted), "Infinity has an effect with disabled option");
        helper.succeed();
    }

    /** Only buckets with water (infinity enchantable fluid) that can hold water can be enchanted with Infinity. */
    public static void waterBucketsAreEnchantable(GameTestHelper helper) {
        helper.assertTrue(Services.CONFIG.isInfinityEnchantmentEnabled(), "test setup broken: infinity enchantment is disabled");
        Holder<Enchantment> enchantment = infinity(helper);
        helper.assertTrue(GameTestPlatform.INSTANCE.canEnchant(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), enchantment), "water bucket cannot be enchanted with Infinity");
        helper.assertFalse(GameTestPlatform.INSTANCE.canEnchant(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), enchantment), "lava bucket can be enchanted with Infinity");
        helper.assertFalse(GameTestPlatform.INSTANCE.canEnchant(TestUtils.bucket(TestBuckets.TEST_BUCKET), enchantment), "empty bucket can be enchanted with Infinity");
        helper.assertFalse(GameTestPlatform.INSTANCE.canEnchant(TestUtils.withMilk(TestBuckets.TEST_BUCKET), enchantment), "milk bucket can be enchanted with Infinity");
        helper.assertFalse(GameTestPlatform.INSTANCE.canEnchant(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET), enchantment), "vanilla water bucket can be enchanted with Infinity");
        helper.assertTrue(BucketLibUtil.isInfinityEnchantmentAllowed(TestUtils.withFluid(TestBuckets.WATER_DENYING_TAG_BUCKET, Fluids.WATER)) == false, "infinity is allowed for a bucket that cannot hold water");
        // other enchantments are not affected
        var mending = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        helper.assertFalse(GameTestPlatform.INSTANCE.canEnchant(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), mending), "water bucket can be enchanted with Mending");
        helper.succeed();
    }

    /** A water bucket with Infinity places water and stays filled. */
    public static void waterIsInfinite(GameTestHelper helper) {
        ItemStack stack = enchanted(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        helper.assertTrue(BucketLibUtil.isAffectedByInfinityEnchantment(stack), "bucket is not affected by Infinity");
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack);
        TestUtils.lookDownAt(helper, player, TestUtils.CENTER.below());
        TestUtils.use(helper, player);
        helper.assertTrue(helper.getBlockState(TestUtils.CENTER).getFluidState().isSource(), "no water was placed");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(player.getMainHandItem()), Fluids.WATER, "fluid after placing with Infinity");
        helper.succeed();
    }

    /** Other mods can drain an Infinity water bucket endlessly through the fluid API. */
    public static void waterIsInfiniteForOtherMods(GameTestHelper helper) {
        ItemStack stack = enchanted(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        var transfer = GameTestPlatform.INSTANCE.extractFluid(stack, Fluids.WATER, 1000);
        TestUtils.assertEquals(helper, transfer.millibuckets(), 1000, "extracted water");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(transfer.result()), Fluids.WATER, "fluid after extracting with Infinity");
        helper.succeed();
    }

    public static void noDurabilityLoss(GameTestHelper helper) {
        ItemStack stack = enchanted(helper, TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER));
        BucketLibUtil.damageByOne(stack, helper.getLevel(), null);
        BucketLibUtil.damageByOne(stack, null, null);
        TestUtils.assertEquals(helper, stack.getDamageValue(), 0, "damage with Infinity");
        helper.succeed();
    }

    /** Crafting with an Infinity water bucket keeps the filled bucket as remainder. */
    public static void craftingKeepsTheWater(GameTestHelper helper) {
        ItemStack stack = enchanted(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        ItemStackTemplate remainder = TestBuckets.TEST_BUCKET.item().getCraftingRemainder(stack);
        helper.assertTrue(remainder != null, "Infinity water bucket has no crafting remainder");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(remainder.create()), Fluids.WATER, "fluid of the crafting remainder");
        helper.succeed();
    }

    /**
     * An Infinity water bucket never cracks, also with water as cracking fluid or a lower cracking temperature, so it
     * stays as crafting remainder.
     */
    public static void preventsCracking(GameTestHelper helper) {
        ItemStack lower = enchanted(helper, TestUtils.withFluid(TestBuckets.LOWER_CRACKING_BUCKET, Fluids.WATER));
        helper.assertFalse(TestBuckets.LOWER_CRACKING_BUCKET.item().isCracked(lower), "Infinity bucket with lower cracking temperature is cracked");
        ItemStack fluids = enchanted(helper, TestUtils.withFluid(TestBuckets.CRACKING_FLUIDS_BUCKET, Fluids.WATER));
        helper.assertTrue(BucketLibUtil.isAffectedByInfinityEnchantment(fluids), "test setup broken: not affected by Infinity");
        KnownIssues.run(helper, "KI-6 Infinity does not prevent cracking by cracking fluids and upper cracking temperature", Platforms.ALL, () -> {
            helper.assertFalse(TestBuckets.CRACKING_FLUIDS_BUCKET.item().isCracked(fluids), "Infinity bucket with water as cracking fluid is cracked");
            helper.assertTrue(TestBuckets.CRACKING_FLUIDS_BUCKET.item().getCraftingRemainder(fluids) != null, "Infinity bucket with water as cracking fluid has no crafting remainder");
        });
    }

    /** A bucket with water and an entity is no infinite water bucket, it cannot be enchanted with Infinity. */
    public static void entityBucketsAreNotEnchantable(GameTestHelper helper) {
        var infinity = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
        ItemStack salmon = TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON);
        helper.assertTrue(de.cech12.bucketlib.gametest.GameTestPlatform.INSTANCE.canEnchant(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), infinity), "test setup broken: water bucket cannot get Infinity");
        KnownIssues.run(helper, "KI-23 buckets with water and an entity can be enchanted with Infinity", Platforms.ALL, () ->
                helper.assertFalse(de.cech12.bucketlib.gametest.GameTestPlatform.INSTANCE.canEnchant(salmon, infinity), "bucket with water and salmon can be enchanted with Infinity"));
    }

}
