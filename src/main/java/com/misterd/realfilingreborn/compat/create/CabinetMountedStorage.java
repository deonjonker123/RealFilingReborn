package com.misterd.realfilingreborn.compat.create;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CabinetMountedStorage extends MountedItemStorage implements SyncedMountedStorage {
    public static final MapCodec<CabinetMountedStorage> CODEC = ItemStack.OPTIONAL_CODEC.listOf()
            .xmap(CabinetMountedStorage::new, storage -> storage.folders)
            .fieldOf("folders");

    private final List<ItemStack> folders;
    private boolean dirty;

    private CabinetMountedStorage(List<ItemStack> folders) {
        super(CreateCompat.CABINET.get());
        this.folders = new ArrayList<>(folders.size());
        for (ItemStack folder : folders) this.folders.add(folder.copy());
    }

    public static CabinetMountedStorage fromCabinet(FilingCabinetBlockEntity cabinet) {
        List<ItemStack> folders = new ArrayList<>(cabinet.inventory.getSlots());
        for (int i = 0; i < cabinet.inventory.getSlots(); i++) {
            folders.add(cabinet.inventory.getStackInSlot(i));
        }
        return new CabinetMountedStorage(folders);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof FilingCabinetBlockEntity cabinet)) return;
        copyInto(cabinet);
    }

    private void copyInto(FilingCabinetBlockEntity cabinet) {
        int slots = Math.min(cabinet.inventory.getSlots(), folders.size());
        for (int i = 0; i < slots; i++) {
            cabinet.inventory.setStackInSlot(i, folders.get(i).copy());
        }
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureBlockInfo info) {
        return false;
    }

    @Nullable
    private FilingFolderItem.FolderContents contents(int slot) {
        if (slot < 0 || slot >= folders.size()) return null;
        ItemStack folder = folders.get(slot);
        if (!(folder.getItem() instanceof FilingFolderItem)) return null;
        return folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
    }

    private int capacity(int slot) {
        return FilingFolderItem.getCapacity(folders.get(slot));
    }

    private void setCount(int slot, ResourceLocation itemId, int count) {
        ItemStack updated = folders.get(slot).copyWithCount(1);
        updated.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(Optional.of(itemId), count));
        folders.set(slot, updated);
        dirty = true;
    }

    @Override
    public int getSlots() {
        return folders.size();
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        FilingFolderItem.FolderContents contents = contents(slot);
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
        return new ItemStack(item, contents.count());
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        FilingFolderItem.FolderContents contents = contents(slot);
        if (contents == null) return;
        if (stack.isEmpty()) {
            if (contents.storedItemId().isPresent()) setCount(slot, contents.storedItemId().get(), 0);
            return;
        }
        if (FilingFolderItem.hasSignificantNBT(stack)) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (contents.storedItemId().isPresent() && !contents.storedItemId().get().equals(id)) return;
        setCount(slot, id, Math.min(stack.getCount(), capacity(slot)));
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || FilingFolderItem.hasSignificantNBT(stack)) return stack;
        FilingFolderItem.FolderContents contents = contents(slot);
        if (contents == null || contents.storedItemId().isEmpty()) return stack;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!contents.storedItemId().get().equals(id)) return stack;

        int toAdd = Math.min(stack.getCount(), capacity(slot) - contents.count());
        if (toAdd <= 0) return stack;
        if (!simulate) setCount(slot, id, contents.count() + toAdd);
        return stack.copyWithCount(stack.getCount() - toAdd);
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        FilingFolderItem.FolderContents contents = contents(slot);
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
        int toExtract = Math.min(Math.min(amount, contents.count()), item.getDefaultMaxStackSize());
        if (!simulate) setCount(slot, contents.storedItemId().get(), contents.count() - toExtract);
        return new ItemStack(item, toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        FilingFolderItem.FolderContents contents = contents(slot);
        return contents == null ? 0 : capacity(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        FilingFolderItem.FolderContents contents = contents(slot);
        if (contents == null || contents.storedItemId().isEmpty() || FilingFolderItem.hasSignificantNBT(stack)) return false;
        return contents.storedItemId().get().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        dirty = false;
    }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
        if (contraption.getBlockEntityClientSide(localPos) instanceof FilingCabinetBlockEntity cabinet) {
            copyInto(cabinet);
        }
    }
}
