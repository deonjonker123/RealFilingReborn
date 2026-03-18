package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.gui.RFRMenuTypes;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Optional;

public class FluidCanisterMenu extends AbstractContainerMenu {

    private final ItemStackHandler assignmentInventory;
    private final int canisterSlot;
    private final Inventory playerInventory;

    public FluidCanisterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readInt());
    }

    public FluidCanisterMenu(int containerId, Inventory playerInventory, int canisterSlot) {
        super(RFRMenuTypes.FLUID_CANISTER_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.canisterSlot = canisterSlot;

        this.assignmentInventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                updateCanisterAssignment();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (!(stack.getItem() instanceof BucketItem bucket) || bucket.content == null || !FluidHelper.isValidFluid(bucket.content)) return false;
                ItemStack canister = getCanister();
                if (canister.isEmpty()) return true;
                FluidCanisterItem.CanisterContents contents = canister.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null || contents.storedFluidId().isEmpty()) return true;
                ResourceLocation placedFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(bucket.content));
                return FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), placedFluidId);
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
                updateCanisterAssignment();
            }
        });
    }

    private ItemStack getCanister() {
        return playerInventory.getItem(canisterSlot);
    }

    private void updateCanisterAssignment() {
        ItemStack canister = getCanister();
        if (canister.isEmpty()) return;

        ItemStack assignedBucket = assignmentInventory.getStackInSlot(0);
        if (assignedBucket.isEmpty() || !(assignedBucket.getItem() instanceof BucketItem bucket)) return;

        Fluid fluid = bucket.content;
        if (!FluidHelper.isValidFluid(fluid)) return;

        FluidCanisterItem.CanisterContents contents = canister.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        ResourceLocation fluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(fluid));

        if (contents == null || contents.storedFluidId().isEmpty()) {
            canister.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                    new FluidCanisterItem.CanisterContents(Optional.of(fluidId), 1000));
            assignmentInventory.setStackInSlot(0, new ItemStack(Items.BUCKET));
        } else {
            if (!FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), fluidId)) return;

            int capacity = FluidCanisterItem.getCapacity(canister);
            int toAdd = Math.min(1000, capacity - contents.amount());
            if (toAdd < 1000) return;

            canister.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                    new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() + toAdd));
            assignmentInventory.setStackInSlot(0, new ItemStack(Items.BUCKET));
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

    public Component getAssignedFluidText() {
        ItemStack canister = getCanister();
        if (canister.isEmpty()) return null;

        FluidCanisterItem.CanisterContents contents = canister.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents == null || contents.storedFluidId().isEmpty()) return null;

        String fluidName = FluidHelper.getFluidDisplayName(contents.storedFluidId().get());
        return Component.translatable("gui.realfilingreborn.assigned_fluid",
                Component.literal(fluidName).withStyle(ChatFormatting.DARK_GRAY));
    }

    public Component getCurrentCountText() {
        ItemStack canister = getCanister();
        if (canister.isEmpty()) return null;

        FluidCanisterItem.CanisterContents contents = canister.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents == null || contents.storedFluidId().isEmpty()) return null;

        int capacity = FluidCanisterItem.getCapacity(canister);
        int buckets = contents.amount() / 1000;
        int mb = contents.amount() % 1000;
        String amountText = buckets > 0
                ? (mb > 0 ? buckets + "." + mb / 100 + "B" : buckets + "B")
                : mb + "mB";
        String capacityText = (capacity / 1000) + "B";
        return Component.translatable("gui.realfilingreborn.current_fluid_amount",
                amountText + "/" + capacityText).withStyle(ChatFormatting.DARK_GRAY);
    }

    public void extractFluid() {
        ItemStack canister = getCanister();
        if (canister.isEmpty()) return;

        FluidCanisterItem.CanisterContents contents = canister.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() < 1000) return;

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

        if (!bucketRemoved) return;

        canister.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                new FluidCanisterItem.CanisterContents(contents.storedFluidId(), Math.max(0, contents.amount() - 1000)));
        if (!player.getInventory().add(bucketToGive)) player.drop(bucketToGive, false);
        playerInventory.setChanged();
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack canister = getCanister();
        return !canister.isEmpty() && canister.getItem() instanceof FluidCanisterItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ItemStack assigned = assignmentInventory.getStackInSlot(0);
        if (!assigned.isEmpty()) player.drop(assigned, false);
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