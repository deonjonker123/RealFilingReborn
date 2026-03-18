package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.ChatFormatting;
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

public enum FluidCabinetProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "fluid_cabinet_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("canisters")) return;

        ListTag canisters = data.getList("canisters", 10);
        if (canisters.isEmpty()) return;

        for (int i = 0; i < canisters.size(); i++) {
            CompoundTag canisterTag = canisters.getCompound(i);

            int slot = canisterTag.getInt("slot");
            String fluidName = canisterTag.getString("fluid_name");
            int amount = canisterTag.getInt("amount");
            int capacity = canisterTag.getInt("capacity");

            int buckets = amount / 1000;
            int mb = amount % 1000;
            String amountText = buckets > 0
                    ? (mb > 0 ? buckets + "." + mb / 100 + "B" : buckets + "B")
                    : mb + "mB";

            double fillPct = capacity > 0 ? (double) amount / capacity * 100.0 : 0.0;
            String pctText = String.format("%.2f%%", fillPct);

            tooltip.add(Component.literal("Canister " + (slot + 1) + ": " + fluidName +
                    " (" + amountText + ", " + pctText + ")").withStyle(ChatFormatting.WHITE));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof FluidCabinetBlockEntity cabinet)) return;

        ListTag canistersList = new ListTag();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = cabinet.inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof FluidCanisterItem canister)) continue;

            FluidCanisterItem.CanisterContents contents = stack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
            if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;

            ResourceLocation fluidId = contents.storedFluidId().get();
            CompoundTag canisterTag = new CompoundTag();
            canisterTag.putInt("slot", i);
            canisterTag.putString("fluid_id", fluidId.toString());
            canisterTag.putString("fluid_name", FluidHelper.getFluidDisplayName(fluidId));
            canisterTag.putInt("amount", contents.amount());
            canisterTag.putInt("capacity", canister.getCapacity());
            canistersList.add(canisterTag);
        }

        if (!canistersList.isEmpty()) {
            data.put("canisters", canistersList);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}