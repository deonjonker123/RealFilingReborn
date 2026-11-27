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
    private static final Map<Identifier, ItemStack> FLUID_BUCKET_CACHE = new ConcurrentHashMap<>();

    public static boolean isValidFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        Identifier id = getFluidId(fluid);
        return id != null && BuiltInRegistries.FLUID.containsKey(id);
    }

    public static boolean isValidFluid(@Nullable Identifier fluidId) {
        return fluidId != null && isValidFluid(getFluidFromId(fluidId));
    }

    @Nullable
    public static Identifier getFluidId(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        return BuiltInRegistries.FLUID.getKey(fluid);
    }

    public static Fluid getFluidFromId(@Nullable Identifier fluidId) {
        if (fluidId == null) return Fluids.EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
        return fluid != null ? fluid : Fluids.EMPTY;
    }

    public static ItemStack getBucketForFluid(@Nullable Identifier fluidId) {
        if (fluidId == null || !isValidFluid(fluidId)) return ItemStack.EMPTY;
        return FLUID_BUCKET_CACHE.computeIfAbsent(fluidId, id -> {
            Fluid fluid = getFluidFromId(id);
            if (fluid == Fluids.WATER) return new ItemStack(Items.WATER_BUCKET);
            if (fluid == Fluids.LAVA) return new ItemStack(Items.LAVA_BUCKET);
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof BucketItem bucket && bucket.content == fluid) return new ItemStack(item);
            }
            return ItemStack.EMPTY;
        }).copy();
    }

    public static String getFluidDisplayName(@Nullable Identifier fluidId) {
        if (fluidId == null) return "Unknown Fluid";
        Fluid fluid = getFluidFromId(fluidId);
        if (fluid != Fluids.EMPTY) return fluid.getFluidType().getDescription().getString();
        String cleaned = fluidId.getPath().replace("_", " ").replace("flowing", "").trim();
        return cleaned.isEmpty() ? fluidId.getPath() : Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    public static boolean areFluidsCompatible(@Nullable Identifier first, @Nullable Identifier second) {
        if (first == null || second == null) return false;
        return getStillFluid(first).equals(getStillFluid(second));
    }

    public static Identifier getStillFluid(Identifier fluidId) {
        String path = fluidId.getPath();
        return path.startsWith("flowing_") ? Identifier.fromNamespaceAndPath(fluidId.getNamespace(), path.substring(8)) : fluidId;
    }

    public static String formatAmount(int amount) {
        int buckets = amount / 1000;
        int mb = amount % 1000;
        if (buckets > 0) return mb > 0 ? buckets + "." + mb / 100 + "B" : buckets + "B";
        return mb + "mB";
    }

    public static String formatShortAmount(int amount) {
        int buckets = amount / 1000;
        if (buckets >= 1_000_000) return (buckets / 1_000_000) + "MB";
        if (buckets >= 1_000) return (buckets / 1_000) + "KB";
        if (buckets > 0) return buckets + "B";
        return (amount % 1000) + "mB";
    }
}
