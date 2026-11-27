package com.misterd.realfilingreborn.compat.jei;

import net.minecraft.world.item.Item;

public final class RFRJeiRecipes {
    private RFRJeiRecipes() {}

    public record Upgrade(Item input, Item output, Item material) {}

    public record Reset(Item input) {}
}
