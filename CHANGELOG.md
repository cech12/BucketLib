# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Forge Recommended Versioning](https://mcforge.readthedocs.io/en/latest/conventions/versioning/).

## [1.21-4.1.0.0] - 2024-07-14
### Changed
- updated NeoForge to 21.0.94-beta
- the `config` directory is used for the default configuration (NeoForge)

### Fixed
- crashed on startup with NeoForge (caused by a breaking change in 21.0.82-beta)

## [1.21-4.0.0.6] - 2024-07-06
### Fixed
- fixed some issues with the FluidStorage interaction to be more compatible with other mods (Fabric/Quilt)

## [1.21-4.0.0.5] - 2024-07-02
### Fixed
- empty buckets disappeared after right-clicking a filled cauldron (Fabric) (thanks to ColinBashful for the report) #42

## [1.21-4.0.0.4] - 2024-07-01
### Fixed
- modded fluids (Dehydration) disappeared when right-clicked with an empty BucketLib bucket (Fabric) (thanks to ColinBashful for the report) https://github.com/cech12/WoodenBucket/issues/20

## [1.21-4.0.0.3] - 2024-06-24
### Fixed
- optimized bucket item model rendering to avoid lags in screens like JEI or EMI (thanks to truskawex for the report) https://github.com/cech12/WoodenBucket/issues/19

## [1.21-4.0.0.2] - 2024-06-22
### Fixed
- Water logged blocks were replaced when right-clicked with a custom water bucket (Fabric/Quilt) (thanks to Janbsh for the report) #41

## [1.21-4.0.0.1] - 2024-06-20
### Fixed
- damage bars of buckets were broken (Neoforge)
- data pack directories were not correct (all loaders)

## [1.21-4.0.0.0] - 2024-06-20
### Changed
- Updated to Minecraft 1.21 (Fabric 0.100.3+1.21, Neoforge 21.0.20-beta)
- (Forge support is still not available until the capability system is re-added)
- Updated Cloth Config support (15.0.127) (Fabric/Quilt)
- Updated ModMenu support (11.0.0) (Fabric/Quilt)
- Updated JEI support (19.0.0.9) (all loaders)
- Updated REI support (16.0.729) (Neoforge, Fabric/Quilt)
- Changed bucket registration for Neoforge: must be called during RegisterCapabilitiesEvent (already done in 1.20.6)

## [1.20.6-3.3.0.3] - 2024-06-19
### Fixed
- Entity buckets were missing in creative menu (Neoforge)
- Dispenser replaced multiple stacked buckets with only one filled bucket (Fabric/Quilt)

## [1.20.6-3.3.0.2] - 2024-06-19
### Fixed
- Data components were not registered correctly (Neoforge)
- Tags were not listed in language file (Fabric/Quilt, Neoforge)

## [1.20.6-3.3.0.1] - 2024-06-19
### Fixed
- JitPack failed to build

## [1.20.6-3.3.0.0] - 2024-06-18
### Changed
- Updated to Minecraft 1.20.6 (Fabric 0.98.0+1.20.6, Neoforge 20.6.119)
- Removed Forge support until the capability system is re-added
- Updated Cloth Config support (14.0.126) (Fabric/Quilt)
- Updated ModMenu support (10.0.0-beta.1) (Fabric/Quilt)
- Updated JEI support (18.0.0.62) (all loaders)
- Updated REI support (15.0.728) (Neoforge, Fabric/Quilt)
- Changed bucket registration for Neoforge: must be called during RegisterCapabilitiesEvent

### Fixed
- fixed known issue: max stack size was not taken into account and defaulted to 1 (Fabric)

## [1.20.4-3.2.1.0] - 2024-06-04
### Added
- added Roughly Enough Items (REI) support (version 14.1.727) (all loaders)

### Changed
- optimize UniversalBucketItem mixins in Forge & Neoforge

## [1.20.4-3.2.0.3] - 2024-05-11
### Fixed
- egg was missing in BucketLib cake recipe

## [1.20.4-3.2.0.2] - 2024-05-11
### Fixed
- added missing accesswidener to fix startup crash (Fabric)
- removed failing Java reflection to fix particle spawn

### Known issues
- Fabric: max stack size is not taken into account and defaults to 1

## [1.20.4-3.2.0.1] - 2024-05-11
### Fixed
- wrong package in mixin files of Jitpack repository

### Known issues
- Fabric: max stack size is not taken into account and defaults to 1

