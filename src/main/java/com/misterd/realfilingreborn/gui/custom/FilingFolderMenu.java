package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Optional;

public class FilingFolderMenu extends AbstractContainerMenu {

    private final ItemStackHandler assignmentInventory;
    private final int folderSlot;
    private final Inventory playerInventory;

    public FilingFolderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readInt());
    }

    public FilingFolderMenu(int containerId, Inventory playerInventory, int folderSlot) {
        super(RFRMenuTypes.FILING_FOLDER_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.folderSlot = folderSlot;

        this.assignmentInventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                updateFolderAssignment();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (FilingFolderItem.hasSignificantNBT(stack)) return false;
                ItemStack folder = getFolder();
                if (folder.isEmpty()) return true;
                FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
                if (contents == null || contents.storedItemId().isEmpty()) return true;
                return contents.storedItemId().get().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
        };

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addSlot(new SlotItemHandler(assignmentInventory, 0, 80, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return assignmentInventory.isItemValid(0, stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                updateFolderAssignment();
            }
        });
    }

    private ItemStack getFolder() {
        return playerInventory.getItem(folderSlot);
    }

    private void updateFolderAssignment() {
        ItemStack folder = getFolder();
        if (folder.isEmpty()) return;

        ItemStack assignedItem = assignmentInventory.getStackInSlot(0);
        if (assignedItem.isEmpty()) return;

        FilingFolderItem.FolderContents currentContents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());

        if (currentContents == null || currentContents.storedItemId().isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(assignedItem.getItem());
            folder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(Optional.of(itemId), assignedItem.getCount()));
            assignmentInventory.setStackInSlot(0, ItemStack.EMPTY);
        } else {
            ResourceLocation existingId = currentContents.storedItemId().get();
            ResourceLocation placedId = BuiltInRegistries.ITEM.getKey(assignedItem.getItem());
            if (!existingId.equals(placedId)) return;

            FilingFolderItem folder2 = (FilingFolderItem) folder.getItem();
            int capacity = folder2.getCapacity();
            int toAdd = Math.min(assignedItem.getCount(), capacity - currentContents.count());
            if (toAdd <= 0) return;

            folder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(currentContents.storedItemId(), currentContents.count() + toAdd));
            assignedItem.shrink(toAdd);
            assignmentInventory.setStackInSlot(0, assignedItem.isEmpty() ? ItemStack.EMPTY : assignedItem);
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
            if (!assignmentInventory.isItemValid(0, stack)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 36, 37, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    public Component getAssignedItemText() {
        ItemStack folder = getFolder();
        if (folder.isEmpty()) return null;

        FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return null;

        Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
        return Component.translatable("gui.realfilingreborn.assigned_item",
                item.getDescription().copy().withStyle(ChatFormatting.DARK_GRAY));
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

        ResourceLocation itemId = contents.storedItemId().get();
        Item item = BuiltInRegistries.ITEM.get(itemId);
        ItemStack dummy = new ItemStack(item);
        int extractAmount = Math.min(contents.count(), item.getMaxStackSize(dummy));
        if (extractAmount <= 0) return;

        ItemStack extracted = new ItemStack(item, extractAmount);
        Player player = playerInventory.player;
        if (!player.getInventory().add(extracted)) {
            player.drop(extracted, false);
        }

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
        ItemStack assigned = assignmentInventory.getStackInSlot(0);
        if (!assigned.isEmpty()) {
            player.drop(assigned, false);
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 70 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 128));
        }
    }
}