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
        this.dropSelf(RFRBlocks.FILING_CABINET.get());
        this.dropSelf(RFRBlocks.FLUID_CABINET.get());
        this.dropSelf(RFRBlocks.FILING_INDEX.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return RFRBlocks.BLOCKS.getEntries().stream()
                .map(Holder::value)
                ::iterator;
    }
}
