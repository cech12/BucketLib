package de.cech12.bucketlib.client.model;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.client.ClientServices;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class UniversalBucketModel implements ItemModel {

    private static final Map<Identifier, Identifier> TEXTURE_MAP = Maps.newHashMap();

    private static final Identifier ITEM_GENERATED_ID = Identifier.withDefaultNamespace("item/generated");

    private static final Material MISSING_LOWER_CONTENT_MATERIAL = getItemMaterial(getContentTexture(BucketLib.id("missing_lower_content")));
    private static final Material DEFAULT_FLUID_MASK = getItemMaterial(getItemTexture(BucketLib.id("mask/bucket_fluid")));
    private static final Material UNIVERSAL_BUCKET_BASE = getItemMaterial(getItemTexture(BucketLib.id("universal_bucket_base")));
    private static final Material UNIVERSAL_BUCKET_CRACKED_BASE = getItemMaterial(getItemTexture(BucketLib.id("universal_bucket_cracked_base")));
    private static final Material UNIVERSAL_BUCKET_LOWER_BASE = getItemMaterial(getItemTexture(BucketLib.id("universal_bucket_lower_base")));
    private static final Material UNIVERSAL_BUCKET_CRACKED_LOWER_BASE = getItemMaterial(getItemTexture(BucketLib.id("universal_bucket_cracked_lower_base")));

    private static final ModelDebugName DEBUG_NAME = () -> "UniversalBucketModel";

    protected final Unbaked unbakedModel;
    private final Map<String, ItemModel> cache = new IdentityHashMap<>(); // contains all the baked models since they'll never change

    private Integer upperBreakTemperature = null;
    private Integer lowerBreakTemperature = null;

    protected final BakingContext bakingContext;
    protected final Matrix4fc transformation;
    protected final ItemTransforms itemTransforms;

    protected UniversalBucketModel(UniversalBucketModel.Unbaked unbakedModel, BakingContext bakingContext, Matrix4fc transformation) {
        this.unbakedModel = unbakedModel;
        this.bakingContext = bakingContext;
        this.transformation = transformation;
        this.itemTransforms = bakingContext.blockModelBaker().getModel(ITEM_GENERATED_ID).getTopTransforms();
    }

    private static Material getItemMaterial(Identifier id) {
        return new Material(id);
    }

    private ItemModel bakeModelForFluid(Fluid fluid, Identifier otherContent, boolean isCracked, boolean isLower) {
        ModelBaker baker = bakingContext.blockModelBaker();
        MaterialBaker materials = baker.materials();
        FluidModel fluidModel = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState());

        ItemTintSource bucketTint = (!unbakedModel.tints().isEmpty()) ? unbakedModel.tints().getFirst() : new Constant(-1);
        Material particleLocation = unbakedModel.textures().particle().orElse(null);

        Material baseLocation;
        if (isLower) {
            if (isCracked) {
                baseLocation = unbakedModel.textures().crackedLowerBase().orElse(UNIVERSAL_BUCKET_CRACKED_LOWER_BASE);
            } else {
                baseLocation = unbakedModel.textures().lowerBase().orElse(UNIVERSAL_BUCKET_LOWER_BASE);
            }
        } else {
            if (isCracked) {
                baseLocation = unbakedModel.textures().crackedBase().orElse(UNIVERSAL_BUCKET_CRACKED_BASE);
            } else {
                baseLocation = unbakedModel.textures().base().orElse(UNIVERSAL_BUCKET_BASE);
            }
        }

        Material otherContentLocation = null;
        Material fluidLocation = null;
        Material fluidMaskLocation = null;
        if (otherContent != null) {
            otherContentLocation = UniversalBucketModel.getItemMaterial(getContentTexture(otherContent));
        }
        if (fluid != Fluids.EMPTY) {
            fluidLocation = UniversalBucketModel.getItemMaterial(getContentTexture(BuiltInRegistries.FLUID.getKey(fluid)));
            if (isCracked) {
                fluidMaskLocation = unbakedModel.textures().crackedFluidMask().orElse(DEFAULT_FLUID_MASK);
            } else {
                fluidMaskLocation = unbakedModel.textures().fluidMask().orElse(DEFAULT_FLUID_MASK);
            }
        }
        //oversteer fluid texture if available
        if (otherContentLocation == null && fluidLocation != null && !MissingTextureAtlasSprite.getLocation().equals(materials.get(fluidLocation, DEBUG_NAME).sprite().contents().name())) {
            otherContentLocation = fluidLocation;
        }

        Material.Baked baseSprite = materials.get(baseLocation, DEBUG_NAME);
        Material.Baked otherContentSprite = null;
        if (otherContentLocation != null) {
            otherContentSprite = materials.get(otherContentLocation, DEBUG_NAME);
            //if content texture is missing - fallback to pink content texture
            if (MissingTextureAtlasSprite.getLocation().equals(otherContentSprite.sprite().contents().name())) {
                otherContentSprite = materials.get(MISSING_LOWER_CONTENT_MATERIAL, DEBUG_NAME);
            }
        }
        Material.Baked fluidSprite = fluid != Fluids.EMPTY ? fluidModel.stillMaterial() : null;
        Material.Baked fluidMaskSprite = (fluidMaskLocation != null && fluidSprite != null) ? materials.get(fluidMaskLocation, DEBUG_NAME) : null;

        Material.Baked particleSprite = particleLocation != null ? materials.get(particleLocation, DEBUG_NAME) : null;
        if (particleSprite == null){
            particleSprite = baseSprite;
        }

        List<ItemModel> itemModels = specialBaking(fluid, bucketTint, baseSprite, otherContentSprite, fluidSprite, fluidMaskSprite, particleSprite);

        return new CompositeModel(itemModels);
    }

    abstract List<ItemModel> specialBaking(Fluid fluid, ItemTintSource bucketTint, Material.Baked baseSprite, Material.Baked otherContentSprite, Material.Baked fluidSprite, Material.Baked fluidMaskSprite, Material.Baked particleSprite);

    @Override
    public void update(@NotNull ItemStackRenderState renderState, ItemStack stack, @NotNull ItemModelResolver modelResolver, @NotNull ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner itemOwner, int integer) {
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
                Identifier location = BuiltInRegistries.FLUID.getKey(fluid);
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
                bakedModel = this.bakeModelForFluid(fluid, ((content != null && fluid == Fluids.EMPTY) ? Identifier.parse(content) : null), isCracked, containsEntityType);
                cache.put(content, bakedModel);
            }
            bakedModel.update(renderState, stack, modelResolver, displayContext, level, itemOwner, integer);
        }

    }

    public static Identifier getContentTexture(Identifier id) {
        return getItemTexture(id.withPath(String.format("bucket_content/%s", id.getPath())));
    }

    public static Identifier getItemTexture(Identifier id) {
        Identifier texture = TEXTURE_MAP.get(id);
        if (texture == null) {
            texture = id.withPath(String.format("item/%s", id.getPath()));
            TEXTURE_MAP.put(id, texture);
        }
        return texture;
    }

    public record Unbaked(Textures textures, List<ItemTintSource> tints) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Textures.CODEC.fieldOf("textures").forGetter(Unbaked::textures), ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(Unbaked::tints))
                .apply(instance, Unbaked::new));

        @Override
        @NotNull
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        @NotNull
        public ItemModel bake(@NotNull BakingContext bakingContext, @NotNull Matrix4fc transformation) {
            return ClientServices.CLIENT.createItemModel(this, bakingContext, transformation);
        }

        @Override
        public void resolveDependencies(@NotNull Resolver resolver) {
            //no dependencies
        }

        public record Textures(
                Optional<Material> particle,
                Optional<Material> base,
                Optional<Material> lowerBase,
                Optional<Material> fluidMask,
                Optional<Material> crackedBase,
                Optional<Material> crackedLowerBase,
                Optional<Material> crackedFluidMask) {
            public static final Codec<Textures> CODEC = RecordCodecBuilder.<Textures>create(
                            instance -> instance
                                    .group(
                                            Material.CODEC.optionalFieldOf("particle").forGetter(Textures::particle),
                                            Material.CODEC.optionalFieldOf("base").forGetter(Textures::base),
                                            Material.CODEC.optionalFieldOf("lowerBase").forGetter(Textures::lowerBase),
                                            Material.CODEC.optionalFieldOf("fluidMask").forGetter(Textures::fluidMask),
                                            Material.CODEC.optionalFieldOf("crackedBase").forGetter(Textures::crackedBase),
                                            Material.CODEC.optionalFieldOf("crackedLowerBase").forGetter(Textures::crackedLowerBase),
                                            Material.CODEC.optionalFieldOf("crackedFluidMask").forGetter(Textures::crackedFluidMask))
                                    .apply(instance, Textures::new))
                    .validate(textures -> {
                        if (textures.base.isPresent()) {
                            return DataResult.success(textures);
                        }
                        return DataResult.error(() -> "Universal bucket model requires at least a base texture.");
                    });
        }

    }

}
