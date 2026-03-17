package com.misterd.realfilingreborn.blockentity;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RFRBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RealFilingReborn.MODID);

    public static final Supplier<BlockEntityType<FilingCabinetBlockEntity>> FILING_CABINET_BE =
            BLOCK_ENTITIES.register("filing_cabinet_be", () -> BlockEntityType.Builder.of(
                    FilingCabinetBlockEntity::new, RFRBlocks.FILING_CABINET.get()).build(null));

    public static final Supplier<BlockEntityType<FluidCabinetBlockEntity>> FLUID_CABINET_BE =
            BLOCK_ENTITIES.register("fluid_cabinet_be", () -> BlockEntityType.Builder.of(
                    FluidCabinetBlockEntity::new, RFRBlocks.FLUID_CABINET.get()).build(null));

    public static final Supplier<BlockEntityType<FilingIndexBlockEntity>> FILING_INDEX_BE =
            BLOCK_ENTITIES.register("filing_index_be", () -> BlockEntityType.Builder.of(
                    FilingIndexBlockEntity::new, RFRBlocks.FILING_INDEX.get()).build(null));

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FILING_CABINET_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUID_CABINET_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FILING_INDEX_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_CABINET_BE.get(),
                (blockEntity, direction) -> blockEntity.getFluidCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FILING_INDEX_BE.get(),
                (blockEntity, direction) -> blockEntity.getFluidCapabilityHandler(direction));
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(RFRBlockEntities::registerCapabilities);
    }
}
