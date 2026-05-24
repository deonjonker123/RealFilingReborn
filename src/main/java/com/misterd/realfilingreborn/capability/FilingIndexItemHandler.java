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
    private final AtomicReference<List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>>> snapshotRef = new AtomicReference<>(List.of());

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
        List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return ItemResource.EMPTY;
        FilingIndexBlockEntity.FolderRef ref = snap.get(slot).getValue();
        if (ref.count() <= 0) return ItemResource.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(snap.get(slot).getKey());
        if (item == null) return ItemResource.EMPTY;
        return ItemResource.of(new ItemStack(item));
    }

    @Override
    public long getAmountAsLong(int slot) {
        List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;
        return snap.get(slot).getValue().count();
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
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
        FilingIndexBlockEntity.FolderRef ref = indexEntity.getFolderRef(itemId);
        if (ref == null) return 0;

        if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) return 0;
        if (!cabinet.isLinkedToController()) return 0;

        ItemStack folderStack = cabinet.getStack(ref.slot());
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return 0;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return 0;
        if (!contents.storedItemId().get().equals(itemId)) return 0;

        int toAdd = (int) Math.min(amount, folder.getCapacity() - contents.count());
        if (toAdd <= 0) return 0;

        folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
        try (Transaction innerTx = Transaction.open(tx)) {
            cabinet.inventory.extract(ref.slot(), ItemResource.of(cabinet.getStack(ref.slot())), 1, innerTx);
            cabinet.inventory.insert(ref.slot(), ItemResource.of(folderStack), 1, innerTx);
            innerTx.commit();
        }
        return toAdd;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
        if (amount <= 0) return 0;

        List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;

        FilingIndexBlockEntity.FolderRef ref = snap.get(slot).getValue();
        if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) return 0;
        if (!cabinet.isLinkedToController()) return 0;

        ItemStack folderStack = cabinet.getStack(ref.slot());
        if (!(folderStack.getItem() instanceof FilingFolderItem)) return 0;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return 0;

        int toExtract = Math.min(amount, contents.count());
        if (toExtract <= 0) return 0;

        folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - toExtract));
        try (Transaction innerTx = Transaction.open(tx)) {
            cabinet.inventory.extract(ref.slot(), ItemResource.of(cabinet.getStack(ref.slot())), 1, innerTx);
            cabinet.inventory.insert(ref.slot(), ItemResource.of(folderStack), 1, innerTx);
            innerTx.commit();
        }
        return toExtract;
    }
}