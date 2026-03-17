package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
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
    private final ItemStack folderStack;
    private final int folderSlot;
    private final Inventory playerInventory;

    public FilingFolderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readInt());
    }

    public FilingFolderMenu(int containerId, Inventory playerInventory, int folderSlot) {
        super(RFRMenuTypes.FILING_FOLDER_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.folderSlot = folderSlot;
        this.folderStack = playerInventory.getItem(folderSlot);

        this.assignmentInventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                updateFolderAssignment();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !FilingFolderItem.hasSignificantNBT(stack);
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

    private void updateFolderAssignment() {
        if (folderStack.isEmpty()) return;

        ItemStack assignedItem = assignmentInventory.getStackInSlot(0);
        if (assignedItem.isEmpty()) return;

        FilingFolderItem.FolderContents currentContents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (currentContents == null || currentContents.storedItemId().isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(assignedItem.getItem());
            folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(Optional.of(itemId), assignedItem.getCount()));
            assignmentInventory.setStackInSlot(0, ItemStack.EMPTY);
        }
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

    public Component getCurrentCountText() {
        if (folderStack.isEmpty()) return null;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents != null && contents.storedItemId().isPresent()) {
            return Component.translatable("gui.realfilingreborn.current_item_count",
                    String.format("%,d", contents.count()));
        }
        return null;
    }

    public void extractItems() {
        if (folderStack.isEmpty()) return;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
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

        folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                new FilingFolderItem.FolderContents(contents.storedItemId(), Math.max(0, contents.count() - extractAmount)));
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return !folderStack.isEmpty() && folderStack.getItem() instanceof FilingFolderItem;
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
