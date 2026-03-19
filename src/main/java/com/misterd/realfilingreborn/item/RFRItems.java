package com.misterd.realfilingreborn.item;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.item.custom.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RFRItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealFilingReborn.MODID);

    public static final DeferredItem<Item> FILING_FOLDER = ITEMS.register("filing_folder",
            () -> new FilingFolderItem(FilingFolderItem.FolderTier.BASE, new Item.Properties()));

    public static final DeferredItem<Item> COPPER_FILING_FOLDER = ITEMS.register("copper_filing_folder",
            () -> new FilingFolderItem(FilingFolderItem.FolderTier.COPPER, new Item.Properties()));

    public static final DeferredItem<Item> IRON_FILING_FOLDER = ITEMS.register("iron_filing_folder",
            () -> new FilingFolderItem(FilingFolderItem.FolderTier.IRON, new Item.Properties()));

    public static final DeferredItem<Item> GOLD_FILING_FOLDER = ITEMS.register("gold_filing_folder",
            () -> new FilingFolderItem(FilingFolderItem.FolderTier.GOLD, new Item.Properties()));

    public static final DeferredItem<Item> DIAMOND_FILING_FOLDER = ITEMS.register("diamond_filing_folder",
            () -> new FilingFolderItem(FilingFolderItem.FolderTier.DIAMOND, new Item.Properties()));

    public static final DeferredItem<Item> NETHERITE_FILING_FOLDER = ITEMS.register("netherite_filing_folder",
            () -> new FilingFolderItem(FilingFolderItem.FolderTier.NETHERITE, new Item.Properties()));

    public static final DeferredItem<Item> FLUID_CANISTER = ITEMS.register("fluid_canister",
            () -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.BASE, new Item.Properties()));

    public static final DeferredItem<Item> COPPER_FLUID_CANISTER = ITEMS.register("copper_fluid_canister",
            () -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.COPPER, new Item.Properties()));

    public static final DeferredItem<Item> IRON_FLUID_CANISTER = ITEMS.register("iron_fluid_canister",
            () -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.IRON, new Item.Properties()));

    public static final DeferredItem<Item> GOLD_FLUID_CANISTER = ITEMS.register("gold_fluid_canister",
            () -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.GOLD, new Item.Properties()));

    public static final DeferredItem<Item> DIAMOND_FLUID_CANISTER = ITEMS.register("diamond_fluid_canister",
            () -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.DIAMOND, new Item.Properties()));

    public static final DeferredItem<Item> NETHERITE_FLUID_CANISTER = ITEMS.register("netherite_fluid_canister",
            () -> new FluidCanisterItem(FluidCanisterItem.CanisterTier.NETHERITE, new Item.Properties()));

    public static final DeferredItem<Item> LEDGER = ITEMS.register("ledger",
            () -> new LedgerItem(new Item.Properties()));

    public static final DeferredItem<Item> IRON_RANGE_UPGRADE = ITEMS.register("iron_range_upgrade",
            () -> new IronRangeUpgradeItem(new Item.Properties()));

    public static final DeferredItem<Item> DIAMOND_RANGE_UPGRADE = ITEMS.register("diamond_range_upgrade",
            () -> new DiamondRangeUpgradeItem(new Item.Properties()));

    public static final DeferredItem<Item> NETHERITE_RANGE_UPGRADE = ITEMS.register("netherite_range_upgrade",
            () -> new NetheriteRangeUpgradeItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}