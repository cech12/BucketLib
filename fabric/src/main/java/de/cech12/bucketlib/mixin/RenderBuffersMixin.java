package de.cech12.bucketlib.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import de.cech12.bucketlib.client.model.FabricUniversalBucketModel;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

@Mixin(RenderBuffers.class)
public class RenderBuffersMixin {

    //workaround for https://github.com/neoforged/NeoForge/issues/3058
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "net/minecraft/util/Util.make(Ljava/lang/Object;Ljava/util/function/Consumer;)Ljava/lang/Object;", ordinal = 0), index = 1)
    private Consumer<Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>> modifyConsumer(Consumer<Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>> original) {
        return (map) -> {
            original.accept(map);
            map.put(FabricUniversalBucketModel.BLOCK_ITEM_UNSORTED_TRANSLUCENT, new ByteBufferBuilder(FabricUniversalBucketModel.BLOCK_ITEM_UNSORTED_TRANSLUCENT.bufferSize()));
        };
    }

}
