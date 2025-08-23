package de.cech12.bucketlib.client.model;

import com.google.common.collect.Maps;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.UnbakedCompositeModel;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This implementation is based on net.neoforged.neoforge.client.model.DynamicFluidContainerModel.
 * Multiple changes were done to simplify the class
 */
public class UniversalBucketModel implements ItemModel {

    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_MAP = Maps.newHashMap();
    // Depth offsets to prevent Z-fighting
    private static final Transformation DEPTH_OFFSET_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());

    private static final Material MISSING_LOWER_CONTENT_MATERIAL = ClientHooks.getBlockMaterial(getContentTexture(BucketLib.id("missing_lower_content")));

    private final Unbaked unbakedModel;
    private final BakingContext bakingContext;
    private final ItemTransforms itemTransforms;
    private final Map<String, ItemModel> cache = new IdentityHashMap<>(); // contains all the baked models since they'll never change

    private Integer upperBreakTemperature = null;
    private Integer lowerBreakTemperature = null;

    public UniversalBucketModel(@NotNull Unbaked unbakedModel, @NotNull BakingContext bakingContext) {
        this.unbakedModel = unbakedModel;
        this.bakingContext = bakingContext;
        var baseItemModel = bakingContext.blockModelBaker().getModel(ResourceLocation.withDefaultNamespace("item/generated"));
        if (baseItemModel == null) {
            throw new IllegalStateException("Failed to access item/generated model");
        }
        this.itemTransforms = baseItemModel.getTransforms();
    }

    private static RenderTypeGroup getLayerRenderTypes(boolean unlit) {
        return new RenderTypeGroup(RenderType.translucent(), unlit ? NeoForgeRenderTypes.ITEM_UNSORTED_UNLIT_TRANSLUCENT.get() : NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
    }

    private ItemModel bakeModelForFluid(Fluid fluid, ResourceLocation otherContent, boolean isCracked, boolean isLower) {
        var sprites = bakingContext.blockModelBaker().sprites();

        Material particleLocation = unbakedModel.textures.particle.map(ClientHooks::getBlockMaterial).orElse(null);

        Material baseLocation = null;
        if (isLower) {
            if (isCracked && unbakedModel.textures.crackedLowerBase.isPresent()) {
                baseLocation = unbakedModel.textures.crackedLowerBase.map(ClientHooks::getBlockMaterial).orElse(null);
            }
            if (baseLocation == null && unbakedModel.textures.lowerBase.isPresent()) {
                baseLocation = unbakedModel.textures.lowerBase.map(ClientHooks::getBlockMaterial).orElse(null);
            }
        } else {
            if (isCracked && unbakedModel.textures.crackedBase.isPresent()) {
                baseLocation = unbakedModel.textures.crackedBase.map(ClientHooks::getBlockMaterial).orElse(null);
            }
            if (baseLocation == null && unbakedModel.textures.base.isPresent()) {
                baseLocation = unbakedModel.textures.base.map(ClientHooks::getBlockMaterial).orElse(null);
            }
        }

        Material otherContentLocation = null;
        Material fluidLocation = null;
        Material fluidMaskLocation = null;
        if (otherContent != null) {
            otherContentLocation = ClientHooks.getBlockMaterial(getContentTexture(otherContent));
        }
        if (fluid != Fluids.EMPTY) {
            fluidLocation = ClientHooks.getBlockMaterial(getContentTexture(BuiltInRegistries.FLUID.getKey(fluid)));
            if (isCracked && unbakedModel.textures.crackedFluidMask.isPresent()) {
                fluidMaskLocation = unbakedModel.textures.crackedFluidMask.map(ClientHooks::getBlockMaterial).orElse(null);
            }
            if (fluidMaskLocation == null && unbakedModel.textures.fluidMask.isPresent()) {
                fluidMaskLocation = unbakedModel.textures.fluidMask.map(ClientHooks::getBlockMaterial).orElse(null);
            }
        }
        //oversteer fluid texture if available
        if (otherContentLocation == null && fluidLocation != null && !MissingTextureAtlasSprite.getLocation().equals(sprites.get(fluidLocation).contents().name())) {
            otherContentLocation = fluidLocation;
        }

        TextureAtlasSprite baseSprite = baseLocation != null ? sprites.get(baseLocation) : null;
        TextureAtlasSprite otherContentSprite = null;
        if (otherContentLocation != null) {
            otherContentSprite = sprites.get(otherContentLocation);
            //if content texture is missing - fallback to pink content texture
            if (MissingTextureAtlasSprite.getLocation().equals(otherContentSprite.contents().name())) {
                otherContentSprite = sprites.get(MISSING_LOWER_CONTENT_MATERIAL);
            }
        }
        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? sprites.get(ClientHooks.getBlockMaterial(IClientFluidTypeExtensions.of(fluid).getStillTexture())) : null;
        TextureAtlasSprite particleSprite = particleLocation != null ? sprites.get(particleLocation) : null;
        if (particleSprite == null) particleSprite = baseSprite;
        if (particleSprite == null) particleSprite = otherContentSprite;
        if (particleSprite == null) particleSprite = fluidSprite;

        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        ModelState state = BlockModelRotation.X0_Y0;
        if (fluid != Fluids.EMPTY && !fluid.defaultFluidState().is(BucketLibTags.Fluids.NO_FLIPPING) && fluid.getFluidType().isLighterThanAir()) {
            state = new SimpleModelState(
                    state.getRotation().compose(
                            new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null)));
        }

        // We need to disable GUI 3D and block lighting for this to render properly
        var modelBuilder = UnbakedCompositeModel.Baked.builder(true, false, false, particleSprite, itemTransforms);

        var normalRenderTypes = getLayerRenderTypes(false);

        if (baseSprite != null) {
            // build base (insidest)
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, baseSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, $ -> baseSprite, state);
            modelBuilder.addQuads(normalRenderTypes, quads);
        }

        if (otherContentSprite != null) {
            //layer 2 to avoid coloring the entity layer
            var transformedState = new SimpleModelState(state.getRotation().compose(DEPTH_OFFSET_TRANSFORM), state.isUvLocked());
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(2, otherContentSprite);
            TextureAtlasSprite finalOtherContentSprite = otherContentSprite;
            var quads = UnbakedElementsHelper.bakeElements(unbaked, $ -> finalOtherContentSprite, transformedState);
            modelBuilder.addQuads(normalRenderTypes, quads);
        } else if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = sprites.get(fluidMaskLocation);
            // build liquid layer (inside)
            var transformedState = new SimpleModelState(state.getRotation().compose(DEPTH_OFFSET_TRANSFORM), state.isUvLocked());
            var unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(1, templateSprite); // Use template as mask
            var quads = UnbakedElementsHelper.bakeElements(unbaked, $ -> fluidSprite, transformedState); // Bake with fluid texture

            var emissive = fluid.getFluidType().getLightLevel() > 0;
            var renderTypes = getLayerRenderTypes(emissive);
            if (emissive) QuadTransformers.settingEmissivity(fluid.getFluidType().getLightLevel()).processInPlace(quads);

            modelBuilder.addQuads(renderTypes, quads);
        }

        modelBuilder.setParticle(particleSprite);

        return new BlockModelWrapper(modelBuilder.build(), List.of(new Constant(-1), FluidContentsTint.INSTANCE));
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity entity, int p_387820_) {
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
                content = (location != BuiltInRegistries.FLUID.getDefaultKey()) ? location.toString() : null;
            }
            //reset cache if temperature config changed
            if (!Objects.equals(upperBreakTemperature, bucket.getUpperBreakTemperature()) || !Objects.equals(lowerBreakTemperature, bucket.getLowerBreakTemperature())) {
                upperBreakTemperature = bucket.getUpperBreakTemperature();
                lowerBreakTemperature = bucket.getLowerBreakTemperature();
                cache.clear();
            }
            ItemModel bakedModel = cache.get(content);
            if (bakedModel == null && content != null) {
                boolean isCracked = bucket.isCracked(stack);
                bakedModel = this.bakeModelForFluid(fluid, ResourceLocation.parse(content), isCracked, containsEntityType);
                cache.put(content, bakedModel);
            }
            bakedModel.update(renderState, stack, modelResolver, displayContext, level, entity, p_387820_);
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
                    if (textures.base.isPresent() && textures.fluidMask.isPresent() && textures.lowerBase.isPresent()) {
                        return DataResult.success(textures);
                    }
                    return DataResult.error(() -> "Universal bucket model requires at least a base, fluidMask and lowerBase texture.");
                });
    }

    public record Unbaked(Textures textures) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Textures.CODEC.fieldOf("textures").forGetter(Unbaked::textures))
                .apply(instance, Unbaked::new));

        @Override
        @NotNull
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        @NotNull
        public ItemModel bake(@NotNull BakingContext bakingContext) {
            return new UniversalBucketModel(this, bakingContext);
        }

        @Override
        public void resolveDependencies(@NotNull Resolver resolver) {

            //No dependencies
        }
    }

}
