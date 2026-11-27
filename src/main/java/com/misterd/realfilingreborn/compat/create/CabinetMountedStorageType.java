package com.misterd.realfilingreborn.compat.create;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CabinetMountedStorageType extends MountedItemStorageType<CabinetMountedStorage> {
    public CabinetMountedStorageType() {
        super(CabinetMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public CabinetMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof FilingCabinetBlockEntity cabinet)) return null;

        BlockPos controllerPos = cabinet.getControllerPos();
        if (controllerPos != null && level.isLoaded(controllerPos)
                && level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index) {
            index.removeCabinet(pos);
        }

        return CabinetMountedStorage.fromCabinet(cabinet);
    }
}
