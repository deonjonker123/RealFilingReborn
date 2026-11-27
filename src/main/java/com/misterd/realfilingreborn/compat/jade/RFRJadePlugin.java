package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RFRJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(FilingCabinetProvider.Data.INSTANCE, FilingCabinetBlockEntity.class);
        registration.registerBlockDataProvider(FluidCabinetProvider.Data.INSTANCE, FluidCabinetBlockEntity.class);
        registration.registerBlockDataProvider(FilingIndexProvider.Data.INSTANCE, FilingIndexBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FilingCabinetProvider.INSTANCE, FilingCabinetBlock.class);
        registration.registerBlockComponent(FluidCabinetProvider.INSTANCE, FluidCabinetBlock.class);
        registration.registerBlockComponent(FilingIndexProvider.INSTANCE, FilingIndexBlock.class);
    }

    static Component holdShift() {
        return Component.translatable("tooltip.realfilingreborn.jade.hold_shift", Component.literal("Shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY);
    }
}