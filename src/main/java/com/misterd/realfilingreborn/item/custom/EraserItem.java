package com.misterd.realfilingreborn.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class EraserItem extends Item {

    public EraserItem(Properties properties) {
        super(properties.durability(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack eraserStack = player.getItemInHand(hand);
        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (offhand.isEmpty()) return InteractionResultHolder.pass(eraserStack);

        Item offhandItem = offhand.getItem();
        if (!(offhandItem instanceof FilingFolderItem) && !(offhandItem instanceof FluidCanisterItem)) {
            return InteractionResultHolder.pass(eraserStack);
        }

        if (offhandItem instanceof FilingFolderItem) {
            FilingFolderItem.FolderContents contents = offhand.get(FilingFolderItem.FOLDER_CONTENTS.value());

            if (contents != null && contents.count() > 0) {
                if (!level.isClientSide()) msg(player, "message.realfilingreborn.folder_not_empty", ChatFormatting.RED);
                return InteractionResultHolder.consume(eraserStack);
            }

            if (contents == null || contents.storedItemId().isEmpty()) {
                if (!level.isClientSide()) msg(player, "message.realfilingreborn.folder_not_assigned", ChatFormatting.RED);
                return InteractionResultHolder.consume(eraserStack);
            }

            // Reset the folder by replacing it with a fresh stack
            ItemStack fresh = new ItemStack(offhandItem, offhand.getCount());
            player.setItemInHand(InteractionHand.OFF_HAND, fresh);
            finishErase(eraserStack, player, level, "message.realfilingreborn.folder_erased");
            return InteractionResultHolder.success(eraserStack);
        }

        if (offhandItem instanceof FluidCanisterItem) {
            FluidCanisterItem.CanisterContents contents = offhand.get(FluidCanisterItem.CANISTER_CONTENTS.value());

            if (contents != null && contents.amount() > 0) {
                if (!level.isClientSide()) msg(player, "message.realfilingreborn.canister_not_empty", ChatFormatting.RED);
                return InteractionResultHolder.consume(eraserStack);
            }

            if (contents == null || contents.storedFluidId().isEmpty()) {
                if (!level.isClientSide()) msg(player, "message.realfilingreborn.canister_not_assigned", ChatFormatting.RED);
                return InteractionResultHolder.consume(eraserStack);
            }

            offhand.set(FluidCanisterItem.CANISTER_CONTENTS.value(), new FluidCanisterItem.CanisterContents(Optional.empty(), 0));
            finishErase(eraserStack, player, level, "message.realfilingreborn.canister_erased");
            return InteractionResultHolder.success(eraserStack);
        }

        return InteractionResultHolder.pass(eraserStack);
    }

    private void finishErase(ItemStack eraserStack, Player player, Level level, String messageKey) {
        if (!player.getAbilities().instabuild) {
            damageEraser(eraserStack);
        }
        if (!level.isClientSide()) {
            msg(player, messageKey, ChatFormatting.GREEN);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    private static void msg(Player player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }

    private void damageEraser(ItemStack eraserStack) {
        int current = eraserStack.getDamageValue();
        if (current < eraserStack.getMaxDamage()) {
            eraserStack.setDamageValue(current + 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.realfilingreborn.eraser_info").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
