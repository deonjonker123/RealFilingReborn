package com.misterd.realfilingreborn.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

public class DiamondRangeUpgradeItem extends Item {
    public DiamondRangeUpgradeItem(Properties properties) {
        super(properties);
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("item.realfilingreborn.diamond_range_upgrade.subtitle").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
