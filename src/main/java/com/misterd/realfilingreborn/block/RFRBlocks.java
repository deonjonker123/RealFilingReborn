package com.misterd.realfilingreborn.block;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RFRBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealFilingReborn.MODID);

    public static final List<DeferredBlock<Block>> SINGLE_CABINETS = new ArrayList<>();
    public static final List<DeferredBlock<Block>> DOUBLE_CABINETS = new ArrayList<>();
    public static final List<DeferredBlock<Block>> QUAD_CABINETS = new ArrayList<>();
    public static final List<DeferredBlock<Block>> ALL_CABINETS = new ArrayList<>();

    public static final DeferredBlock<Block> ACACIA_FILING_CABINET = cabinet("acacia_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> BIRCH_FILING_CABINET = cabinet("birch_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> CHERRY_FILING_CABINET = cabinet("cherry_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> CRIMSON_FILING_CABINET = cabinet("crimson_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> DARK_OAK_FILING_CABINET = cabinet("dark_oak_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> JUNGLE_FILING_CABINET = cabinet("jungle_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> MANGROVE_FILING_CABINET = cabinet("mangrove_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> OAK_FILING_CABINET = cabinet("oak_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> PALE_OAK_FILING_CABINET = cabinet("pale_oak_filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> FILING_CABINET = cabinet("filing_cabinet", 4, QUAD_CABINETS);
    public static final DeferredBlock<Block> WARPED_FILING_CABINET = cabinet("warped_filing_cabinet", 4, QUAD_CABINETS);

    public static final DeferredBlock<Block> SINGLE_ACACIA_FILING_CABINET = cabinet("single_acacia_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_BIRCH_FILING_CABINET = cabinet("single_birch_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_CHERRY_FILING_CABINET = cabinet("single_cherry_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_CRIMSON_FILING_CABINET = cabinet("single_crimson_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_DARK_OAK_FILING_CABINET = cabinet("single_dark_oak_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_JUNGLE_FILING_CABINET = cabinet("single_jungle_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_MANGROVE_FILING_CABINET = cabinet("single_mangrove_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_OAK_FILING_CABINET = cabinet("single_oak_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_PALE_OAK_FILING_CABINET = cabinet("single_pale_oak_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_SPRUCE_FILING_CABINET = cabinet("single_spruce_filing_cabinet", 1, SINGLE_CABINETS);
    public static final DeferredBlock<Block> SINGLE_WARPED_FILING_CABINET = cabinet("single_warped_filing_cabinet", 1, SINGLE_CABINETS);

    public static final DeferredBlock<Block> DOUBLE_ACACIA_FILING_CABINET = cabinet("double_acacia_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_BIRCH_FILING_CABINET = cabinet("double_birch_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_CHERRY_FILING_CABINET = cabinet("double_cherry_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_CRIMSON_FILING_CABINET = cabinet("double_crimson_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_DARK_OAK_FILING_CABINET = cabinet("double_dark_oak_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_JUNGLE_FILING_CABINET = cabinet("double_jungle_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_MANGROVE_FILING_CABINET = cabinet("double_mangrove_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_OAK_FILING_CABINET = cabinet("double_oak_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_PALE_OAK_FILING_CABINET = cabinet("double_pale_oak_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_SPRUCE_FILING_CABINET = cabinet("double_spruce_filing_cabinet", 2, DOUBLE_CABINETS);
    public static final DeferredBlock<Block> DOUBLE_WARPED_FILING_CABINET = cabinet("double_warped_filing_cabinet", 2, DOUBLE_CABINETS);

    public static final DeferredBlock<Block> FLUID_CABINET = registerBlock("fluid_cabinet",
            id -> new FluidCabinetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final DeferredBlock<Block> FILING_INDEX = registerBlock("filing_index",
            id -> new FilingIndexBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .strength(3.0F, 5.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    private static DeferredBlock<Block> cabinet(String name, int slots, List<DeferredBlock<Block>> group) {
        DeferredBlock<Block> block = registerBlock(name, id -> new FilingCabinetBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .strength(2.0F, 5.0F)
                .sound(SoundType.WOOD), slots));
        group.add(block);
        ALL_CABINETS.add(block);
        return block;
    }

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
