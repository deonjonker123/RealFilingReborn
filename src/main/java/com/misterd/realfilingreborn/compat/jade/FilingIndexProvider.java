package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.item.custom.DiamondRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.IronRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.NetheriteRangeUpgradeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum FilingIndexProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath("realfilingreborn", "filing_index_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("linked_count")) return;

        int linkedCount = data.getIntOr("linked_count", 0);
        tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.index.linked",
                Component.literal(String.valueOf(linkedCount)).withStyle(linkedCount > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
        tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.index.range",
                Component.literal(data.getIntOr("range", 0) + " blocks").withStyle(ChatFormatting.AQUA),
                Component.literal(data.getStringOr("upgrade_name", "")).withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.index.connected",
                linkedCount > 0
                        ? Component.translatable("tooltip.realfilingreborn.jade.index.yes").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("tooltip.realfilingreborn.jade.index.no").withStyle(ChatFormatting.RED)));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    public enum Data implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof FilingIndexBlockEntity index)) return;

            data.putInt("linked_count", index.getLinkedCabinetCount());
            data.putInt("range", index.getRange());
            Item upgrade = index.getUpgradeStack().getItem();
            String upgradeName;
            if (upgrade instanceof NetheriteRangeUpgradeItem) upgradeName = "Netherite Upgrade";
            else if (upgrade instanceof DiamondRangeUpgradeItem) upgradeName = "Diamond Upgrade";
            else if (upgrade instanceof IronRangeUpgradeItem) upgradeName = "Iron Upgrade";
            else upgradeName = "No Upgrade";
            data.putString("upgrade_name", upgradeName);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
