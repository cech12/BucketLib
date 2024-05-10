package de.cech12.bucketlib.client.model;

import com.google.common.collect.Maps;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.client.BucketLibClientMod;
import de.cech12.bucketlib.mixin.BlockModelAccessor;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * This implementation is based on net.neoforged.neoforge.client.model.DynamicFluidContainerModel.
 * Multiple changes were done to simplify the class
 */
public class UniversalBucketUnbakedModel extends BlockModel implements UnbakedModel {

    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_MAP = Maps.newHashMap();
    private static final Material MISSING_LOWER_CONTENT_MATERIAL = new Material(InventoryMenu.BLOCK_ATLAS, getContentTexture(new ResourceLocation(BucketLib.MOD_ID, "missing_lower_content")));

    @Nonnull
    private Fluid fluid = Fluids.EMPTY;
    @Nullable
    private ResourceLocation otherContent = null;

    private boolean isCracked = false;
    private boolean isLower = false;

    public UniversalBucketUnbakedModel(BlockModel blockModel) {
        super(
                ((BlockModelAccessor) blockModel).bucketlib_getParentLocation(),
                blockModel.getElements(),
                ((BlockModelAccessor) blockModel).bucketlib_getTextureMap(),
                blockModel.hasAmbientOcclusion(),
                blockModel.getGuiLight(),
                blockModel.getTransforms(),
                blockModel.getOverrides()
        );
    }

    public UniversalBucketUnbakedModel(BlockModel blockModel, @Nonnull Fluid fluid, @Nullable ResourceLocation otherContent, boolean isCracked, boolean isLower) {
        this(blockModel);
        this.fluid = fluid;
        this.otherContent = otherContent;
        this.isCracked = isCracked;
        this.isLower = isLower;
    }

    /**
     * Returns a new UniversalBucketModel with the given fluid.
     */
    public UniversalBucketUnbakedModel withFluid(Fluid newFluid, boolean isCracked) {
        return new UniversalBucketUnbakedModel(this, newFluid, null, isCracked, false);
    }

    /**
     * Returns a new UniversalBucketModel with the given other content.
     */
    public UniversalBucketUnbakedModel withOtherContent(ResourceLocation otherContent, boolean isCracked, boolean isLower) {
        return new UniversalBucketUnbakedModel(this, Fluids.EMPTY, otherContent, isCracked, isLower);
    }

    private static ResourceLocation getContentTexture(ResourceLocation otherContentLocation) {
        ResourceLocation texture = TEXTURE_MAP.get(otherContentLocation);
        if (texture == null) {
            String textureLocation = String.format("item/bucket_content/%s", otherContentLocation.getPath());
            texture = new ResourceLocation(otherContentLocation.getNamespace(), textureLocation);
            TEXTURE_MAP.put(otherContentLocation, texture);
        }
        return texture;
    }

    public ResourceLocation getParentLocation() {
        return this.parentLocation;
    }

    @Nonnull
    @Override
    public BlockModel getRootModel() {
        resolveParents(BucketLibClientMod::getModel);
        if (this.parent == null) {
            return this;
        }
        if (BucketLibClientMod.UNIVERSAL_BUCKET_MODEL.equals(this.getParentLocation())) {
            //skip the BlockModel swap of the vanilla ItemRendering (ItemModelGenerator)
            return this.parent;
        }
        return this.parent.getRootModel();
    }

