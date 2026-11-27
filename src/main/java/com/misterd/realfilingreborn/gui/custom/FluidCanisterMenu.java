package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Optional;

public class FluidCanisterMenu extends AbstractContainerMenu {
    private final ItemStacksResourceHandler assignmentInventory;
    private final int canisterSlot;
    private final Inventory playerInventory;

    public FluidCanisterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readInt());
    }

    public FluidCanisterMenu(int containerId, Inventory playerInventory, int canisterSlot) {
        super(RFRMenuTypes.FLUID_CANISTER_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.canisterSlot = canisterSlot;

        this.assignmentInventory = new ItemStacksResourceHandler(1) {
            @Override
            public boolean isValid(int slot, ItemResource resource) {
                if (resource.isEmpty() || !(resource.getItem() instanceof BucketItem bucket)) return false;
                if (bucket.content == Fluids.EMPTY || !FluidHelper.isValidFluid(bucket.content)) return false;
                FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(getCanister());
                if (contents.storedFluidId().isEmpty()) return true;
                return FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), FluidHelper.getFluidId(bucket.content));
            }

            @Override
            protected void onContentsChanged(int slot, ItemStack previous) {
                updateCanisterAssignment();
            }
        };

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addSlot(new AssignmentSlot(assignmentInventory, 0, 80, 43));
    }

    private ItemStack getCanister() {
        return playerInventory.getItem(canisterSlot);
    }

    private ItemStack getSingleCanister() {
        ItemStack stack = getCanister();
        if (stack.getCount() <= 1) return stack;

        ItemStack remainder = stack.copyWithCount(stack.getCount() - 1);
        ItemStack single = stack.copyWithCount(1);
        playerInventory.setItem(canisterSlot, single);

        Player player = playerInventory.player;
        if (!player.level().isClientSide()) {
            int free = playerInventory.getFreeSlot();
            if (free >= 0) playerInventory.setItem(free, remainder);
            else player.drop(remainder, false);
        }
        return single;
    }

    private void updateCanisterAssignment() {
        ItemStack canister = getCanister();
        if (!(canister.getItem() instanceof FluidCanisterItem canisterItem)) return;

        ItemResource res = assignmentInventory.getResource(0);
        if (res.isEmpty() || !(res.getItem() instanceof BucketItem bucket) || bucket.content == Fluids.EMPTY) return;
        if (!FluidHelper.isValidFluid(bucket.content)) return;

        Identifier fluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(bucket.content));
        FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canister);
        if (contents.storedFluidId().isPresent() && !FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), fluidId)) return;
        if (canisterItem.getCapacity() - contents.amount() < 1000) return;

        canister = getSingleCanister();
        canister.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                new FluidCanisterItem.CanisterContents(Optional.of(contents.storedFluidId().orElse(fluidId)), contents.amount() + 1000));
        assignmentInventory.set(0, ItemResource.of(Items.BUCKET), 1);

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

    public Component getAssignedFluidText() {
        FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(getCanister());
        if (contents.storedFluidId().isEmpty()) return null;
        return Component.translatable("gui.realfilingreborn.assigned_fluid",
                Component.literal(FluidHelper.getFluidDisplayName(contents.storedFluidId().get())).withStyle(ChatFormatting.DARK_GRAY));
    }

    public Component getCurrentCountText() {
        ItemStack canister = getCanister();
        FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canister);
        if (contents.storedFluidId().isEmpty()) return null;
        String capacityText = (FluidCanisterItem.getCapacity(canister) / 1000) + "B";
        return Component.translatable("gui.realfilingreborn.current_fluid_amount",
                FluidHelper.formatAmount(contents.amount()) + "/" + capacityText).withStyle(ChatFormatting.DARK_GRAY);
    }

    public void extractFluid() {
        ItemStack canister = getCanister();
        FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canister);
        if (contents.storedFluidId().isEmpty() || contents.amount() < 1000) return;

        Player player = playerInventory.player;
        ItemStack bucketToGive = FluidHelper.getBucketForFluid(contents.storedFluidId().get());
        if (bucketToGive.isEmpty()) return;

        boolean bucketRemoved = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.BUCKET) {
                stack.shrink(1);
                bucketRemoved = true;
                break;
            }
        }
        if (!bucketRemoved) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.need_empty_bucket"));
            return;
        }

        canister = getSingleCanister();
        canister.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - 1000));
        if (!player.getInventory().add(bucketToGive)) player.drop(bucketToGive, false);
        playerInventory.setChanged();
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return getCanister().getItem() instanceof FluidCanisterItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ItemResource res = assignmentInventory.getResource(0);
        if (!res.isEmpty()) {
            ItemStack leftover = res.toStack(assignmentInventory.getAmountAsInt(0));
            if (!player.getInventory().add(leftover)) player.drop(leftover, false);
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
            if (stack.isEmpty()) handler.set(index, ItemResource.EMPTY, 0);
            else handler.set(index, ItemResource.of(stack), stack.getCount());
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
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
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
