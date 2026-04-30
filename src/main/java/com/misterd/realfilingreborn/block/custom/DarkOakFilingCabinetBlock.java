package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class DarkOakFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<DarkOakFilingCabinetBlock> CODEC = simpleCodec(DarkOakFilingCabinetBlock::new);

    public DarkOakFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
