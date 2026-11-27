package com.misterd.realfilingreborn.blockentity;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class RFRBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RealFilingReborn.MODID);

    public static final Supplier<BlockEntityType<FilingCabinetBlockEntity>> FILING_CABINET_BE =
            cabinetType("filing_cabinet_be", RFRBlocks.QUAD_CABINETS);

    public static final Supplier<BlockEntityType<FilingCabinetBlockEntity>> DOUBLE_FILING_CABINET_BE =
            cabinetType("double_filing_cabinet_be", RFRBlocks.DOUBLE_CABINETS);

    public static final Supplier<BlockEntityType<FilingCabinetBlockEntity>> SINGLE_FILING_CABINET_BE =
            cabinetType("single_filing_cabinet_be", RFRBlocks.SINGLE_CABINETS);

    public static final Supplier<BlockEntityType<FluidCabinetBlockEntity>> FLUID_CABINET_BE =
            BLOCK_ENTITIES.register("fluid_cabinet_be", () -> new BlockEntityType<>(
                    FluidCabinetBlockEntity::new, RFRBlocks.FLUID_CABINET.get()));

    public static final Supplier<BlockEntityType<FilingIndexBlockEntity>> FILING_INDEX_BE =
            BLOCK_ENTITIES.register("filing_index_be", () -> new BlockEntityType<>(
                    FilingIndexBlockEntity::new, RFRBlocks.FILING_INDEX.get()));

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var type : List.of(FILING_CABINET_BE, DOUBLE_FILING_CABINET_BE, SINGLE_FILING_CABINET_BE)) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, type.get(),
                    (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));
        }

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, FLUID_CABINET_BE.get(),
                (blockEntity, direction) -> blockEntity.getFluidCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, FILING_INDEX_BE.get(),
                (blockEntity, direction) -> blockEntity.getFluidCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.Item.BLOCK, FILING_INDEX_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));
    }

    private static Supplier<BlockEntityType<FilingCabinetBlockEntity>> cabinetType(String name, List<DeferredBlock<Block>> blocks) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(FilingCabinetBlockEntity::new,
                blocks.stream().map(DeferredBlock::get).toArray(Block[]::new)));
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(RFRBlockEntities::registerCapabilities);
    }
}