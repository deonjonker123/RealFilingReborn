package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.gui.custom.FilingCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import javax.annotation.Nullable;

public class FilingCabinetBlockEntity extends BlockEntity implements MenuProvider {

    @Nullable
    private BlockPos controllerPos = null;

    private final boolean[] dirtySlots = new boolean[5];
    private boolean anySlotDirty = false;

    public final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(5) {
        @Override
        protected void onContentsChanged(int slot, ItemStack previous) {
            setChanged();
            if (slot >= 0 && slot < 5) {
                dirtySlots[slot] = true;
                anySlotDirty = true;
            }
            if (controllerPos == null && level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
            } else if (controllerPos != null && level != null && !level.isClientSide()) {
                if (level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index) {
                    index.scheduleFlush();
                }
            }
        }
    };

    public FilingCabinetBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.FILING_CABINET_BE.get(), pos, blockState);
    }

    @Nullable
    public ResourceHandler<ItemResource> getCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FilingCabinetBlock.FACING) == side) return null;
        return inventory;
    }

    public boolean[] consumeDirtySlots() {
        if (!anySlotDirty) return null;
        boolean[] snapshot = dirtySlots.clone();
        for (int i = 0; i < 5; i++) dirtySlots[i] = false;
        anySlotDirty = false;
        return snapshot;
    }

    public boolean hasAnyDirtySlot() {
        return anySlotDirty;
    }

    public void sendUpdatePacket() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }

    public void setControllerPos(BlockPos pos) {
        controllerPos = pos;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void clearControllerPos() {
        controllerPos = null;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public boolean isLinkedToController() {
        return controllerPos != null;
    }

    public ItemStack getStack(int slot) {
        ItemResource res = inventory.getResource(slot);
        if (res.isEmpty()) return ItemStack.EMPTY;
        return res.toStack(inventory.getAmountAsInt(slot));
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.size());
        for (int i = 0; i < inventory.size(); i++) {
            inv.setItem(i, getStack(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        drops();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        if (controllerPos != null) {
            output.putLong("controllerPos", controllerPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        long packed = input.getLongOr("controllerPos", Long.MIN_VALUE);
        controllerPos = packed != Long.MIN_VALUE ? BlockPos.of(packed) : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("blockentity.realfilingreborn.name");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new FilingCabinetMenu(id, playerInventory, this);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}