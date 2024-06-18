package de.cech12.bucketlib.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.impl.transfer.VariantCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FluidStorageData(FluidVariant fluidVariant, long amount) {

    public static final Codec<FluidStorageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    VariantCodecs.FLUID_CODEC.fieldOf("fluidVariant").forGetter(data -> data.fluidVariant),
                    Codec.LONG.fieldOf("amount").forGetter(data -> data.amount))
            .apply(instance, FluidStorageData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStorageData> STREAM_CODEC = StreamCodec.composite(
            VariantCodecs.FLUID_PACKET_CODEC, fluidStorageData -> fluidStorageData.fluidVariant,
            ByteBufCodecs.VAR_LONG, fluidStorageData -> fluidStorageData.amount,
            FluidStorageData::new
    );

    public static final FluidStorageData EMPTY = new FluidStorageData(null, -1);

    public boolean isEmpty() {
        return this == EMPTY || fluidVariant() == null || amount <= 0;
    }
}
