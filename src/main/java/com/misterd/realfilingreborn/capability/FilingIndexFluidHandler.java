package com.misterd.realfilingreborn.capability;

import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class FilingIndexFluidHandler implements ResourceHandler<FluidResource> {

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
                ItemStack canisterStack = fluidCabinet.getStack(slot);
                if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;
                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;
                Fluid fluid = FluidHelper.getFluidFromId(contents.storedFluidId().get());
                if (fluid == null) continue;
                tanks.add(new FluidTankInfo(cabinetPos, slot, FluidResource.of(fluid), contents.amount(), canister.getCapacity()));
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
    public int size() {
        return snapshot().size();
    }

    @Override
    public FluidResource getResource(int slot) {
        List<FluidTankInfo> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return FluidResource.EMPTY;
        return snap.get(slot).resource();
    }

    @Override
    public long getAmountAsLong(int slot) {
        List<FluidTankInfo> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;
        return snap.get(slot).amount();
    }

    @Override
    public long getCapacityAsLong(int slot, FluidResource resource) {
        List<FluidTankInfo> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;
        return snap.get(slot).capacity();
    }

    @Override
    public boolean isValid(int slot, FluidResource resource) {
        return !resource.isEmpty() && FluidHelper.isValidFluid(resource.getFluid());
    }

    @Override
    public int insert(int slot, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0 || !FluidHelper.isValidFluid(resource.getFluid())) return 0;

        try {
            Identifier resourceFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));

            List<FluidTankInfo> snap = snapshot();
            if (slot >= 0 && slot < snap.size()) {
                FluidTankInfo tankInfo = snap.get(slot);
                Identifier tankFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(tankInfo.resource().getFluid()));
                if (FluidHelper.areFluidsCompatible(resourceFluidId, tankFluidId)) {
                    int result = insertIntoTank(tankInfo, resourceFluidId, amount, tx);
                    if (result > 0) return result;
                }
            }

            for (BlockPos cabinetPos : new ArrayList<>(indexEntity.getLinkedCabinets())) {
                if (!isInRangeCached(cabinetPos)) continue;
                if (!(level.getBlockEntity(cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet)) continue;
                if (!fluidCabinet.isLinkedToController()) continue;

                for (int s = 0; s < 4; s++) {
                    ItemStack canisterStack = fluidCabinet.getStack(s);
                    if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;
                    FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                    if (contents == null || !contents.storedFluidId().isEmpty()) continue;

                    int toAdd = Math.min(amount, canister.getCapacity());
                    if (toAdd <= 0) continue;

                    ItemStack updated = canisterStack.copy();
                    updated.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(Optional.of(resourceFluidId), toAdd));
                    try (Transaction innerTx = Transaction.open(tx)) {
                        fluidCabinet.inventory.extract(s, ItemResource.of(canisterStack), 1, innerTx);
                        fluidCabinet.inventory.insert(s, ItemResource.of(updated), 1, innerTx);
                        innerTx.commit();
                    }
                    return toAdd;
                }
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int insertIntoTank(FluidTankInfo tankInfo, Identifier fluidId, int amount, TransactionContext tx) {
        if (!(level.getBlockEntity(tankInfo.cabinetPos()) instanceof FluidCabinetBlockEntity fluidCabinet)) return 0;
        ItemStack canisterStack = fluidCabinet.getStack(tankInfo.slotIndex());
        if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) return 0;
        FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents == null || contents.storedFluidId().isEmpty()) return 0;

        int toAdd = Math.min(amount, canister.getCapacity() - contents.amount());
        if (toAdd <= 0) return 0;

        ItemStack updated = canisterStack.copy();
        updated.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() + toAdd));
        try (Transaction innerTx = Transaction.open(tx)) {
            fluidCabinet.inventory.extract(tankInfo.slotIndex(), ItemResource.of(canisterStack), 1, innerTx);
            fluidCabinet.inventory.insert(tankInfo.slotIndex(), ItemResource.of(updated), 1, innerTx);
            innerTx.commit();
        }
        return toAdd;
    }

    @Override
    public int extract(int slot, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;

        try {
            Identifier resourceFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));
            List<FluidTankInfo> snap = snapshot();

            if (slot >= 0 && slot < snap.size()) {
                FluidTankInfo tankInfo = snap.get(slot);
                Identifier tankFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(tankInfo.resource().getFluid()));
                if (!FluidHelper.areFluidsCompatible(resourceFluidId, tankFluidId)) return 0;

                if (!(level.getBlockEntity(tankInfo.cabinetPos()) instanceof FluidCabinetBlockEntity fluidCabinet)) return 0;
                ItemStack canisterStack = fluidCabinet.getStack(tankInfo.slotIndex());
                if (!(canisterStack.getItem() instanceof FluidCanisterItem)) return 0;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.amount() <= 0) return 0;

                int toDrain = Math.min(amount, contents.amount());
                if (toDrain <= 0) return 0;

                ItemStack updated = canisterStack.copy();
                updated.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                        new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - toDrain));
                try (Transaction innerTx = Transaction.open(tx)) {
                    fluidCabinet.inventory.extract(tankInfo.slotIndex(), ItemResource.of(canisterStack), 1, innerTx);
                    fluidCabinet.inventory.insert(tankInfo.slotIndex(), ItemResource.of(updated), 1, innerTx);
                    innerTx.commit();
                }
                return toDrain;
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private record FluidTankInfo(BlockPos cabinetPos, int slotIndex, FluidResource resource, int amount, int capacity) {}
}