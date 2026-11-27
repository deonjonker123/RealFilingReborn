package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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

public enum FluidCabinetProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath("realfilingreborn", "fluid_cabinet_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        ListTag canisters = accessor.getServerData().getListOrEmpty("canisters");
        if (canisters.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.empty_fluid_cabinet").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!Minecraft.getInstance().hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.realfilingreborn.jade.canister_summary", canisters.size()).withStyle(ChatFormatting.GRAY));
            tooltip.add(RFRJadePlugin.holdShift());
            return;
        }

        for (int i = 0; i < canisters.size(); i++) {
            CompoundTag tag = canisters.getCompoundOrEmpty(i);
            int amount = tag.getIntOr("amount", 0);
            int capacity = tag.getIntOr("capacity", 0);
            double fillPct = capacity > 0 ? (double) amount / capacity * 100.0 : 0.0;
            tooltip.add(Component.literal("Canister " + (tag.getIntOr("slot", 0) + 1) + ": " + tag.getStringOr("fluid_name", "")
                    + " (" + FluidHelper.formatAmount(amount) + ", " + String.format("%.2f%%", fillPct) + ")").withStyle(ChatFormatting.WHITE));
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
            if (!(accessor.getBlockEntity() instanceof FluidCabinetBlockEntity cabinet)) return;

            ListTag canistersList = new ListTag();
            for (int i = 0; i < FluidCabinetBlockEntity.SLOTS; i++) {
                ItemStack stack = cabinet.getStack(i);
                if (!(stack.getItem() instanceof FluidCanisterItem canister)) continue;
                FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(stack);
                if (contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;
                CompoundTag tag = new CompoundTag();
                tag.putInt("slot", i);
                tag.putString("fluid_name", FluidHelper.getFluidDisplayName(contents.storedFluidId().get()));
                tag.putInt("amount", contents.amount());
                tag.putInt("capacity", canister.getCapacity());
                canistersList.add(tag);
            }
            data.put("canisters", canistersList);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}