    @Nullable
    @Override
    public BakedModel bake(@Nonnull ModelBaker modelBaker, @Nonnull Function<Material, TextureAtlasSprite> spriteGetter, @Nonnull ModelState modelState, @Nonnull ResourceLocation resourceLocation) {
        //resolve parents to use their defined materials, too
        resolveParents(BucketLibClientMod::getModel);

        Material particleLocation = this.getMaterial("particle");
        TextureAtlasSprite particleSprite = null;
        if (isValid(particleLocation)) {
            particleSprite = spriteGetter.apply(particleLocation);
        }

        Material baseLocation = null;
        if (this.isLower) {
            if (this.isCracked) {
                baseLocation = this.getMaterial("crackedLowerBase");
            }
            if (!isValid(baseLocation)) {
                baseLocation = this.getMaterial("lowerBase");
            }
        } else {
            if (this.isCracked) {
                baseLocation = this.getMaterial("crackedBase");
            }
            if (!isValid(baseLocation)) {
                baseLocation = this.getMaterial("base");
            }
        }

        Material otherContentLocation = null;
        Material fluidMaskLocation = null;
        if (this.otherContent != null) {
            otherContentLocation = new Material(InventoryMenu.BLOCK_ATLAS, getContentTexture(this.otherContent));
        } else if (this.fluid != Fluids.EMPTY) {
            if (this.isCracked) {
                fluidMaskLocation = this.getMaterial("crackedFluidMask");
            }
            if (!isValid(fluidMaskLocation)) {
                fluidMaskLocation = this.getMaterial("fluidMask");
            }
        }

        TextureAtlasSprite baseSprite = null;
        if (isValid(baseLocation)) {
            baseSprite = spriteGetter.apply(baseLocation);
            if (particleSprite == null)
                particleSprite = baseSprite;
        }
        TextureAtlasSprite otherContentSprite;
        if (isValid(otherContentLocation)) {
            otherContentSprite = spriteGetter.apply(otherContentLocation);
            if (particleSprite == null)
                particleSprite = otherContentSprite;
        } else {
            //if content texture is missing - fallback to pink content texture>
            otherContentSprite = spriteGetter.apply(MISSING_LOWER_CONTENT_MATERIAL);
        }

        TextureAtlasSprite fluidSprite = null;
        if (fluid != Fluids.EMPTY && FluidRenderHandlerRegistry.INSTANCE.get(fluid) != null) {
            TextureAtlasSprite[] sprites = Objects.requireNonNull(FluidRenderHandlerRegistry.INSTANCE.get(fluid)).getFluidSprites(null, null, fluid.defaultFluidState());
            if (sprites.length > 0) {
                fluidSprite = sprites[0];
                if (particleSprite == null)
                    particleSprite = fluidSprite;
            }
        }

        ItemOverrides itemOverrides = new ContainedFluidOverrideHandler(modelBaker, this);

        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        if (fluid != Fluids.EMPTY && FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid))) {
            modelState = BlockModelRotation.X180_Y0;
        }

        List<BakedQuad> quads = new ArrayList<>();

        if (baseSprite != null) {
            quads.addAll(GeometryUtils.bakeElements(this, itemOverrides,
                    GeometryUtils.createUnbakedItemElements(0, "base", baseSprite.contents()),
                    baseSprite, modelState, resourceLocation));
        }
        if (isValid(otherContentLocation)) {
            assert otherContentSprite != null;
            quads.addAll(GeometryUtils.bakeElements(this, itemOverrides,
                    GeometryUtils.createUnbakedItemElements(-1, "content", otherContentSprite.contents()),
                    otherContentSprite, modelState, resourceLocation));
        } else if (isValid(fluidMaskLocation) && fluid != Fluids.EMPTY) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            quads.addAll(GeometryUtils.bakeElements(this, itemOverrides,
                    GeometryUtils.createUnbakedItemMaskElements(1, "fluid", templateSprite.contents()),
                    fluidSprite, modelState, resourceLocation));
        }

        return new UniversalBucketBakedModel(quads, getTransforms(), itemOverrides, particleSprite);
    }

    private boolean isValid(Material material) {
        return material != null && !material.texture().equals(MissingTextureAtlasSprite.getLocation());
    }

    private static final class ContainedFluidOverrideHandler extends ItemOverrides {

        private static final ResourceLocation REBAKE_LOCATION = new ResourceLocation(BucketLib.MOD_ID, "bucket_override");

        private final Map<ResourceLocation, BakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change
        private final ItemOverrides nested;
        private final ModelBaker baker;
        private final UniversalBucketUnbakedModel parent;

        private boolean isCracked;

        private ContainedFluidOverrideHandler(ModelBaker baker, UniversalBucketUnbakedModel parent) {
            super();
            this.nested = ItemOverrides.EMPTY;
            this.baker = baker;
            this.parent = parent;
        }

        @Nullable
        @Override
        public BakedModel resolve(@Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int number) {
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
                    UniversalBucketUnbakedModel unbaked = (entityType != null || fluid == null) ? this.parent.withOtherContent(content, isCracked, entityType != null) : this.parent.withFluid(fluid, isCracked);
                    BakedModel bakedModel = unbaked.bake(baker, Material::sprite, BlockModelRotation.X0_Y0, REBAKE_LOCATION);
                    cache.put(content, bakedModel);
                    return bakedModel;
                }
                return cache.get(content);
            }
            return originalModel;
        }
    }

}
