# AGENTS.md

Instructions for AI coding agents working on this repository.

## About the mod

BucketLib is a library for mods that add custom buckets (e.g. Ceramic Bucket, Wooden Bucket). A mod creates
`UniversalBucketItem`s with `UniversalBucketItem.Properties` (allowed/denied fluids, entities, blocks, temperatures,
cracking, burning/freezing, durability, milking, stack size, creative tab, cooking fuel) and registers them with
`BucketLibApi.registerBucket`. BucketLib then gives them the behavior of vanilla buckets: fluids, milk, mob buckets,
powder snow, cauldrons, dispensers, furnaces, the fluid API of the loader, recipe ingredients and bucket filling
recipes. The complete feature catalog and the tests are in [TESTING.md](TESTING.md).

## Project structure

- `common/` - all game logic, common mixins, resources and data. `common/src/gametest/` contains all game tests
  (vanilla and BucketLib API only) and the test mod `bucketlibtest` with its test buckets (`TestBuckets`).
- `fabric/`, `neoforge/` - loader specific code: registration, fluid storage/capability, config, ingredients, client.
  `<loader>/src/gametest/` is a thin adapter (test mod registration, `IGameTestPlatform`, loader specific recipes).
  Never put test logic there.
- `forge/` - currently not part of the build (see `settings.gradle`), not tested.
- `buildSrc/` - shared Gradle logic, incl. the gametest source set and the JaCoCo coverage of the game tests.
- `scripts/` - harmless log lines of the server start test (`server-starttest.allowlist`, script in
  cech12/MinecraftModActions).

## Build and test

```bash
./gradlew build                                        # all loaders (does not compile the game tests)
./gradlew runGameTestServer                            # game tests of all loaders (NeoForge also with milk fluid)
./gradlew :fabric:runGameTestServer                    # one loader (also :neoforge:)
./gradlew runGameTestServer jacocoMergedGameTestReport # + coverage of the common code over all loaders
```

## Conventions

- All versions live in `gradle.properties`; new fields must also be added to `expandProps` in
  `buildSrc/src/main/groovy/multiloader-common.gradle`.
- One branch per Minecraft version, the default branch is `26.x`.
- `CHANGELOG.md` follows Keep a Changelog, version scheme `<MC version>-<mod version>`.
- Every new feature or bugfix gets a game test, and `TESTING.md` is updated. Check that the test fails when the
  feature is broken. Each test needs a test instance json in `common/src/gametest/resources/data/bucketlib/test_instance/`.
- New property combinations get a test bucket in `TestBuckets` (+ item model and translation, checked by tests).

## Do not

- Push directly to version branches or `main`. Use a feature branch and a pull request.
- Create tags or releases, change `mod_version`, touch the publish jobs of the CI or any secrets.
- Change mod behavior in a test PR. If a test reveals a bug, mark it with `KnownIssues` and list it in TESTING.md.

## Mod specific notes

- Contents are stored in the custom data component `bucketlib:bucket_content` ("BucketContent" = block or milk id,
  "EntityType" = entity id) and the fluid in the loader component `bucketlib:fluid`. Keep this format compatible.
- The durability is not stored in the item: `DataComponentHolderMixin` answers `MAX_DAMAGE`/`DAMAGE` from the
  properties, so config changes apply to existing buckets.
- On NeoForge, `NeoforgeUniversalBucketItemMixin` changes `UniversalBucketItem`, so JaCoCo cannot measure this class
  on NeoForge; it is measured by the Fabric run.
- Since Minecraft 26.3 every dyeable bucket needs its own `minecraft:crafting_dye` recipe.
