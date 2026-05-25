package com.misterd.realfilingreborn.datagen.custom;

import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.item.RFRItems;
import com.misterd.realfilingreborn.util.RFRTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class RFRRecipeProvider extends RecipeProvider {
    public RFRRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
        super(provider, recipeOutput);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
            return new RFRRecipeProvider(provider, recipeOutput);
        }

        @Override
        public String getName() {
            return "RFR Recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.MISC, RFRItems.FILING_FOLDER.get(), 4)
                .pattern("PPP")
                .pattern("PG ")
                .pattern("PPP")
                .define('P', Items.PAPER)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.COPPER_FILING_FOLDER.get(), 4)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.COPPER_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.IRON_FILING_FOLDER.get(), 4)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.IRON_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.GOLD_FILING_FOLDER.get(), 4)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.GOLD_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.DIAMOND_FILING_FOLDER.get(), 4)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.DIAMOND)
                .define('P', Items.PAPER)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.NETHERITE_FILING_FOLDER.get(), 4)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.NETHERITE_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.IRON_RANGE_UPGRADE.get())
                .pattern("IRI")
                .pattern("RGR")
                .pattern("IRI")
                .define('R', Items.REDSTONE)
                .define('G', Items.IRON_BLOCK)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.DIAMOND_RANGE_UPGRADE.get())
                .pattern("DRD")
                .pattern("RGR")
                .pattern("DRD")
                .define('R', Items.REDSTONE)
                .define('G', RFRItems.IRON_RANGE_UPGRADE.get())
                .define('D', Items.DIAMOND)
                .unlockedBy("has_iron_upgrade", has(RFRItems.IRON_RANGE_UPGRADE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.NETHERITE_RANGE_UPGRADE.get())
                .pattern("NRN")
                .pattern("RGR")
                .pattern("NRN")
                .define('R', Items.REDSTONE)
                .define('G', RFRItems.DIAMOND_RANGE_UPGRADE.get())
                .define('N', Items.NETHERITE_INGOT)
                .unlockedBy("has_diamond_upgrade", has(RFRItems.DIAMOND_RANGE_UPGRADE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRItems.LEDGER.get())
                .pattern("RQR")
                .pattern("QBQ")
                .pattern("RQR")
                .define('R', Items.REDSTONE)
                .define('B', Items.BOOK)
                .define('Q', Items.QUARTZ)
                .unlockedBy("has_quartz", has(Items.QUARTZ))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_SPRUCE_LOG)
                .define('P', Items.SPRUCE_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', ItemTags.LOGS)
                .define('P', ItemTags.PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output, "realfilingreborn:filing_cabinet_from_wood");

        shaped(RecipeCategory.MISC, RFRBlocks.ACACIA_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_ACACIA_LOG)
                .define('P', Items.ACACIA_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.BIRCH_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_BIRCH_LOG)
                .define('P', Items.BIRCH_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.CHERRY_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_CHERRY_LOG)
                .define('P', Items.CHERRY_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.CRIMSON_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_CRIMSON_STEM)
                .define('P', Items.CRIMSON_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.DARK_OAK_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_DARK_OAK_LOG)
                .define('P', Items.DARK_OAK_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.JUNGLE_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_JUNGLE_LOG)
                .define('P', Items.JUNGLE_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.MANGROVE_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_MANGROVE_LOG)
                .define('P', Items.MANGROVE_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.OAK_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_OAK_LOG)
                .define('P', Items.OAK_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.WARPED_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_WARPED_STEM)
                .define('P', Items.WARPED_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.PALE_OAK_FILING_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Items.STRIPPED_PALE_OAK_LOG)
                .define('P', Items.PALE_OAK_PLANKS)
                .define('B', Items.BARREL)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(output);

        shaped(RecipeCategory.MISC, RFRBlocks.FILING_INDEX.get())
                .pattern("IXI")
                .pattern("RFR")
                .pattern("ICI")
                .define('R', Items.REDSTONE)
                .define('F', RFRTags.Items.FILING_CABINET_ITEMS)
                .define('X', Items.REPEATER)
                .define('C', Items.COMPARATOR)
                .define('I', Items.QUARTZ_BLOCK)
                .unlockedBy("has_cabinet", has(RFRBlocks.FILING_CABINET))
                .save(output);
    }
}