## [1.20.4-3.2.0.0] - 2024-05-10
### Added
- add Fabric (>=0.96.11+1.20.4) support (Fabric, Quilt)
- entity textures of Upgrade Aquatic mod (all loaders)

### Known issues
- Fabric: max stack size is not taken into account and defaults to 1

## [1.20.4-3.1.3.2] - 2024-02-27
### Fixed
- The result of "bucketlib:bucket_filling_shaped" and "bucketlib:bucket_filling_shapeless" recipes was not shown correctly in JEI (thanks to FreeFull for the report)

## [1.20.4-3.1.3.1] - 2024-02-26
### Fixed
- durability bar of buckets was not rendered
- breaking particles of buckets where not triggered

## [1.20.4-3.1.3.0] - 2024-02-20
### Added
- new recipe type "bucketlib:bucket_filling_shaped" to specify crafting recipes that fill buckets
- new recipe type "bucketlib:bucket_filling_shapeless" to specify crafting recipes that fill buckets

### Fixed
- ingredients of type "bucketlib:empty" detected some not intended items

## [1.20.4-3.1.2.1] - 2024-02-12
### Fixed
- startup crash when starting in Forge environment was fixed (thanks to Ellesmera for the report)

## [1.20.4-3.1.2.0] - 2024-01-31
### Added
- new ingredient type "bucketlib:block" to specify buckets with blocks in a recipe
- new ingredient type "bucketlib:entity" to specify buckets with entities in a recipe

### Fixed
- JEI showed vanilla bucket in recipes of specific "bucketlib:empty" ingredients (thanks to FreeFull for the report)

## [1.20.4-3.1.1.0] - 2024-01-31
### Added
- new ingredient type "bucketlib:empty" to specify an empty bucket in a recipe (thanks to FreeFull for the idea) #34

## [1.20.4-3.1.0.1] - 2024-01-29
### Fixed
- Fluid capability in NeoForge was not working correctly 

## [1.20.4-3.1.0.0] - 2024-01-29
### Changed
- Update to Minecraft 1.20.4 (Forge 49.0.22, Neoforge 20.4.138-beta)
- Update JEI support to 17.3.0.43 (Forge & Neoforge)

## [1.20.2-3.0.0.5] - 2024-01-04
### Fixed
- powder snow buckets were missing
- entity buckets could not be emptied using a dispenser

## [1.20.2-3.0.0.4] - 2024-01-03
### Fixed
- Fixed that version didn't contain the minecraft version

## [1.20.2-3.0.0.3] - 2024-01-02
### Changed
- Another fix for jar building in JitPack

## [1.20.2-3.0.0.2] - 2024-01-02
### Changed
- fixed building jars in JitPack

## [1.20.2-3.0.0.1] - 2024-01-02
### Changed
- fixed building api jar

## [1.20.2-3.0.0.0] - 2023-12-31
### Changed
- Move to Multiloader mod template to support Forge and Neoforge
- Update to Minecraft 1.20.2 (Forge 48.1.0)
- all classes were moved from cech12.bucketlib to de.cech12.bucketlib package
- changed the property methods with forge config objects to suppliers 

### Removed
- removed deprecated property methods: blockedFluids, blockedEntities, blockedBlocks (please use the deniedX methods instead)

## [1.20.2-2.4.0.4] - 2023-11-05
### Fixed
- dedicated servers could not start with version 1.20.2-2.4.0.3

