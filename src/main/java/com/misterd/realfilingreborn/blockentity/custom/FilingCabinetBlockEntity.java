package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.gui.custom.FilingCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FilingCabinetBlockEntity extends BlockEntity implements MenuProvider {
    @Nullable
    private BlockPos controllerPos = null;

    private final int slotCount;
    private final boolean[] dirtySlots;
    private boolean anySlotDirty = false;

    private final List<ItemStack> overflow = new ArrayList<>();

    private long lastDepositTime = -100;
    @Nullable
    private UUID lastDepositPlayer = null;

    public final CabinetInventory inventory;

    private final Map<Direction, IItemHandler> handlers = new HashMap<>();

    public FilingCabinetBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.FILING_CABINET_BE.get(), pos, blockState);
        this.slotCount = blockState.getBlock() instanceof FilingCabinetBlock cabinet ? cabinet.getSlotCount() : 4;
        this.dirtySlots = new boolean[slotCount];
        this.inventory = new CabinetInventory(slotCount);
    }

    public int getSlotCount() {
        return slotCount;
    }

    public class CabinetInventory extends ItemStackHandler {
        CabinetInventory(int size) {
            super(size);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof FilingFolderItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot >= 0 && slot < dirtySlots.length) {
                dirtySlots[slot] = true;
                anySlotDirty = true;
            }
            if (level == null || level.isClientSide()) return;
            if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index) {
                index.scheduleFlush();
            } else {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
            }
        }

        void normalize(int size) {
            List<ItemStack> extras = new ArrayList<>();
            NonNullList<ItemStack> resized = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty()) continue;
                if (i < size) {
                    resized.set(i, stack.copyWithCount(1));
                    for (int n = 1; n < stack.getCount(); n++) extras.add(stack.copyWithCount(1));
                } else {
                    for (int n = 0; n < stack.getCount(); n++) extras.add(stack.copyWithCount(1));
                }
            }
            for (ItemStack extra : extras) {
                int free = -1;
                for (int i = 0; i < size; i++) {
                    if (resized.get(i).isEmpty()) { free = i; break; }
                }
                if (free >= 0) resized.set(free, extra);
                else overflow.add(extra);
            }
            stacks = resized;
        }
    }

    @Nullable
    public IItemHandler getCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FilingCabinetBlock.FACING) == side) return null;
        return handlers.computeIfAbsent(side != null ? side : Direction.UP,
                s -> new FilingCabinetItemHandler(this));
    }

    public boolean[] consumeDirtySlots() {
        if (!anySlotDirty) return null;
        boolean[] snapshot = dirtySlots.clone();
        java.util.Arrays.fill(dirtySlots, false);
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

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots() + overflow.size());
        int i = 0;
        for (; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        for (ItemStack extra : overflow) inv.setItem(i++, extra);
        overflow.clear();
        Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && controllerPos != null) {
            var server = serverLevel.getServer();
            server.tell(new TickTask(server.getTickCount() + 1, this::validateController));
        }
    }

    public boolean registerDepositClick(Player player, long gameTime) {
        boolean doubleClick = player.getUUID().equals(lastDepositPlayer) && gameTime - lastDepositTime < 10;
        lastDepositTime = doubleClick ? -100 : gameTime;
        lastDepositPlayer = player.getUUID();
        return doubleClick;
    }

    public boolean hasOverflow() {
        return !overflow.isEmpty();
    }

    public int getOverflowCount() {
        return overflow.size();
    }

    public int giveOverflowTo(Player player) {
        int given = 0;
        Iterator<ItemStack> it = overflow.iterator();
        while (it.hasNext()) {
            if (!player.getInventory().add(it.next())) break;
            it.remove();
            given++;
        }
        if (given > 0) setChanged();
        return given;
    }

    private void validateController() {
        if (isRemoved() || level == null || controllerPos == null || !level.isLoaded(controllerPos)) return;
        if (!(level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index)
                || !index.getLinkedCabinets().contains(worldPosition)) {
            clearControllerPos();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (controllerPos != null) {
            tag.putLong("controllerPos", controllerPos.asLong());
        }
        if (!overflow.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack extra : overflow) list.add(extra.save(registries));
            tag.put("overflow", list);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        overflow.clear();
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        inventory.normalize(slotCount);
        controllerPos = tag.contains("controllerPos") ?
                BlockPos.of(tag.getLong("controllerPos")) : null;
        if (tag.contains("overflow", Tag.TAG_LIST)) {
            ListTag list = tag.getList("overflow", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(overflow::add);
            }
        }
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

    private static class FilingCabinetItemHandler implements IItemHandler {
        private final FilingCabinetBlockEntity cabinet;

        FilingCabinetItemHandler(FilingCabinetBlockEntity cabinet) {
            this.cabinet = cabinet;
        }

        private boolean validSlot(int slot) {
            return slot >= 0 && slot < cabinet.slotCount;
        }

        @Override
        public int getSlots() {
            return cabinet.slotCount;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (!validSlot(slot)) return ItemStack.EMPTY;
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
            if (stack.isEmpty() || !validSlot(slot)) return stack;
            if (FilingFolderItem.hasSignificantNBT(stack)) return stack;
            ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
            if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return stack;
            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null) return stack;
            ResourceLocation incomingId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (contents.storedItemId().isEmpty() || !contents.storedItemId().get().equals(incomingId)) return stack;
            int toAdd = Math.min(stack.getCount(), folder.getCapacity() - contents.count());
            if (toAdd <= 0) return stack;
            if (!simulate) {
                ItemStack updated = folderStack.copyWithCount(1);
                updated.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
                cabinet.inventory.setStackInSlot(slot, updated);
            }
            return stack.copyWithCount(stack.getCount() - toAdd);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !validSlot(slot)) return ItemStack.EMPTY;
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
                ItemStack updated = folderStack.copyWithCount(1);
                updated.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - actualExtract));
                cabinet.inventory.setStackInSlot(slot, updated);
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (!validSlot(slot)) return 0;
            ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
            if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return 0;
            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            return contents != null ? folder.getCapacity() : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return !FilingFolderItem.hasSignificantNBT(stack);
        }
    }
}