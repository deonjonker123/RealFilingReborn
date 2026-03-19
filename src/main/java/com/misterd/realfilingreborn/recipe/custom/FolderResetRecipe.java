package com.misterd.realfilingreborn.recipe.custom;

import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class FolderResetRecipe extends CustomRecipe {

    public FolderResetRecipe(CraftingBookCategory category) {
        super(category);
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof FilingFolderItem) {
                return new ItemStack(stack.getItem());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RFRRecipes.FOLDER_RESET_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<FolderResetRecipe> {

        private static final MapCodec<FolderResetRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                                .forGetter(CustomRecipe::category)
                ).apply(instance, FolderResetRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FolderResetRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, recipe) -> buf.writeEnum(recipe.category()),
                        buf -> new FolderResetRecipe(buf.readEnum(CraftingBookCategory.class)));

        @Override
        public MapCodec<FolderResetRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FolderResetRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
