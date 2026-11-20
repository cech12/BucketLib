package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.platform.Services;

import java.util.Set;

/**
 * Platform names of the loaders (see IPlatformHelper#getPlatformName), for tests that intentionally expect different
 * behavior per loader and for {@link KnownIssues}.
 */
public final class Platforms {

    /** platform name of Fabric */
    public static final String FABRIC = "Fabric";
    /** platform name of NeoForge */
    public static final String NEOFORGE = "NeoForge";
    /** all loaders BucketLib is built for (Forge is currently not part of the build, see settings.gradle) */
    public static final Set<String> ALL = Set.of(FABRIC, NEOFORGE);

    private Platforms() {}

    /** @return platform name of the running loader */
    public static String current() {
        return Services.PLATFORM.getPlatformName();
    }

    /** @return true if the tests run on Fabric */
    public static boolean isFabric() {
        return FABRIC.equals(current());
    }

    /** @return true if the tests run on NeoForge */
    public static boolean isNeoForge() {
        return NEOFORGE.equals(current());
    }

}
