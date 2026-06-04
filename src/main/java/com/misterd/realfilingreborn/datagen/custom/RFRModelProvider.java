package com.misterd.realfilingreborn.datagen.custom;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.item.RFRItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.stream.Stream;

public class RFRModelProvider extends ModelProvider {
    public RFRModelProvider(PackOutput output) {
        super(output, RealFilingReborn.MODID);
    }

    private static final PropertyDispatch<VariantMutator> ROTATION_HORIZONTAL_FACING =
            PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                    .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                    .select(Direction.WEST, BlockModelGenerators.Y_ROT_270)
                    .select(Direction.NORTH, BlockModelGenerators.NOP);

    @Override
    protected void registerModels(BlockModelGenerators g, ItemModelGenerators itemModels) {

        registerCabinet(g, "single_acacia_filing_cabinet", RFRBlocks.SINGLE_ACACIA_FILING_CABINET);
        registerCabinet(g, "single_birch_filing_cabinet", RFRBlocks.SINGLE_BIRCH_FILING_CABINET);
        registerCabinet(g, "single_cherry_filing_cabinet", RFRBlocks.SINGLE_CHERRY_FILING_CABINET);
        registerCabinet(g, "single_crimson_filing_cabinet", RFRBlocks.SINGLE_CRIMSON_FILING_CABINET);
        registerCabinet(g, "single_dark_oak_filing_cabinet", RFRBlocks.SINGLE_DARK_OAK_FILING_CABINET);
        registerCabinet(g, "single_jungle_filing_cabinet", RFRBlocks.SINGLE_JUNGLE_FILING_CABINET);
        registerCabinet(g, "single_mangrove_filing_cabinet", RFRBlocks.SINGLE_MANGROVE_FILING_CABINET);
        registerCabinet(g, "single_oak_filing_cabinet", RFRBlocks.SINGLE_OAK_FILING_CABINET);
        registerCabinet(g, "single_pale_oak_filing_cabinet", RFRBlocks.SINGLE_PALE_OAK_FILING_CABINET);
        registerCabinet(g, "single_spruce_filing_cabinet", RFRBlocks.SINGLE_SPRUCE_FILING_CABINET);
        registerCabinet(g, "single_warped_filing_cabinet", RFRBlocks.SINGLE_WARPED_FILING_CABINET);

        registerCabinet(g, "double_acacia_filing_cabinet", RFRBlocks.DOUBLE_ACACIA_FILING_CABINET);
        registerCabinet(g, "double_birch_filing_cabinet", RFRBlocks.DOUBLE_BIRCH_FILING_CABINET);
        registerCabinet(g, "double_cherry_filing_cabinet", RFRBlocks.DOUBLE_CHERRY_FILING_CABINET);
        registerCabinet(g, "double_crimson_filing_cabinet", RFRBlocks.DOUBLE_CRIMSON_FILING_CABINET);
        registerCabinet(g, "double_dark_oak_filing_cabinet", RFRBlocks.DOUBLE_DARK_OAK_FILING_CABINET);
        registerCabinet(g, "double_jungle_filing_cabinet", RFRBlocks.DOUBLE_JUNGLE_FILING_CABINET);
        registerCabinet(g, "double_mangrove_filing_cabinet", RFRBlocks.DOUBLE_MANGROVE_FILING_CABINET);
        registerCabinet(g, "double_oak_filing_cabinet", RFRBlocks.DOUBLE_OAK_FILING_CABINET);
        registerCabinet(g, "double_pale_oak_filing_cabinet", RFRBlocks.DOUBLE_PALE_OAK_FILING_CABINET);
        registerCabinet(g, "double_spruce_filing_cabinet", RFRBlocks.DOUBLE_SPRUCE_FILING_CABINET);
        registerCabinet(g, "double_warped_filing_cabinet", RFRBlocks.DOUBLE_WARPED_FILING_CABINET);

        registerCabinet(g, "acacia_filing_cabinet", RFRBlocks.ACACIA_FILING_CABINET);
        registerCabinet(g, "birch_filing_cabinet", RFRBlocks.BIRCH_FILING_CABINET);
        registerCabinet(g, "cherry_filing_cabinet", RFRBlocks.CHERRY_FILING_CABINET);
        registerCabinet(g, "crimson_filing_cabinet", RFRBlocks.CRIMSON_FILING_CABINET);
        registerCabinet(g, "dark_oak_filing_cabinet", RFRBlocks.DARK_OAK_FILING_CABINET);
        registerCabinet(g, "jungle_filing_cabinet", RFRBlocks.JUNGLE_FILING_CABINET);
        registerCabinet(g, "mangrove_filing_cabinet", RFRBlocks.MANGROVE_FILING_CABINET);
        registerCabinet(g, "oak_filing_cabinet", RFRBlocks.OAK_FILING_CABINET);
        registerCabinet(g, "pale_oak_filing_cabinet", RFRBlocks.PALE_OAK_FILING_CABINET);
        registerCabinet(g, "filing_cabinet", RFRBlocks.FILING_CABINET);
        registerCabinet(g, "warped_filing_cabinet", RFRBlocks.WARPED_FILING_CABINET);

        g.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(RFRBlocks.FILING_INDEX.get(),
                        BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(RealFilingReborn.MODID, "block/filing_index")))
                .with(ROTATION_HORIZONTAL_FACING));

        itemModels.generateFlatItem(RFRItems.FILING_FOLDER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.COPPER_FILING_FOLDER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.IRON_FILING_FOLDER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.GOLD_FILING_FOLDER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.DIAMOND_FILING_FOLDER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.NETHERITE_FILING_FOLDER.get(), ModelTemplates.FLAT_ITEM);

        itemModels.generateFlatItem(RFRItems.LEDGER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.IRON_RANGE_UPGRADE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.DIAMOND_RANGE_UPGRADE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RFRItems.NETHERITE_RANGE_UPGRADE.get(), ModelTemplates.FLAT_ITEM);
    }

    private void registerCabinet(BlockModelGenerators g, String name, DeferredBlock<Block> block) {
        g.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block.get(),
                        BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(RealFilingReborn.MODID, "block/" + name)))
                .with(ROTATION_HORIZONTAL_FACING));
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return RFRBlocks.BLOCKS.getEntries().stream();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return RFRItems.ITEMS.getEntries().stream();
    }
}