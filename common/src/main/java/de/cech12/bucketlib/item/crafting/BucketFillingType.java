package de.cech12.bucketlib.item.crafting;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public enum BucketFillingType implements StringRepresentable {

    BLOCK(0, "block"),
    ENTITY(1, "entity"),
    FLUID(2, "fluid"),
    MILK(3, "milk");

    public static final StringRepresentable.EnumCodec<BucketFillingType> CODEC = StringRepresentable.fromEnum(BucketFillingType::values);
    public static final IntFunction<BucketFillingType> BY_ID = ByIdMap.continuous(BucketFillingType::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, BucketFillingType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, BucketFillingType::getId);

    private final int id;
    private final String name;

    BucketFillingType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return this.name;
    }

}
