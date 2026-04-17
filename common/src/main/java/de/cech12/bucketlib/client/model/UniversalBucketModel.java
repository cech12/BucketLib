package de.cech12.bucketlib.client.model;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.client.color.BucketFluidTint;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class UniversalBucketModel implements ItemModel {

    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_MAP = Maps.newHashMap();

    private static final Material MISSING_LOWER_CONTENT_MATERIAL = getBlockMaterial(getContentTexture(BucketLib.id("missing_lower_content")));
    private static final Material DEFAULT_FLUID_MASK = getBlockMaterial(getContentTexture(BucketLib.id("mask/bucket_fluid")));
    private static final Material UNIVERSAL_BUCKET_BASE = getBlockMaterial(getContentTexture(BucketLib.id("universal_bucket_base")));
    private static final Material UNIVERSAL_BUCKET_CRACKED_BASE = getBlockMaterial(getContentTexture(BucketLib.id("universal_bucket_cracked_base")));
    private static final Material UNIVERSAL_BUCKET_LOWER_BASE = getBlockMaterial(getContentTexture(BucketLib.id("universal_bucket_lower_base")));
    private static final Material UNIVERSAL_BUCKET_CRACKED_LOWER_BASE = getBlockMaterial(getContentTexture(BucketLib.id("universal_bucket_cracked_lower_base")));

    private final Unbaked unbakedModel;
    private final Map<String, ItemModel> cache = new IdentityHashMap<>(); // contains all the baked models since they'll never change

    private Integer upperBreakTemperature = null;
    private Integer lowerBreakTemperature = null;

    protected final BakingContext bakingContext;
    protected final BakedModel baseModel;

    protected UniversalBucketModel(UniversalBucketModel.Unbaked unbakedModel, BakingContext bakingContext, BakedModel baseModel) {
        this.unbakedModel = unbakedModel;
        this.bakingContext = bakingContext;
        this.baseModel = baseModel;
    }

    private static Material getBlockMaterial(ResourceLocation id) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, id);
    }

    private ItemModel bakeModelForFluid(Fluid fluid, ResourceLocation otherContent, boolean isCracked, boolean isLower) {
        var sprites = bakingContext.blockModelBaker().sprites();

        ItemTintSource bucketTint = (!unbakedModel.tints().isEmpty()) ? unbakedModel.tints().getFirst() : new Constant(-1);
        Material particleLocation = unbakedModel.textures().particle().map(UniversalBucketModel::getBlockMaterial).orElse(null);

        Material baseLocation;
        if (isLower) {
            if (isCracked) {
                baseLocation = unbakedModel.textures().crackedLowerBase().map(UniversalBucketModel::getBlockMaterial).orElse(UNIVERSAL_BUCKET_CRACKED_LOWER_BASE);
            } else {
                baseLocation = unbakedModel.textures().lowerBase().map(UniversalBucketModel::getBlockMaterial).orElse(UNIVERSAL_BUCKET_LOWER_BASE);
            }
        } else {
            if (isCracked) {
                baseLocation = unbakedModel.textures().crackedBase().map(UniversalBucketModel::getBlockMaterial).orElse(UNIVERSAL_BUCKET_CRACKED_BASE);
            } else {
                baseLocation = unbakedModel.textures().base().map(UniversalBucketModel::getBlockMaterial).orElse(UNIVERSAL_BUCKET_BASE);
            }
        }

        Material otherContentLocation = null;
        Material fluidLocation = null;
        Material fluidMaskLocation = null;
        if (otherContent != null) {
            otherContentLocation = UniversalBucketModel.getBlockMaterial(getContentTexture(otherContent));
        }
        if (fluid != Fluids.EMPTY) {
            fluidLocation = UniversalBucketModel.getBlockMaterial(getContentTexture(BuiltInRegistries.FLUID.getKey(fluid)));
            if (isCracked) {
                fluidMaskLocation = unbakedModel.textures().crackedFluidMask().map(UniversalBucketModel::getBlockMaterial).orElse(DEFAULT_FLUID_MASK);
            } else {
                fluidMaskLocation = unbakedModel.textures().fluidMask().map(UniversalBucketModel::getBlockMaterial).orElse(DEFAULT_FLUID_MASK);
            }
        }
        //oversteer fluid texture if available
        if (otherContentLocation == null && fluidLocation != null && !MissingTextureAtlasSprite.getLocation().equals(sprites.get(fluidLocation).contents().name())) {
            otherContentLocation = fluidLocation;
        }

        TextureAtlasSprite baseSprite = sprites.get(baseLocation);
        TextureAtlasSprite otherContentSprite = null;
        if (otherContentLocation != null) {
            otherContentSprite = sprites.get(otherContentLocation);
            //if content texture is missing - fallback to pink content texture
            if (MissingTextureAtlasSprite.getLocation().equals(otherContentSprite.contents().name())) {
                otherContentSprite = sprites.get(MISSING_LOWER_CONTENT_MATERIAL);
            }
        }
        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? Services.CLIENT.getFluidTextureMaterial(sprites, fluid) : null;
        TextureAtlasSprite particleSprite = particleLocation != null ? sprites.get(particleLocation) : null;
        if (particleSprite == null){
            particleSprite = baseSprite;
        }

        BakedModel specialModel = specialBaking(sprites, fluid, baseSprite, otherContentSprite, fluidSprite, particleSprite, fluidMaskLocation);

        return new BlockModelWrapper(specialModel, List.of(bucketTint, BucketFluidTint.INSTANCE));
    }

    abstract BakedModel specialBaking(SpriteGetter spriteGetter, Fluid fluid, TextureAtlasSprite baseSprite, TextureAtlasSprite otherContentSprite, TextureAtlasSprite fluidSprite, TextureAtlasSprite particleSprite, Material fluidMaskLocation);

    @Override
    public void update(@NotNull ItemStackRenderState renderState, ItemStack stack, @NotNull ItemModelResolver modelResolver, @NotNull ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity entity, int integer) {
        if (stack.getItem() instanceof UniversalBucketItem bucket) {
            boolean containsEntityType = false;
            String content = BucketLibUtil.getEntityTypeString(stack);
            if (content != null) {
                containsEntityType = true;
            } else {
                content = BucketLibUtil.getContentString(stack);
            }
            Fluid fluid = Fluids.EMPTY;
            if (content == null) {
                fluid = BucketLibUtil.getFluid(stack);
                ResourceLocation location = BuiltInRegistries.FLUID.getKey(fluid);
                content = !BuiltInRegistries.FLUID.getDefaultKey().equals(location) ? location.toString() : null;
            }
            //reset cache if temperature config changed
            if (!Objects.equals(upperBreakTemperature, bucket.getUpperBreakTemperature()) || !Objects.equals(lowerBreakTemperature, bucket.getLowerBreakTemperature())) {
                upperBreakTemperature = bucket.getUpperBreakTemperature();
                lowerBreakTemperature = bucket.getLowerBreakTemperature();
                cache.clear();
            }
            ItemModel bakedModel = cache.get(content);
            if (bakedModel == null) {
                boolean isCracked = bucket.isCracked(stack);
                bakedModel = this.bakeModelForFluid(fluid, ((content != null && fluid == Fluids.EMPTY) ? ResourceLocation.parse(content) : null), isCracked, containsEntityType);
                cache.put(content, bakedModel);
            }
            bakedModel.update(renderState, stack, modelResolver, displayContext, level, entity, integer);
        }

    }

    public static ResourceLocation getContentTexture(ResourceLocation otherContentLocation) {
        ResourceLocation texture = TEXTURE_MAP.get(otherContentLocation);
        if (texture == null) {
            String textureLocation = String.format("item/bucket_content/%s", otherContentLocation.getPath());
            texture = otherContentLocation.withPath(textureLocation);
            TEXTURE_MAP.put(otherContentLocation, texture);
        }
        return texture;
    }

    public record Unbaked(Textures textures, List<ItemTintSource> tints) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Textures.CODEC.fieldOf("textures").forGetter(Unbaked::textures), ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(Unbaked::tints))
                .apply(instance, Unbaked::new));

        private static final ResourceLocation ITEM_GENERATED_ID = ResourceLocation.withDefaultNamespace("item/generated");

        @Override
        @NotNull
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        @NotNull
        public ItemModel bake(@NotNull ItemModel.BakingContext bakingContext) {
            return Services.CLIENT.createItemModel(this, bakingContext, bakingContext.bake(ITEM_GENERATED_ID));
        }

        @Override
        public void resolveDependencies(@NotNull Resolver resolver) {
            resolver.resolve(ITEM_GENERATED_ID);
        }

        public record Textures(
                Optional<ResourceLocation> particle,
                Optional<ResourceLocation> base,
                Optional<ResourceLocation> lowerBase,
                Optional<ResourceLocation> fluidMask,
                Optional<ResourceLocation> crackedBase,
                Optional<ResourceLocation> crackedLowerBase,
                Optional<ResourceLocation> crackedFluidMask) {
            public static final Codec<Textures> CODEC = RecordCodecBuilder.<Textures>create(
                            instance -> instance
                                    .group(
                                            ResourceLocation.CODEC.optionalFieldOf("particle").forGetter(Textures::particle),
                                            ResourceLocation.CODEC.optionalFieldOf("base").forGetter(Textures::base),
                                            ResourceLocation.CODEC.optionalFieldOf("lowerBase").forGetter(Textures::lowerBase),
                                            ResourceLocation.CODEC.optionalFieldOf("fluidMask").forGetter(Textures::fluidMask),
                                            ResourceLocation.CODEC.optionalFieldOf("crackedBase").forGetter(Textures::crackedBase),
                                            ResourceLocation.CODEC.optionalFieldOf("crackedLowerBase").forGetter(Textures::crackedLowerBase),
                                            ResourceLocation.CODEC.optionalFieldOf("crackedFluidMask").forGetter(Textures::crackedFluidMask))
                                    .apply(instance, Textures::new))
                    .validate(textures -> {
                        if (textures.base.isPresent() && textures.lowerBase.isPresent()) {
                            return DataResult.success(textures);
                        }
                        return DataResult.error(() -> "Universal bucket model requires at least a base and lowerBase texture.");
                    });
        }

    }

}
