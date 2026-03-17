package com.misterd.realfilingreborn.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormattingCache {

    private static final Map<Integer, String> ITEM_COUNT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, String> FLUID_AMOUNT_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000;

    public static String getFormattedItemCount(int count) {
        if (ITEM_COUNT_CACHE.size() > MAX_CACHE_SIZE) ITEM_COUNT_CACHE.clear();
        return ITEM_COUNT_CACHE.computeIfAbsent(count, c -> {
            if (c >= 1_000_000) return String.format("%.1fM", c / 1_000_000.0);
            if (c >= 1_000)     return String.format("%.1fK", c / 1_000.0);
            return String.valueOf(c);
        });
    }

    public static String getFormattedFluidAmount(int amount) {
        if (FLUID_AMOUNT_CACHE.size() > MAX_CACHE_SIZE) FLUID_AMOUNT_CACHE.clear();
        return FLUID_AMOUNT_CACHE.computeIfAbsent(amount, a -> {
            if (a >= 1_000_000_000) return String.format("%.1fB", a / 1_000_000_000.0);
            if (a >= 1_000_000)     return String.format("%.1fM", a / 1_000_000.0);
            if (a >= 1_000) {
                float buckets = a / 1_000.0f;
                return buckets == (int) buckets
                        ? String.format("%d", (int) buckets)
                        : String.format("%.1f", buckets);
            }
            return String.valueOf(a);
        });
    }

    public static void clearCaches() {
        ITEM_COUNT_CACHE.clear();
        FLUID_AMOUNT_CACHE.clear();
    }
}
