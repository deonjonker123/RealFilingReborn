package com.misterd.realfilingreborn.recipe;


import com.misterd.realfilingreborn.recipe.custom.CanisterUpgradeRecipe;
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
            RECIPE_SERIALIZERS.register("folder_upgrade", FolderUpgradeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CanisterUpgradeRecipe>> CANISTER_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("canister_upgrade", CanisterUpgradeRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}