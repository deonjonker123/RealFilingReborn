package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.Config;
import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.gui.custom.FilingCabinetMenu;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FilingCabinetBlockEntity extends BlockEntity implements MenuProvider {

    @Nullable
    private BlockPos controllerPos = null;

    public final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            FilingCabinetBlockEntity.this.setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final Map<Direction, IItemHandler> handlers = new HashMap<>();

    public FilingCabinetBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.FILING_CABINET_BE.get(), pos, blockState);
    }

    @Nullable
    public IItemHandler getCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FilingCabinetBlock.FACING) == side) return null;
        return handlers.computeIfAbsent(side != null ? side : Direction.UP,
                s -> new FilingCabinetItemHandler(this, s));
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (controllerPos != null) {
            tag.putLong("controllerPos", controllerPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        controllerPos = tag.contains("controllerPos") ? BlockPos.of(tag.getLong("controllerPos")) : null;
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

    private void notifyFolderContentsChanged() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
            setChanged();
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

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private static class FilingCabinetItemHandler implements IItemHandler {

        private final FilingCabinetBlockEntity cabinet;
        private final Direction side;

        public FilingCabinetItemHandler(FilingCabinetBlockEntity cabinet, @Nullable Direction side) {
            this.cabinet = cabinet;
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 5;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;

            ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
            if (!(folderStack.getItem() instanceof FilingFolderItem)) return ItemStack.EMPTY;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemStack.EMPTY;

            Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
            return new ItemStack(item, contents.count());
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            if (FilingFolderItem.hasSignificantNBT(stack)) return stack;

            ResourceLocation stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (side != null) {
                // Automation: search all slots for a matching folder
                for (int i = 0; i < 5; i++) {
                    ItemStack folderStack = cabinet.inventory.getStackInSlot(i);
                    if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) continue;

                    FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
                    if (contents == null || contents.storedItemId().isEmpty()) continue;
                    if (!contents.storedItemId().get().equals(stackItemId)) continue;

                    int toAdd = Math.min(stack.getCount(), Config.getMaxFolderStorage() - contents.count());
                    if (toAdd <= 0) continue;

                    if (!simulate) {
                        folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                                new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
                        cabinet.notifyFolderContentsChanged();
                    }

                    ItemStack remaining = stack.copy();
                    remaining.shrink(toAdd);
                    return remaining;
                }
                return stack;
            } else {
                // Direct slot insertion
                ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
                if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) return stack;

                FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
                if (contents == null) contents = new FilingFolderItem.FolderContents(Optional.empty(), 0);

                if (contents.storedItemId().isEmpty()) {
                    int toAdd = Math.min(stack.getCount(), Config.getMaxFolderStorage());
                    if (!simulate) {
                        folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                                new FilingFolderItem.FolderContents(Optional.of(stackItemId), toAdd));
                        cabinet.notifyFolderContentsChanged();
                    }
                    ItemStack remaining = stack.copy();
                    remaining.shrink(toAdd);
                    return remaining;
                }

                if (!contents.storedItemId().get().equals(stackItemId)) return stack;

                int availableSpace = Config.getMaxFolderStorage() - contents.count();
                int toAdd = Math.min(stack.getCount(), availableSpace);
                if (toAdd <= 0) return stack;

                if (!simulate) {
                    folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                            new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
                    cabinet.notifyFolderContentsChanged();
                }

                ItemStack remaining = stack.copy();
                remaining.shrink(toAdd);
                return remaining;
            }
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= getSlots() || amount <= 0) return ItemStack.EMPTY;

            ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
            if (!(folderStack.getItem() instanceof FilingFolderItem)) return ItemStack.EMPTY;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemStack.EMPTY;

            Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
            ItemStack result = new ItemStack(item, 1);
            int actualExtract = Math.min(Math.min(contents.count(), amount), item.getMaxStackSize(result));
            if (actualExtract <= 0) return ItemStack.EMPTY;

            result.setCount(actualExtract);
            if (!simulate) {
                folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - actualExtract));
                cabinet.notifyFolderContentsChanged();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return !(stack.getItem() instanceof FilingFolderItem);
        }
    }
}
