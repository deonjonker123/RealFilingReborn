package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class WarpedFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<WarpedFilingCabinetBlock> CODEC = simpleCodec(WarpedFilingCabinetBlock::new);

    public WarpedFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
