# BucketLib

[![Curseforge](http://cf.way2muchnoise.eu/full_bucketlib_downloads(0D0D0D-F16436-fff-010101-fff).svg)](https://www.curseforge.com/minecraft/mc-mods/bucketlib)
[![Curseforge](http://cf.way2muchnoise.eu/versions/For%20MC_bucketlib_all(0D0D0D-F16436-fff-010101).svg)](https://www.curseforge.com/minecraft/mc-mods/bucketlib/files)
[![CI/CD](https://github.com/cech12/BucketLib/actions/workflows/cicd-workflow.yml/badge.svg)](https://github.com/cech12/BucketLib/actions/workflows/cicd-workflow.yml)
[![License](https://img.shields.io/github/license/cech12/BucketLib)](http://opensource.org/licenses/MIT)
[![](https://img.shields.io/discord/752506676719910963.svg?style=flat&color=informational&logo=discord&label=Discord)](https://discord.gg/gRUFH5t)

BucketLib is a **Minecraft Forge** library mod for developers. The purpose is to provide functionality for developers to add their own buckets 
without having trouble implementing all special cases.

## Features

- **Highly configurable buckets**:
  - Obtaining fluids, entities and blocks can be limited by using allow lists and block lists or by using the fluid temperature.
  - Buckets can be configured to crack when a fluid temperature exceeds a special value.
  - Coloring a bucket can be enabled
  - Milking entities can be disabled
- **Compatible with all fluids**: Water and Lava as well as all modded fluids are supported by buckets generated with this library mod. 
- **Entities can be obtained**: Entities like Axolotl, fish and mobs of other mods can be obtained.
- **Bucketable Blocks can be obtained**: Powder Snow and bucketable blocks of other mods can be obtained.
- **Compatible with all milk special cases**: Entities like cows and goats can be milked and the milk is drinkable. Buckets filled with milk are also compatible with mods that contains a milk fluid.
- **Dispense Behaviour**:

## Adding it to your project:

[![](https://jitpack.io/v/cech12/bucketlib.svg)](https://jitpack.io/#cech12/bucketlib)

Add the following to your `build.gradle` file:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly fg.deobf("com.github.cech12:bucketlib:${project.bucketlib_version}:api")
    runtimeOnly fg.deobf("com.github.cech12:bucketlib:${project.bucketlib_version}")
}
```

Replace `${project.bucketlib_version}` with the version of BucketLib that you want to use. The actual versions can be found on the Github Releases page.

For detailed information please see the [Developer Guide](https://github.com/cech12/BucketLib/wiki/Developer-Guide).