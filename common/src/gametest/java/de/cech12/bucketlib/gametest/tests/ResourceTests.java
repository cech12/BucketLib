package de.cech12.bucketlib.gametest.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Resources of BucketLib: translations, textures of the bucket model and of the bucket contents, data pack entries. A
 * dedicated server does not load client resources, so the tests read them directly from the mod resources. How the
 * buckets look in game must be checked manually (see TESTING.md).
 */
public final class ResourceTests {

    private static final String LANG_DIR = "assets/bucketlib/lang/";
    /** Languages that must always be found, so that a broken directory listing does not check nothing. */
    private static final List<String> REQUIRED_LANGUAGES = List.of("en_us", "fr_fr", "hu_hu");

    private ResourceTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("resources/translations", ResourceTests::translations);
        tests.add("resources/bucket_model_textures_exist", ResourceTests::bucketModelTexturesExist);
        tests.add("resources/content_textures_exist_for_vanilla_contents", ResourceTests::contentTexturesExistForVanillaContents);
        tests.add("resources/test_buckets_have_models_and_names", ResourceTests::testBucketsHaveModelsAndNames);
        tests.add("resources/default_cooking_fuel_is_registered", ResourceTests::defaultCookingFuelIsRegistered);
    }

    private static InputStream open(String path) {
        return Services.class.getClassLoader().getResourceAsStream(path);
    }

    private static boolean exists(String path) {
        try (InputStream stream = open(path)) {
            return stream != null && stream.read() != -1;
        } catch (IOException e) {
            return false;
        }
    }

    private static JsonObject readJson(GameTestHelper helper, String path) {
        try (InputStream stream = open(path)) {
            helper.assertTrue(stream != null, "resource " + path + " is missing");
            JsonElement json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            helper.assertTrue(json.isJsonObject(), "resource " + path + " is no json object");
            return json.getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            helper.fail("resource " + path + " cannot be read: " + e);
            return new JsonObject();
        }
    }

    /** All shipped languages: the json files next to en_us.json (in a directory or a jar). */
    private static List<String> shippedLanguages(GameTestHelper helper) {
        URL english = Services.class.getClassLoader().getResource(LANG_DIR + "en_us.json");
        helper.assertTrue(english != null, "resource " + LANG_DIR + "en_us.json is missing");
        try {
            URI uri = english.toURI();
            try {
                return listLanguages(Path.of(uri).getParent());
            } catch (FileSystemNotFoundException e) {
                FileSystem jar;
                try {
                    jar = FileSystems.newFileSystem(uri, Map.of());
                } catch (FileSystemAlreadyExistsException opened) {
                    return listLanguages(FileSystems.getFileSystem(uri).provider().getPath(uri).getParent());
                }
                try (jar) {
                    return listLanguages(jar.provider().getPath(uri).getParent());
                }
            }
        } catch (URISyntaxException | IOException | RuntimeException e) {
            helper.fail("cannot list the language files in " + english + ": " + e);
            return List.of();
        }
    }

    private static List<String> listLanguages(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted().toList();
        }
    }

    /**
     * Every shipped language is a valid json file with the same keys as English. English contains the keys used by
     * the code (milk name on Fabric, tag names, config screen).
     */
    public static void translations(GameTestHelper helper) {
        List<String> languages = shippedLanguages(helper);
        helper.assertTrue(languages.containsAll(REQUIRED_LANGUAGES), "language files " + REQUIRED_LANGUAGES + " not found, found only " + languages);
        JsonObject english = readJson(helper, LANG_DIR + "en_us.json");
        List<String> required = new ArrayList<>(List.of("tag.fluid.c.milk", "tag.entity_type.bucketlib.milkable", "tag.fluid.bucketlib.infinity_enchantable",
                "text.autoconfig.bucketlib.title", "text.autoconfig.bucketlib.option.INFINITY_ENCHANTMENT_ENABLED"));
        if (Services.PLATFORM.getMilkTranslationKey().startsWith("tag.")) {
            required.add(Services.PLATFORM.getMilkTranslationKey());
        }
        for (String key : required) {
            helper.assertTrue(english.has(key) && !english.get(key).getAsString().isBlank(), "en_us does not translate " + key);
        }
        List<String> missing = new ArrayList<>();
        for (String language : languages) {
            JsonObject lang = readJson(helper, LANG_DIR + language + ".json");
            for (String key : english.keySet()) {
                if (!lang.has(key) || lang.get(key).getAsString().isBlank()) {
                    missing.add(language + ": " + key);
                }
            }
        }
        helper.assertTrue(missing.isEmpty(), "missing translations: " + missing);
        BucketLib.LOG.info("Checked the translations of {} languages: {}", languages.size(), languages);
        helper.succeed();
    }

    /** The textures of the bucket model (base, cracked, lower, fluid mask, missing content) exist. */
    public static void bucketModelTexturesExist(GameTestHelper helper) {
        for (String texture : List.of("universal_bucket_base", "universal_bucket_cracked_base", "universal_bucket_lower_base",
                "universal_bucket_cracked_lower_base", "mask/bucket_fluid", "bucket_content/missing_lower_content")) {
            helper.assertTrue(exists("assets/bucketlib/textures/item/" + texture + ".png"), "texture bucketlib:item/" + texture + " is missing");
        }
        helper.succeed();
    }

    /**
     * Every vanilla entity and block that a bucket can hold (and milk) has a content texture
     * ({@code assets/<namespace>/textures/item/bucket_content/<path>.png}). When a new Minecraft version adds a mob
     * bucket, this test fails until the texture is added.
     */
    public static void contentTexturesExistForVanillaContents(GameTestHelper helper) {
        List<Identifier> contents = new ArrayList<>();
        RegistryUtil.getBucketEntities().forEach(entity -> contents.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.entityType())));
        RegistryUtil.getBucketBlocks().forEach(block -> contents.add(BuiltInRegistries.BLOCK.getKey(block.block())));
        contents.add(Identifier.withDefaultNamespace("milk"));
        List<Identifier> missing = contents.stream()
                .filter(id -> id.getNamespace().equals("minecraft"))
                .filter(id -> !exists("assets/minecraft/textures/item/bucket_content/" + id.getPath() + ".png"))
                .toList();
        helper.assertTrue(missing.isEmpty(), "content textures missing for " + missing);
        helper.succeed();
    }

    /** Every test bucket has an item model definition and a name (needed for the manual tests in the client run). */
    public static void testBucketsHaveModelsAndNames(GameTestHelper helper) {
        JsonObject lang = readJson(helper, "assets/" + TestBuckets.NAMESPACE + "/lang/en_us.json");
        for (TestBuckets.Definition bucket : TestBuckets.ALL) {
            JsonObject model = readJson(helper, "assets/" + TestBuckets.NAMESPACE + "/items/" + bucket.name() + ".json");
            TestUtils.assertEquals(helper, model.getAsJsonObject("model").get("type").getAsString(), "bucketlib:universal_bucket", "model type of " + bucket.name());
            String key = "item." + TestBuckets.NAMESPACE + "." + bucket.name();
            for (String suffix : List.of("", ".entity", ".filled")) {
                helper.assertTrue(lang.has(key + suffix), "test bucket " + bucket.name() + " has no translation " + key + suffix);
            }
        }
        helper.succeed();
    }

    /** The default cooking fuel of empty buckets (bucketlib:cooking/time_bucketlib) is registered by the data pack. */
    public static void defaultCookingFuelIsRegistered(GameTestHelper helper) {
        var providers = helper.getLevel().getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.CONTEXT_INT_PROVIDER);
        TestUtils.assertEquals(helper, BucketLib.DEFAULT_COOKING_FUEL, ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, BucketLib.id("cooking/time_bucketlib")), "default cooking fuel key");
        helper.assertTrue(providers.get(BucketLib.DEFAULT_COOKING_FUEL).isPresent(), "default cooking fuel is not registered");
        helper.assertTrue(providers.get(TestBuckets.TEST_FUEL).isPresent(), "test fuel is not registered");
        helper.succeed();
    }

}
