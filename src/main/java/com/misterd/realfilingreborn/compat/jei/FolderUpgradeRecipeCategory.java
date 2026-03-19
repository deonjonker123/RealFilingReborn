package com.misterd.realfilingreborn.compat.jei;

import com.misterd.realfilingreborn.item.RFRItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FolderUpgradeRecipeCategory implements IRecipeCategory<RFRJeiRecipes.FolderUpgradeJeiRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "folder_upgrade");
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "textures/gui/upgrades_gui.png");
    public static final RecipeType<RFRJeiRecipes.FolderUpgradeJeiRecipe> RECIPE_TYPE =
            new RecipeType<>(UID, RFRJeiRecipes.FolderUpgradeJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public FolderUpgradeRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 122, 72);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RFRItems.COPPER_FILING_FOLDER.get()));
    }

    @Override public RecipeType<RFRJeiRecipes.FolderUpgradeJeiRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.realfilingreborn.folder_upgrade"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 122; }
    @Override public int getHeight() { return 72; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RFRJeiRecipes.FolderUpgradeJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 10)
                .addItemStack(new ItemStack(recipe.input()));

        builder.addSlot(RecipeIngredientRole.INPUT, 28, 10)
                .addItemStack(new ItemStack(recipe.material()));
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 28)
                .addItemStack(new ItemStack(recipe.material()));
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 28)
                .addItemStack(new ItemStack(recipe.material()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 28)
                .addItemStack(new ItemStack(recipe.output()));
    }

    @Override
    public void draw(RFRJeiRecipes.FolderUpgradeJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
    }
}
