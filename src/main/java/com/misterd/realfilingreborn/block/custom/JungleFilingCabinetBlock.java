package com.misterd.realfilingreborn.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class JungleFilingCabinetBlock extends FilingCabinetBlock {
    public static final MapCodec<JungleFilingCabinetBlock> CODEC = simpleCodec(JungleFilingCabinetBlock::new);

    public JungleFilingCabinetBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
