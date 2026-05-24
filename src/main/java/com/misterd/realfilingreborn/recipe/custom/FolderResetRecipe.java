package com.misterd.realfilingreborn.recipe.custom;

import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class FolderResetRecipe extends CustomRecipe {

    public FolderResetRecipe(CraftingBookCategory category) {
        super();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack found = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!found.isEmpty()) return false;
            found = stack;
        }

        if (found.isEmpty() || !(found.getItem() instanceof FilingFolderItem)) return false;

        FilingFolderItem.FolderContents contents = found.get(FilingFolderItem.FOLDER_CONTENTS.value());
        return contents != null && contents.storedItemId().isPresent() && contents.count() == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof FilingFolderItem) {
                return new ItemStack(stack.getItem());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<FolderResetRecipe> getSerializer() {
        return RFRRecipes.FOLDER_RESET_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final MapCodec<FolderResetRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                            .forGetter(CustomRecipe::category)
            ).apply(instance, FolderResetRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FolderResetRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, recipe) -> buf.writeEnum(recipe.category()),
                    buf -> new FolderResetRecipe(buf.readEnum(CraftingBookCategory.class)));
}