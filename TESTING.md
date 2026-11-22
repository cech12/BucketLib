# Testing

BucketLib is tested with GameTests on Fabric and NeoForge (NeoForge twice: without and with the NeoForge milk fluid).
They run in CI in the build job (`gametest: true`); publishing needs build, game tests and the server start test.

```bash
./gradlew runGameTestServer            # all loaders (NeoForge also runs runGameTestServerMilk)
./gradlew :fabric:runGameTestServer
./gradlew :neoforge:runGameTestServer
```

Reports: `<loader>/build/gametest-report.xml` (`gametest-milk-report.xml`), code coverage (JaCoCo, BucketLib classes
without client code and mixin classes): `<loader>/build/reports/jacoco/gametest/html/index.html`.

## Structure

```
common/src/gametest/java/.../gametest/   test logic (vanilla + BucketLib API only), shared by all loaders
  BucketLibGameTests                     central list of all test classes
  TestBuckets                            buckets of the test mod "bucketlibtest" (one per property combination)
  TestFluids, TestBlocks                 fluids and blocks of "other mods" for the cases vanilla does not have
  KnownIssues                            marks known bugs ("deactivated" tests, see below)
  ExpectedLogs                           hides log errors that tests provoke on purpose (and counts them)
  IGameTestPlatform                      loader specific operations (config, fluid API, ingredient sync, enchanting)
  tests/*Tests                           the tests, grouped by feature
common/src/gametest/resources/           test instances, environments, test mod assets/data
<loader>/src/gametest/                   thin adapter: test mod registration + IGameTestPlatform, loader specific recipes
```

The old `src/test` test mods were moved into the gametest source sets; the client runs load the test mod, so the
test buckets are still available for manual tests. `meta/milk_fluid_matches_the_run` needs the system property
`bucketlibtest.milkEnabled` of the NeoForge game test runs, so it fails when started by hand in the NeoForge client.
The test fluid with a throwing `getBucket()` (simulates a broken mod) is only registered in the game test runs
(system property `bucketlibtest.gameTestRun`); in the client runs it would fill the log, so its tests fail there when
started by hand.

### Writing a new test

1. Add a method to the matching class in `tests/` and register it: `tests.add("group/my_test", Class::myTest);`.
2. Add `common/src/gametest/resources/data/bucketlib/test_instance/group/my_test.json` (copy an existing one;
   `infinity/enabled/*` uses the environment `bucketlib:infinity_enabled`). `meta/every_test_function_has_an_instance`
   fails if it is missing or orphaned.
3. A found bug is not fixed in a test change: wrap only its assertion in
   `KnownIssues.run(helper, "KI-<n> ...", <platforms>, () -> ...)` and list it below. The test passes while the bug
   exists and fails as soon as it is fixed, then remove the marker.

## Known issues (marked with `KnownIssues`)