## [1.20.2-2.4.0.3] - 2023-11-04
### Fixed
- MobBucketItems with non-bucketable entities were added to creative menu (found in Naturalist mod 4.0)
- milk bucket effect curing was triggered before advancement trigger (see https://github.com/neoforged/NeoForge/pull/170)

## [1.20.2-2.4.0.2] - 2023-10-30
### Fixed
- Spinefish texture of Aquamirea mod was not rendered correctly

## [1.20.2-2.4.0.1] - 2023-10-16
### Fixed
- Milk string was not translated correctly

## [1.20.2-2.4.0.0] - 2023-10-16
### Changed
- update and move back to Forge 1.20.2-48.0.23 (from NeoForge) until it is stable
- deactivate game tests, because they are not working yet

## [1.20.1-2.3.0.1] - 2023-09-10
### Fixed
- textures of catfish and cosmic cod (Alex's Mobs) were not rendered correctly
- cracked bucket texture was not used for stradpole (Alex's Mobs) bucket texture

## [1.20.1-2.3.0.0] - 2023-08-09
### Changed
- Changed Forge to NeoForge 1.20.1-47.1.54 (compatible with Forge 47.1.0)
- Updated compat with JEI to 1.20.1-15.2.0.23

## [1.20.1-2.2.0.2] - 2023-07-30
### Fixed
- Milk interaction and cauldron interaction in creative mode added a vanilla bucket to the inventory (thanks to Sinhika for the report) #28

## [1.20.1-2.2.0.1] - 2023-07-25
### Fixed
- Version of mod was wrong (thanks to Sinhika for the report) #27

## [1.20.1-2.2.0.0] - 2023-07-06
### Changed
- update mod to Forge 1.20.1-47.0.49 to fix interaction with already waterlogged blocks

### Removed
- Snail texture of Naturalist mod, because the mod authors added it by themselves

## [1.20.1-2.1.0.0] - 2023-06-14
### Changed
- update mod to Forge 1.20.1-47.0.1
- update JEI support to version 15.0.0.12

### Fixed
- placing axolotls in 1.20.1 crashed the game

## [1.20-2.0.0.0] - 2023-06-14
### Changed
- update mod to Forge 1.20-46.0.12 #25
- update JEI support to version 14.0.0.11 #25

## [1.19.3-1.2.0.0] - 2023-06-03
### Added
- feeding Axolotls with buckets of tropical fish (or other mob buckets listed in "minecraft:axolotl_tempt_items" item tag) is possible now

### Fixed
- fix waterlogging blocks

## [1.19.3-1.1.2.2] - 2023-05-20
### Fixed
- revert JEI version fix from 1.19.3-1.1.1.1, because "REI Plugin Compatibilities (REIPC)" version 10.0.49 supports JEI 12 now

## [1.19.3-1.1.2.1] - 2023-02-19
### Added
- recipe for dough of Farmer's Delight (thanks to VivaGabe for the hint)

## [1.19.3-1.1.2.0] - 2023-02-06
### Added
- Gwibling texture of The Undergarden mod
- Snail texture of Naturalist mod #24 (temporary until Naturalist added it by themselves)

### Fixed
- fix bucket rendering when bucket entity texture of other mod is missing #24

## [1.19.3-1.1.1.1] - 2023-01-18
### Fixed
- fix startup issue with mod "REI Plugin Compatibilities (REIPC)" version 10.0.45 (thanks to subsonicer for the report)

## [1.19.3-1.1.1.0] - 2023-01-17
### Changed
- re-add JEI support

## [1.19.3-1.1.0.1] - 2023-01-09
### Fixed
- flipped buckets were not rendered at correct spot (thanks to benbenlaw for the report)

## [1.19.3-1.1.0.0] - 2022-12-31
### Changed
- Update mod to Forge 1.19.3-44.0.41
- Temporary deactivation of JEI until it is ported to 1.19.3

## [1.19-1.0.3.2] - 2022-08-30
### Fixed
- fluid handler of milk bucket didn't work with milk fluid of Forge #22 (thanks to TimeheroTH for the report)

## [1.19-1.0.3.1] - 2022-08-27
### Fixed
- fluid handler of milk bucket interacted with other fluids as if it was empty #22 (thanks to TimeheroTH for the report)

## [1.19-1.0.3.0] - 2022-07-24
### Changed
- Update mod to Forge 1.19-41.0.105 to fix translucent water buckets #19

## [1.19-1.0.2.0] - 2022-07-14
### Changed
- Update mod to Forge 1.19-41.0.96 to fix startup crashes since Forge 1.19-41.0.94

## [1.19-1.0.1.0] - 2022-07-09
### Changed
- Updated mod to Forge 1.19-41.0.76 to fix startup crash since Forge 1.19-41.0.64 #18

## [1.19-1.0.0.0] - 2022-07-06
### Changed
- Updated mod to Forge 1.19-41.0.62 #16
- support tadpole buckets #16

### Fixed
- fixed advancements when obtaining entities (axolotl, fish, tadpole, ...)

## [1.18.2-0.5.3.6] - 2022-06-30
### Fixed
- fixed incompatibility with KubeJS recipes #17 (thanks to benbenlaw for the report)

## [1.18.2-0.5.3.5] - 2022-06-30
### Fixed
- fixed crafting bug with empty buckets #17 (thanks to benbenlaw for the report)
- fixed that fluid handler could drain the fluid out of entity buckets

### Removed
- removed bucket content textures of Aquaculture 2 mod. They are supported out of the box since Aquaculture-1.18.2-2.3.7.
- removed bucket content textures of The Undergarden mod

## [1.18.2-0.5.3.4] - 2022-05-27
### Fixed
- fixed adding empty NBT tag to items in crafting grid #13 (thanks to Peca21 for the report)

## [1.18.2-0.5.3.3] - 2022-05-12
### Fixed
- fixed potion effect curing after drinking milk #12 (thanks to AngleWyrm10 for the report)

## [1.18.2-0.5.3.2] - 2022-05-12
### Fixed
- fix jitpack build

## [1.18.2-0.5.3.1] - 2022-05-06
### Fixed
- hopefully fix jitpack build

## [1.18.2-0.5.3.0] - 2022-05-05
### Changed
- rename "block" properties to "deny" properties ("blockedX" methods are deprecated and will be removed soon)

### Added
- Flipped bucket model if the bucket contains a fluid that is lighter than air

### Fixed
- Fixed that the breaking sound does not play when the bucket breaks #10
- Fixed a bug where a bucket with an entity could have the burn time of its fluid.

## [1.18.2-0.5.2.1] - 2022-03-30
### Fixed
- FluidIngredient throws errors on clientside when it contained a fluid #8

### Removed
- unused textures

## [1.18.2-0.5.2.0] - 2022-03-23
### Changed
- Update to Forge 40.0.18

### Fixed
- Fixed crash on startup when using allow & deny tags
- Interacting on powder snow with a stack of buckets duplicated the stack

## [1.18.2-0.5.1.1] - 2022-03-22
### Changed
- simplify code of durability bar (Forge 40.0.8)
- added first game tests

### Fixed
- bucket disappeared when milking a cow or goat #7 (thanks to AlitaTeal for the report)

## [1.18.2-0.5.1.0] - 2022-03-03
### Changed
- Update to 1.18.2

## [1.18.1-0.5.0.1] - 2022-03-01
### Fixed
- fixed possible startup crash when another mod uses tags in `issame` method of fluid #6

### Removed
- Quicksand mod added reusable textures -> removed own textures

## [1.18.1-0.5.0.0] - 2022-02-22
### Changed
- Move bucket content textures to mod side to let mods add & change their bucket content textures #4

## [1.18.1-0.4.1.2] - 2022-02-18
### Changed
- Updated bucket content textures of Alex's Mobs mod #1

### Fixed
- fixed support of mob buckets without fluid #1

## [1.18.1-0.4.1.1] - 2022-02-16
### Fixed
- Jitpack Maven publishing fixed

## [1.18.1-0.4.1.0] - 2022-02-16
### Added
- Option to define burning & freezing effect for fluid temperatures, fluids & blocks #3

## [1.18.1-0.4.0.0] - 2022-02-03
### Added
- Option to define durability for buckets #2

### Fixed
- Milk fluid emptying did not work correctly via FluidHandler
- Milking entities with a stack of buckets did not work as intended
- Interact with a caldron with a stack of buckets did not work as intended

## [1.18.1-0.3.2.0] - 2022-01-29
### Fixed
- Textures of entity buckets of other mods did not work correctly
- Milk bucket was not registered correctly when a mod enables milk fluid

## [1.18.1-0.3.1.0] - 2022-01-28
### Added
- add dispense behaviour for buckets filled with entities or blocks 

### Fixed
- filled buckets could be stacked in special cases

## [1.18.1-0.3.0.3] - 2022-01-27
### Fixed
- Jitpack publishing fixed

## [1.18.1-0.3.0.1] - 2022-01-26
### Added
- add mod logo

## [1.18.1-0.3.0.0] - 2022-01-26
### Added
- add bucket content textures of Alex Mobs, Aquaculture, Quark, Quicksand, The Undergarden
- add recipes for cake & for the mods Aquaculture (Turtle Soup) and Create (dough)

## [1.18.1-0.2.2.0] - 2022-01-25
### Fixed
- library mod could not be started because the mods.toml was wrong

## [1.18.1-0.2.1.0] - 2022-01-25
### Fixed
- fixed java version for Jitpack Maven publishing

## [1.18.1-0.2.0.0] - 2022-01-25
### Fixed
- Jitpack Maven publishing fixed

## [1.18.1-0.1.0.0] - 2022-01-24
### Changed
- Initial version
- Highly configurable UniversalBucketItem (cracking; color; allow/blocking fluids, entities, blocks)
- infinity enchantment can be enabled in lib config
- UniversalBucketItem can contain fluids, milk, fish entities & powder snow.
- UniversalBucketItem has a dispense behaviour for fluids.
- UniversalBucketItem can interact with Cauldrons
- UniversalBucketItem can milk entities (cow, mooshroom, goat)
