package com.misterd.realfilingreborn.datagen.custom;

import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.item.RFRItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class RFRRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public RFRRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.FILING_FOLDER.get(), 8)
                .pattern("PPP")
                .pattern("PG ")
                .pattern("PPP")
                .define('P', Items.PAPER)
                .define('G', net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.FLUID_CANISTER.get(), 4)
                .pattern("   ")
                .pattern("BGB")
                .pattern(" B ")
                .define('B', Items.BUCKET)
                .define('G', net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.ERASER.get())
                .pattern("  Q")
                .pattern("QR ")
                .pattern("QQ ")
                .define('Q', Items.QUARTZ)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.CABINET_CONVERSION_KIT.get(), 3)
                .pattern("C C")
                .pattern("BIB")
                .pattern("RBR")
                .define('C', Items.COPPER_INGOT)
                .define('B', RFRItems.FLUID_CANISTER.get())
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_BLOCK)
                .unlockedBy("has_fluid_canister", has(RFRItems.FLUID_CANISTER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.LEDGER.get())
                .pattern("RQR")
                .pattern("QBQ")
                .pattern("RQR")
                .define('R', Items.REDSTONE)
                .define('B', Items.BOOK)
                .define('Q', Items.QUARTZ)
                .unlockedBy("has_quartz", has(Items.QUARTZ))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.IRON_RANGE_UPGRADE.get())
                .pattern("IRI")
                .pattern("RGR")
                .pattern("IRI")
                .define('R', Items.REDSTONE)
                .define('G', net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.DIAMOND_RANGE_UPGRADE.get())
                .pattern("DRD")
                .pattern("RGR")
                .pattern("DRD")
                .define('R', Items.REDSTONE)
                .define('G', RFRItems.IRON_RANGE_UPGRADE.get())
                .define('D', Items.IRON_INGOT)
                .unlockedBy("has_iron_upgrade", has(RFRItems.IRON_RANGE_UPGRADE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.NETHERITE_RANGE_UPGRADE.get())
                .pattern("NRN")
                .pattern("RGR")
                .pattern("NRN")
                .define('R', Items.REDSTONE)
                .define('G', RFRItems.DIAMOND_RANGE_UPGRADE.get())
                .define('N', Items.NETHERITE_INGOT)
                .unlockedBy("has_diamond_upgrade", has(RFRItems.DIAMOND_RANGE_UPGRADE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRBlocks.FILING_CABINET.get())
                .pattern("CFC")
                .pattern("FIF")
                .pattern("CFC")
                .define('C', Items.COPPER_INGOT)
                .define('F', RFRItems.FILING_FOLDER.get())
                .define('I', Items.IRON_BLOCK)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRBlocks.FLUID_CABINET.get())
                .pattern("CFC")
                .pattern("FIF")
                .pattern("CFC")
                .define('C', Items.COPPER_INGOT)
                .define('F', RFRItems.FLUID_CANISTER.get())
                .define('I', Items.IRON_BLOCK)
                .unlockedBy("has_fluid_canister", has(RFRItems.FLUID_CANISTER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRBlocks.FILING_INDEX.get())
                .pattern("IXI")
                .pattern("RFR")
                .pattern("ICI")
                .define('R', Items.REDSTONE)
                .define('F', RFRBlocks.FILING_CABINET)
                .define('X', Items.REPEATER)
                .define('C', Items.COMPARATOR)
                .define('I', Items.COPPER_INGOT)
                .unlockedBy("has_cabinet", has(RFRBlocks.FILING_CABINET))
                .save(recipeOutput);
    }
}
