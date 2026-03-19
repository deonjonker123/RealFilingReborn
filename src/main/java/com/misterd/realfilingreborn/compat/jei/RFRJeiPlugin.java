package com.misterd.realfilingreborn.compat.jei;

import com.misterd.realfilingreborn.item.RFRItems;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

@JeiPlugin
public class RFRJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new FolderUpgradeRecipeCategory(guiHelper),
                new CanisterUpgradeRecipeCategory(guiHelper),
                new FolderResetRecipeCategory(guiHelper),
                new CanisterResetRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FolderUpgradeRecipeCategory.RECIPE_TYPE, generateFolderUpgradeRecipes());
        registration.addRecipes(CanisterUpgradeRecipeCategory.RECIPE_TYPE, generateCanisterUpgradeRecipes());
        registration.addRecipes(FolderResetRecipeCategory.RECIPE_TYPE, generateFolderResetRecipes());
        registration.addRecipes(CanisterResetRecipeCategory.RECIPE_TYPE, generateCanisterResetRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE),
                FolderUpgradeRecipeCategory.RECIPE_TYPE,
                CanisterUpgradeRecipeCategory.RECIPE_TYPE,
                FolderResetRecipeCategory.RECIPE_TYPE,
                CanisterResetRecipeCategory.RECIPE_TYPE);
    }

    private List<RFRJeiRecipes.FolderUpgradeJeiRecipe> generateFolderUpgradeRecipes() {
        return List.of(
                new RFRJeiRecipes.FolderUpgradeJeiRecipe((FilingFolderItem) RFRItems.FILING_FOLDER.get(), (FilingFolderItem) RFRItems.COPPER_FILING_FOLDER.get(),    Items.COPPER_INGOT),
                new RFRJeiRecipes.FolderUpgradeJeiRecipe((FilingFolderItem) RFRItems.COPPER_FILING_FOLDER.get(),  (FilingFolderItem) RFRItems.IRON_FILING_FOLDER.get(),      Items.IRON_INGOT),
                new RFRJeiRecipes.FolderUpgradeJeiRecipe((FilingFolderItem) RFRItems.IRON_FILING_FOLDER.get(),    (FilingFolderItem) RFRItems.GOLD_FILING_FOLDER.get(),      Items.GOLD_INGOT),
                new RFRJeiRecipes.FolderUpgradeJeiRecipe((FilingFolderItem) RFRItems.GOLD_FILING_FOLDER.get(),    (FilingFolderItem) RFRItems.DIAMOND_FILING_FOLDER.get(),   Items.DIAMOND),
                new RFRJeiRecipes.FolderUpgradeJeiRecipe((FilingFolderItem) RFRItems.DIAMOND_FILING_FOLDER.get(), (FilingFolderItem) RFRItems.NETHERITE_FILING_FOLDER.get(), Items.NETHERITE_INGOT)
        );
    }

    private List<RFRJeiRecipes.CanisterUpgradeJeiRecipe> generateCanisterUpgradeRecipes() {
        return List.of(
                new RFRJeiRecipes.CanisterUpgradeJeiRecipe((FluidCanisterItem) RFRItems.FLUID_CANISTER.get(),    (FluidCanisterItem) RFRItems.COPPER_FLUID_CANISTER.get(),    Items.COPPER_INGOT),
                new RFRJeiRecipes.CanisterUpgradeJeiRecipe((FluidCanisterItem) RFRItems.COPPER_FLUID_CANISTER.get(),  (FluidCanisterItem) RFRItems.IRON_FLUID_CANISTER.get(),      Items.IRON_INGOT),
                new RFRJeiRecipes.CanisterUpgradeJeiRecipe((FluidCanisterItem) RFRItems.IRON_FLUID_CANISTER.get(),    (FluidCanisterItem) RFRItems.GOLD_FLUID_CANISTER.get(),      Items.GOLD_INGOT),
                new RFRJeiRecipes.CanisterUpgradeJeiRecipe((FluidCanisterItem) RFRItems.GOLD_FLUID_CANISTER.get(),    (FluidCanisterItem) RFRItems.DIAMOND_FLUID_CANISTER.get(),   Items.DIAMOND),
                new RFRJeiRecipes.CanisterUpgradeJeiRecipe((FluidCanisterItem) RFRItems.DIAMOND_FLUID_CANISTER.get(), (FluidCanisterItem) RFRItems.NETHERITE_FLUID_CANISTER.get(), Items.NETHERITE_INGOT)
        );
    }

    private List<RFRJeiRecipes.FolderResetJeiRecipe> generateFolderResetRecipes() {
        return List.of(
                new RFRJeiRecipes.FolderResetJeiRecipe((FilingFolderItem) RFRItems.FILING_FOLDER.get()),
                new RFRJeiRecipes.FolderResetJeiRecipe((FilingFolderItem) RFRItems.COPPER_FILING_FOLDER.get()),
                new RFRJeiRecipes.FolderResetJeiRecipe((FilingFolderItem) RFRItems.IRON_FILING_FOLDER.get()),
                new RFRJeiRecipes.FolderResetJeiRecipe((FilingFolderItem) RFRItems.GOLD_FILING_FOLDER.get()),
                new RFRJeiRecipes.FolderResetJeiRecipe((FilingFolderItem) RFRItems.DIAMOND_FILING_FOLDER.get()),
                new RFRJeiRecipes.FolderResetJeiRecipe((FilingFolderItem) RFRItems.NETHERITE_FILING_FOLDER.get())
        );
    }

    private List<RFRJeiRecipes.CanisterResetJeiRecipe> generateCanisterResetRecipes() {
        return List.of(
                new RFRJeiRecipes.CanisterResetJeiRecipe((FluidCanisterItem) RFRItems.FLUID_CANISTER.get()),
                new RFRJeiRecipes.CanisterResetJeiRecipe((FluidCanisterItem) RFRItems.COPPER_FLUID_CANISTER.get()),
                new RFRJeiRecipes.CanisterResetJeiRecipe((FluidCanisterItem) RFRItems.IRON_FLUID_CANISTER.get()),
                new RFRJeiRecipes.CanisterResetJeiRecipe((FluidCanisterItem) RFRItems.GOLD_FLUID_CANISTER.get()),
                new RFRJeiRecipes.CanisterResetJeiRecipe((FluidCanisterItem) RFRItems.DIAMOND_FLUID_CANISTER.get()),
                new RFRJeiRecipes.CanisterResetJeiRecipe((FluidCanisterItem) RFRItems.NETHERITE_FLUID_CANISTER.get())
        );
    }
}
