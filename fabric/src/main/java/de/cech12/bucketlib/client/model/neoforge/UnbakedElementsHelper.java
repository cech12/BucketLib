/*
 * This class contains code derived from NeoForge's UnbakedElementsHelper
 * (net.neoforged.neoforge.client.model.UnbakedElementsHelper)
 * Source: https://github.com/neoforged/NeoForge
 *
 * The original code is copyright (c) NeoForged and contributors,
 * licensed under the GNU Lesser General Public License v2.1 only (LGPL-2.1-only).
 * See LICENSE-LGPLv2.1 in the project root for the full license text.
 *
 * All other code in this project is licensed under the MIT License.
 */
package de.cech12.bucketlib.client.model.neoforge;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class UnbakedElementsHelper {
    private static final ModelBaker.PartCache DUMMY_PART_CACHE = (vector) -> vector;

    private UnbakedElementsHelper() {}

    public static List<BlockElement> createUnbakedItemElements(int layerIndex, TextureAtlasSprite sprite) {
        return ItemModelGenerator.processFrames(layerIndex, "layer" + layerIndex, sprite.contents());
    }

    public static List<BlockElement> createUnbakedItemMaskElements(int layerIndex, TextureAtlasSprite sprite) {
        List<BlockElement> elements = createUnbakedItemElements(layerIndex, sprite);
        elements.removeFirst(); // Remove north and south faces
        SpriteContents spriteContents = sprite.contents();
        int width = spriteContents.width();
        int height = spriteContents.height();
        BitSet bits = new BitSet(width * height);

        // For every frame in the texture, mark all the opaque pixels (this is what vanilla does too)
        spriteContents.getUniqueFrames().forEach(frame -> {
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    if (!spriteContents.isTransparent(frame, x, y))
                        bits.set(x + y * width);
        });

        // Scan in search of opaque pixels
        for (int y = 0; y < height; y++) {
            int xStart = -1;
            for (int x = 0; x < width; x++) {
                boolean opaque = bits.get(x + y * width);
                if (opaque == (xStart == -1)) { // (opaque && -1) || (!opaque && !-1)
                    if (xStart == -1) {
                        // We have found the start of a new segment, continue
                        xStart = x;
                        continue;
                    }

                    // The segment is over, expand down as far as possible
                    int yEnd = y + 1;
                    expand:
                    for (; yEnd < height; yEnd++)
                        for (int x2 = xStart; x2 <= x; x2++)
                            if (!bits.get(x2 + yEnd * width))
                                break expand;

                    // Mark all pixels in the area as visited
                    for (int i = xStart; i < x; i++)
                        for (int j = y; j < yEnd; j++)
                            bits.clear(i + j * width);

                    Vector3f from = new Vector3f(16 * xStart / (float) width, 16 - 16 * yEnd / (float) height, 7.5F);
                    Vector3f to = new Vector3f(16 * x / (float) width, 16 - 16 * y / (float) height, 8.5F);
                    // Create initial default UVs
                    BlockElementFace.UVs northUvs = FaceBakery.defaultFaceUV(from, to, Direction.NORTH);
                    BlockElementFace.UVs southUvs = FaceBakery.defaultFaceUV(from, to, Direction.SOUTH);
                    Map<Direction, BlockElementFace> faces = Map.of(Direction.NORTH, new BlockElementFace(null, layerIndex, "layer" + layerIndex, northUvs, Quadrant.R0), Direction.SOUTH, new BlockElementFace((Direction)null, layerIndex, "layer" + layerIndex, southUvs, Quadrant.R0));
                    elements.add(new BlockElement(from, to, faces, null, true, 0));
                    xStart = -1;
                }
            }
        }
        return elements;
    }

    public static List<BakedQuad> bakeElements(List<BlockElement> elements, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        if (elements.isEmpty())
            return List.of();
        var builder = new QuadCollection.Builder();
        bakeElements(builder, elements, spriteGetter, modelState);
        return builder.build().getAll();
    }

    private static void bakeElements(QuadCollection.Builder builder, List<BlockElement> elements, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        for (BlockElement element : elements) {
            element.faces().forEach((side, face) -> {
                var sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.parse(face.texture())));
                BakedQuad quad = FaceBakery.bakeQuad(DUMMY_PART_CACHE, element.from(), element.to(), face, sprite, side, modelState, element.rotation(), element.shade(), element.lightEmission());
                if (face.cullForDirection() == null)
                    builder.addUnculledFace(quad);
                else
                    builder.addCulledFace(Direction.rotate(modelState.transformation().getMatrix(), face.cullForDirection()), quad);
            });
        }
    }

}
