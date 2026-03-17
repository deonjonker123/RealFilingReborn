package com.misterd.realfilingreborn.block;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
import com.misterd.realfilingreborn.item.RFRItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RFRBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealFilingReborn.MODID);

    public static final DeferredBlock<Block> FILING_CABINET = registerBlock("filing_cabinet",
            () -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> FLUID_CABINET = registerBlock("fluid_cabinet",
            () -> new FluidCabinetBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> FILING_INDEX = registerBlock("filing_index",
            () -> new FilingIndexBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        RFRItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
