package com.misterd.realfilingreborn.compat.jei;

import com.misterd.realfilingreborn.item.RFRItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class RFRJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath("realfilingreborn", "jei_plugin");

    public static final IRecipeType<RFRJeiRecipes.Upgrade> FOLDER_UPGRADE = IRecipeType.create("realfilingreborn", "folder_upgrade", RFRJeiRecipes.Upgrade.class);
    public static final IRecipeType<RFRJeiRecipes.Upgrade> CANISTER_UPGRADE = IRecipeType.create("realfilingreborn", "canister_upgrade", RFRJeiRecipes.Upgrade.class);
    public static final IRecipeType<RFRJeiRecipes.Reset> FOLDER_RESET = IRecipeType.create("realfilingreborn", "folder_reset", RFRJeiRecipes.Reset.class);
    public static final IRecipeType<RFRJeiRecipes.Reset> CANISTER_RESET = IRecipeType.create("realfilingreborn", "canister_reset", RFRJeiRecipes.Reset.class);

    private static final List<DeferredItem<Item>> FOLDERS = List.of(RFRItems.FILING_FOLDER, RFRItems.COPPER_FILING_FOLDER, RFRItems.IRON_FILING_FOLDER,
            RFRItems.GOLD_FILING_FOLDER, RFRItems.DIAMOND_FILING_FOLDER, RFRItems.NETHERITE_FILING_FOLDER);
    private static final List<DeferredItem<Item>> CANISTERS = List.of(RFRItems.FLUID_CANISTER, RFRItems.COPPER_FLUID_CANISTER, RFRItems.IRON_FLUID_CANISTER,
            RFRItems.GOLD_FLUID_CANISTER, RFRItems.DIAMOND_FLUID_CANISTER, RFRItems.NETHERITE_FLUID_CANISTER);
    private static final List<Item> MATERIALS = List.of(Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_INGOT);

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new UpgradeRecipeCategory(gui, FOLDER_UPGRADE, "jei.realfilingreborn.folder_upgrade", RFRItems.COPPER_FILING_FOLDER.get()),
                new UpgradeRecipeCategory(gui, CANISTER_UPGRADE, "jei.realfilingreborn.canister_upgrade", RFRItems.COPPER_FLUID_CANISTER.get()),
                new ResetRecipeCategory(gui, FOLDER_RESET, "jei.realfilingreborn.folder_reset", RFRItems.FILING_FOLDER.get()),
                new ResetRecipeCategory(gui, CANISTER_RESET, "jei.realfilingreborn.canister_reset", RFRItems.FLUID_CANISTER.get()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FOLDER_UPGRADE, upgrades(FOLDERS));
        registration.addRecipes(CANISTER_UPGRADE, upgrades(CANISTERS));
        registration.addRecipes(FOLDER_RESET, FOLDERS.stream().map(f -> new RFRJeiRecipes.Reset(f.get())).toList());
        registration.addRecipes(CANISTER_RESET, CANISTERS.stream().map(c -> new RFRJeiRecipes.Reset(c.get())).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(FOLDER_UPGRADE, Items.CRAFTING_TABLE);
        registration.addCraftingStation(CANISTER_UPGRADE, Items.CRAFTING_TABLE);
        registration.addCraftingStation(FOLDER_RESET, Items.CRAFTING_TABLE);
        registration.addCraftingStation(CANISTER_RESET, Items.CRAFTING_TABLE);
    }

    private static List<RFRJeiRecipes.Upgrade> upgrades(List<DeferredItem<Item>> tiers) {
        List<RFRJeiRecipes.Upgrade> recipes = new ArrayList<>();
        for (int i = 0; i < MATERIALS.size(); i++) {
            recipes.add(new RFRJeiRecipes.Upgrade(tiers.get(i).get(), tiers.get(i + 1).get(), MATERIALS.get(i)));
        }
        return recipes;
    }
}
