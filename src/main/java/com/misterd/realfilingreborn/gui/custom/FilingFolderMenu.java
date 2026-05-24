package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Optional;

public class FilingFolderMenu extends AbstractContainerMenu {

    private final ItemStacksResourceHandler assignmentInventory;
    private final int folderSlot;
    private final Inventory playerInventory;

    public FilingFolderMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readInt());
    }

    public FilingFolderMenu(int containerId, Inventory playerInventory, int folderSlot) {
        super(RFRMenuTypes.FILING_FOLDER_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.folderSlot = folderSlot;

        this.assignmentInventory = new ItemStacksResourceHandler(1) {
            @Override
            public boolean isValid(int slot, ItemResource resource) {
                if (resource.isEmpty()) return false;
                ItemStack stack = resource.toStack();
                if (FilingFolderItem.hasSignificantNBT(stack)) return false;
                ItemStack folder = getFolder();
                if (folder.isEmpty()) return true;
                FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
                if (contents == null || contents.storedItemId().isEmpty()) return true;
                return contents.storedItemId().get().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }

            @Override
            protected void onContentsChanged(int slot, ItemStack previous) {
                updateFolderAssignment();
            }
        };

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addSlot(new AssignmentSlot(assignmentInventory, 0, 80, 43));
    }

    private ItemStack getFolder() {
        return playerInventory.getItem(folderSlot);
    }

    private void updateFolderAssignment() {
        ItemStack folder = getFolder();
        if (folder.isEmpty()) return;

        ItemResource res = assignmentInventory.getResource(0);
        if (res.isEmpty()) return;
        ItemStack assignedItem = res.toStack(assignmentInventory.getAmountAsInt(0));
        if (assignedItem.isEmpty()) return;

        FilingFolderItem.FolderContents currentContents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());

        if (currentContents == null || currentContents.storedItemId().isEmpty()) {
            int remainderCount = folder.getCount() - 1;
            ItemStack remainder = remainderCount > 0 ? folder.copyWithCount(remainderCount) : ItemStack.EMPTY;

            ItemStack singleFolder = folder.copyWithCount(1);
            Identifier itemId = BuiltInRegistries.ITEM.getKey(assignedItem.getItem());
            singleFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(Optional.of(itemId), assignedItem.getCount()));
            playerInventory.setItem(folderSlot, singleFolder);

            if (!remainder.isEmpty() && !playerInventory.player.level().isClientSide())
                playerInventory.player.drop(remainder, false);

            try (Transaction tx = Transaction.openRoot()) {
                assignmentInventory.extract(0, res, assignmentInventory.getAmountAsInt(0), tx);
                tx.commit();
            }
        } else {
            Identifier existingId = currentContents.storedItemId().get();
            Identifier placedId = BuiltInRegistries.ITEM.getKey(assignedItem.getItem());
            if (!existingId.equals(placedId)) return;

            int capacity = ((FilingFolderItem) folder.getItem()).getCapacity();
            int toAdd = Math.min(assignedItem.getCount(), capacity - currentContents.count());
            if (toAdd <= 0) return;

            folder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(currentContents.storedItemId(), currentContents.count() + toAdd));
            try (Transaction tx = Transaction.openRoot()) {
                assignmentInventory.extract(0, res, toAdd, tx);
                tx.commit();
            }
        }

        playerInventory.setChanged();
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 36) {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        } else {
            if (!assignmentInventory.isValid(0, ItemResource.of(stack))) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 36, 37, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    public Component getAssignedItemText() {
        ItemStack folder = getFolder();
        if (folder.isEmpty()) return null;
        FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return null;
        Item item = BuiltInRegistries.ITEM.getValue(contents.storedItemId().get());
        return Component.translatable("gui.realfilingreborn.assigned_item",
                new ItemStack(item).getHoverName().copy().withStyle(ChatFormatting.DARK_GRAY));
    }

    public Component getCurrentCountText() {
        ItemStack folder = getFolder();
        if (folder.isEmpty()) return null;
        FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return null;
        int capacity = ((FilingFolderItem) folder.getItem()).getCapacity();
        return Component.translatable("gui.realfilingreborn.current_item_count",
                String.format("%,d ", contents.count()) + "/" + String.format("%,d", capacity)).withStyle(ChatFormatting.DARK_GRAY);
    }

    public void extractItems() {
        ItemStack folder = getFolder();
        if (folder.isEmpty()) return;
        FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return;
        Identifier itemId = contents.storedItemId().get();
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        ItemStack dummy = new ItemStack(item);
        int extractAmount = Math.min(contents.count(), item.getMaxStackSize(dummy));
        if (extractAmount <= 0) return;
        ItemStack extracted = new ItemStack(item, extractAmount);
        Player player = playerInventory.player;
        if (!player.getInventory().add(extracted)) player.drop(extracted, false);
        folder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                new FilingFolderItem.FolderContents(contents.storedItemId(), Math.max(0, contents.count() - extractAmount)));
        playerInventory.setChanged();
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack folder = getFolder();
        return !folder.isEmpty() && folder.getItem() instanceof FilingFolderItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ItemResource res = assignmentInventory.getResource(0);
        if (!res.isEmpty()) {
            ItemStack leftover = res.toStack(assignmentInventory.getAmountAsInt(0));
            if (!leftover.isEmpty()) player.drop(leftover, false);
        }
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 70 + row * 18));
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inv, i, 8 + i * 18, 128));
    }

    private class AssignmentSlot extends Slot {
        private final ItemStacksResourceHandler handler;
        private final int index;

        AssignmentSlot(ItemStacksResourceHandler handler, int index, int x, int y) {
            super(new SimpleContainer(1), index, x, y);
            this.handler = handler;
            this.index = index;
        }

        @Override
        public ItemStack getItem() {
            ItemResource res = handler.getResource(index);
            if (res.isEmpty()) return ItemStack.EMPTY;
            return res.toStack(handler.getAmountAsInt(index));
        }

        @Override
        public boolean hasItem() {
            return !handler.getResource(index).isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            try (Transaction tx = Transaction.openRoot()) {
                ItemResource existing = handler.getResource(index);
                if (!existing.isEmpty())
                    handler.extract(index, existing, handler.getAmountAsInt(index), tx);
                if (!stack.isEmpty())
                    handler.insert(index, ItemResource.of(stack), stack.getCount(), tx);
                tx.commit();
            }
            updateFolderAssignment();
        }

        @Override
        public void setChanged() {
            broadcastChanges();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return handler.isValid(index, ItemResource.of(stack));
        }

        @Override
        public boolean mayPickup(Player player) {
            return !handler.getResource(index).isEmpty();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemResource res = handler.getResource(index);
            if (res.isEmpty()) return ItemStack.EMPTY;
            int toExtract = Math.min(amount, handler.getAmountAsInt(index));
            try (Transaction tx = Transaction.openRoot()) {
                int extracted = handler.extract(index, res, toExtract, tx);
                tx.commit();
                return res.toStack(extracted);
            }
        }
    }
}