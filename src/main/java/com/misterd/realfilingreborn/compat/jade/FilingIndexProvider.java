package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.item.custom.DiamondRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.IronRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.NetheriteRangeUpgradeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum FilingIndexProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "filing_index_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("linked_count")) return;

        int linkedCount = data.getInt("linked_count");
        int range = data.getInt("range");
        String upgradeName = data.getString("upgrade_name");
        boolean connected = data.getBoolean("connected");

        tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.index.linked",
                Component.literal(String.valueOf(linkedCount)).withStyle(linkedCount > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY)));

        tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.index.range",
                Component.literal(range + " blocks").withStyle(ChatFormatting.AQUA),
                Component.literal(upgradeName).withStyle(ChatFormatting.DARK_GRAY)));

        tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.index.connected",
                connected
                        ? Component.translatable("tooltip.realfilingreborn.jade.index.yes").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("tooltip.realfilingreborn.jade.index.no").withStyle(ChatFormatting.RED)));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof FilingIndexBlockEntity index)) return;

        data.putInt("linked_count", index.getLinkedCabinetCount());
        data.putInt("range", index.getRange());
        data.putBoolean("connected", index.getLinkedCabinetCount() > 0);

        ItemStack upgrade = index.inventory.getStackInSlot(0);
        String upgradeName = switch (upgrade.getItem()) {
            case NetheriteRangeUpgradeItem ignored -> "Netherite Upgrade";
            case DiamondRangeUpgradeItem ignored  -> "Diamond Upgrade";
            case IronRangeUpgradeItem ignored     -> "Iron Upgrade";
            default                           -> "No Upgrade";
        };
        data.putString("upgrade_name", upgradeName);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
