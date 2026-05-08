package de.cech12.bucketlib.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public class BucketLibTags {

    private BucketLibTags() {
        //avoid instantiating this class
    }

    /**
     * Call to force make sure this is all initialized
     */
    public static void init() {
        EntityTypes.init();
        Fluids.init();
    }

    public static class EntityTypes {

        public static final TagKey<EntityType<?>> MILKABLE = tag("milkable");

        private EntityTypes() {
            //avoid instantiating this class
        }

        private static void init() {
            //empty method to be called to initialize the static fields
        }

        private static TagKey<EntityType<?>> tag(@NotNull String name) {
            return TagKey.create(Registries.ENTITY_TYPE, BucketLib.id(name));
        }

    }

    public static class Fluids {

        public static final TagKey<Fluid> INFINITY_ENCHANTABLE = tag("infinity_enchantable");
        public static final TagKey<Fluid> NO_FLIPPING = tag("no_flipping");

        private Fluids() {
            //avoid instantiating this class
        }

        private static void init() {
            //empty method to be called to initialize the static fields
        }

        private static TagKey<Fluid> tag(@NotNull String name) {
            return TagKey.create(Registries.FLUID, BucketLib.id(name));
        }

    }

}
