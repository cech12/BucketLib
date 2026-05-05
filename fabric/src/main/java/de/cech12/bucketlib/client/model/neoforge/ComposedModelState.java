/*
 * This class contains code derived from NeoForge's ComposedModelState
 * (net.neoforged.neoforge.client.model.ComposedModelState)
 * Source: https://github.com/neoforged/NeoForge
 *
 * The original code is copyright (c) NeoForged and contributors,
 * licensed under the GNU Lesser General Public License v2.1 only (LGPL-2.1-only).
 * See LICENSE-LGPLv2.1 in the project root for the full license text.
 *
 * All other code in this project is licensed under the MIT License.
 */
package de.cech12.bucketlib.client.model.neoforge;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4fc;

public class ComposedModelState implements ModelState {
    private final ModelState parent;
    private final Transformation transformation;

    public ComposedModelState(ModelState parent, Transformation transformation) {
        this.parent = parent;
        this.transformation = parent.transformation().compose(transformation);
    }

    @Override
    @NotNull
    public Transformation transformation() {
        return transformation;
    }

    @Override
    @NotNull
    public Matrix4fc faceTransformation(@NotNull Direction side) {
        return parent.faceTransformation(side);
    }

    @Override
    @NotNull
    public Matrix4fc inverseFaceTransformation(@NotNull Direction side) {
        return parent.inverseFaceTransformation(side);
    }
}
