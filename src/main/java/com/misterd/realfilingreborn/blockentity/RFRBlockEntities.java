package com.misterd.realfilingreborn.blockentity;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.blockentity.custom.DoubleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.SingleFilingCabinetBlockEntity;
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
            BLOCK_ENTITIES.register("filing_cabinet_be", () -> new BlockEntityType<>(
                    FilingCabinetBlockEntity::new,
                    RFRBlocks.FILING_CABINET.get(),
                    RFRBlocks.ACACIA_FILING_CABINET.get(),
                    RFRBlocks.BIRCH_FILING_CABINET.get(),
                    RFRBlocks.CHERRY_FILING_CABINET.get(),
                    RFRBlocks.CRIMSON_FILING_CABINET.get(),
                    RFRBlocks.DARK_OAK_FILING_CABINET.get(),
                    RFRBlocks.JUNGLE_FILING_CABINET.get(),
                    RFRBlocks.MANGROVE_FILING_CABINET.get(),
                    RFRBlocks.OAK_FILING_CABINET.get(),
                    RFRBlocks.WARPED_FILING_CABINET.get(),
                    RFRBlocks.PALE_OAK_FILING_CABINET.get()
            ));

    public static final Supplier<BlockEntityType<DoubleFilingCabinetBlockEntity>> DOUBLE_FILING_CABINET_BE =
            BLOCK_ENTITIES.register("double_filing_cabinet_be", () -> new BlockEntityType<>(
                    DoubleFilingCabinetBlockEntity::new,
                    RFRBlocks.DOUBLE_SPRUCE_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_ACACIA_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_BIRCH_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_CHERRY_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_CRIMSON_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_DARK_OAK_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_JUNGLE_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_MANGROVE_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_OAK_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_WARPED_FILING_CABINET.get(),
                    RFRBlocks.DOUBLE_PALE_OAK_FILING_CABINET.get()
            ));

    public static final Supplier<BlockEntityType<SingleFilingCabinetBlockEntity>> SINGLE_FILING_CABINET_BE =
            BLOCK_ENTITIES.register("single_filing_cabinet_be", () -> new BlockEntityType<>(
                    SingleFilingCabinetBlockEntity::new,
                    RFRBlocks.SINGLE_SPRUCE_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_ACACIA_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_BIRCH_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_CHERRY_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_CRIMSON_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_DARK_OAK_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_JUNGLE_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_MANGROVE_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_OAK_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_WARPED_FILING_CABINET.get(),
                    RFRBlocks.SINGLE_PALE_OAK_FILING_CABINET.get()
            ));

    public static final Supplier<BlockEntityType<FilingIndexBlockEntity>> FILING_INDEX_BE =
            BLOCK_ENTITIES.register("filing_index_be", () -> new BlockEntityType<>(
                    FilingIndexBlockEntity::new, RFRBlocks.FILING_INDEX.get()));

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, FILING_CABINET_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.Item.BLOCK, DOUBLE_FILING_CABINET_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.Item.BLOCK, SINGLE_FILING_CABINET_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));

        event.registerBlockEntity(Capabilities.Item.BLOCK, FILING_INDEX_BE.get(),
                (blockEntity, direction) -> blockEntity.getCapabilityHandler(direction));
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(RFRBlockEntities::registerCapabilities);
    }
}