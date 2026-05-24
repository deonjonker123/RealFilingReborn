package com.misterd.realfilingreborn.recipe.custom;

import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;

public class FolderUpgradeRecipe extends CustomRecipe {

    private final FilingFolderItem inputTier;
    private final FilingFolderItem outputTier;
    private final Ingredient material;

    public FolderUpgradeRecipe(CraftingBookCategory category, FilingFolderItem inputTier, FilingFolderItem outputTier, Ingredient material) {
        super();
        this.inputTier = inputTier;
        this.outputTier = outputTier;
        this.material = material;
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
        return PlacementInfo.create(List.of(
                Ingredient.of(inputTier),
                material,
                material,
                material
        ));
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 2 || input.height() != 2) return false;

        ItemStack topLeft     = input.getItem(0, 0);
        ItemStack topRight    = input.getItem(1, 0);
        ItemStack bottomLeft  = input.getItem(0, 1);
        ItemStack bottomRight = input.getItem(1, 1);

        if (topLeft.getItem() != inputTier) return false;

        FilingFolderItem.FolderContents contents = topLeft.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return false;

        return material.test(topRight) && material.test(bottomLeft) && material.test(bottomRight);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack folderStack = input.getItem(0, 0);
        ItemStack result = new ItemStack(outputTier);

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents != null) {
            result.set(FilingFolderItem.FOLDER_CONTENTS.value(), contents);
        }

        return result;
    }

    @Override
    public RecipeSerializer<FolderUpgradeRecipe> getSerializer() {
        return RFRRecipes.FOLDER_UPGRADE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final MapCodec<FolderUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
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

    public static final StreamCodec<RegistryFriendlyByteBuf, FolderUpgradeRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, recipe) -> {
                        buf.writeEnum(recipe.category());
                        Identifier.STREAM_CODEC.encode(buf, BuiltInRegistries.ITEM.getKey(recipe.inputTier));
                        Identifier.STREAM_CODEC.encode(buf, BuiltInRegistries.ITEM.getKey(recipe.outputTier));
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.material);
                    },
                    buf -> {
                        CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
                        FilingFolderItem input = (FilingFolderItem) BuiltInRegistries.ITEM.get(Identifier.STREAM_CODEC.decode(buf)).orElseThrow().value();
                        FilingFolderItem output = (FilingFolderItem) BuiltInRegistries.ITEM.get(Identifier.STREAM_CODEC.decode(buf)).orElseThrow().value();
                        Ingredient material = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        return new FolderUpgradeRecipe(category, input, output, material);
                    });
}