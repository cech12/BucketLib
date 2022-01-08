package cech12.bucketlib.api;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nonnull;

public class BucketLibTags {

    /**
     * Call to force make sure this is all initialized
     */
    public static void init() {
        EntityTypes.init();
    }

    public static class EntityTypes {

        public static final Tag.Named<EntityType<?>> MILKABLE = tag("milkable");

        private static void init() {
        }

        private static Tag.Named<EntityType<?>> tag(@Nonnull String name) {
            return EntityTypeTags.bind(BucketLibApi.MOD_ID + ":" + name);
        }

    }

}
