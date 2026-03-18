package com.misterd.realfilingreborn.capability;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FilingIndexItemHandler implements IItemHandler {

    private final FilingIndexBlockEntity indexEntity;
    private final Level level;

    private volatile List<VirtualSlotInfo> cachedVirtualSlots = null;
    private final AtomicLong lastCacheTime = new AtomicLong(0L);
    private final AtomicLong cacheVersion = new AtomicLong(0L);
    private static final long CACHE_DURATION_MS = 500L;
    private static final int MAX_VIRTUAL_SLOTS_PER_SCAN = 1000;

    private final Map<BlockPos, Boolean> inRangeCache = new ConcurrentHashMap<>();
    private volatile long lastRangeCacheTime = 0L;
    private static final long RANGE_CACHE_DURATION_MS = 2000L;

    public FilingIndexItemHandler(FilingIndexBlockEntity indexEntity) {
        this.indexEntity = indexEntity;
        this.level = indexEntity.getLevel();
    }

    private void notifyUpdate(BlockPos cabinetPos) {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(cabinetPos, level.getBlockState(cabinetPos), level.getBlockState(cabinetPos), 2);
            invalidateCache();
        }
    }

    public void invalidateCache() {
        cachedVirtualSlots = null;
        cacheVersion.incrementAndGet();
        lastCacheTime.set(0L);
    }

    private void invalidateRangeCache() {
        inRangeCache.clear();
        lastRangeCacheTime = 0L;
    }

    private List<VirtualSlotInfo> getAllVirtualSlots() {
        long currentTime = System.currentTimeMillis();
        long currentVersion = cacheVersion.get();
        List<VirtualSlotInfo> cached = cachedVirtualSlots;
        if (cached != null && currentTime - lastCacheTime.get() < CACHE_DURATION_MS) {
            return cached;
        }

        List<VirtualSlotInfo> virtualSlots = new ArrayList<>();
        int slotCount = 0;

        for (BlockPos cabinetPos : new ArrayList<>(indexEntity.getLinkedCabinets())) {
            if (slotCount >= MAX_VIRTUAL_SLOTS_PER_SCAN) break;
            if (!isInRangeCached(cabinetPos)) continue;

            if (!(level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet)) continue;
            if (!cabinet.isLinkedToController()) continue;

            for (int slot = 0; slot < 5 && slotCount < MAX_VIRTUAL_SLOTS_PER_SCAN; slot++) {
                ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
                if (!(folderStack.getItem() instanceof FilingFolderItem)) continue;

                FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
                if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) continue;

                Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
                virtualSlots.add(new VirtualSlotInfo(cabinetPos, slot, new ItemStack(item, contents.count())));
                slotCount++;
            }
        }

        if (currentVersion == cacheVersion.get()) {
            cachedVirtualSlots = virtualSlots;
            lastCacheTime.set(currentTime);
        }

        return virtualSlots;
    }

    private boolean isInRangeCached(BlockPos cabinetPos) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRangeCacheTime > RANGE_CACHE_DURATION_MS) {
            invalidateRangeCache();
            lastRangeCacheTime = currentTime;
        }
        return inRangeCache.computeIfAbsent(cabinetPos, pos -> {
            try {
                return indexEntity.isInRange(pos);
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public int getSlots() {
        return Math.max(indexEntity.getLinkedCabinetCount() * 5, 1);
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        List<BlockPos> cabinets = new ArrayList<>(indexEntity.getLinkedCabinets());
        int cabinetIndex = slot / 5;
        int cabinetSlot = slot % 5;
        if (cabinetIndex >= cabinets.size()) return ItemStack.EMPTY;

        BlockPos cabinetPos = cabinets.get(cabinetIndex);
        if (!isInRangeCached(cabinetPos)) return ItemStack.EMPTY;
        if (!(level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet)) return ItemStack.EMPTY;
        if (!cabinet.isLinkedToController()) return ItemStack.EMPTY;

        ItemStack folderStack = cabinet.inventory.getStackInSlot(cabinetSlot);
        if (!(folderStack.getItem() instanceof FilingFolderItem)) return ItemStack.EMPTY;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
        return new ItemStack(item, contents.count());
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        if (FilingFolderItem.hasSignificantNBT(stack)) return stack;

        List<BlockPos> cabinets = new ArrayList<>(indexEntity.getLinkedCabinets());
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        // Try the targeted slot first
        int cabinetIndex = slot / 5;
        int cabinetSlot = slot % 5;
        if (cabinetIndex < cabinets.size()) {
            BlockPos cabinetPos = cabinets.get(cabinetIndex);
            if (isInRangeCached(cabinetPos) &&
                    level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet &&
                    cabinet.isLinkedToController()) {
                ItemStack result = tryInsertIntoFolder(cabinet, cabinetSlot, stack, itemId, simulate, cabinetPos);
                if (result.getCount() < stack.getCount()) return result;
            }
        }

        // Fall back: search all cabinets for a matching folder
        for (BlockPos cabinetPos : cabinets) {
            if (!isInRangeCached(cabinetPos)) continue;
            if (!(level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet)) continue;
            if (!cabinet.isLinkedToController()) continue;

            for (int i = 0; i < 5; i++) {
                ItemStack result = tryInsertIntoFolder(cabinet, i, stack, itemId, simulate, cabinetPos);
                if (result.getCount() < stack.getCount()) return result;
            }
        }

        return stack;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        List<BlockPos> cabinets = new ArrayList<>(indexEntity.getLinkedCabinets());
        int cabinetIndex = slot / 5;
        int cabinetSlot = slot % 5;
        if (cabinetIndex >= cabinets.size()) return ItemStack.EMPTY;

        BlockPos cabinetPos = cabinets.get(cabinetIndex);
        if (!isInRangeCached(cabinetPos)) return ItemStack.EMPTY;
        if (!(level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet)) return ItemStack.EMPTY;
        if (!cabinet.isLinkedToController()) return ItemStack.EMPTY;

        return extractFromCabinet(cabinet, cabinetSlot, amount, simulate, cabinetPos);
    }

    private ItemStack tryInsertIntoFolder(FilingCabinetBlockEntity cabinet, int cabinetSlot, ItemStack stack, ResourceLocation itemId, boolean simulate, BlockPos cabinetPos) {
        try {
            ItemStack folderStack = cabinet.inventory.getStackInSlot(cabinetSlot);
            if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return stack;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null) return stack;
            if (contents.storedItemId().isEmpty() || !contents.storedItemId().get().equals(itemId)) return stack;
            if (contents.count() > Integer.MAX_VALUE - stack.getCount()) return stack;

            int capacity = folder.getCapacity();
            int toAdd = Math.min(stack.getCount(), capacity - contents.count());
            if (toAdd <= 0) return stack;

            if (!simulate) {
                folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
                cabinet.setChanged();
                notifyUpdate(cabinetPos);
            }

            ItemStack remaining = stack.copy();
            remaining.shrink(toAdd);
            return remaining;
        } catch (Exception e) {
            return stack;
        }
    }

    private ItemStack extractFromCabinet(FilingCabinetBlockEntity cabinet, int cabinetSlot, int amount, boolean simulate, BlockPos cabinetPos) {
        try {
            ItemStack folderStack = cabinet.inventory.getStackInSlot(cabinetSlot);
            if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) return ItemStack.EMPTY;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemStack.EMPTY;

            Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
            int extractAmount = Math.min(amount, contents.count());
            if (extractAmount <= 0) return ItemStack.EMPTY;

            if (!simulate) {
                folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - extractAmount));
                cabinet.setChanged();
                notifyUpdate(cabinetPos);
            }

            return new ItemStack(item, extractAmount);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        List<BlockPos> cabinets = new ArrayList<>(indexEntity.getLinkedCabinets());
        int cabinetIndex = slot / 5;
        int cabinetSlot = slot % 5;
        if (cabinetIndex >= cabinets.size()) return FilingFolderItem.FolderTier.BASE.getCapacity();

        BlockPos cabinetPos = cabinets.get(cabinetIndex);
        if (!(level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet))
            return FilingFolderItem.FolderTier.BASE.getCapacity();

        ItemStack folderStack = cabinet.inventory.getStackInSlot(cabinetSlot);
        if (!(folderStack.getItem() instanceof FilingFolderItem folder))
            return FilingFolderItem.FolderTier.BASE.getCapacity();

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        return contents != null ? Math.max(contents.count(), folder.getCapacity()) : folder.getCapacity();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !FilingFolderItem.hasSignificantNBT(stack);
    }

    private record VirtualSlotInfo(BlockPos cabinetPos, int slotIndex, ItemStack virtualStack) {}
}