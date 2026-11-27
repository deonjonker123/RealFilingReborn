package com.misterd.realfilingreborn.recipe.custom;

import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.recipe.RFRRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;

public class CanisterUpgradeRecipe extends CustomRecipe {
    private final FluidCanisterItem inputTier;
    private final FluidCanisterItem outputTier;
    private final Ingredient material;

    public CanisterUpgradeRecipe(CraftingBookCategory category, FluidCanisterItem inputTier, FluidCanisterItem outputTier, Ingredient material) {
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
        return PlacementInfo.create(List.of(Ingredient.of(inputTier), material));
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

        FluidCanisterItem.CanisterContents contents = found.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents == null || contents.storedFluidId().isEmpty()) return null;
        return found;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack canisterStack = findInput(input);
        if (canisterStack == null) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(outputTier);

        FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents != null) {
            result.set(FluidCanisterItem.CANISTER_CONTENTS.value(), contents);
        }

        return result;
    }

    @Override
    public RecipeSerializer<CanisterUpgradeRecipe> getSerializer() {
        return RFRRecipes.CANISTER_UPGRADE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final MapCodec<CanisterUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("input_tier")
                            .xmap(item -> (FluidCanisterItem) item, i -> i)
                            .forGetter(r -> r.inputTier),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("output_tier")
                            .xmap(item -> (FluidCanisterItem) item, i -> i)
                            .forGetter(r -> r.outputTier),
                    Ingredient.CODEC.fieldOf("material").forGetter(r -> r.material)
            ).apply(instance, CanisterUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanisterUpgradeRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, recipe) -> {
                        buf.writeEnum(recipe.category());
                        Identifier.STREAM_CODEC.encode(buf, BuiltInRegistries.ITEM.getKey(recipe.inputTier));
                        Identifier.STREAM_CODEC.encode(buf, BuiltInRegistries.ITEM.getKey(recipe.outputTier));
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.material);
                    },
                    buf -> {
                        CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
                        FluidCanisterItem input = (FluidCanisterItem) BuiltInRegistries.ITEM.get(Identifier.STREAM_CODEC.decode(buf)).orElseThrow().value();
                        FluidCanisterItem output = (FluidCanisterItem) BuiltInRegistries.ITEM.get(Identifier.STREAM_CODEC.decode(buf)).orElseThrow().value();
                        Ingredient material = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        return new CanisterUpgradeRecipe(category, input, output, material);
                    });
}