| Id | Issue | Loader | Description | Test |
|---|---|---|---|---|
| KI-1 | [#92](https://github.com/cech12/BucketLib/issues/92) | all | creative tab lists a milk bucket also for buckets that cannot milk | `registration/creative_tab_only_lists_possible_contents` |
| KI-2 | [#93](https://github.com/cech12/BucketLib/issues/93) | all | invalid content ids (e.g. from commands) crash `getBlock`/item name | `content/invalid_content_ids` |
| KI-3 | [#94](https://github.com/cech12/BucketLib/issues/94) | NeoForge (milk) | bucket filled with the milk fluid through the fluid API is no drinkable milk bucket | `milk/milk_fluid_of_other_mods_can_be_drunk` |
| KI-4 | [#95](https://github.com/cech12/BucketLib/issues/95) | Fabric | dispensers keep unusable buckets instead of dropping them like vanilla | `dispenser/drops_unusable_buckets_like_vanilla` |
| KI-5 | [#96](https://github.com/cech12/BucketLib/issues/96) | Fabric | dispensed milk bucket removes a water source in front of it | `dispenser/milk_bucket_does_not_remove_water` |
| KI-12 | [#103](https://github.com/cech12/BucketLib/issues/103) | all | emptied bucket keeps the custom name of the released entity | `entity/keeps_entity_data` |
| KI-14 | [#105](https://github.com/cech12/BucketLib/issues/105) | all | burning/freezing block lists created with `List.of` crash the inventory tick (`contains(null)`) | `temperature/normal_contents_do_nothing` |
| KI-16 | [#107](https://github.com/cech12/BucketLib/issues/107) | NeoForge | `BucketLibUtil.containsFluid/isEmpty` throw for empty stacks | `content/empty_stack_has_no_content` |
| KI-17 | [#108](https://github.com/cech12/BucketLib/issues/108) | NeoForge | fluid API fills fluids into buckets with a block (powder snow) | `fluid_api/does_not_fill_filled_buckets` |
| KI-21 | [#112](https://github.com/cech12/BucketLib/issues/112) | all | filled buckets are not emptied into cauldrons that are not empty like vanilla buckets (they place the content above the cauldron; all other combinations match vanilla) | `cauldron/behaves_like_vanilla_buckets` |
| KI-22 | [#113](https://github.com/cech12/BucketLib/issues/113) | all (milk: without milk fluid) | buckets with powder snow or milk pick up sulfur cubes | `entity/filled_buckets_do_not_pick_up_entities` |
| KI-24 | [#115](https://github.com/cech12/BucketLib/issues/115) | all | filling and emptying buckets does not count the "used" statistic of the bucket (vanilla buckets do) | `fluid/picks_up_*`, `fluid/places_*` |

All known issues are confirmed by the game test runs of all loaders (a marker fails as soon as its bug is fixed).
Every known issue has a GitHub issue (column "Issue").

## Features and tests

All test names are relative to `bucketlib:`. Every test runs on Fabric and NeoForge (NeoForge also with milk fluid).

| Area | Tests | What is checked |
|---|---|---|
| Registration | `registration/*` | data components, recipe serializers, ingredient types, fluid storage/capability of every bucket, error handling of `BucketLibApi`, tags, vanilla bucket blocks/entities, creative tabs, registry and fluid helpers |
| Properties | `properties/*` | allowed/denied fluids, entities, blocks (lists and tags), temperatures, milking, config suppliers, stack size, invalid values, getters |
| Contents | `content/*` | adding/removing fluid, milk, entity, block, storage format (compatibility), unknown/invalid ids, item names, item stack templates |
| World interaction | `fluid/*`, `cauldron/*`, `block/*` | picking up and placing fluids (waterlogging, replaceable blocks, nether), cauldrons, powder snow, creative mode, cracking |
| Entities and milk | `entity/*`, `milk/*` | fish, axolotl, tadpole, sulfur cube, entity data, axolotl/nautilus feeding and taming, milking, drinking |
| Dispenser | `dispenser/*` | picking up/placing fluids, powder snow and entities, stacked buckets, full dispenser |
| Durability, Infinity, temperature | `durability/*`, `infinity/*`, `temperature/*` | damage per use, breaking, config changes, Unbreaking, Infinity config and effects, burning/freezing holders |
| Furnace | `furnace/*` | burn times, remainders, wet sponge, hoppers, fuel slot |
| Fluid API | `fluid_api/*` | other mods reading, filling and draining buckets through the loader API |
| Recipes | `ingredient/*`, `recipe/*`, `codec/*` | all ingredient types, crafting remainders, cake recipe, bucket filling recipes, dyeing, repairing, json and network codecs |
| Fluids of other mods | `modded_fluid/*` | test fluids: own bucket and attributes (temperature, light, gas, name), no bucket, bucket of another fluid, milk bucket, throwing `getBucket()`, incompatible fluid source block |
| Resources, setup | `resources/*`, `meta/*` | translations, textures, test buckets, cooking fuel, every test function has an instance, KnownIssues marker, milk fluid of the NeoForge runs |

## Code coverage

Every game test run is measured with JaCoCo (only BucketLib classes):

| Report | Content |
|---|---|
| `<loader>/build/reports/jacoco/gametest/html/index.html` | common + loader specific classes of one loader |
| `build/reports/jacoco/gametest/html/index.html` (`./gradlew jacocoMergedGameTestReport`) | common classes, merged over all loaders |

The old test suites of Ceramic Bucket (`CeramicBucket/forge/src/test`) and the former Forge tests of BucketLib
(`forge/src/test`) were compared with these tests; all applicable cases are part of the feature tests above (cases for
removed Minecraft mechanics are obsolete).

Excluded: client only code (models, tints, JEI, REI, ModMenu) and mixin classes (their code is merged into Minecraft
classes, the tests check their behavior). On NeoForge `UniversalBucketItem` is changed by a mixin, so it is excluded from the NeoForge report and only
measured by the Fabric run (the mixin adds the NeoForge methods `supportsEnchantment`/`isPrimaryItemFor`; the Mixin
session id differs per run, so even the dumped classes of two runs do not match). Declaring both methods directly in
`UniversalBucketItem` (like `getMaxStackSize(ItemStack)`) would make the mixin unnecessary and the class measurable on
NeoForge; this is a mod change for a later PR. The remaining gaps are mostly the code paths of the known issues (they are covered as soon
as the bugs are fixed), code that only runs on clients (`CommonLoader.createLootContext`, `RegistryUtil.getRegistryAccess`,
`IFluidHelper.getFluidTintColor`) or in production, and defensive code for fluids of other mods (e.g. fluids whose
`getBucket()` throws) that would need test fluids.

## CI

`.github/workflows/cicd-workflow.yml` runs build and game tests in one job (`gametest: true` of the shared build
workflow of cech12/MinecraftModActions), then the server start test of the release jars (Fabric, NeoForge). Publishing
on a tag needs both. `server-starttest-newest.yml` runs the server start test weekly with the newest versions.

## Log output

The game test logs only contain real problems and the "Known issue still present" warnings of the `KnownIssues`
markers. `ExpectedLogs` hides the errors that BucketLib logs for the test fluid whose `getBucket()` throws (it would
log a stack trace for every bucket when the creative tabs are built). REI is not loaded on the Fabric game test server
(it logs an error for every brewing recipe there); it stays in the client run. The generated IDE run configuration
"Fabric GameTest Server" uses the module classpath and still loads REI, so these errors can appear there.

## Not tested automatically

Client only code (bucket model/textures, fluid tint, JEI, REI, ModMenu config screen, `RegistryUtil.getRegistryAccess`)
cannot run on a game test server; check it manually in the client runs (test buckets of `bucketlibtest`).
Note: since Minecraft 26.3 dyeing needs a `minecraft:crafting_dye` recipe per bucket (the tag `#minecraft:dyeable` is
not enough), see `data/bucketlibtest/recipe/colored_bucket_dyed.json`.
