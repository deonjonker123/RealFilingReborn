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
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FilingIndexFluidHandler implements ResourceHandler<FluidResource> {
    private record Tank(Identifier fluidId, int amount, int capacity) {}

    private final FilingIndexBlockEntity indexEntity;
    private final AtomicReference<List<Tank>> snapshotRef = new AtomicReference<>(List.of());

    public FilingIndexFluidHandler(FilingIndexBlockEntity indexEntity) {
        this.indexEntity = indexEntity;
        refreshSnapshot();
    }

    private Level level() {
        return indexEntity.getLevel();
    }

    private List<FluidCabinetBlockEntity> linkedFluidCabinets() {
        List<FluidCabinetBlockEntity> cabinets = new ArrayList<>();
        Level level = level();
        if (level == null) return cabinets;
        for (BlockPos pos : indexEntity.getLinkedCabinets()) {
            if (!indexEntity.isInRange(pos) || !level.isLoaded(pos)) continue;
            if (level.getBlockEntity(pos) instanceof FluidCabinetBlockEntity cabinet && cabinet.isLinkedToController()) {
                cabinets.add(cabinet);
            }
        }
        return cabinets;
    }

    public void refreshSnapshot() {
        List<Tank> tanks = new ArrayList<>();
        for (FluidCabinetBlockEntity cabinet : linkedFluidCabinets()) {
            for (int slot = 0; slot < FluidCabinetBlockEntity.SLOTS; slot++) {
                ItemStack canister = cabinet.getStack(slot);
                if (!(canister.getItem() instanceof FluidCanisterItem item)) continue;
                FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canister);
                if (contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;
                tanks.add(new Tank(contents.storedFluidId().get(), contents.amount(), item.getCapacity()));
            }
        }
        snapshotRef.set(List.copyOf(tanks));
    }

    @Nullable
    private static Identifier idOf(FluidResource resource) {
        if (resource.isEmpty() || !FluidHelper.isValidFluid(resource.getFluid())) return null;
        return FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));
    }

    @Override
    public int size() {
        return Math.max(snapshotRef.get().size(), 1);
    }

    @Override
    public FluidResource getResource(int index) {
        List<Tank> snap = snapshotRef.get();
        if (index < 0 || index >= snap.size()) return FluidResource.EMPTY;
        Fluid fluid = FluidHelper.getFluidFromId(snap.get(index).fluidId());
        return FluidHelper.isValidFluid(fluid) ? FluidResource.of(fluid) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        List<Tank> snap = snapshotRef.get();
        return index < 0 || index >= snap.size() ? 0 : snap.get(index).amount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        List<Tank> snap = snapshotRef.get();
        return index < 0 || index >= snap.size() ? 0 : snap.get(index).capacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return idOf(resource) != null;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        Identifier id = idOf(resource);
        if (id == null || amount <= 0) return 0;
        int inserted = 0;
        List<FluidCabinetBlockEntity> cabinets = linkedFluidCabinets();
        for (int pass = 0; pass < 2 && inserted < amount; pass++) {
            for (FluidCabinetBlockEntity cabinet : cabinets) {
                for (int slot = 0; slot < FluidCabinetBlockEntity.SLOTS && inserted < amount; slot++) {
                    ItemStack canister = cabinet.getStack(slot);
                    if (!(canister.getItem() instanceof FluidCanisterItem)) continue;
                    boolean assigned = FluidCanisterItem.contentsOf(canister).storedFluidId().isPresent();
                    if (assigned != (pass == 0)) continue;
                    inserted += FluidCabinetBlockEntity.changeCanister(cabinet.inventory, slot, canister, id, amount - inserted, transaction);
                }
            }
        }
        return inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return extract(resource, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        Identifier id = idOf(resource);
        if (id == null || amount <= 0) return 0;
        int extracted = 0;
        for (FluidCabinetBlockEntity cabinet : linkedFluidCabinets()) {
            for (int slot = 0; slot < FluidCabinetBlockEntity.SLOTS && extracted < amount; slot++) {
                ItemStack canister = cabinet.getStack(slot);
                if (!(canister.getItem() instanceof FluidCanisterItem)) continue;
                if (FluidCanisterItem.contentsOf(canister).storedFluidId().isEmpty()) continue;
                extracted += FluidCabinetBlockEntity.changeCanister(cabinet.inventory, slot, canister, id, -(amount - extracted), transaction);
            }
        }
        return extracted;
    }
}
