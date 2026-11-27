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
        var axe = tag(BlockTags.MINEABLE_WITH_AXE);
        RFRBlocks.ALL_CABINETS.forEach(cabinet -> axe.add(cabinet.get()));

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(RFRBlocks.FLUID_CABINET.get())
                .add(RFRBlocks.FILING_INDEX.get());
    }
}
