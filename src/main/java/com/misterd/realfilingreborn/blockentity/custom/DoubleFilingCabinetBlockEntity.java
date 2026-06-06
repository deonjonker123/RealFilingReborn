package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.gui.custom.DoubleFilingCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.Optional;

public class DoubleFilingCabinetBlockEntity extends BlockEntity implements MenuProvider {

    private long lastDepositTime = -100;

    @Nullable
    private BlockPos controllerPos = null;

    private final boolean[] dirtySlots = new boolean[2];
    private boolean anySlotDirty = false;

    public final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(2) {
        @Override
        protected void onContentsChanged(int slot, ItemStack previous) {
            System.out.println("onContentsChanged slot=" + slot + " level.isClientSide=" + (level != null && level.isClientSide()));
            setChanged();
            if (slot >= 0 && slot < 2) {
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

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            if (resource.isEmpty()) return false;
            return resource.toStack().getItem() instanceof FilingFolderItem;
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) return 0;
            ItemStack incoming = resource.toStack(amount);

            if (incoming.getItem() instanceof FilingFolderItem) {
                return super.insert(slot, resource, amount, tx);
            }

            if (FilingFolderItem.hasSignificantNBT(incoming)) return 0;

            Identifier itemId = BuiltInRegistries.ITEM.getKey(incoming.getItem());

            for (int i = 0; i < 2; i++) {
                ItemStack folderStack = getStack(i);
                if (!(folderStack.getItem() instanceof FilingFolderItem folder)) continue;

                FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
                if (contents == null) continue;

                if (contents.storedItemId().isPresent() && !contents.storedItemId().get().equals(itemId)) continue;

                int capacity = folder.getCapacity();
                int toAdd = Math.min(amount, capacity - contents.count());
                if (toAdd <= 0) continue;

                ItemStack updatedFolder = folderStack.copy();
                updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(Optional.of(itemId), contents.count() + toAdd));

                super.extract(i, ItemResource.of(folderStack), 1, tx);
                super.insert(i, ItemResource.of(updatedFolder), 1, tx);

                return toAdd;
            }

            return 0;
        }
    };

    public DoubleFilingCabinetBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.DOUBLE_FILING_CABINET_BE.get(), pos, blockState);
    }

    @Nullable
    public ResourceHandler<ItemResource> getCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FilingCabinetBlock.FACING) == side) return null;
        return inventory;
    }

    public boolean[] consumeDirtySlots() {
        if (!anySlotDirty) return null;
        boolean[] snapshot = dirtySlots.clone();
        for (int i = 0; i < 2; i++) dirtySlots[i] = false;
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

    public long getLastDepositTime() {
        return lastDepositTime;
    }

    public void setLastDepositTime(long time) {
        lastDepositTime = time;
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
        return new DoubleFilingCabinetMenu(id, playerInventory, this);
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