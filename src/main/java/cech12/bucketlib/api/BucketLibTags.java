package cech12.bucketlib.api;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nonnull;

public class BucketLibTags {

    /**
     * Call to force make sure this is all initialized
     */
    public static void init() {
        EntityTypes.init();
        Fluids.init();
    }

    public static class EntityTypes {

        public static final Tag.Named<EntityType<?>> MILKABLE = tag("milkable");

        private static void init() {
        }

        private static Tag.Named<EntityType<?>> tag(@Nonnull String name) {
            return EntityTypeTags.bind(BucketLibApi.MOD_ID + ":" + name);
        }

    }

    public static class Fluids {

        public static final Tag.Named<Fluid> INFINITY_ENCHANTABLE = tag("infinity_enchantable");

        private static void init() {
        }

        private static Tag.Named<Fluid> tag(@Nonnull String name) {
            return FluidTags.bind(BucketLibApi.MOD_ID + ":" + name);
        }

    }

}
