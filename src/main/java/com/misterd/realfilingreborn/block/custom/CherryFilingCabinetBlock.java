package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class CherryFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<CherryFilingCabinetBlock> CODEC = simpleCodec(CherryFilingCabinetBlock::new);

    public CherryFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
