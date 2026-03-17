package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.Config;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.text.NumberFormat;
import java.util.Locale;

public enum FilingCabinetProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "filing_cabinet_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("folders")) return;

        ListTag folders = data.getList("folders", 10);
        if (folders.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.empty_cabinet").withStyle(ChatFormatting.GRAY));
            return;
        }

        for (int i = 0; i < folders.size(); i++) {
            CompoundTag folderTag = folders.getCompound(i);
            if (!folderTag.contains("item_id")) continue;

            int slot = folderTag.getInt("slot");
            String itemName = folderTag.getString("item_name");
            int count = folderTag.getInt("count");
            String formattedCount = NumberFormat.getNumberInstance(Locale.US).format((long) count);
            double fillPct = (double) count / Config.getMaxFolderStorage() * 100.0;
            String pctText = String.format("%.2f%%", fillPct);
            tooltip.add(Component.literal("Folder " + (slot + 1) + ": " + itemName +
                    " (" + formattedCount + ", " + pctText + ")").withStyle(ChatFormatting.WHITE));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof FilingCabinetBlockEntity cabinet)) return;

        ListTag foldersList = new ListTag();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = cabinet.inventory.getStackInSlot(i);
            CompoundTag folderTag = new CompoundTag();
            folderTag.putInt("slot", i);

            if (!stack.isEmpty() && stack.getItem() instanceof FilingFolderItem) {
                FilingFolderItem.FolderContents contents = stack.get(FilingFolderItem.FOLDER_CONTENTS.value());
                if (contents != null && contents.storedItemId().isPresent()) {
                    ResourceLocation itemId = contents.storedItemId().get();
                    folderTag.putString("item_id", itemId.toString());
                    folderTag.putString("item_name", BuiltInRegistries.ITEM.get(itemId).getDescription().getString());
                    folderTag.putInt("count", contents.count());
                }
            }

            foldersList.add(folderTag);
        }
        data.put("folders", foldersList);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}