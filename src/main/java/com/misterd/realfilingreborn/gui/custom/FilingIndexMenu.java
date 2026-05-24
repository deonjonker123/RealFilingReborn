package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.block.RFRBlocks;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.item.custom.DiamondRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.IronRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.NetheriteRangeUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class FilingIndexMenu extends AbstractContainerMenu {

    public final FilingIndexBlockEntity blockEntity;
    private final Level level;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 36;
    private static final int TE_INVENTORY_SLOT_COUNT = 1;

    public FilingIndexMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FilingIndexMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(RFRMenuTypes.FILING_INDEX_MENU.get(), containerId);
        this.blockEntity = (FilingIndexBlockEntity) blockEntity;
        this.level = inv.player.level();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addSlot(new UpgradeSlot(this.blockEntity, 0, 80, 23));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            Item item = sourceStack.getItem();
            if (!(item instanceof IronRangeUpgradeItem) && !(item instanceof DiamondRangeUpgradeItem) && !(item instanceof NetheriteRangeUpgradeItem)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (index >= TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) return ItemStack.EMPTY;
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
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, RFRBlocks.FILING_INDEX.get());
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

    private static class UpgradeSlot extends Slot {
        private final FilingIndexBlockEntity be;
        private final int index;

        public UpgradeSlot(FilingIndexBlockEntity be, int index, int x, int y) {
            super(new SimpleContainer(be.inventory.size()), index, x, y);
            this.be = be;
            this.index = index;
        }

        @Override
        public ItemStack getItem() {
            return be.getUpgradeStack();
        }

        @Override
        public boolean hasItem() {
            return !be.getUpgradeStack().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            try (Transaction tx = Transaction.openRoot()) {
                ItemStack existing = be.getUpgradeStack();
                if (!existing.isEmpty())
                    be.inventory.extract(index, ItemResource.of(existing), existing.getCount(), tx);
                if (!stack.isEmpty())
                    be.inventory.insert(index, ItemResource.of(stack), stack.getCount(), tx);
                tx.commit();
            }
            setChanged();
        }

        @Override
        public void setChanged() {
            be.setChanged();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof IronRangeUpgradeItem
                    || stack.getItem() instanceof DiamondRangeUpgradeItem
                    || stack.getItem() instanceof NetheriteRangeUpgradeItem;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !be.getUpgradeStack().isEmpty();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack existing = getItem();
            if (existing.isEmpty()) return ItemStack.EMPTY;
            int toExtract = Math.min(amount, existing.getCount());
            try (Transaction tx = Transaction.openRoot()) {
                int extracted = be.inventory.extract(index, ItemResource.of(existing), toExtract, tx);
                tx.commit();
                return new ItemStack(existing.getItem(), extracted);
            }
        }
    }
}