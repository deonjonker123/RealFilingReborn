package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FilingCabinetMenu extends AbstractContainerMenu {

    public final FilingCabinetBlockEntity blockEntity;
    private final Level level;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 36;
    private static final int TE_INVENTORY_SLOT_COUNT = 5;

    public FilingCabinetMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FilingCabinetMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(RFRMenuTypes.FILING_CABINET_MENU.get(), containerId);
        this.blockEntity = (FilingCabinetBlockEntity) blockEntity;
        this.level = inv.player.level();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        for (int i = 0; i < 5; i++) {
            int x = 44 + i * 18;
            addSlot(new SlotItemHandler(this.blockEntity.inventory, i, x, 23) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof FilingFolderItem;
                }
            });
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            if (!(sourceStack.getItem() instanceof FilingFolderItem)) return ItemStack.EMPTY;
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (index >= TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, RFRBlocks.FILING_CABINET.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < PLAYER_INVENTORY_ROW_COUNT; row++) {
            for (int col = 0; col < PLAYER_INVENTORY_COLUMN_COUNT; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 61 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 119));
        }
    }
}
