package com.misterd.realfilingreborn.compat.jei;

import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import net.minecraft.world.item.Item;

public class RFRJeiRecipes {
    public record FolderUpgradeJeiRecipe(FilingFolderItem input, FilingFolderItem output, Item material) {}

    public record CanisterUpgradeJeiRecipe(FluidCanisterItem input, FluidCanisterItem output, Item material) {}

    public record FolderResetJeiRecipe(FilingFolderItem input) {}

    public record CanisterResetJeiRecipe(FluidCanisterItem input) {}
}
