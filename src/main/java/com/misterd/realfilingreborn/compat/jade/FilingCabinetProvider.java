package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.text.NumberFormat;
import java.util.Locale;

public enum FilingCabinetProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath("realfilingreborn", "filing_cabinet_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("folders")) return;

        ListTag folders = data.getListOrEmpty("folders");
        int assigned = 0;
        for (int i = 0; i < folders.size(); i++) {
            if (folders.getCompoundOrEmpty(i).contains("item_name")) assigned++;
        }
        if (assigned == 0) {
            tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.empty_cabinet").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!Minecraft.getInstance().hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.folder_summary", assigned, folders.size()).withStyle(ChatFormatting.GRAY));
            tooltip.add(RFRJadePlugin.holdShift());
            return;
        }

        for (int i = 0; i < folders.size(); i++) {
            CompoundTag folderTag = folders.getCompoundOrEmpty(i);
            if (!folderTag.contains("item_name")) continue;
            int slot = folderTag.getIntOr("slot", 0);
            int count = folderTag.getIntOr("count", 0);
            int capacity = folderTag.getIntOr("capacity", 0);
            double fillPct = capacity > 0 ? (double) count / capacity * 100.0 : 0.0;
            tooltip.add(Component.literal("Folder " + (slot + 1) + ": " + folderTag.getStringOr("item_name", "") + " ("
                    + NumberFormat.getNumberInstance(Locale.US).format(count) + ", " + String.format("%.2f%%", fillPct) + ")").withStyle(ChatFormatting.WHITE));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    public enum Data implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof FilingCabinetBlockEntity cabinet)) return;

            ListTag foldersList = new ListTag();
            for (int i = 0; i < cabinet.getSlotCount(); i++) {
                ItemStack stack = cabinet.getStack(i);
                CompoundTag folderTag = new CompoundTag();
                folderTag.putInt("slot", i);
                if (stack.getItem() instanceof FilingFolderItem folder) {
                    FilingFolderItem.FolderContents contents = stack.get(FilingFolderItem.FOLDER_CONTENTS.value());
                    if (contents != null && contents.storedItemId().isPresent()) {
                        folderTag.putString("item_name", new ItemStack(BuiltInRegistries.ITEM.getValue(contents.storedItemId().get())).getHoverName().getString());
                        folderTag.putInt("count", contents.count());
                        folderTag.putInt("capacity", folder.getCapacity());
                    }
                }
                foldersList.add(folderTag);
            }
            data.put("folders", foldersList);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}