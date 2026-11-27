package com.misterd.realfilingreborn.compat.create;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreateCompat {
    private static final DeferredRegister<MountedItemStorageType<?>> MOUNTED_ITEM_STORAGE_TYPES =
            DeferredRegister.create(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, RealFilingReborn.MODID);

    public static final DeferredHolder<MountedItemStorageType<?>, CabinetMountedStorageType> CABINET =
            MOUNTED_ITEM_STORAGE_TYPES.register("filing_cabinet", CabinetMountedStorageType::new);

    private CreateCompat() {}

    public static void init(IEventBus modEventBus) {
        MOUNTED_ITEM_STORAGE_TYPES.register(modEventBus);
        modEventBus.addListener(CreateCompat::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> RFRBlocks.ALL_CABINETS.forEach(
                cabinet -> MountedItemStorageType.REGISTRY.register(cabinet.get(), CABINET.get())));
    }
}
