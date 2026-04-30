package com.misterd.realfilingreborn.datagen.custom;

import com.misterd.realfilingreborn.block.RFRBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class RFRBlockTagProvider extends BlockTagsProvider {

    public RFRBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, "realfilingreborn", existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(RFRBlocks.FILING_CABINET.get())
                .add(RFRBlocks.ACACIA_FILING_CABINET.get())
                .add(RFRBlocks.BIRCH_FILING_CABINET.get())
                .add(RFRBlocks.CHERRY_FILING_CABINET.get())
                .add(RFRBlocks.CRIMSON_FILING_CABINET.get())
                .add(RFRBlocks.DARKOAK_FILING_CABINET.get())
                .add(RFRBlocks.JUNGLE_FILING_CABINET.get())
                .add(RFRBlocks.MANGROVE_FILING_CABINET.get())
                .add(RFRBlocks.OAK_FILING_CABINET.get())
                .add(RFRBlocks.WARPED_FILING_CABINET.get());

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(RFRBlocks.FLUID_CABINET.get())
                .add(RFRBlocks.FILING_INDEX.get());
    }
}
