package com.misterd.realfilingreborn;

import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.client.ber.FilingCabinetBlockEntityRenderer;
import com.misterd.realfilingreborn.client.ber.FluidCabinetBlockEntityRenderer;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.gui.custom.*;
import com.misterd.realfilingreborn.item.RFRCreativeTab;
import com.misterd.realfilingreborn.item.RFRItems;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(RealFilingReborn.MODID)
public class RealFilingReborn {
    public static final String MODID = "realfilingreborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealFilingReborn(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        RFRBlocks.register(modEventBus);
        RFRItems.register(modEventBus);
        RFRCreativeTab.register(modEventBus);
        RFRBlockEntities.register(modEventBus);
        RFRMenuTypes.register(modEventBus);
        RFRDataComponents.register(modEventBus);
        RFRRecipes.register(modEventBus);

        FilingFolderItem.DATA_COMPONENTS.register(modEventBus);
        FluidCanisterItem.DATA_COMPONENTS.register(modEventBus);

        Config.register(modContainer);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(RFRBlockEntities.FILING_CABINET_BE.get(), FilingCabinetBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(RFRBlockEntities.FLUID_CABINET_BE.get(), FluidCabinetBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(RFRMenuTypes.FILING_CABINET_MENU.get(), FilingCabinetScreen::new);
            event.register(RFRMenuTypes.FLUID_CABINET_MENU.get(), FluidCabinetScreen::new);
            event.register(RFRMenuTypes.FILING_INDEX_MENU.get(), FilingIndexScreen::new);
            event.register(RFRMenuTypes.FILING_FOLDER_MENU.get(), FilingFolderScreen::new);
            event.register(RFRMenuTypes.FLUID_CANISTER_MENU.get(), FluidCanisterScreen::new);
        }
    }
}
