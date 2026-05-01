package com.misterd.realfilingreborn.datagen.custom;


import com.misterd.realfilingreborn.item.RFRItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class RFRItemModelProvider extends ItemModelProvider {

    public RFRItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, "realfilingreborn", existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(RFRItems.FILING_FOLDER.get());
        basicItem(RFRItems.COPPER_FILING_FOLDER.get());
        basicItem(RFRItems.IRON_FILING_FOLDER.get());
        basicItem(RFRItems.GOLD_FILING_FOLDER.get());
        basicItem(RFRItems.DIAMOND_FILING_FOLDER.get());
        basicItem(RFRItems.NETHERITE_FILING_FOLDER.get());

        basicItem(RFRItems.FLUID_CANISTER.get());
        basicItem(RFRItems.COPPER_FLUID_CANISTER.get());
        basicItem(RFRItems.IRON_FLUID_CANISTER.get());
        basicItem(RFRItems.GOLD_FLUID_CANISTER.get());
        basicItem(RFRItems.DIAMOND_FLUID_CANISTER.get());
        basicItem(RFRItems.NETHERITE_FLUID_CANISTER.get());

        basicItem(RFRItems.LEDGER.get());
        basicItem(RFRItems.IRON_RANGE_UPGRADE.get());
        basicItem(RFRItems.DIAMOND_RANGE_UPGRADE.get());
        basicItem(RFRItems.NETHERITE_RANGE_UPGRADE.get());
    }
}
