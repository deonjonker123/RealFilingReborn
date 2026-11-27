package com.misterd.realfilingreborn.recipe.custom;

import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class CanisterResetRecipe extends CustomRecipe {
    public CanisterResetRecipe(CraftingBookCategory category) {
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

        if (found.isEmpty() || !(found.getItem() instanceof FluidCanisterItem)) return false;

        FluidCanisterItem.CanisterContents contents = found.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        return contents != null && contents.storedFluidId().isPresent() && contents.amount() == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof FluidCanisterItem) {
                return new ItemStack(stack.getItem());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<CanisterResetRecipe> getSerializer() {
        return RFRRecipes.CANISTER_RESET_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final MapCodec<CanisterResetRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                            .forGetter(CustomRecipe::category)
            ).apply(instance, CanisterResetRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanisterResetRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, recipe) -> buf.writeEnum(recipe.category()),
                    buf -> new CanisterResetRecipe(buf.readEnum(CraftingBookCategory.class)));
}