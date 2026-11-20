package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.nautilus.NautilusAi;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Map;

/**
 * Picking up and releasing entities (fish, axolotl, tadpole, sulfur cube) and feeding entities with bucket entities,
 * like vanilla mob buckets.
 */
public final class EntityTests {

    private static final BlockPos TARGET = TestUtils.CENTER;

    /** vanilla mob bucket of each vanilla bucket entity */
    private static final Map<EntityType<?>, Item> VANILLA_BUCKETS = Map.of(
            EntityTypes.PUFFERFISH, Items.PUFFERFISH_BUCKET, EntityTypes.SALMON, Items.SALMON_BUCKET,
            EntityTypes.COD, Items.COD_BUCKET, EntityTypes.TROPICAL_FISH, Items.TROPICAL_FISH_BUCKET,
            EntityTypes.AXOLOTL, Items.AXOLOTL_BUCKET, EntityTypes.TADPOLE, Items.TADPOLE_BUCKET,
            EntityTypes.SULFUR_CUBE, Items.SULFUR_CUBE_BUCKET);

    private EntityTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("entity/picks_up_salmon", EntityTests::picksUpSalmon);
        tests.add("entity/picks_up_every_vanilla_bucket_entity", EntityTests::picksUpEveryVanillaBucketEntity);
        tests.add("entity/does_not_pick_up_fish_without_water", EntityTests::doesNotPickUpFishWithoutWater);
        tests.add("entity/does_not_pick_up_denied_entities", EntityTests::doesNotPickUpDeniedEntities);
        tests.add("entity/does_not_pick_up_a_second_entity", EntityTests::doesNotPickUpASecondEntity);
        tests.add("entity/releases_salmon_with_water", EntityTests::releasesSalmonWithWater);
        tests.add("entity/releases_sulfur_cube", EntityTests::releasesSulfurCube);
        tests.add("entity/keeps_entity_data", EntityTests::keepsEntityData);
        tests.add("entity/creative_mode_keeps_the_bucket", EntityTests::creativeModeKeepsTheBucket);
        tests.add("entity/spawning_without_entity_does_nothing", EntityTests::spawningWithoutEntityDoesNothing);
        tests.add("entity/feeds_axolotl", EntityTests::feedsAxolotl);
        tests.add("entity/axolotl_food_like_vanilla_buckets", EntityTests::axolotlFoodLikeVanillaBuckets);
        tests.add("entity/nautilus_food_like_vanilla_buckets", EntityTests::nautilusFoodLikeVanillaBuckets);
        tests.add("entity/tames_nautilus", EntityTests::tamesNautilus);
    }

    private static InteractionResult interact(ServerPlayer player, Entity entity) {
        return TestUtils.interact(player, entity);
    }

    /** A water bucket picks up a salmon, the salmon is removed and the bucket contains water and salmon. */
    public static void picksUpSalmon(GameTestHelper helper) {
        Salmon salmon = helper.spawnWithNoFreeWill(EntityTypes.SALMON, TARGET);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        InteractionResult result = interact(player, salmon);
        helper.assertTrue(result.consumesAction(), "picking up a salmon did not consume the action: " + result);
        ItemStack hand = player.getMainHandItem();
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(hand), EntityTypes.SALMON, "entity in the bucket");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(hand), Fluids.WATER, "fluid in the bucket");
        helper.assertTrue(salmon.isRemoved(), "salmon was not removed");
        helper.succeed();
    }

    /** Every vanilla bucket entity can be picked up with the fluid of its vanilla bucket (sulfur cubes without fluid). */
    public static void picksUpEveryVanillaBucketEntity(GameTestHelper helper) {
        VANILLA_BUCKETS.keySet().forEach(type -> {
            Entity entity = helper.spawnWithNoFreeWill((EntityType<? extends net.minecraft.world.entity.Mob>) type, TARGET);
            if (entity instanceof SulfurCube sulfurCube) {
                sulfurCube.setSize(2, true);
                sulfurCube.setBaby(false);
            }
            var fluid = type == EntityTypes.SULFUR_CUBE ? Fluids.EMPTY : Fluids.WATER;
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, fluid));
            interact(player, entity);
            TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(player.getMainHandItem()), type, "entity in the bucket after picking up " + type);
            helper.assertTrue(entity.isRemoved(), type + " was not removed");
        });
        helper.succeed();
    }

    /** Like vanilla, fish can only be picked up with a water bucket. An empty bucket does nothing (also no milking). */
    public static void doesNotPickUpFishWithoutWater(GameTestHelper helper) {
        Salmon salmon = helper.spawnWithNoFreeWill(EntityTypes.SALMON, TARGET);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        interact(player, salmon);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "empty bucket picked up a salmon");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA));
        interact(player, salmon);
        helper.assertFalse(BucketLibUtil.containsEntityType(player.getMainHandItem()), "lava bucket picked up a salmon");
        helper.assertFalse(salmon.isRemoved(), "salmon was removed");
        helper.succeed();
    }

    /** Buckets that cannot hold an entity do not pick it up. */
    public static void doesNotPickUpDeniedEntities(GameTestHelper helper) {
        for (TestBuckets.Definition bucket : List.of(TestBuckets.NO_ENTITIES_BUCKET, TestBuckets.ANTI_SALMON_BUCKET, TestBuckets.ANTI_SALMON_TAG_BUCKET, TestBuckets.ONLY_PUFFER_BUCKET, TestBuckets.ONLY_PUFFER_TAG_BUCKET)) {
            Salmon salmon = helper.spawnWithNoFreeWill(EntityTypes.SALMON, TARGET);
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(bucket, Fluids.WATER));
            interact(player, salmon);
            helper.assertFalse(BucketLibUtil.containsEntityType(player.getMainHandItem()), bucket.name() + " picked up a salmon");
            helper.assertFalse(salmon.isRemoved(), "salmon was removed by " + bucket.name());
            salmon.discard();
        }
        // only pufferfish
        var pufferfish = helper.spawnWithNoFreeWill(EntityTypes.PUFFERFISH, TARGET);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.ONLY_PUFFER_BUCKET, Fluids.WATER));
        interact(player, pufferfish);
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(player.getMainHandItem()), EntityTypes.PUFFERFISH, "entity in the only puffer bucket");
        helper.succeed();
    }

    /** A bucket that already contains an entity does not pick up another one. */
    public static void doesNotPickUpASecondEntity(GameTestHelper helper) {
        Salmon salmon = helper.spawnWithNoFreeWill(EntityTypes.SALMON, TARGET);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.COD));
        interact(player, salmon);
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(player.getMainHandItem()), EntityTypes.COD, "entity in the bucket");
        helper.assertFalse(salmon.isRemoved(), "salmon was removed");
        helper.succeed();
    }

    /** A bucket with water and salmon places the water and the salmon (marked as from a bucket) and becomes empty. */
    public static void releasesSalmonWithWater(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON));
        TestUtils.lookDownAt(helper, player, TARGET.below());
        TestUtils.use(helper, player);
        helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSourceOfType(Fluids.WATER), "no water was placed");
        Salmon salmon = helper.findOneEntity(EntityTypes.SALMON);
        helper.assertTrue(salmon.fromBucket(), "salmon is not marked as from a bucket");
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(hand.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(hand), "bucket is not empty: " + TestUtils.describe(hand));
        helper.assertTrue(hand.getComponentsPatch().isEmpty(), "empty bucket still stores components: " + hand.getComponentsPatch());
        helper.succeed();
    }

    /** A bucket with a sulfur cube (no fluid) releases it like a vanilla sulfur cube bucket. */
    public static void releasesSulfurCube(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE));
        TestUtils.lookDownAt(helper, player, TARGET.below());
        TestUtils.use(helper, player);
        helper.assertEntityPresent(EntityTypes.SULFUR_CUBE);
        helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isEmpty(), "a fluid was placed with a sulfur cube");
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "bucket is not empty: " + TestUtils.describe(player.getMainHandItem()));
        helper.succeed();
    }

    /**
     * The entity data (variant, custom name, ...) is stored in the bucket like in a vanilla bucket and restored when
     * the entity is released. The emptied bucket keeps none of these components.
     */
    public static void keepsEntityData(GameTestHelper helper) {
        TropicalFish fish = helper.spawnWithNoFreeWill(EntityTypes.TROPICAL_FISH, TARGET);
        ItemStack variant = new ItemStack(Items.TROPICAL_FISH_BUCKET);
        variant.set(DataComponents.TROPICAL_FISH_PATTERN, TropicalFish.Pattern.BETTY);
        variant.set(DataComponents.TROPICAL_FISH_BASE_COLOR, DyeColor.LIME);
        variant.set(DataComponents.TROPICAL_FISH_PATTERN_COLOR, DyeColor.PURPLE);
        fish.applyComponentsFromItemStack(variant);
        fish.setCustomName(Component.literal("Nemo"));
        TestUtils.assertEquals(helper, fish.getPattern(), TropicalFish.Pattern.BETTY, "test setup broken: pattern");
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        interact(player, fish);
        ItemStack filled = player.getMainHandItem();
        TestUtils.assertEquals(helper, filled.get(DataComponents.TROPICAL_FISH_PATTERN), TropicalFish.Pattern.BETTY, "pattern stored in the bucket");
        helper.assertTrue(filled.has(DataComponents.BUCKET_ENTITY_DATA), "no bucket entity data stored");
        TestUtils.lookDownAt(helper, player, TARGET.below());
        TestUtils.use(helper, player);
        TropicalFish released = helper.findOneEntity(EntityTypes.TROPICAL_FISH);
        TestUtils.assertEquals(helper, released.getPattern(), TropicalFish.Pattern.BETTY, "pattern of the released fish");
        TestUtils.assertEquals(helper, released.getBaseColor(), DyeColor.LIME, "base color of the released fish");
        TestUtils.assertEquals(helper, released.getCustomName() != null ? released.getCustomName().getString() : null, "Nemo", "name of the released fish");
        ItemStack empty = player.getMainHandItem();
        helper.assertTrue(BucketLibUtil.isEmpty(empty), "bucket is not empty: " + TestUtils.describe(empty));
        helper.assertFalse(empty.has(DataComponents.TROPICAL_FISH_PATTERN) || empty.has(DataComponents.BUCKET_ENTITY_DATA), "emptied bucket keeps the fish data: " + empty.getComponentsPatch());
        KnownIssues.run(helper, "KI-12 emptied bucket keeps the custom name of the released entity", Platforms.ALL, () ->
                helper.assertTrue(empty.getComponentsPatch().isEmpty(), "emptied bucket keeps components of the fish: " + empty.getComponentsPatch()));
    }

    /** Spawning the entity of a bucket without entity does nothing and returns an unchanged copy. */
    public static void spawningWithoutEntityDoesNothing(GameTestHelper helper) {
        ItemStack water = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        ItemStack result = TestBuckets.TEST_BUCKET.item().spawnEntityFromBucket(null, helper.getLevel(), water, helper.absolutePos(TARGET), true);
        helper.assertTrue(result != water && ItemStack.isSameItemSameComponents(result, water), "spawning without entity changed the bucket: " + TestUtils.describe(result));
        helper.assertTrue(helper.getLevel().getEntities((Entity) null, helper.getBounds(), entity -> !(entity instanceof net.minecraft.world.entity.player.Player)).isEmpty(), "an entity was spawned");
        helper.succeed();
    }

    /** In creative mode the bucket keeps the entity when releasing it. */
    public static void creativeModeKeepsTheBucket(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE));
        TestUtils.lookDownAt(helper, player, TARGET.below());
        TestUtils.use(helper, player);
        helper.assertEntityPresent(EntityTypes.SULFUR_CUBE);
        TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(player.getMainHandItem()), EntityTypes.SULFUR_CUBE, "entity in the bucket after releasing in creative mode");
        helper.succeed();
    }

    /** Like a vanilla tropical fish bucket, a bucket with a tropical fish feeds an axolotl and keeps the water. */
    public static void feedsAxolotl(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER);
        Axolotl axolotl = helper.spawnWithNoFreeWill(EntityTypes.AXOLOTL, TARGET);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.TROPICAL_FISH));
        InteractionResult result = interact(player, axolotl);
        helper.assertTrue(result.consumesAction(), "feeding the axolotl did not consume the action: " + result);
        helper.assertTrue(axolotl.isInLove(), "axolotl is not in love after feeding");
        ItemStack hand = player.getMainHandItem();
        helper.assertFalse(BucketLibUtil.containsEntityType(hand), "fish was not removed from the bucket");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(hand), Fluids.WATER, "fluid after feeding");
        helper.succeed();
    }

    /** An axolotl accepts the same bucket entities as food as with vanilla buckets. */
    public static void axolotlFoodLikeVanillaBuckets(GameTestHelper helper) {
        Axolotl axolotl = helper.spawnWithNoFreeWill(EntityTypes.AXOLOTL, TARGET);
        VANILLA_BUCKETS.forEach((type, vanilla) -> {
            ItemStack bucket = TestUtils.withEntity(TestBuckets.TEST_BUCKET, type == EntityTypes.SULFUR_CUBE ? null : Fluids.WATER, type);
            TestUtils.assertEquals(helper, axolotl.isFood(bucket), axolotl.isFood(new ItemStack(vanilla)), "axolotl food: bucket with " + type);
        });
        helper.assertFalse(axolotl.isFood(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)), "water bucket is axolotl food");
        helper.succeed();
    }

    /** A nautilus accepts the same bucket entities as food, taming item and temptation as with vanilla buckets. */
    public static void nautilusFoodLikeVanillaBuckets(GameTestHelper helper) {
        AbstractNautilus nautilus = helper.spawnWithNoFreeWill(EntityTypes.NAUTILUS, TARGET);
        var temptations = NautilusAi.getTemptations();
        VANILLA_BUCKETS.forEach((type, vanilla) -> {
            ItemStack bucket = TestUtils.withEntity(TestBuckets.TEST_BUCKET, type == EntityTypes.SULFUR_CUBE ? null : Fluids.WATER, type);
            TestUtils.assertEquals(helper, nautilus.isFood(bucket), nautilus.isFood(new ItemStack(vanilla)), "nautilus taming item: bucket with " + type);
            TestUtils.assertEquals(helper, temptations.test(bucket), temptations.test(new ItemStack(vanilla)), "nautilus temptation: bucket with " + type);
        });
        helper.assertFalse(temptations.test(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)), "water bucket tempts a nautilus");
        helper.assertFalse(nautilus.isFood(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)), "water bucket is nautilus food");
        helper.succeed();
    }

    /** Taming a nautilus with a bucket entity (like a vanilla fish bucket) removes the entity and keeps the water. */
    public static void tamesNautilus(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER);
        AbstractNautilus nautilus = helper.spawnWithNoFreeWill(EntityTypes.NAUTILUS, TARGET);
        ItemStack food = VANILLA_BUCKETS.entrySet().stream()
                .filter(entry -> nautilus.isFood(new ItemStack(entry.getValue())))
                .map(entry -> TestUtils.withEntity(TestBuckets.TEST_BUCKET, entry.getKey() == EntityTypes.SULFUR_CUBE ? null : Fluids.WATER, entry.getKey()))
                .findFirst().orElse(null);
        if (food == null) {
            // no vanilla bucket tames a nautilus in this Minecraft version
            helper.succeed();
            return;
        }
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, food);
        interact(player, nautilus);
        ItemStack hand = player.getMainHandItem();
        helper.assertFalse(BucketLibUtil.containsEntityType(hand), "entity was not removed from the bucket: " + TestUtils.describe(hand));
        helper.assertTrue(hand.is(TestBuckets.TEST_BUCKET.item()), "bucket was consumed: " + hand);
        helper.succeed();
    }

}
