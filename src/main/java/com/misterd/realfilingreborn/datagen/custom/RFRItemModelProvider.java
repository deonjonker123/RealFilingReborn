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
        this.basicItem(RFRItems.FILING_FOLDER.get());
        this.basicItem(RFRItems.FLUID_CANISTER.get());
        this.basicItem(RFRItems.ERASER.get());
        this.basicItem(RFRItems.CABINET_CONVERSION_KIT.get());
        this.basicItem(RFRItems.LEDGER.get());
        this.basicItem(RFRItems.IRON_RANGE_UPGRADE.get());
        this.basicItem(RFRItems.DIAMOND_RANGE_UPGRADE.get());
        this.basicItem(RFRItems.NETHERITE_RANGE_UPGRADE.get());
    }
}
