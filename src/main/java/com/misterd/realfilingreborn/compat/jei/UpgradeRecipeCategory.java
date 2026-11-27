package com.misterd.realfilingreborn.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class UpgradeRecipeCategory extends AbstractRecipeCategory<RFRJeiRecipes.Upgrade> {
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("realfilingreborn", "textures/gui/upgrades_gui.png");

    public UpgradeRecipeCategory(IGuiHelper guiHelper, IRecipeType<RFRJeiRecipes.Upgrade> type, String titleKey, ItemLike icon) {
        super(type, Component.translatable(titleKey), guiHelper.createDrawableItemLike(icon), 122, 72);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RFRJeiRecipes.Upgrade recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 10).add(new ItemStack(recipe.input()));
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 10).add(new ItemStack(recipe.material()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 28).add(new ItemStack(recipe.output()));
    }

    @Override
    public void draw(RFRJeiRecipes.Upgrade recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 122, 72, 256, 256);
    }
}
