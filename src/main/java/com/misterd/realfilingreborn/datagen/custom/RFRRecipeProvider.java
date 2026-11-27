package com.misterd.realfilingreborn.datagen.custom;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.item.RFRItems;
import com.misterd.realfilingreborn.util.RFRTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class RFRRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public RFRRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.FILING_FOLDER.get(), 5)
                .pattern("PPP")
                .pattern("PG ")
                .pattern("PPP")
                .define('P', Items.PAPER)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.COPPER_FILING_FOLDER.get(), 5)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.COPPER_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.IRON_FILING_FOLDER.get(), 5)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.IRON_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.GOLD_FILING_FOLDER.get(), 5)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.GOLD_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.DIAMOND_FILING_FOLDER.get(), 5)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.DIAMOND)
                .define('P', Items.PAPER)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.NETHERITE_FILING_FOLDER.get(), 5)
                .pattern("IPI")
                .pattern("IGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.NETHERITE_INGOT)
                .define('P', Items.PAPER)
                .unlockedBy("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.FLUID_CANISTER.get(), 4)
                .pattern("BGB")
                .pattern(" B ")
                .define('B', Items.BUCKET)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.COPPER_FLUID_CANISTER.get(), 4)
                .pattern("I I")
                .pattern("PGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.COPPER_INGOT)
                .define('P', Items.BUCKET)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.IRON_FLUID_CANISTER.get(), 4)
                .pattern("I I")
                .pattern("PGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.IRON_INGOT)
                .define('P', Items.BUCKET)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.GOLD_FLUID_CANISTER.get(), 4)
                .pattern("I I")
                .pattern("PGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.GOLD_INGOT)
                .define('P', Items.BUCKET)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.DIAMOND_FLUID_CANISTER.get(), 4)
                .pattern("I I")
                .pattern("PGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.DIAMOND)
                .define('P', Items.BUCKET)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.NETHERITE_FLUID_CANISTER.get(), 4)
                .pattern("I I")
                .pattern("PGP")
                .pattern("IPI")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.NETHERITE_INGOT)
                .define('P', Items.BUCKET)
                .unlockedBy("has_netherite_ingot", has(Items.NETHERITE_INGOT))
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
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRItems.DIAMOND_RANGE_UPGRADE.get())
                .pattern("DRD")
                .pattern("RGR")
                .pattern("DRD")
                .define('R', Items.REDSTONE)
                .define('G', RFRItems.IRON_RANGE_UPGRADE.get())
                .define('D', Items.DIAMOND)
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

        cabinetRecipes(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRBlocks.FLUID_CABINET.get())
                .pattern("LPL")
                .pattern("PBP")
                .pattern("LPL")
                .define('L', Tags.Items.STONES)
                .define('P', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .unlockedBy("has_fluid_canister", has(RFRItems.FLUID_CANISTER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RFRBlocks.FILING_INDEX.get())
                .pattern("IXI")
                .pattern("RFR")
                .pattern("ICI")
                .define('R', Items.REDSTONE)
                .define('F', RFRTags.Items.FILING_CABINET_ITEMS)
                .define('X', Items.REPEATER)
                .define('C', Items.COMPARATOR)
                .define('I', Items.QUARTZ_BLOCK)
                .unlockedBy("has_cabinet", has(RFRBlocks.FILING_CABINET))
                .save(recipeOutput);
    }

    private record Wood(String name, ItemLike planks, ItemLike single, ItemLike dbl, ItemLike quad) {}

    private static List<Wood> woods() {
        return List.of(
                new Wood("acacia", Items.ACACIA_PLANKS, RFRBlocks.SINGLE_ACACIA_FILING_CABINET, RFRBlocks.DOUBLE_ACACIA_FILING_CABINET, RFRBlocks.ACACIA_FILING_CABINET),
                new Wood("birch", Items.BIRCH_PLANKS, RFRBlocks.SINGLE_BIRCH_FILING_CABINET, RFRBlocks.DOUBLE_BIRCH_FILING_CABINET, RFRBlocks.BIRCH_FILING_CABINET),
                new Wood("cherry", Items.CHERRY_PLANKS, RFRBlocks.SINGLE_CHERRY_FILING_CABINET, RFRBlocks.DOUBLE_CHERRY_FILING_CABINET, RFRBlocks.CHERRY_FILING_CABINET),
                new Wood("crimson", Items.CRIMSON_PLANKS, RFRBlocks.SINGLE_CRIMSON_FILING_CABINET, RFRBlocks.DOUBLE_CRIMSON_FILING_CABINET, RFRBlocks.CRIMSON_FILING_CABINET),
                new Wood("dark_oak", Items.DARK_OAK_PLANKS, RFRBlocks.SINGLE_DARK_OAK_FILING_CABINET, RFRBlocks.DOUBLE_DARK_OAK_FILING_CABINET, RFRBlocks.DARKOAK_FILING_CABINET),
                new Wood("jungle", Items.JUNGLE_PLANKS, RFRBlocks.SINGLE_JUNGLE_FILING_CABINET, RFRBlocks.DOUBLE_JUNGLE_FILING_CABINET, RFRBlocks.JUNGLE_FILING_CABINET),
                new Wood("mangrove", Items.MANGROVE_PLANKS, RFRBlocks.SINGLE_MANGROVE_FILING_CABINET, RFRBlocks.DOUBLE_MANGROVE_FILING_CABINET, RFRBlocks.MANGROVE_FILING_CABINET),
                new Wood("oak", Items.OAK_PLANKS, RFRBlocks.SINGLE_OAK_FILING_CABINET, RFRBlocks.DOUBLE_OAK_FILING_CABINET, RFRBlocks.OAK_FILING_CABINET),
                new Wood("spruce", Items.SPRUCE_PLANKS, RFRBlocks.SINGLE_SPRUCE_FILING_CABINET, RFRBlocks.DOUBLE_SPRUCE_FILING_CABINET, RFRBlocks.FILING_CABINET),
                new Wood("warped", Items.WARPED_PLANKS, RFRBlocks.SINGLE_WARPED_FILING_CABINET, RFRBlocks.DOUBLE_WARPED_FILING_CABINET, RFRBlocks.WARPED_FILING_CABINET)
        );
    }

    private void cabinetRecipes(RecipeOutput recipeOutput) {
        List<Wood> woods = woods();

        for (Wood wood : woods) {
            cabinetShape(wood.single(), 1, "LLL", "LBL", "LLL", Ingredient.of(wood.planks()), recipeOutput, null);
            cabinetShape(wood.dbl(), 2, "LBL", "LLL", "LBL", Ingredient.of(wood.planks()), recipeOutput, null);
            cabinetShape(wood.quad(), 4, "BLB", "LLL", "BLB", Ingredient.of(wood.planks()), recipeOutput, null);
        }

        Ingredient otherPlanks = DifferenceIngredient.of(Ingredient.of(ItemTags.PLANKS),
                Ingredient.of(woods.stream().map(Wood::planks).toArray(ItemLike[]::new)));
        cabinetShape(RFRBlocks.SINGLE_SPRUCE_FILING_CABINET, 1, "LLL", "LBL", "LLL", otherPlanks, recipeOutput, "single_filing_cabinet_from_wood");
        cabinetShape(RFRBlocks.DOUBLE_SPRUCE_FILING_CABINET, 2, "LBL", "LLL", "LBL", otherPlanks, recipeOutput, "double_filing_cabinet_from_wood");
        cabinetShape(RFRBlocks.FILING_CABINET, 4, "BLB", "LLL", "BLB", otherPlanks, recipeOutput, "filing_cabinet_from_wood");
    }

    private static void cabinetShape(ItemLike result, int count, String row1, String row2, String row3, Ingredient planks, RecipeOutput recipeOutput, @Nullable String id) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count)
                .pattern(row1)
                .pattern(row2)
                .pattern(row3)
                .define('L', planks)
                .define('B', Tags.Items.CHESTS)
                .unlockedBy("has_filing_folder", has(RFRItems.FILING_FOLDER.get()));
        if (id == null) builder.save(recipeOutput);
        else builder.save(recipeOutput, "realfilingreborn:" + id);
    }
}
