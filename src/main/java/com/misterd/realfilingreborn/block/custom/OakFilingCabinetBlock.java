package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class OakFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<OakFilingCabinetBlock> CODEC = simpleCodec(OakFilingCabinetBlock::new);

    public OakFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
