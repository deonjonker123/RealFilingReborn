package com.misterd.realfilingreborn.item;

import com.misterd.realfilingreborn.RealFilingReborn;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RFRItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealFilingReborn.MODID);

    public static final DeferredItem<Item> FILING_FOLDER = ITEMS.register("filing_folder",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ERASER = ITEMS.register("eraser",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> LEDGER = ITEMS.register("ledger",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> FLUID_CANISTER = ITEMS.register("fluid_canister",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CABINET_CONVERSION_KIT = ITEMS.register("cabinet_conversion_kit",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> IRON_RANGE_UPGRADE = ITEMS.register("iron_range_upgrade",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DIAMOND_RANGE_UPGRADE = ITEMS.register("diamond_range_upgrade",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> NETHERITE_RANGE_UPGRADE = ITEMS.register("netherite_range_upgrade",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
