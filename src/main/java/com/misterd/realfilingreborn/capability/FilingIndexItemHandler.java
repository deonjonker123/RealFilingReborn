package com.misterd.realfilingreborn.capability;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FilingIndexItemHandler implements ResourceHandler<ItemResource> {
    private final FilingIndexBlockEntity indexEntity;
    private final AtomicReference<List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>>> snapshotRef = new AtomicReference<>(List.of());

    public FilingIndexItemHandler(FilingIndexBlockEntity indexEntity) {
        this.indexEntity = indexEntity;
        refreshSnapshot();
    }

    public void refreshSnapshot() {
        snapshotRef.set(List.copyOf(indexEntity.getIndexEntries()));
    }

    private List<Map.Entry<Identifier, FilingIndexBlockEntity.FolderRef>> snapshot() {
        return snapshotRef.get();
    }

    private Level level() {
        return indexEntity.getLevel();
    }

    @Override
    public int size() {
        return Math.max(snapshot().size(), 1);
    }

    @Override
    public ItemResource getResource(int slot) {
        var snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return ItemResource.EMPTY;
        if (snap.get(slot).getValue().count() <= 0) return ItemResource.EMPTY;
        return ItemResource.of(BuiltInRegistries.ITEM.getValue(snap.get(slot).getKey()));
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
        if (FilingFolderItem.hasSignificantNBT(resource.toStack())) return 0;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(resource.getItem());
        int remaining = amount;

        for (FilingIndexBlockEntity.FolderRef ref : indexEntity.getFolderRefs(itemId)) {
            if (remaining <= 0) break;
            remaining -= changeFolder(ref, itemId, remaining, tx);
        }

        return amount - remaining;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
        if (amount <= 0) return 0;

        var snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;

        Identifier expectedId = snap.get(slot).getKey();
        if (!resource.isEmpty() && !expectedId.equals(BuiltInRegistries.ITEM.getKey(resource.getItem()))) return 0;

        int remaining = amount;
        for (FilingIndexBlockEntity.FolderRef ref : indexEntity.getFolderRefs(expectedId)) {
            if (remaining <= 0) break;
            remaining -= changeFolder(ref, expectedId, -remaining, tx);
        }

        return amount - remaining;
    }

    private int changeFolder(FilingIndexBlockEntity.FolderRef ref, Identifier itemId, int delta, TransactionContext tx) {
        if (!(level().getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) return 0;
        if (!cabinet.isLinkedToController() || ref.slot() >= cabinet.getSlotCount()) return 0;

        ItemStack folderStack = cabinet.getStack(ref.slot());
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return 0;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || !contents.storedItemId().get().equals(itemId)) return 0;

        int moved = delta > 0
                ? Math.min(delta, folder.getCapacity() - contents.count())
                : Math.min(-delta, contents.count());
        if (moved <= 0) return 0;

        int newCount = delta > 0 ? contents.count() + moved : contents.count() - moved;
        ItemStack updated = folderStack.copyWithCount(1);
        updated.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(contents.storedItemId(), newCount));
        return FilingCabinetBlockEntity.swapFolder(cabinet.inventory, ref.slot(), folderStack, updated, tx) ? moved : 0;
    }
}
