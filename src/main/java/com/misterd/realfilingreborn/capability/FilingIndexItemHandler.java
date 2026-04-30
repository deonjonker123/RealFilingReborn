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

        FilingIndexBlockEntity.FolderRef ref = indexEntity.getFolderRef(itemId);
        if (ref != null) {
            return insertIntoRef(ref, stack, itemId, simulate);
        }

        return stack;
    }

    private ItemStack insertIntoRef(FilingIndexBlockEntity.FolderRef ref, ItemStack stack, ResourceLocation itemId, boolean simulate) {
        if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) return stack;
        if (!cabinet.isLinkedToController()) return stack;

        ItemStack folderStack = cabinet.inventory.getStackInSlot(ref.slot());
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return stack;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return stack;
        if (!contents.storedItemId().get().equals(itemId)) return stack;
        if (contents.count() > Integer.MAX_VALUE - stack.getCount()) return stack;

        int toAdd = Math.min(stack.getCount(), folder.getCapacity() - contents.count());
        if (toAdd <= 0) return stack;

        if (!simulate) {
            folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
            cabinet.inventory.setStackInSlot(ref.slot(), folderStack);
        }

        ItemStack remaining = stack.copy();
        remaining.shrink(toAdd);
        return remaining;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        List<Map.Entry<ResourceLocation, FilingIndexBlockEntity.FolderRef>> snap = snapshot();
        if (slot < 0 || slot >= snap.size()) return ItemStack.EMPTY;

        FilingIndexBlockEntity.FolderRef ref = snap.get(slot).getValue();
        if (!(level.getBlockEntity(ref.cabinetPos()) instanceof FilingCabinetBlockEntity cabinet)) return ItemStack.EMPTY;
        if (!cabinet.isLinkedToController()) return ItemStack.EMPTY;

        ItemStack folderStack = cabinet.inventory.getStackInSlot(ref.slot());
        if (!(folderStack.getItem() instanceof FilingFolderItem)) return ItemStack.EMPTY;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(snap.get(slot).getKey());

        int extractAmount = Math.min(amount, contents.count());
        if (extractAmount <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - extractAmount));
            cabinet.inventory.setStackInSlot(ref.slot(), folderStack);
        }

        return new ItemStack(item, extractAmount);
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