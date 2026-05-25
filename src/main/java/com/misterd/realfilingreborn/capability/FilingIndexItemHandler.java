package com.misterd.realfilingreborn.capability;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FilingIndexItemHandler implements ResourceHandler<ItemResource> {

    private final FilingIndexBlockEntity indexEntity;
    private final Level level;
    private final AtomicReference<List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>>> snapshotRef
            = new AtomicReference<>(List.of());

    public FilingIndexItemHandler(FilingIndexBlockEntity indexEntity) {
        this.indexEntity = indexEntity;
        this.level = indexEntity.getLevel();
        refreshSnapshot();
    }

    public void refreshSnapshot() {
        snapshotRef.set(List.copyOf(indexEntity.getIndexEntries()));
    }

    private List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>> snapshot() {
        return snapshotRef.get();
    }

    @Override
    public int size() {
        return Math.max(snapshot().size(), 1);
    }

    @Override
    public ItemResource getResource(int slot) {
        var snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return ItemResource.EMPTY;
        Identifier id = snap.get(slot).getKey();
        if (snap.get(slot).getValue().count() <= 0) return ItemResource.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null) return ItemResource.EMPTY;
        return ItemResource.of(new ItemStack(item));
    }

    @Override
    public long getAmountAsLong(int slot) {
        var snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;
        return snap.get(slot).getValue().count();
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        var snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;
        return snap.get(slot).getValue().capacity();
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return !resource.isEmpty() && !FilingFolderItem.hasSignificantNBT(resource.toStack());
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        ItemStack stack = resource.toStack(amount);
        if (FilingFolderItem.hasSignificantNBT(stack)) return 0;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<FilingIndexBlockEntity.FolderRef> refs = indexEntity.getFolderRefs(itemId);
        if (refs == null || refs.isEmpty()) return 0;

        int remaining = amount;

        for (FilingIndexBlockEntity.FolderRef ref : refs) {
            if (remaining <= 0) break;

            if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) continue;
            if (!cabinet.isLinkedToController()) continue;

            ItemStack originalFolder = cabinet.getStack(ref.slot());
            if (!(originalFolder.getItem() instanceof FilingFolderItem folder)) continue;

            FilingFolderItem.FolderContents contents = originalFolder.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty()) continue;
            if (!contents.storedItemId().get().equals(itemId)) continue;

            int toAdd = Math.min(remaining, folder.getCapacity() - contents.count());
            if (toAdd <= 0) continue;

            ItemStack updatedFolder = originalFolder.copy();
            updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));

            try (Transaction innerTx = Transaction.open(tx)) {
                cabinet.inventory.extract(ref.slot(), ItemResource.of(originalFolder), 1, innerTx);
                cabinet.inventory.insert(ref.slot(), ItemResource.of(updatedFolder), 1, innerTx);
                innerTx.commit();
            }

            remaining -= toAdd;
        }

        return amount - remaining;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
        if (amount <= 0) return 0;

        var snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;

        Identifier expectedId = snap.get(slot).getKey();
        if (!resource.isEmpty()) {
            Identifier requestedId = BuiltInRegistries.ITEM.getKey(resource.toStack().getItem());
            if (!expectedId.equals(requestedId)) return 0;
        }

        List<FilingIndexBlockEntity.FolderRef> refs = indexEntity.getFolderRefs(expectedId);
        if (refs == null || refs.isEmpty()) return 0;

        int remaining = amount;

        for (FilingIndexBlockEntity.FolderRef ref : refs) {
            if (remaining <= 0) break;

            if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) continue;
            if (!cabinet.isLinkedToController()) continue;

            ItemStack originalFolder = cabinet.getStack(ref.slot());
            if (!(originalFolder.getItem() instanceof FilingFolderItem)) continue;

            FilingFolderItem.FolderContents contents = originalFolder.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) continue;
            if (!contents.storedItemId().get().equals(expectedId)) continue;

            int toExtract = Math.min(remaining, contents.count());
            if (toExtract <= 0) continue;

            ItemStack updatedFolder = originalFolder.copy();
            updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - toExtract));

            try (Transaction innerTx = Transaction.open(tx)) {
                cabinet.inventory.extract(ref.slot(), ItemResource.of(originalFolder), 1, innerTx);
                cabinet.inventory.insert(ref.slot(), ItemResource.of(updatedFolder), 1, innerTx);
                innerTx.commit();
            }

            remaining -= toExtract;
        }

        return amount - remaining;
    }
}