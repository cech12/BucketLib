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
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.BitSet;
import java.util.function.UnaryOperator;

public class UnbakedElementsHelper {

    private UnbakedElementsHelper() {}

    /**
     * Bakes quads in the shape of the specified mask texture with the specified output texture applied to them.
     * <p>
     * The {@link Direction#NORTH} and {@link Direction#SOUTH} faces take up only the pixels the mask texture uses.
     */
    public static QuadCollection bakeItemMaskQuads(ModelBaker baker, int layerIndex, Material.Baked maskMaterial, Material.Baked outputMaterial, ModelState modelState, UnaryOperator<BakedQuad.MaterialInfo> materialModifier) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        ModelBaker.Interner interner = baker.interner();
        BakedQuad.MaterialInfo maskMaterialInfo = interner.materialInfo(BakedQuad.MaterialInfo.of(maskMaterial, maskMaterial.sprite().transparency(), layerIndex, true, 0));
        BakedQuad.MaterialInfo outMaterialInfo = interner.materialInfo(materialModifier.apply(BakedQuad.MaterialInfo.of(outputMaterial, outputMaterial.sprite().transparency(), layerIndex, true, 0)));

        //why are the side faces included at all?
        ItemModelGenerator.bakeSideFaces(builder, interner, modelState, maskMaterialInfo);

        SpriteContents spriteContents = maskMaterial.sprite().contents();
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
                    // Create UVs
                    CuboidFace.UVs northUvs = FaceBakery.defaultFaceUV(from, to, Direction.NORTH);
                    CuboidFace.UVs southUvs = FaceBakery.defaultFaceUV(from, to, Direction.SOUTH);
                    // Create quads
                    builder.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, northUvs, Quadrant.R0, outMaterialInfo, Direction.SOUTH, modelState, null));
                    builder.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, southUvs, Quadrant.R0, outMaterialInfo, Direction.NORTH, modelState, null));

                    // Reset xStart
                    xStart = -1;
                }
            }
        }
        return builder.build();
    }

}
