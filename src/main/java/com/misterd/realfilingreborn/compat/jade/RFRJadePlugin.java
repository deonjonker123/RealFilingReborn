package com.misterd.realfilingreborn.compat.jade;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RFRJadePlugin implements IWailaPlugin {
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(FilingCabinetProvider.INSTANCE, FilingCabinetBlockEntity.class);
        registration.registerBlockDataProvider(FluidCabinetProvider.INSTANCE, FluidCabinetBlockEntity.class);
    }

    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FilingCabinetProvider.INSTANCE, FilingCabinetBlock.class);
        registration.registerBlockComponent(FluidCabinetProvider.INSTANCE, FluidCabinetBlock.class);
    }
}