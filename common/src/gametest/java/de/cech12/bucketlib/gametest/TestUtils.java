package de.cech12.bucketlib.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import de.cech12.bucketlib.util.BucketLibUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Small helpers shared by all game tests. Only uses vanilla API and the BucketLib API, so it works on every loader.
 */
public final class TestUtils {

    /** Position in the middle of the "bucketlib:platform" structure (5x4x5, stone floor), one block above the floor. */
    public static final BlockPos CENTER = new BlockPos(2, 1, 2);

    private TestUtils() {}

    /** @return new empty stack of the test bucket */
    public static ItemStack bucket(TestBuckets.Definition bucket) {
        return new ItemStack(bucket.item());
    }

    /** @return new stack of the test bucket filled with the fluid */
    public static ItemStack withFluid(TestBuckets.Definition bucket, Fluid fluid) {
        return BucketLibUtil.addFluid(bucket(bucket), fluid);
    }

    /** @return new stack of the test bucket filled with milk */
    public static ItemStack withMilk(TestBuckets.Definition bucket) {
        return BucketLibUtil.addMilk(bucket(bucket));
    }

    /** @return new stack of the test bucket that contains the entity (and the fluid, if not null) */
    public static ItemStack withEntity(TestBuckets.Definition bucket, Fluid fluid, EntityType<?> entityType) {
        ItemStack stack = bucket(bucket);
        if (fluid != null) {
            stack = BucketLibUtil.addFluid(stack, fluid);
        }
        return BucketLibUtil.addEntityType(stack, entityType);
    }

    /** @return new stack of the test bucket that contains the block */
    public static ItemStack withBlock(TestBuckets.Definition bucket, Block block) {
        return BucketLibUtil.addBlock(bucket(bucket), block);
    }

    /**
     * Creates a server player (not added to the level) with a network connection that discards all packets, so code
     * that sends messages to the player works like in a real game. It holds the given stack in its main hand and stands
     * one block north of {@link #CENTER}.
     */
    public static ServerPlayer player(GameTestHelper helper, GameType gameType, ItemStack mainHand) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "test-mock-player");
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, ClientInformation.createDefault()) {
            @Override
            public GameType gameMode() {
                return gameType;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        gameType.updatePlayerAbilities(player.getAbilities());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, CommonListenerCookie.createInitial(profile, false));
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(CENTER.north()));
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        return player;
    }

    /**
     * Places the player one block above the given block (relative position) and lets it look straight down at it, so
     * using an item hits the top face of this block (or a fluid source in it).
     */
    public static void lookDownAt(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(relativePos.above()));
        player.snapTo(pos.x, pos.y, pos.z, 0, 90);
    }

    /**
     * Uses (right-clicks into the air or at the looked at block) the item in the main hand like a real player would do,
     * including the replacement of the held item.
     *
     * @return interaction result
     */
    public static InteractionResult use(GameTestHelper helper, ServerPlayer player) {
        return player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    /**
     * Right-clicks an entity with the item in the main hand like a real player would do.
     */
    public static InteractionResult interact(ServerPlayer player, Entity entity) {
        return player.interactOn(entity, InteractionHand.MAIN_HAND, entity.position());
    }

    /**
     * Runs code under test and turns an exception into a failed assertion (with the exception in the message), so that
     * a crash caused by a bug of the mod is reported as failed test and can be marked with {@link KnownIssues}.
     */
    public static <T> T noException(GameTestHelper helper, String action, Supplier<T> code) {
        try {
            return code.get();
        } catch (RuntimeException | LinkageError e) {
            StackTraceElement origin = java.util.Arrays.stream(e.getStackTrace())
                    .filter(element -> element.getClassName().startsWith("de.cech12.bucketlib") && !element.getClassName().startsWith("de.cech12.bucketlib.gametest"))
                    .findFirst().orElse(e.getStackTrace().length > 0 ? e.getStackTrace()[0] : null);
            throw helper.assertionException(net.minecraft.network.chat.Component.literal(action + " threw " + e + " at " + origin));
        }
    }

    /**
     * Runs code under test and turns an exception into a failed assertion, see {@link #noException(GameTestHelper, String, Supplier)}.
     */
    public static void noException(GameTestHelper helper, String action, Runnable code) {
        noException(helper, action, () -> {
            code.run();
            return null;
        });
    }

    /**
     * Like {@link GameTestHelper#assertValueEqual}, but also works with null values (e.g. a missing entity type).
     */
    public static <T> void assertEquals(GameTestHelper helper, T actual, T expected, String name) {
        if (!java.util.Objects.equals(actual, expected)) {
            throw helper.assertionException(net.minecraft.network.chat.Component.literal("Expected " + name + " to be " + expected + ", but was " + actual));
        }
    }

    /** @return content of the bucket as text for assertion messages */
    public static String describe(ItemStack stack) {
        return stack + " " + stack.getComponentsPatch();
    }

    /**
     * Decodes a value from json with the registries of the level.
     */
    public static <T> T decode(GameTestHelper helper, Codec<T> codec, JsonElement json) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        return codec.parse(ops, json).getOrThrow(error -> helper.assertionException(net.minecraft.network.chat.Component.literal("cannot decode " + json + ": " + error)));
    }

    /**
     * Encodes a value to json with the registries of the level.
     */
    public static <T> JsonElement encode(GameTestHelper helper, Codec<T> codec, T value) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        return codec.encodeStart(ops, value).getOrThrow(error -> helper.assertionException(net.minecraft.network.chat.Component.literal("cannot encode " + value + ": " + error)));
    }

    /**
     * Creates a custom ingredient of BucketLib like a data pack does it.
     *
     * @param type ingredient type without namespace ("fluid", "milk", "entity", "block", "empty")
     * @param fields further json fields (e.g. "fluid" -> "minecraft:water")
     */
    public static Ingredient ingredient(GameTestHelper helper, String type, Map<String, String> fields) {
        return decode(helper, Ingredient.CODEC, ingredientJson(type, fields));
    }

    /** @return json of a custom ingredient of BucketLib for the running loader */
    public static JsonObject ingredientJson(String type, Map<String, String> fields) {
        JsonObject json = new JsonObject();
        json.addProperty(GameTestPlatform.INSTANCE.customIngredientTypeKey(), "bucketlib:" + type);
        fields.forEach(json::addProperty);
        return json;
    }

    /** @return parsed json */
    public static JsonElement json(String json) {
        return JsonParser.parseString(json);
    }

}
