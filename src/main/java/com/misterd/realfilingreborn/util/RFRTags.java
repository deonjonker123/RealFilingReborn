package com.misterd.realfilingreborn.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class RFRTags {
    public static class Items {
        public static final TagKey<Item> FILING_CABINET_ITEMS = createTag("filing_cabinet_items");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath("realfilingreborn", name));
        }
    }
}
