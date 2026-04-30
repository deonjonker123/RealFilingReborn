package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class MangroveFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<MangroveFilingCabinetBlock> CODEC = simpleCodec(MangroveFilingCabinetBlock::new);

    public MangroveFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
