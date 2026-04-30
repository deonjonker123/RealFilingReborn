package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class AcaciaFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<AcaciaFilingCabinetBlock> CODEC = simpleCodec(AcaciaFilingCabinetBlock::new);

    public AcaciaFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
