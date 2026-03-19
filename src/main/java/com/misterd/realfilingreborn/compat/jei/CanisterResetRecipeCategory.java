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

public class CanisterResetRecipeCategory implements IRecipeCategory<RFRJeiRecipes.CanisterResetJeiRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "canister_reset");
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "textures/gui/upgrades_gui.png");
    public static final RecipeType<RFRJeiRecipes.CanisterResetJeiRecipe> RECIPE_TYPE =
            new RecipeType<>(UID, RFRJeiRecipes.CanisterResetJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CanisterResetRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 122, 72);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RFRItems.FLUID_CANISTER.get()));
    }

    @Override public RecipeType<RFRJeiRecipes.CanisterResetJeiRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.realfilingreborn.canister_reset"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 122; }
    @Override public int getHeight() { return 72; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RFRJeiRecipes.CanisterResetJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 28)
                .addItemStack(new ItemStack(recipe.input()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 28)
                .addItemStack(new ItemStack(recipe.input()));
    }

    @Override
    public void draw(RFRJeiRecipes.CanisterResetJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
    }
}