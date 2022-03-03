package cech12.bucketlib.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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

        public static final TagKey<EntityType<?>> MILKABLE = tag("milkable");

        private static void init() {
        }

        private static TagKey<EntityType<?>> tag(@Nonnull String name) {
            return TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(BucketLibApi.MOD_ID, name));
        }

    }

    public static class Fluids {

        public static final TagKey<Fluid> INFINITY_ENCHANTABLE = tag("infinity_enchantable");

        private static void init() {
        }

        private static TagKey<Fluid> tag(@Nonnull String name) {
            return TagKey.create(Registry.FLUID_REGISTRY, new ResourceLocation(BucketLibApi.MOD_ID, name));
        }

    }

}
