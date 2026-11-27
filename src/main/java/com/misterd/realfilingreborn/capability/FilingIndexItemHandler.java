package com.misterd.realfilingreborn.capability;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FilingIndexItemHandler implements IItemHandler {
    private final FilingIndexBlockEntity indexEntity;
    private final Level level;
    private final AtomicReference<List<Map.Entry<ResourceLocation, FilingIndexBlockEntity.FolderRef>>> snapshotRef = new AtomicReference<>(List.of());

    public FilingIndexItemHandler(FilingIndexBlockEntity indexEntity) {
        this.indexEntity = indexEntity;
        this.level = indexEntity.getLevel();
        refreshSnapshot();
    }

    public void refreshSnapshot() {
        snapshotRef.set(List.copyOf(indexEntity.getIndexEntries()));
    }

    private List<Map.Entry<ResourceLocation, FilingIndexBlockEntity.FolderRef>> snapshot() {
        return snapshotRef.get();
    }

    @Override
    public int getSlots() {
        return Math.max(snapshot().size(), 1);
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        List<Map.Entry<ResourceLocation, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return ItemStack.EMPTY;
        FilingIndexBlockEntity.FolderRef ref = snap.get(slot).getValue();
        if (ref.count() <= 0) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(snap.get(slot).getKey());
        return new ItemStack(item, ref.count());
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || FilingFolderItem.hasSignificantNBT(stack)) return stack;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int remaining = stack.getCount();

        for (FilingIndexBlockEntity.FolderRef ref : indexEntity.getFolderRefs(itemId)) {
            if (remaining <= 0) break;
            remaining -= insertIntoRef(ref, itemId, remaining, simulate);
        }

        return remaining == stack.getCount() ? stack : stack.copyWithCount(remaining);
    }

    private int insertIntoRef(FilingIndexBlockEntity.FolderRef ref, ResourceLocation itemId, int amount, boolean simulate) {
        if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) return 0;
        if (!cabinet.isLinkedToController()) return 0;
        if (ref.slot() >= cabinet.inventory.getSlots()) return 0;

        ItemStack folderStack = cabinet.inventory.getStackInSlot(ref.slot());
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return 0;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return 0;
        if (!contents.storedItemId().get().equals(itemId)) return 0;

        int toAdd = Math.min(amount, folder.getCapacity() - contents.count());
        if (toAdd <= 0) return 0;

        if (!simulate) {
            ItemStack updated = folderStack.copyWithCount(1);
            updated.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
            cabinet.inventory.setStackInSlot(ref.slot(), updated);
        }
        return toAdd;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        List<Map.Entry<ResourceLocation, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return ItemStack.EMPTY;

        ResourceLocation itemId = snap.get(slot).getKey();
        Item item = BuiltInRegistries.ITEM.get(itemId);
        int wanted = Math.min(amount, item.getDefaultMaxStackSize());
        int extracted = 0;

        for (FilingIndexBlockEntity.FolderRef ref : indexEntity.getFolderRefs(itemId)) {
            if (extracted >= wanted) break;
            extracted += extractFromRef(ref, itemId, wanted - extracted, simulate);
        }

        return extracted > 0 ? new ItemStack(item, extracted) : ItemStack.EMPTY;
    }

    private int extractFromRef(FilingIndexBlockEntity.FolderRef ref, ResourceLocation itemId, int amount, boolean simulate) {
        if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) return 0;
        if (!cabinet.isLinkedToController()) return 0;
        if (ref.slot() >= cabinet.inventory.getSlots()) return 0;

        ItemStack folderStack = cabinet.inventory.getStackInSlot(ref.slot());
        if (!(folderStack.getItem() instanceof FilingFolderItem)) return 0;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return 0;
        if (!contents.storedItemId().get().equals(itemId)) return 0;

        int toExtract = Math.min(amount, contents.count());
        if (!simulate) {
            ItemStack updated = folderStack.copyWithCount(1);
            updated.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - toExtract));
            cabinet.inventory.setStackInSlot(ref.slot(), updated);
        }
        return toExtract;
    }

    @Override
    public int getSlotLimit(int slot) {
        List<Map.Entry<ResourceLocation, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return 0;
        return snap.get(slot).getValue().capacity();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !stack.isEmpty() && !FilingFolderItem.hasSignificantNBT(stack);
    }
}