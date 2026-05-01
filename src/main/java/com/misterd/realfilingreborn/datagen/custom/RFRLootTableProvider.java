package com.misterd.realfilingreborn.datagen.custom;


import com.misterd.realfilingreborn.block.RFRBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class RFRLootTableProvider extends BlockLootSubProvider {

    public RFRLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(RFRBlocks.FILING_CABINET.get());
        dropSelf(RFRBlocks.ACACIA_FILING_CABINET.get());
        dropSelf(RFRBlocks.BIRCH_FILING_CABINET.get());
        dropSelf(RFRBlocks.CHERRY_FILING_CABINET.get());
        dropSelf(RFRBlocks.CRIMSON_FILING_CABINET.get());
        dropSelf(RFRBlocks.DARKOAK_FILING_CABINET.get());
        dropSelf(RFRBlocks.JUNGLE_FILING_CABINET.get());
        dropSelf(RFRBlocks.MANGROVE_FILING_CABINET.get());
        dropSelf(RFRBlocks.OAK_FILING_CABINET.get());
        dropSelf(RFRBlocks.WARPED_FILING_CABINET.get());

        dropSelf(RFRBlocks.FLUID_CABINET.get());
        dropSelf(RFRBlocks.FILING_INDEX.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return RFRBlocks.BLOCKS.getEntries().stream()
                .map(Holder::value)
                ::iterator;
    }
}
