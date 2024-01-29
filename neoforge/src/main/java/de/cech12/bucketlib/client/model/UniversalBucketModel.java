package de.cech12.bucketlib.client.model;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.StandaloneGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

/**
 * This implementation is based on net.neoforged.neoforge.client.model.DynamicFluidContainerModel.
 * Multiple changes were done to simplify the class
 */
public class UniversalBucketModel implements IUnbakedGeometry<UniversalBucketModel> {

    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_MAP = Maps.newHashMap();
    // Depth offsets to prevent Z-fighting
    private static final Transformation DEPTH_OFFSET_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());
    // Transformer to set quads to max brightness
    private static final IQuadTransformer MAX_LIGHTMAP_TRANSFORMER = QuadTransformers.applyingLightmap(0x00F000F0);

    private static final Material MISSING_LOWER_CONTENT_MATERIAL = new Material(InventoryMenu.BLOCK_ATLAS, getContentTexture(new ResourceLocation(BucketLib.MOD_ID, "missing_lower_content")));

    @Nonnull
    private final Fluid fluid;
    @Nullable
    private final ResourceLocation otherContent;

    private final boolean isCracked;
    private final boolean isLower;

    public UniversalBucketModel(@Nonnull Fluid fluid, @Nullable ResourceLocation otherContent, boolean isCracked, boolean isLower) {
        this.fluid = fluid;
        this.otherContent = otherContent;
        this.isCracked = isCracked;
        this.isLower = isLower;
    }

    /**
     * Returns a new UniversalBucketModel with the given fluid.
     */
    public UniversalBucketModel withFluid(Fluid newFluid, boolean isCracked) {
        return new UniversalBucketModel(newFluid, null, isCracked, false);
    }

    /**
     * Returns a new UniversalBucketModel with the given other content.
     */
    public UniversalBucketModel withOtherContent(ResourceLocation otherContent, boolean isCracked, boolean isLower) {
        return new UniversalBucketModel(Fluids.EMPTY, otherContent, isCracked, isLower);
    }

    public static ResourceLocation getContentTexture(ResourceLocation otherContentLocation) {
        ResourceLocation texture = TEXTURE_MAP.get(otherContentLocation);
        if (texture == null) {
            String textureLocation = String.format("item/bucket_content/%s", otherContentLocation.getPath());
            texture = new ResourceLocation(otherContentLocation.getNamespace(), textureLocation);
            TEXTURE_MAP.put(otherContentLocation, texture);
        }
        return texture;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
        Material particleLocation = owner.hasMaterial("particle") ? owner.getMaterial("particle") : null;

        Material baseLocation = null;
        if (this.isLower) {
            if (this.isCracked && owner.hasMaterial("crackedLowerBase")) {
                baseLocation = owner.getMaterial("crackedLowerBase");
            }
            if (baseLocation == null && owner.hasMaterial("lowerBase")) {
                baseLocation = owner.getMaterial("lowerBase");
            }
        } else {
            if (this.isCracked && owner.hasMaterial("crackedBase")) {
                baseLocation = owner.getMaterial("crackedBase");
            }
            if (baseLocation == null && owner.hasMaterial("base")) {
                baseLocation = owner.getMaterial("base");
            }
        }

        Material otherContentLocation = null;
        Material fluidMaskLocation = null;
        if (this.otherContent != null) {
            otherContentLocation = new Material(InventoryMenu.BLOCK_ATLAS, getContentTexture(this.otherContent));
        } else if (this.fluid != Fluids.EMPTY) {
            if (this.isCracked && owner.hasMaterial("crackedFluidMask")) {
                fluidMaskLocation = owner.getMaterial("crackedFluidMask");
            }
            if (fluidMaskLocation == null && owner.hasMaterial("fluidMask")) {
                fluidMaskLocation = owner.getMaterial("fluidMask");
            }
        }

        TextureAtlasSprite baseSprite = baseLocation != null ? spriteGetter.apply(baseLocation) : null;
        TextureAtlasSprite otherContentSprite = null;
        if (otherContentLocation != null) {
            otherContentSprite = spriteGetter.apply(otherContentLocation);
            //if content texture is missing - fallback to pink content texture
            if (MissingTextureAtlasSprite.getLocation().equals(otherContentSprite.contents().name())) {
                otherContentSprite = spriteGetter.apply(MISSING_LOWER_CONTENT_MATERIAL);
            }
        }
        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ClientHooks.getBlockMaterial(IClientFluidTypeExtensions.of(fluid).getStillTexture())) : null;
        TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;
        if (particleSprite == null) particleSprite = baseSprite;
        if (particleSprite == null) particleSprite = otherContentSprite;
        if (particleSprite == null) particleSprite = fluidSprite;

        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        if (fluid != Fluids.EMPTY && fluid.getFluidType().isLighterThanAir()) {
            modelState = new SimpleModelState(
                    modelState.getRotation().compose(
                            new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null)));
        }

        // We need to disable GUI 3D and block lighting for this to render properly
        var itemContext = StandaloneGeometryBakingContext.builder(owner).withGui3d(false).withUseBlockLight(false).build(modelLocation);
        var modelBuilder = CompositeModel.Baked.builder(itemContext, particleSprite, new ContainedFluidOverrideHandler(overrides, baker, itemContext, this), owner.getTransforms());

        var normalRenderTypes = DynamicFluidContainerModel.getLayerRenderTypes(false);

        if (baseSprite != null) {
            // build base (insidest)
            var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite);
            var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> baseSprite, modelState, modelLocation);
            modelBuilder.addQuads(normalRenderTypes, quads);
        }

        if (otherContentSprite != null) {
            //layer 2 to avoid coloring the entity layer
            var transformedState = new SimpleModelState(modelState.getRotation().compose(DEPTH_OFFSET_TRANSFORM), modelState.isUvLocked());
            var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(2, otherContentSprite);
            TextureAtlasSprite finalOtherContentSprite = otherContentSprite;
            var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> finalOtherContentSprite, transformedState, modelLocation);
            modelBuilder.addQuads(normalRenderTypes, quads);
        } else if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null) {
                // build liquid layer (inside)
                var transformedState = new SimpleModelState(modelState.getRotation().compose(DEPTH_OFFSET_TRANSFORM), modelState.isUvLocked());
                var unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(1, templateSprite); // Use template as mask
                var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> fluidSprite, transformedState, modelLocation); // Bake with fluid texture

                var unlit = fluid.getFluidType().getLightLevel() > 0;
                var renderTypes = DynamicFluidContainerModel.getLayerRenderTypes(unlit);
                if (unlit) MAX_LIGHTMAP_TRANSFORMER.processInPlace(quads);

                modelBuilder.addQuads(renderTypes, quads);
            }
        }

        modelBuilder.setParticle(particleSprite);

        return modelBuilder.build();
    }

    public static final class Loader implements IGeometryLoader<UniversalBucketModel>
    {
        public static final Loader INSTANCE = new Loader();

        @Override
        @Nonnull
        public UniversalBucketModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext)
        {
            // create new model
            return new UniversalBucketModel(Fluids.EMPTY, null, false, false);
        }
    }

    private static final class ContainedFluidOverrideHandler extends ItemOverrides
    {
        private static final ResourceLocation REBAKE_LOCATION = new ResourceLocation(BucketLib.MOD_ID, "bucket_override");

        private final Map<ResourceLocation, BakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change
        private final ItemOverrides nested;
        private final ModelBaker baker;
        private final IGeometryBakingContext owner;
        private final UniversalBucketModel parent;

        private boolean isCracked;

        private ContainedFluidOverrideHandler(ItemOverrides nested, ModelBaker baker, IGeometryBakingContext owner, UniversalBucketModel parent)
        {
            this.nested = nested;
            this.baker = baker;
            this.owner = owner;
            this.parent = parent;
        }

        @Nullable
        @Override
        public BakedModel resolve(@Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int number)
        {
            BakedModel overridden = nested.resolve(originalModel, stack, world, entity, number);
            if (overridden != originalModel) return overridden;
            if (stack.getItem() instanceof UniversalBucketItem bucket) {
                ResourceLocation content = null;
                EntityType<?> entityType = BucketLibUtil.getEntityType(stack);
                if (entityType != null) {
                    content = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                }
                if (content == null) {
                    content = BucketLibUtil.getContent(stack);
                }
                Fluid fluid = null;
                if (content == null) {
                    fluid = BucketLibUtil.getFluid(stack);
                    content = BuiltInRegistries.FLUID.getKey(fluid);
                }
                //reset cache if temperature config changed
                boolean isCracked = bucket.isCracked(stack);
                if (this.isCracked != isCracked) {
                    this.isCracked = isCracked;
                    cache.clear();
                }
                if (!cache.containsKey(content)) {
                    UniversalBucketModel unbaked = (entityType != null || fluid == null) ? this.parent.withOtherContent(content, isCracked, entityType != null) : this.parent.withFluid(fluid, isCracked);
                    BakedModel bakedModel = unbaked.bake(owner, baker, Material::sprite, BlockModelRotation.X0_Y0, this, REBAKE_LOCATION);
                    cache.put(content, bakedModel);
                    return bakedModel;
                }
                return cache.get(content);
            }
            return originalModel;
        }
    }

}
