package com.misterd.realfilingreborn.recipe.custom;

import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
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

public class CanisterResetRecipe extends CustomRecipe {

    public CanisterResetRecipe(CraftingBookCategory category) {
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

        if (found.isEmpty() || !(found.getItem() instanceof FluidCanisterItem)) return false;

        FluidCanisterItem.CanisterContents contents = found.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        return contents != null && contents.storedFluidId().isPresent() && contents.amount() == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof FluidCanisterItem) {
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
        return RFRRecipes.CANISTER_RESET_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<CanisterResetRecipe> {

        private static final MapCodec<CanisterResetRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                                .forGetter(CustomRecipe::category)
                ).apply(instance, CanisterResetRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CanisterResetRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, recipe) -> buf.writeEnum(recipe.category()),
                        buf -> new CanisterResetRecipe(buf.readEnum(CraftingBookCategory.class)));

        @Override
        public MapCodec<CanisterResetRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CanisterResetRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
