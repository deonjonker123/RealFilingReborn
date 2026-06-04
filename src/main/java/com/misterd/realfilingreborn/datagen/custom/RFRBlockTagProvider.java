package com.misterd.realfilingreborn.datagen.custom;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class RFRBlockTagProvider extends BlockTagsProvider {

    public RFRBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, RealFilingReborn.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(RFRBlocks.SINGLE_SPRUCE_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_ACACIA_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_BIRCH_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_CHERRY_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_CRIMSON_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_DARK_OAK_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_JUNGLE_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_MANGROVE_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_OAK_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_WARPED_FILING_CABINET.get())
                .add(RFRBlocks.SINGLE_PALE_OAK_FILING_CABINET.get())

                .add(RFRBlocks.DOUBLE_SPRUCE_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_ACACIA_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_BIRCH_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_CHERRY_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_CRIMSON_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_DARK_OAK_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_JUNGLE_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_MANGROVE_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_OAK_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_WARPED_FILING_CABINET.get())
                .add(RFRBlocks.DOUBLE_PALE_OAK_FILING_CABINET.get())

                .add(RFRBlocks.FILING_CABINET.get())
                .add(RFRBlocks.ACACIA_FILING_CABINET.get())
                .add(RFRBlocks.BIRCH_FILING_CABINET.get())
                .add(RFRBlocks.CHERRY_FILING_CABINET.get())
                .add(RFRBlocks.CRIMSON_FILING_CABINET.get())
                .add(RFRBlocks.DARK_OAK_FILING_CABINET.get())
                .add(RFRBlocks.JUNGLE_FILING_CABINET.get())
                .add(RFRBlocks.MANGROVE_FILING_CABINET.get())
                .add(RFRBlocks.OAK_FILING_CABINET.get())
                .add(RFRBlocks.WARPED_FILING_CABINET.get())
                .add(RFRBlocks.PALE_OAK_FILING_CABINET.get());

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(RFRBlocks.FILING_INDEX.get());
    }
}
