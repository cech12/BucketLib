package de.cech12.bucketlib.client.color;

import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BucketFluidTint implements ItemTintSource  {

    public static final BucketFluidTint INSTANCE = new BucketFluidTint();
    public static final MapCodec<BucketFluidTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private BucketFluidTint() {}

    @Override
    public int calculate(@NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        return Services.FLUID.getFluidTintColor(stack);
    }

    @Override
    @NotNull
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }

}
