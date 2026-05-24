package com.misterd.realfilingreborn.datagen.custom;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.util.RFRTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class RFRItemTagProvider extends ItemTagsProvider {
    public RFRItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, RealFilingReborn.MODID);
    }

    protected void addTags(HolderLookup.Provider provider) {
        tag(RFRTags.Items.FILING_CABINET_ITEMS)
                .add(RFRBlocks.ACACIA_FILING_CABINET.asItem())
                .add(RFRBlocks.BIRCH_FILING_CABINET.asItem())
                .add(RFRBlocks.CHERRY_FILING_CABINET.asItem())
                .add(RFRBlocks.CRIMSON_FILING_CABINET.asItem())
                .add(RFRBlocks.DARK_OAK_FILING_CABINET.asItem())
                .add(RFRBlocks.JUNGLE_FILING_CABINET.asItem())
                .add(RFRBlocks.MANGROVE_FILING_CABINET.asItem())
                .add(RFRBlocks.OAK_FILING_CABINET.asItem())
                .add(RFRBlocks.FILING_CABINET.asItem())
                .add(RFRBlocks.WARPED_FILING_CABINET.asItem());
    }
}
