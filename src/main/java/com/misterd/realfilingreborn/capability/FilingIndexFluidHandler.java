package com.misterd.realfilingreborn.capability;

import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FilingIndexFluidHandler implements IFluidHandler {

    private final FilingIndexBlockEntity indexEntity;
    private final Level level;

    private volatile List<FluidTankInfo> cachedFluidTanks = null;
    private final AtomicLong lastCacheTime = new AtomicLong(0L);
    private final AtomicLong cacheVersion = new AtomicLong(0L);
    private static final long CACHE_DURATION_MS = 500L;
    private static final int MAX_FLUID_TANKS_PER_SCAN = 500;

    private final Map<BlockPos, Boolean> inRangeCache = new ConcurrentHashMap<>();
    private volatile long lastRangeCacheTime = 0L;
    private static final long RANGE_CACHE_DURATION_MS = 2000L;

    public FilingIndexFluidHandler(FilingIndexBlockEntity indexEntity) {
        this.indexEntity = indexEntity;
        this.level = indexEntity.getLevel();
    }

    private void notifyUpdate(BlockPos cabinetPos) {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(cabinetPos, level.getBlockState(cabinetPos), level.getBlockState(cabinetPos), 2);
            invalidateCache();
        }
    }

    public void invalidateCache() {
        cachedFluidTanks = null;
        cacheVersion.incrementAndGet();
        lastCacheTime.set(0L);
    }

    private void invalidateRangeCache() {
        inRangeCache.clear();
        lastRangeCacheTime = 0L;
    }

    private List<FluidTankInfo> getAllFluidTanks() {
        long currentTime = System.currentTimeMillis();
        long currentVersion = cacheVersion.get();
        List<FluidTankInfo> cached = cachedFluidTanks;
        if (cached != null && currentTime - lastCacheTime.get() < CACHE_DURATION_MS) {
            return cached;
        }

        List<FluidTankInfo> tanks = new ArrayList<>();
        int tankCount = 0;

        for (BlockPos cabinetPos : new ArrayList<>(indexEntity.getLinkedCabinets())) {
            if (tankCount >= MAX_FLUID_TANKS_PER_SCAN) break;
            if (!isInRangeCached(cabinetPos)) continue;

            try {
                if (!(level.getBlockEntity(cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;
                if (!fluidCabinet.isLinkedToController()) continue;

                for (int slot = 0; slot < 4 && tankCount < MAX_FLUID_TANKS_PER_SCAN; slot++) {
                    ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(slot);
                    if (!(canisterStack.getItem() instanceof FluidCanisterItem)) continue;

                    FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                    if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;

                    Fluid fluid = FluidHelper.getFluidFromId(contents.storedFluidId().get());
                    if (fluid == null) continue;

                    tanks.add(new FluidTankInfo(cabinetPos, slot, new FluidStack(fluid, contents.amount())));
                    tankCount++;
                }
            } catch (Exception ignored) {}
        }

        if (currentVersion == cacheVersion.get()) {
            cachedFluidTanks = tanks;
            lastCacheTime.set(currentTime);
        }

        return tanks;
    }

    private boolean isInRangeCached(BlockPos cabinetPos) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRangeCacheTime > RANGE_CACHE_DURATION_MS) {
            invalidateRangeCache();
            lastRangeCacheTime = currentTime;
        }

        return inRangeCache.computeIfAbsent(cabinetPos, pos -> {
            try {
                return indexEntity.isInRange(pos);
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public int getTanks() {
        try {
            return getAllFluidTanks().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    @NotNull
    public FluidStack getFluidInTank(int tank) {
        try {
            List<FluidTankInfo> tanks = getAllFluidTanks();
            return tank >= 0 && tank < tanks.size() ? tanks.get(tank).fluidStack.copy() : FluidStack.EMPTY;
        } catch (Exception e) {
            return FluidStack.EMPTY;
        }
    }

    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        try {
            List<FluidTankInfo> tanks = getAllFluidTanks();
            if (tank < 0 || tank >= tanks.size() || stack.isEmpty()) return false;
            FluidTankInfo tankInfo = tanks.get(tank);
            return FluidHelper.areFluidsCompatible(
                    FluidHelper.getFluidId(stack.getFluid()),
                    FluidHelper.getFluidId(tankInfo.fluidStack.getFluid()));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidHelper.isValidFluid(resource.getFluid())) return 0;

        try {
            ResourceLocation resourceFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));

            // Pass 1: fill into an existing matching canister
            for (FluidTankInfo tankInfo : getAllFluidTanks()) {
                ResourceLocation tankFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(tankInfo.fluidStack.getFluid()));
                if (!FluidHelper.areFluidsCompatible(resourceFluidId, tankFluidId)) continue;

                if (!(level.getBlockEntity(tankInfo.cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;

                ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(tankInfo.slotIndex);
                if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null) continue;

                int toAdd = Math.min(resource.getAmount(), canister.getCapacity() - contents.amount());
                if (toAdd <= 0) continue;

                if (action.execute()) {
                    canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(Optional.of(resourceFluidId), contents.amount() + toAdd));
                    fluidCabinet.setChanged();
                    notifyUpdate(tankInfo.cabinetPos);
                }
                return toAdd;
            }

            // Pass 2: fill into an empty canister
            for (BlockPos cabinetPos : new ArrayList<>(indexEntity.getLinkedCabinets())) {
                if (!isInRangeCached(cabinetPos)) continue;
                if (!(level.getBlockEntity(cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;
                if (!fluidCabinet.isLinkedToController()) continue;

                for (int slot = 0; slot < 4; slot++) {
                    ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(slot);
                    if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;

                    FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                    if (contents == null || !contents.storedFluidId().isEmpty()) continue;

                    int toAdd = Math.min(resource.getAmount(), canister.getCapacity());
                    if (toAdd <= 0) continue;

                    if (action.execute()) {
                        canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                                new FluidCanisterItem.CanisterContents(Optional.of(resourceFluidId), toAdd));
                        fluidCabinet.setChanged();
                        notifyUpdate(cabinetPos);
                    }
                    return toAdd;
                }
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    @NotNull
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidHelper.isValidFluid(resource.getFluid())) return FluidStack.EMPTY;

        try {
            ResourceLocation resourceFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));

            for (FluidTankInfo tankInfo : getAllFluidTanks()) {
                ResourceLocation tankFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(tankInfo.fluidStack.getFluid()));
                if (!FluidHelper.areFluidsCompatible(resourceFluidId, tankFluidId)) continue;

                if (!(level.getBlockEntity(tankInfo.cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;

                ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(tankInfo.slotIndex);
                if (!(canisterStack.getItem() instanceof FluidCanisterItem)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.amount() <= 0) continue;

                int toDrain = Math.min(resource.getAmount(), contents.amount());
                if (toDrain > 0 && action.execute()) {
                    canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - toDrain));
                    fluidCabinet.setChanged();
                    notifyUpdate(tankInfo.cabinetPos);
                }
                return new FluidStack(resource.getFluid(), toDrain);
            }

            return FluidStack.EMPTY;
        } catch (Exception e) {
            return FluidStack.EMPTY;
        }
    }

    @Override
    @NotNull
    public FluidStack drain(int maxDrain, FluidAction action) {
        try {
            for (FluidTankInfo tankInfo : getAllFluidTanks()) {
                if (tankInfo.fluidStack.getAmount() <= 0) continue;

                if (!(level.getBlockEntity(tankInfo.cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;

                ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(tankInfo.slotIndex);
                if (!(canisterStack.getItem() instanceof FluidCanisterItem)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.amount() <= 0) continue;

                int toDrain = Math.min(maxDrain, contents.amount());
                if (toDrain > 0 && action.execute()) {
                    canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - toDrain));
                    fluidCabinet.setChanged();
                    notifyUpdate(tankInfo.cabinetPos);
                }
                return new FluidStack(tankInfo.fluidStack.getFluid(), toDrain);
            }

            return FluidStack.EMPTY;
        } catch (Exception e) {
            return FluidStack.EMPTY;
        }
    }

    private record FluidTankInfo(BlockPos cabinetPos, int slotIndex, FluidStack fluidStack) {}
}