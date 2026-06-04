package com.misterd.realfilingreborn;

import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.client.ber.DoubleFilingCabinetBlockEntityRenderer;
import com.misterd.realfilingreborn.client.ber.FilingCabinetBlockEntityRenderer;
import com.misterd.realfilingreborn.client.ber.SingleFilingCabinetBlockEntityRenderer;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.gui.custom.*;
import com.misterd.realfilingreborn.item.RFRCreativeTab;
import com.misterd.realfilingreborn.item.RFRItems;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(RealFilingReborn.MODID)
public class RealFilingReborn {
    public static final String MODID = "realfilingreborn";

    public RealFilingReborn(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        RFRBlocks.register(modEventBus);
        RFRItems.register(modEventBus);
        RFRCreativeTab.register(modEventBus);
        RFRBlockEntities.register(modEventBus);
        RFRMenuTypes.register(modEventBus);
        RFRDataComponents.register(modEventBus);
        RFRRecipes.register(modEventBus);

        FilingFolderItem.DATA_COMPONENTS.register(modEventBus);

        Config.register(modContainer);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(RFRBlockEntities.FILING_CABINET_BE.get(), FilingCabinetBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(RFRBlockEntities.DOUBLE_FILING_CABINET_BE.get(), DoubleFilingCabinetBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(RFRBlockEntities.SINGLE_FILING_CABINET_BE.get(), SingleFilingCabinetBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(RFRMenuTypes.FILING_CABINET_MENU.get(), FilingCabinetScreen::new);
            event.register(RFRMenuTypes.DOUBLE_FILING_CABINET_MENU.get(), DoubleFilingCabinetScreen::new);
            event.register(RFRMenuTypes.SINGLE_FILING_CABINET_MENU.get(), SingleFilingCabinetScreen::new);
            event.register(RFRMenuTypes.FILING_INDEX_MENU.get(), FilingIndexScreen::new);
            event.register(RFRMenuTypes.FILING_FOLDER_MENU.get(), FilingFolderScreen::new);
        }
    }
}
