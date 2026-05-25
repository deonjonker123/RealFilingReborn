package com.misterd.realfilingreborn.block;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.custom.*;
import com.misterd.realfilingreborn.item.RFRItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class RFRBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealFilingReborn.MODID);

    public static final DeferredBlock<Block> FILING_CABINET = registerBlock("filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> ACACIA_FILING_CABINET = registerBlock("acacia_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BIRCH_FILING_CABINET = registerBlock("birch_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> CHERRY_FILING_CABINET = registerBlock("cherry_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> CRIMSON_FILING_CABINET = registerBlock("crimson_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DARK_OAK_FILING_CABINET = registerBlock("dark_oak_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> JUNGLE_FILING_CABINET = registerBlock("jungle_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MANGROVE_FILING_CABINET = registerBlock("mangrove_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> OAK_FILING_CABINET = registerBlock("oak_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> WARPED_FILING_CABINET = registerBlock("warped_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PALE_OAK_FILING_CABINET = registerBlock("pale_oak_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> FILING_INDEX = registerBlock("filing_index",
            id -> new FilingIndexBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<Identifier, T> factory) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, factory);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        RFRItems.ITEMS.register(name, id -> new BlockItem(block.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}