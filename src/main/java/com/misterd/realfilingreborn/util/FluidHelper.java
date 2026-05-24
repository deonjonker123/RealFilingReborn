package com.misterd.realfilingreborn.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FluidHelper {

    private static final Map<Identifier, Boolean> VALID_FLUID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, ItemStack> FLUID_BUCKET_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, String> FLUID_NAME_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    public static boolean isValidFluid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        Identifier fluidId = getFluidId(fluid);
        if (fluidId == null) return false;

        if (VALID_FLUID_CACHE.size() > MAX_CACHE_SIZE) VALID_FLUID_CACHE.clear();
        return VALID_FLUID_CACHE.computeIfAbsent(fluidId, id -> {
            if (!BuiltInRegistries.FLUID.containsKey(id)) return false;
            try {
                return fluid.getFluidType() != null;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static boolean isValidFluid(Identifier fluidId) {
        if (fluidId == null) return false;
        try {
            return isValidFluid(BuiltInRegistries.FLUID.getValue(fluidId));
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static Identifier getFluidId(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        try {
            return fluid.builtInRegistryHolder().key().identifier();
        } catch (Exception e) {
            return null;
        }
    }

    public static Fluid getFluidFromId(Identifier fluidId) {
        if (fluidId == null) return Fluids.EMPTY;
        try {
            Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
            return fluid != null ? fluid : Fluids.EMPTY;
        } catch (Exception e) {
            return Fluids.EMPTY;
        }
    }

    public static ItemStack getBucketForFluid(Identifier fluidId) {
        if (fluidId == null || !isValidFluid(fluidId)) return ItemStack.EMPTY;

        if (FLUID_BUCKET_CACHE.size() > MAX_CACHE_SIZE) FLUID_BUCKET_CACHE.clear();
        return FLUID_BUCKET_CACHE.computeIfAbsent(fluidId, id -> {
            Identifier waterId = Fluids.WATER.builtInRegistryHolder().key().identifier();
            Identifier lavaId = Fluids.LAVA.builtInRegistryHolder().key().identifier();
            if (id.equals(waterId)) return new ItemStack(Items.WATER_BUCKET);
            if (id.equals(lavaId)) return new ItemStack(Items.LAVA_BUCKET);

            try {
                Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (item instanceof BucketItem bucket && bucket.content == fluid) {
                            return new ItemStack(item);
                        }
                    }
                }
            } catch (Exception ignored) {}

            return ItemStack.EMPTY;
        }).copy();
    }

    public static String getFluidDisplayName(Identifier fluidId) {
        if (fluidId == null) return "Unknown Fluid";

        if (FLUID_NAME_CACHE.size() > MAX_CACHE_SIZE) FLUID_NAME_CACHE.clear();
        return FLUID_NAME_CACHE.computeIfAbsent(fluidId, id -> {
            Identifier waterId = Fluids.WATER.builtInRegistryHolder().key().identifier();
            Identifier lavaId = Fluids.LAVA.builtInRegistryHolder().key().identifier();
            if (id.equals(waterId)) return "Water";
            if (id.equals(lavaId)) return "Lava";

            try {
                Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    try {
                        return fluid.getFluidType().getDescription().getString();
                    } catch (Exception e) {
                        return formatFluidName(id.getPath());
                    }
                }
            } catch (Exception ignored) {}

            return formatFluidName(id.getPath());
        });
    }

    private static String formatFluidName(String path) {
        String cleaned = path.replace("_", " ").replace("flowing", "").trim();
        if (cleaned.isEmpty()) return path;
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    public static boolean areFluidsCompatible(Identifier fluidId1, Identifier fluidId2) {
        if (fluidId1 == null || fluidId2 == null) return false;
        if (fluidId1.equals(fluidId2)) return true;
        String path1 = fluidId1.getPath().replace("flowing_", "");
        String path2 = fluidId2.getPath().replace("flowing_", "");
        return fluidId1.getNamespace().equals(fluidId2.getNamespace()) && path1.equals(path2);
    }

    @Nullable
    public static Identifier getStillFluid(Identifier fluidId) {
        if (fluidId == null) return null;
        String path = fluidId.getPath();
        return path.startsWith("flowing_")
                ? Identifier.fromNamespaceAndPath(fluidId.getNamespace(), path.substring(8))
                : fluidId;
    }

    public static void clearCaches() {
        VALID_FLUID_CACHE.clear();
        FLUID_BUCKET_CACHE.clear();
        FLUID_NAME_CACHE.clear();
    }
}