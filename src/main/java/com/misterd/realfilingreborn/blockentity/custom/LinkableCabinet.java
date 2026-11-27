package com.misterd.realfilingreborn.blockentity.custom;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public interface LinkableCabinet {
    @Nullable
    BlockPos getControllerPos();

    void setControllerPos(BlockPos pos);

    void clearControllerPos();

    default boolean isLinkedToController() {
        return getControllerPos() != null;
    }
}
