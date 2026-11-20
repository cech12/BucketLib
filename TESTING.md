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
  KnownIssues                            marks known bugs ("deactivated" tests, see below)
  IGameTestPlatform                      loader specific operations (config, fluid API, ingredient sync, enchanting)
  tests/*Tests                           the tests, grouped by feature
common/src/gametest/resources/           test instances, environments, test mod assets/data
<loader>/src/gametest/                   thin adapter: test mod registration + IGameTestPlatform, loader specific recipes
```

The old `src/test` test mods were moved into the gametest source sets; the client runs load the test mod, so the
test buckets are still available for manual tests.

### Writing a new test

1. Add a method to the matching class in `tests/` and register it: `tests.add("group/my_test", Class::myTest);`.
2. Add `common/src/gametest/resources/data/bucketlib/test_instance/group/my_test.json` (copy an existing one;
   `infinity/enabled/*` uses the environment `bucketlib:infinity_enabled`). `meta/every_test_function_has_an_instance`
   fails if it is missing or orphaned.
3. A found bug is not fixed in a test change: wrap only its assertion in
   `KnownIssues.run(helper, "KI-<n> ...", <platforms>, () -> ...)` and list it below. The test passes while the bug
   exists and fails as soon as it is fixed, then remove the marker.

## Known issues (marked with `KnownIssues`)

| Id | Loader | Issue | Test |
|---|---|---|---|
| KI-1 | all | creative tab lists a milk bucket also for buckets that cannot milk | `registration/creative_tab_only_lists_possible_contents` |
| KI-2 | all | invalid content ids (e.g. from commands) crash `getBlock`/item name | `content/invalid_content_ids` |
| KI-3 | NeoForge (milk) | bucket filled with the milk fluid through the fluid API is no drinkable milk bucket | `milk/milk_fluid_of_other_mods_can_be_drunk` |
| KI-4 | Fabric | dispensers keep unusable buckets instead of dropping them like vanilla | `dispenser/drops_unusable_buckets_like_vanilla` |
| KI-5 | Fabric | dispensed milk bucket removes a water source in front of it | `dispenser/milk_bucket_does_not_remove_water` |
| KI-6 | all | Infinity does not prevent cracking by cracking fluids (operator precedence in `isCracked`) | `infinity/enabled/prevents_cracking` |
| KI-7 | all | furnace fuel checks (hopper, furnace menu) use client classes (`CommonLoader.createLootContext`) and crash on dedicated servers | `furnace/fuel_rules_work_on_a_dedicated_server` |
| KI-8 | all | fluid ingredient accepts buckets with water and an entity (entity is destroyed when crafting) | `ingredient/fluid_does_not_accept_buckets_with_entities` |
| KI-9 | all | bucket filling recipes fail to load if the first bucket of the ingredients cannot hold the content | `recipe/bucket_filling_with_several_buckets` |
| KI-10 | all (network: NeoForge) | json codec of block/entity/empty ingredients writes default registry entries for missing ids | `codec/tag_ingredients_json_round_trip`, `codec/ingredients_network_round_trip` |
| KI-11 | all | bucket filling recipes cannot be encoded (shapeless json, network) if not all of fluid/block/entity are set | `codec/bucket_filling_recipes_*_round_trip` |
| KI-12 | all | emptied bucket keeps the custom name of the released entity | `entity/keeps_entity_data` |
| KI-13 | all | entity/block ingredients with unknown ids crash the recipe matching | `ingredient/unknown_ids_match_nothing` |
| KI-14 | all | burning/freezing block lists created with `List.of` crash the inventory tick (`contains(null)`) | `temperature/normal_contents_do_nothing` |
| KI-15 | all | empty ingredient with an unknown item accepts every empty bucket | `ingredient/unknown_ids_match_nothing` |
| KI-16 | NeoForge | `BucketLibUtil.containsFluid/isEmpty` throw for empty stacks | `content/empty_stack_has_no_content` |
| KI-17 | NeoForge | fluid API fills fluids into buckets with a block (powder snow) | `fluid_api/does_not_fill_filled_buckets` |
| KI-18 | NeoForge | empty ingredient with item tag shows no buckets (recipe viewers) | `ingredient/empty_tag` |

All known issues are confirmed by the game test runs of all loaders (a marker fails as soon as its bug is fixed).
GitHub issues for them are not opened yet.

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
| Resources, setup | `resources/*`, `meta/*` | translations, textures, test buckets, cooking fuel, every test function has an instance, KnownIssues marker |

## Code coverage

Every game test run is measured with JaCoCo (only BucketLib classes):

| Report | Content |
|---|---|
| `<loader>/build/reports/jacoco/gametest/html/index.html` | common + loader specific classes of one loader |
| `build/reports/jacoco/gametest/html/index.html` (`./gradlew jacocoMergedGameTestReport`) | common classes, merged over all loaders |

Excluded: client only code (models, tints, JEI, REI, ModMenu) and mixin classes (their code is merged into Minecraft
classes, the tests check their behavior). On NeoForge `UniversalBucketItem` is changed by a mixin, so it is only
measured by the Fabric run. The remaining gaps are mostly the code paths of the known issues (they are covered as soon
as the bugs are fixed), code that only runs on clients (`CommonLoader.createLootContext`, `RegistryUtil.getRegistryAccess`,
`IFluidHelper.getFluidTintColor`) or in production, and defensive code for fluids of other mods (e.g. fluids whose
`getBucket()` throws) that would need test fluids.

## CI

`.github/workflows/cicd-workflow.yml` runs build and game tests in one job (`gametest: true` of the shared build
workflow of cech12/MinecraftModActions), then the server start test of the release jars (Fabric, NeoForge). Publishing
on a tag needs both. `server-starttest-newest.yml` runs the server start test weekly with the newest versions.

## Not tested automatically

Client only code (bucket model/textures, fluid tint, JEI, REI, ModMenu config screen, `RegistryUtil.getRegistryAccess`)
cannot run on a game test server; check it manually in the client runs (test buckets of `bucketlibtest`).
Note: since Minecraft 26.3 dyeing needs a `minecraft:crafting_dye` recipe per bucket (the tag `#minecraft:dyeable` is
not enough), see `data/bucketlibtest/recipe/colored_bucket_dyed.json`.
