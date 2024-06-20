package de.cech12.bucketlib.client.model;

import de.cech12.bucketlib.mixin.ItemModelGeneratorAccessor;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class GeometryUtils {

    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ItemModelGeneratorAccessor ITEM_MODEL_GENERATOR = (ItemModelGeneratorAccessor) new ItemModelGenerator();

    private static final float MASK_OFFSET = 0.002F;

    private GeometryUtils() {}

    public static List<BlockElement> createUnbakedItemElements(int tintIndex, String name, SpriteContents spriteContents) {
        return ITEM_MODEL_GENERATOR.bucketlib_processFrames(tintIndex, name, spriteContents);
    }

    public static List<BlockElement> createUnbakedItemMaskElements(int tintIndex, String name, SpriteContents spriteContents) {
        List<BlockElement> elements = createUnbakedItemElements(tintIndex, name, spriteContents);
        elements.remove(0);
        int width = spriteContents.width();
        int height = spriteContents.height();
        BitSet bits = new BitSet(width * height);
        spriteContents.getUniqueFrames().forEach((frame) -> {
            for(int x = 0; x < width; ++x) {
                for(int y = 0; y < height; ++y) {
                    if (!spriteContents.isTransparent(frame, x, y)) {
                        bits.set(x + y * width);
                    }
                }
            }
        });

        for(int y = 0; y < height; ++y) {
            int xStart = -1;

            for(int x = 0; x < width; ++x) {
                boolean opaque = bits.get(x + y * width);
                if (opaque == (xStart == -1)) {
                    if (xStart == -1) {
                        xStart = x;
                    } else {
                        int yEnd;
                        int i;
                        label63:
                        for(yEnd = y + 1; yEnd < height; ++yEnd) {
                            for(i = xStart; i <= x; ++i) {
                                if (!bits.get(i + yEnd * width)) {
                                    break label63;
                                }
                            }
                        }

                        for(i = xStart; i < x; ++i) {
                            for(int j = y; j < yEnd; ++j) {
                                bits.clear(i + j * width);
                            }
                        }

                        elements.add(new BlockElement(new Vector3f((float)(16 * xStart) / (float)width, 16.0F - (float)(16 * yEnd) / (float)height, 7.5F - MASK_OFFSET), new Vector3f((float)(16 * x) / (float)width, 16.0F - (float)(16 * y) / (float)height, 8.5F + MASK_OFFSET), Util.make(new HashMap<>(), (map) -> {
                            for(Direction direction : Direction.values()) {
                                map.put(direction, new BlockElementFace(null, tintIndex, name, new BlockFaceUV(null, 0)));
                            }
                        }), null, true));

                        xStart = -1;
                    }
                }
            }
        }

        return elements;
    }

    public static List<BakedQuad> bakeElements(BlockModel blockModel, ItemOverrides itemOverrides, List<BlockElement> elements, TextureAtlasSprite sprite, ModelState modelState) {
        if (elements.isEmpty()) {
            return List.of();
        } else {
            SimpleBakedModel.Builder simplebakedmodel$builder = (new SimpleBakedModel.Builder(blockModel, itemOverrides, false)).particle(sprite);
            bakeElements(simplebakedmodel$builder, elements, sprite, modelState);
            return simplebakedmodel$builder.build().getQuads(null, null, RandomSource.create());
        }
    }

    private static void bakeElements(SimpleBakedModel.Builder builder, List<BlockElement> elements, TextureAtlasSprite sprite, ModelState modelState) {
        for (BlockElement element : elements) {
            element.faces.forEach((side, face) -> {
                BakedQuad quad = bakeElementFace(element, face, sprite, side, modelState);
                if (face.cullForDirection() == null) {
                    builder.addUnculledFace(quad);
                } else {
                    builder.addCulledFace(Direction.rotate(modelState.getRotation().getMatrix(), face.cullForDirection()), quad);
                }
            });
        }
    }

    private static BakedQuad bakeElementFace(BlockElement element, BlockElementFace face, TextureAtlasSprite sprite, Direction direction, ModelState state) {
        return FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, direction, state, element.rotation, element.shade);
    }

}
