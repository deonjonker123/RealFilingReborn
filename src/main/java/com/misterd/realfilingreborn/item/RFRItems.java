package com.misterd.realfilingreborn.item;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.item.custom.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RFRItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealFilingReborn.MODID);

    public static final DeferredItem<Item> FILING_FOLDER = ITEMS.registerItem("filing_folder",
            props -> new FilingFolderItem(FilingFolderItem.FolderTier.BASE, props));

    public static final DeferredItem<Item> COPPER_FILING_FOLDER = ITEMS.registerItem("copper_filing_folder",
            props -> new FilingFolderItem(FilingFolderItem.FolderTier.COPPER, props));

    public static final DeferredItem<Item> IRON_FILING_FOLDER = ITEMS.registerItem("iron_filing_folder",
            props -> new FilingFolderItem(FilingFolderItem.FolderTier.IRON, props));

    public static final DeferredItem<Item> GOLD_FILING_FOLDER = ITEMS.registerItem("gold_filing_folder",
            props -> new FilingFolderItem(FilingFolderItem.FolderTier.GOLD, props));

    public static final DeferredItem<Item> DIAMOND_FILING_FOLDER = ITEMS.registerItem("diamond_filing_folder",
            props -> new FilingFolderItem(FilingFolderItem.FolderTier.DIAMOND, props));

    public static final DeferredItem<Item> NETHERITE_FILING_FOLDER = ITEMS.registerItem("netherite_filing_folder",
            props -> new FilingFolderItem(FilingFolderItem.FolderTier.NETHERITE, props));

    public static final DeferredItem<Item> FLUID_CANISTER = ITEMS.registerItem("fluid_canister",
            props -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.BASE, props));

    public static final DeferredItem<Item> COPPER_FLUID_CANISTER = ITEMS.registerItem("copper_fluid_canister",
            props -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.COPPER, props));

    public static final DeferredItem<Item> IRON_FLUID_CANISTER = ITEMS.registerItem("iron_fluid_canister",
            props -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.IRON, props));

    public static final DeferredItem<Item> GOLD_FLUID_CANISTER = ITEMS.registerItem("gold_fluid_canister",
            props -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.GOLD, props));

    public static final DeferredItem<Item> DIAMOND_FLUID_CANISTER = ITEMS.registerItem("diamond_fluid_canister",
            props -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.DIAMOND, props));

    public static final DeferredItem<Item> NETHERITE_FLUID_CANISTER = ITEMS.registerItem("netherite_fluid_canister",
            props -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.NETHERITE, props));

    public static final DeferredItem<Item> LEDGER = ITEMS.registerItem("ledger",
            props -> new LedgerItem(props));

    public static final DeferredItem<Item> IRON_RANGE_UPGRADE = ITEMS.registerItem("iron_range_upgrade",
            props -> new IronRangeUpgradeItem(props));

    public static final DeferredItem<Item> DIAMOND_RANGE_UPGRADE = ITEMS.registerItem("diamond_range_upgrade",
            props -> new DiamondRangeUpgradeItem(props));

    public static final DeferredItem<Item> NETHERITE_RANGE_UPGRADE = ITEMS.registerItem("netherite_range_upgrade",
            props -> new NetheriteRangeUpgradeItem(props));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}