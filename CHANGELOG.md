# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Forge Recommended Versioning](https://mcforge.readthedocs.io/en/latest/conventions/versioning/).

## [1.18.2-0.5.1.1] - 2022-03-??
### Changed
- added first game tests

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
