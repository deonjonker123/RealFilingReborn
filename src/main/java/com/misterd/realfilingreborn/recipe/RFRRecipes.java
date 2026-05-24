package com.misterd.realfilingreborn.recipe;

import com.misterd.realfilingreborn.recipe.custom.FolderResetRecipe;
import com.misterd.realfilingreborn.recipe.custom.FolderUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RFRRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, "realfilingreborn");

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderUpgradeRecipe>> FOLDER_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("folder_upgrade",
                    () -> new RecipeSerializer<>(FolderUpgradeRecipe.CODEC, FolderUpgradeRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FolderResetRecipe>> FOLDER_RESET_SERIALIZER =
            RECIPE_SERIALIZERS.register("folder_reset",
                    () -> new RecipeSerializer<>(FolderResetRecipe.CODEC, FolderResetRecipe.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}