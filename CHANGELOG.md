# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Forge Recommended Versioning](https://mcforge.readthedocs.io/en/latest/conventions/versioning/).

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
