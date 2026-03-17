package com.misterd.realfilingreborn.item;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RFRCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RealFilingReborn.MODID);

    public static final Supplier<CreativeModeTab> REAL_FILING_REBORN = CREATIVE_MODE_TAB.register("realfilingreborn_creativetab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(RFRItems.FILING_FOLDER.get()))
                    .title(Component.translatable("creativetab.realfilingreborn.real_filing_reborn"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(RFRBlocks.FILING_CABINET);
                        output.accept(RFRBlocks.FLUID_CABINET);
                        output.accept(RFRBlocks.FILING_INDEX);

                        output.accept(RFRItems.LEDGER);
                        output.accept(RFRItems.FILING_FOLDER);
                        output.accept(RFRItems.FLUID_CANISTER);
                        output.accept(RFRItems.ERASER);
                        output.accept(RFRItems.CABINET_CONVERSION_KIT);
                        output.accept(RFRItems.IRON_RANGE_UPGRADE);
                        output.accept(RFRItems.DIAMOND_RANGE_UPGRADE);
                        output.accept(RFRItems.NETHERITE_RANGE_UPGRADE);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
