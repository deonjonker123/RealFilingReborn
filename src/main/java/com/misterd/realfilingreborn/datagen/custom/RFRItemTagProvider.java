package com.misterd.realfilingreborn.datagen.custom;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.util.RFRTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class RFRItemTagProvider extends ItemTagsProvider {
    public RFRItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, RealFilingReborn.MODID);
    }

    private static ResourceKey<Item> key(Item item) {
        return item.builtInRegistryHolder().key();
    }

    protected void addTags(HolderLookup.Provider provider) {
        tag(RFRTags.Items.FILING_CABINET_ITEMS)
                .add(key(RFRBlocks.SINGLE_ACACIA_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_BIRCH_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_CHERRY_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_CRIMSON_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_DARK_OAK_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_JUNGLE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_MANGROVE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_OAK_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_SPRUCE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_WARPED_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.SINGLE_PALE_OAK_FILING_CABINET.asItem()))

                .add(key(RFRBlocks.DOUBLE_ACACIA_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_BIRCH_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_CHERRY_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_CRIMSON_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_DARK_OAK_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_JUNGLE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_MANGROVE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_OAK_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_SPRUCE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_WARPED_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DOUBLE_PALE_OAK_FILING_CABINET.asItem()))

                .add(key(RFRBlocks.ACACIA_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.BIRCH_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.CHERRY_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.CRIMSON_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.DARK_OAK_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.JUNGLE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.MANGROVE_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.OAK_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.FILING_CABINET.asItem()))
                .add(key(RFRBlocks.WARPED_FILING_CABINET.asItem()))
                .add(key(RFRBlocks.PALE_OAK_FILING_CABINET.asItem()));
    }
}
