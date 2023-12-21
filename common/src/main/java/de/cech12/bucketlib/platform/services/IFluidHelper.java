package de.cech12.bucketlib.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * Common fluid helper service interface.
 */
public interface IFluidHelper {

    Component getFluidDescription(Fluid fluid);

    int getFluidTemperature(Fluid fluid);

    boolean hasMilkFluid();

    Fluid getMilkFluid();

    ItemStack dispenseFluidContainer(BlockSource source, ItemStack stack);

    Fluid getContainedFluid(ItemStack stack);

    ItemStack addFluid(ItemStack stack, Fluid fluid);

    ItemStack removeFluid(ItemStack stack);

    Tuple<Boolean, ItemStack> tryPickUpFluid(ItemStack stack, Player player, Level level, InteractionHand interactionHand, BlockPos pos, Direction direction);

    Tuple<Boolean, ItemStack> tryPlaceFluid(ItemStack stack, Player player, Level level, InteractionHand interactionHand, BlockPos pos);

    void curePotionEffects(LivingEntity entity, ItemStack curativeItem);

}
