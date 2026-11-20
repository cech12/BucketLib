package de.cech12.bucketlib.gametest.tests;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.GameTestPlatform;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests that check the test setup itself.
 */
public final class MetaTests {

    private MetaTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("meta/every_test_function_has_an_instance", MetaTests::everyTestFunctionHasAnInstance);
        tests.add("meta/platform_name_matches_the_loader", MetaTests::platformNameMatchesTheLoader);
        tests.add("meta/test_buckets_are_registered", MetaTests::testBucketsAreRegistered);
        tests.add("meta/known_issue_marker_works", MetaTests::knownIssueMarkerWorks);
    }

    /**
     * Every test function has a test_instance json with the same id that runs exactly this function. A test function
     * without one would silently never run, and a copied json that names another function would run that one twice.
     */
    public static void everyTestFunctionHasAnInstance(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var instances = registries.lookupOrThrow(Registries.TEST_INSTANCE);
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<Identifier> missing = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        for (Identifier id : BucketLibGameTests.getTests().keySet()) {
            var instance = instances.get(ResourceKey.create(Registries.TEST_INSTANCE, id));
            if (instance.isEmpty()) {
                missing.add(id);
                continue;
            }
            GameTestInstance test = instance.get().value();
            String function = test instanceof FunctionGameTestInstance
                    ? GameTestInstance.DIRECT_CODEC.encodeStart(ops, test).result()
                            .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
                            .map(json -> json.get("function")).map(JsonElement::getAsString).orElse("?")
                    : "(no function instance)";
            if (!id.toString().equals(function)) {
                wrong.add(id + " runs " + function);
            }
        }
        // test instances of this namespace without a test function would fail at runtime with an unclear error
        List<Identifier> orphaned = instances.listElementIds()
                .map(ResourceKey::identifier)
                .filter(id -> id.getNamespace().equals("bucketlib") && !BucketLibGameTests.getTests().containsKey(id))
                .toList();
        helper.assertTrue(missing.isEmpty(), "Test functions without test_instance json: " + missing);
        helper.assertTrue(wrong.isEmpty(), "test_instance jsons that do not run their own test function: " + wrong);
        helper.assertTrue(orphaned.isEmpty(), "test_instance jsons without test function: " + orphaned);
        helper.succeed();
    }

    /** Loader classes that identify the running loader, keyed by the expected platform name. */
    private static final Map<String, String> LOADER_CLASSES = Map.of(
            Platforms.FABRIC, "net.fabricmc.loader.api.FabricLoader",
            Platforms.NEOFORGE, "net.neoforged.fml.loading.FMLLoader");

    /**
     * The platform name of BucketLib (IPlatformHelper#getPlatformName) names the loader that actually runs. Tests with
     * intended loader differences and {@link KnownIssues} rely on it. The other platform helper methods are checked
     * here, too (the game tests always run in a development environment with BucketLib loaded).
     */
    public static void platformNameMatchesTheLoader(GameTestHelper helper) {
        List<String> running = LOADER_CLASSES.entrySet().stream()
                .filter(entry -> classExists(entry.getValue())).map(Map.Entry::getKey).toList();
        TestUtils.assertEquals(helper, running.size(), 1, "number of detected loaders " + running);
        TestUtils.assertEquals(helper, Platforms.current(), running.getFirst(), "platform name");
        helper.assertTrue(Services.PLATFORM.isModLoaded("bucketlib"), "bucketlib is not reported as loaded mod");
        helper.assertTrue(Services.PLATFORM.isModLoaded(TestBuckets.NAMESPACE), "the test mod is not reported as loaded mod");
        helper.assertFalse(Services.PLATFORM.isModLoaded("bucketlib_not_existing_mod"), "a not existing mod is reported as loaded");
        helper.assertTrue(Services.PLATFORM.isDevelopmentEnvironment(), "game tests do not run in a development environment");
        TestUtils.assertEquals(helper, Services.PLATFORM.getEnvironmentName(), "development", "environment name");
        helper.succeed();
    }

    /** All test buckets of {@link TestBuckets} are registered as items and at BucketLib. */
    public static void testBucketsAreRegistered(GameTestHelper helper) {
        var registered = Services.REGISTRY.getRegisteredBuckets();
        for (TestBuckets.Definition bucket : TestBuckets.ALL) {
            helper.assertTrue(registered.contains(bucket.item()), bucket.id() + " is not registered at BucketLib");
        }
        TestUtils.assertEquals(helper, registered.size(), TestBuckets.ALL.size(), "number of registered buckets " + registered);
        helper.assertTrue(GameTestPlatform.INSTANCE != null, "no game test platform");
        helper.succeed();
    }

    /**
     * {@link KnownIssues#check} passes while the issue is present and fails as soon as it is gone, so a fixed bug is
     * noticed. Checked here once for every loader, because the other known issues only cover their own loaders.
     */
    public static void knownIssueMarkerWorks(GameTestHelper helper) {
        // issue present: no failure
        KnownIssues.check(helper, "meta test", Platforms.ALL, () -> helper.assertTrue(false, "issue present"));
        // issue fixed: the marker fails
        boolean failed = false;
        try {
            KnownIssues.check(helper, "meta test", Platforms.ALL, () -> {});
        } catch (net.minecraft.gametest.framework.GameTestAssertException e) {
            failed = true;
        }
        helper.assertTrue(failed, "KnownIssues does not fail when the issue is fixed");
        // not affected platform: assertions run normally
        boolean notAffectedFailed = false;
        try {
            KnownIssues.check(helper, "meta test", java.util.Set.of(), () -> helper.assertTrue(false, "issue present"));
        } catch (net.minecraft.gametest.framework.GameTestAssertException e) {
            notAffectedFailed = true;
        }
        helper.assertTrue(notAffectedFailed, "KnownIssues hides failures on not affected platforms");
        helper.succeed();
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, MetaTests.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

}
