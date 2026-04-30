package com.misterd.realfilingreborn.capability;

import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class FilingIndexFluidHandler implements IFluidHandler {

    private final FilingIndexBlockEntity indexEntity;
    private final Level level;
    private final AtomicReference<List<FluidTankInfo>> snapshotRef = new AtomicReference<>(List.of());
    private final Map<BlockPos, Boolean> inRangeCache = new ConcurrentHashMap<>();
    private volatile long lastRangeCacheTime = 0L;
    private static final long RANGE_CACHE_DURATION_MS = 2000L;

    public FilingIndexFluidHandler(FilingIndexBlockEntity indexEntity) {
        this.indexEntity = indexEntity;
        this.level = indexEntity.getLevel();
        refreshSnapshot();
    }

    public void refreshSnapshot() {
        snapshotRef.set(List.copyOf(buildFluidTanks()));
    }

    public void invalidateRangeCache() {
        inRangeCache.clear();
        lastRangeCacheTime = 0L;
    }

    private List<FluidTankInfo> snapshot() {
        return snapshotRef.get();
    }

    private List<FluidTankInfo> buildFluidTanks() {
        List<FluidTankInfo> tanks = new ArrayList<>();
        for (BlockPos cabinetPos : new ArrayList<>(indexEntity.getLinkedCabinets())) {
            if (!isInRangeCached(cabinetPos)) continue;
            if (!(level.getBlockEntity(cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;
            if (!fluidCabinet.isLinkedToController()) continue;
            for (int slot = 0; slot < 4; slot++) {
                ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(slot);
                if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;
                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;
                var fluid = FluidHelper.getFluidFromId(contents.storedFluidId().get());
                if (fluid == null) continue;
                tanks.add(new FluidTankInfo(cabinetPos, slot, new FluidStack(fluid, contents.amount()), canister.getCapacity()));
            }
        }
        return tanks;
    }

    private boolean isInRangeCached(BlockPos cabinetPos) {
        long now = System.currentTimeMillis();
        if (now - lastRangeCacheTime > RANGE_CACHE_DURATION_MS) {
            inRangeCache.clear();
            lastRangeCacheTime = now;
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
        return snapshot().size();
    }

    @Override
    @NotNull
    public FluidStack getFluidInTank(int tank) {
        List<FluidTankInfo> snap = snapshot();
        if (tank < 0 || tank >= snap.size()) return FluidStack.EMPTY;
        return snap.get(tank).fluidStack().copy();
    }

    @Override
    public int getTankCapacity(int tank) {
        List<FluidTankInfo> snap = snapshot();
        if (tank < 0 || tank >= snap.size()) return 0;
        return snap.get(tank).capacity();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        List<FluidTankInfo> snap = snapshot();
        if (tank < 0 || tank >= snap.size() || stack.isEmpty()) return false;
        return FluidHelper.areFluidsCompatible(
                FluidHelper.getFluidId(stack.getFluid()),
                FluidHelper.getFluidId(snap.get(tank).fluidStack().getFluid()));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidHelper.isValidFluid(resource.getFluid())) return 0;

        try {
            ResourceLocation resourceFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));

            for (FluidTankInfo tankInfo : snapshot()) {
                ResourceLocation tankFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(tankInfo.fluidStack().getFluid()));
                if (!FluidHelper.areFluidsCompatible(resourceFluidId, tankFluidId)) continue;

                if (!(level.getBlockEntity(tankInfo.cabinetPos()) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;
                ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(tankInfo.slotIndex());
                if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.storedFluidId().isEmpty()) continue;

                int toAdd = Math.min(resource.getAmount(), canister.getCapacity() - contents.amount());
                if (toAdd <= 0) continue;

                if (action.execute()) {
                    canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() + toAdd));
                    fluidCabinet.inventory.setStackInSlot(tankInfo.slotIndex(), canisterStack);
                }
                return toAdd;
            }

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
                        fluidCabinet.inventory.setStackInSlot(slot, canisterStack);
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

            for (FluidTankInfo tankInfo : snapshot()) {
                ResourceLocation tankFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(tankInfo.fluidStack().getFluid()));
                if (!FluidHelper.areFluidsCompatible(resourceFluidId, tankFluidId)) continue;

                if (!(level.getBlockEntity(tankInfo.cabinetPos()) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;
                ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(tankInfo.slotIndex());
                if (!(canisterStack.getItem() instanceof FluidCanisterItem)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.amount() <= 0) continue;

                int toDrain = Math.min(resource.getAmount(), contents.amount());
                if (toDrain <= 0) continue;

                if (action.execute()) {
                    canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - toDrain));
                    fluidCabinet.inventory.setStackInSlot(tankInfo.slotIndex(), canisterStack);
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
        if (maxDrain <= 0) return FluidStack.EMPTY;

        try {
            for (FluidTankInfo tankInfo : snapshot()) {
                if (tankInfo.fluidStack().getAmount() <= 0) continue;

                if (!(level.getBlockEntity(tankInfo.cabinetPos()) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;
                ItemStack canisterStack = fluidCabinet.inventory.getStackInSlot(tankInfo.slotIndex());
                if (!(canisterStack.getItem() instanceof FluidCanisterItem)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.amount() <= 0) continue;

                int toDrain = Math.min(maxDrain, contents.amount());
                if (toDrain <= 0) continue;

                if (action.execute()) {
                    canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - toDrain));
                    fluidCabinet.inventory.setStackInSlot(tankInfo.slotIndex(), canisterStack);
                }
                return new FluidStack(tankInfo.fluidStack().getFluid(), toDrain);
            }

            return FluidStack.EMPTY;
        } catch (Exception e) {
            return FluidStack.EMPTY;
        }
    }

    private record FluidTankInfo(BlockPos cabinetPos, int slotIndex, FluidStack fluidStack, int capacity) {}
}