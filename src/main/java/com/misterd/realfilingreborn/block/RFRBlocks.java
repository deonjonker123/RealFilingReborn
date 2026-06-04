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

    // Quad
    public static final DeferredBlock<Block> ACACIA_FILING_CABINET = registerBlock("acacia_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> BIRCH_FILING_CABINET = registerBlock("birch_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> CHERRY_FILING_CABINET = registerBlock("cherry_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> CRIMSON_FILING_CABINET = registerBlock("crimson_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DARK_OAK_FILING_CABINET = registerBlock("dark_oak_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> JUNGLE_FILING_CABINET = registerBlock("jungle_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> MANGROVE_FILING_CABINET = registerBlock("mangrove_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> OAK_FILING_CABINET = registerBlock("oak_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> PALE_OAK_FILING_CABINET = registerBlock("pale_oak_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> FILING_CABINET = registerBlock("filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> WARPED_FILING_CABINET = registerBlock("warped_filing_cabinet",
            id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    // Single
    public static final DeferredBlock<Block> SINGLE_ACACIA_FILING_CABINET = registerBlock("single_acacia_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_BIRCH_FILING_CABINET = registerBlock("single_birch_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_CHERRY_FILING_CABINET = registerBlock("single_cherry_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_CRIMSON_FILING_CABINET = registerBlock("single_crimson_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_DARK_OAK_FILING_CABINET = registerBlock("single_dark_oak_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_JUNGLE_FILING_CABINET = registerBlock("single_jungle_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_MANGROVE_FILING_CABINET = registerBlock("single_mangrove_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_OAK_FILING_CABINET = registerBlock("single_oak_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_PALE_OAK_FILING_CABINET = registerBlock("single_pale_oak_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_SPRUCE_FILING_CABINET = registerBlock("single_spruce_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> SINGLE_WARPED_FILING_CABINET = registerBlock("single_warped_filing_cabinet",
            id -> new SingleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    // Double
    public static final DeferredBlock<Block> DOUBLE_ACACIA_FILING_CABINET = registerBlock("double_acacia_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_BIRCH_FILING_CABINET = registerBlock("double_birch_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_CHERRY_FILING_CABINET = registerBlock("double_cherry_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_CRIMSON_FILING_CABINET = registerBlock("double_crimson_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_DARK_OAK_FILING_CABINET = registerBlock("double_dark_oak_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_JUNGLE_FILING_CABINET = registerBlock("double_jungle_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_MANGROVE_FILING_CABINET = registerBlock("double_mangrove_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_OAK_FILING_CABINET = registerBlock("double_oak_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
                    ));

    public static final DeferredBlock<Block> DOUBLE_PALE_OAK_FILING_CABINET = registerBlock("double_pale_oak_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_SPRUCE_FILING_CABINET = registerBlock("double_spruce_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> DOUBLE_WARPED_FILING_CABINET = registerBlock("double_warped_filing_cabinet",
            id -> new DoubleFilingCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)
            ));

    public static final DeferredBlock<Block> FILING_INDEX = registerBlock("filing_index",
            id -> new FilingIndexBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

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