package com.misterd.realfilingreborn.recipe.custom;

import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class FolderUpgradeRecipe extends CustomRecipe {
    private final FilingFolderItem inputTier;
    private final FilingFolderItem outputTier;
    private final Ingredient material;

    public FolderUpgradeRecipe(CraftingBookCategory category, FilingFolderItem inputTier, FilingFolderItem outputTier, Ingredient material) {
        super(category);
        this.inputTier = inputTier;
        this.outputTier = outputTier;
        this.material = material;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInput(input) != null;
    }

    @Nullable
    private ItemStack findInput(CraftingInput input) {
        if (input.ingredientCount() != 2) return null;

        ItemStack found = null;
        boolean hasMaterial = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (found == null && stack.getItem() == inputTier) found = stack;
            else if (!hasMaterial && material.test(stack)) hasMaterial = true;
            else return null;
        }
        if (found == null || !hasMaterial) return null;

        FilingFolderItem.FolderContents contents = found.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return null;
        return found;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack folderStack = findInput(input);
        if (folderStack == null) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(outputTier);

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents != null) {
            result.set(FilingFolderItem.FOLDER_CONTENTS.value(), contents);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(outputTier);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RFRRecipes.FOLDER_UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<FolderUpgradeRecipe> {
        private static final MapCodec<FolderUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("input_tier")
                                .xmap(item -> (FilingFolderItem) item, i -> i)
                                .forGetter(r -> r.inputTier),
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("output_tier")
                                .xmap(item -> (FilingFolderItem) item, i -> i)
                                .forGetter(r -> r.outputTier),
                        Ingredient.CODEC.fieldOf("material").forGetter(r -> r.material)
                ).apply(instance, FolderUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FolderUpgradeRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, recipe) -> {
                            buf.writeEnum(recipe.category());
                            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(recipe.inputTier));
                            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(recipe.outputTier));
                            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.material);
                        },
                        buf -> {
                            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
                            FilingFolderItem input = (FilingFolderItem) BuiltInRegistries.ITEM.get(buf.readResourceLocation());
                            FilingFolderItem output = (FilingFolderItem) BuiltInRegistries.ITEM.get(buf.readResourceLocation());
                            Ingredient material = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                            return new FolderUpgradeRecipe(category, input, output, material);
                        });

        @Override
        public MapCodec<FolderUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FolderUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}