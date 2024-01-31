# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Forge Recommended Versioning](https://mcforge.readthedocs.io/en/latest/conventions/versioning/).

## [1.20.1-2.3.1.0] - 2024-01-31
### Added
- new ingredient type "bucketlib:empty" to specify an empty bucket in a recipe (thanks to FreeFull for the idea) #34

## [1.20.1-2.3.0.5] - 2023-11-05
### Fixed
- MobBucketItems were not loaded correctly in version 1.20.1-2.3.0.4

## [1.20.1-2.3.0.4] - 2023-11-04
### Fixed
- dedicated servers could not start with version 1.20.1-2.3.0.3

## [1.20.1-2.3.0.3] - 2023-11-04
### Fixed
- MobBucketItems with non-bucketable entities were added to creative menu (found in Naturalist mod 4.0)

## [1.20.1-2.3.0.2] - 2023-10-30
### Fixed
- Milk string was not translated correctly https://github.com/cech12/CeramicBucket/issues/74
- Spinefish texture of Aquamirea mod was not rendered correctly

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
