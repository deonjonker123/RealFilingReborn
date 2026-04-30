package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class BirchFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<BirchFilingCabinetBlock> CODEC = simpleCodec(BirchFilingCabinetBlock::new);

    public BirchFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
