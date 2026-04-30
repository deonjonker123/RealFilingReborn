package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.Config;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.capability.FilingIndexFluidHandler;
import com.misterd.realfilingreborn.capability.FilingIndexItemHandler;
import com.misterd.realfilingreborn.gui.custom.FilingIndexMenu;
import com.misterd.realfilingreborn.item.custom.DiamondRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.item.custom.IronRangeUpgradeItem;
import com.misterd.realfilingreborn.item.custom.NetheriteRangeUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FilingIndexBlockEntity extends BlockEntity implements MenuProvider {

    public record FolderRef(BlockPos cabinetPos, int slot, int count, int capacity) {}
    private final LinkedHashMap<ResourceLocation, FolderRef> itemIndex = new LinkedHashMap<>();
    private final List<Map.Entry<ResourceLocation, FolderRef>> indexEntries = new ArrayList<>();
    private boolean itemIndexDirty = true;
    private final Set<BlockPos> pendingFlush = Collections.synchronizedSet(new LinkedHashSet<>());
    private boolean flushScheduled = false;
    private final Set<BlockPos> linkedCabinets = new LinkedHashSet<>();
    private final ReentrantReadWriteLock cabinetLock = new ReentrantReadWriteLock();

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            clearRangeCache();
            setChanged();
            itemIndexDirty = true;
            invalidateHandlerRangeCaches();
            if (level != null && !level.isClientSide()) {
                scheduleBlockUpdate();
            }
        }
    };

    private final Map<BlockPos, Boolean> rangeCache = new ConcurrentHashMap<>();
    private long lastRangeCacheTime = 0L;
    private int lastKnownRange = -1;
    private static final long RANGE_CACHE_DURATION_MS = 2000L;
    private static final int MAX_HANDLER_CACHE_SIZE = 8;
    private final Map<Direction, IItemHandler> handlers = new HashMap<>();
    private final Map<Direction, IFluidHandler> fluidHandlers = new HashMap<>();
    private long lastUpdateTime = 0L;
    private static final long MIN_UPDATE_INTERVAL_MS = 100L;
    private boolean updateScheduled = false;

    public FilingIndexBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.FILING_INDEX_BE.get(), pos, blockState);
    }

    public void scheduleFlush(BlockPos cabinetPos) {
        pendingFlush.add(cabinetPos);
        if (!flushScheduled && level != null && !level.isClientSide()) {
            flushScheduled = true;
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
        }
    }

    public void scheduleFlush() {
        if (!flushScheduled && level != null && !level.isClientSide()) {
            flushScheduled = true;
            itemIndexDirty = true;
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
        }
    }

    public void performScheduledUpdate() {
        flushScheduled = false;

        if (itemIndexDirty) {
            rebuildItemIndex();
        } else if (!pendingFlush.isEmpty()) {
            Set<BlockPos> toFlush;
            synchronized (pendingFlush) {
                toFlush = new LinkedHashSet<>(pendingFlush);
                pendingFlush.clear();
            }
            for (BlockPos cabinetPos : toFlush) {
                patchCabinetInIndex(cabinetPos);
            }
            rebuildIndexEntries();
        }

        refreshHandlerSnapshots();

        if (updateScheduled) {
            updateScheduled = false;
            updateConnectedStateImmediate();
        }

        scheduleBlockUpdate();
    }

    private void rebuildItemIndex() {
        itemIndex.clear();
        cabinetLock.readLock().lock();
        try {
            for (BlockPos cabinetPos : linkedCabinets) {
                if (!isInRange(cabinetPos)) continue;
                if (!(level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet)) continue;
                if (!cabinet.isLinkedToController()) continue;
                readCabinetIntoIndex(cabinet, cabinetPos);
                cabinet.sendUpdatePacket();
            }
        } finally {
            cabinetLock.readLock().unlock();
        }
        pendingFlush.clear();
        itemIndexDirty = false;
        rebuildIndexEntries();
    }

    private void patchCabinetInIndex(BlockPos cabinetPos) {
        if (!(level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet)) return;
        if (!cabinet.isLinkedToController()) return;
        itemIndex.entrySet().removeIf(e -> e.getValue().cabinetPos().equals(cabinetPos));
        readCabinetIntoIndex(cabinet, cabinetPos);
        cabinet.sendUpdatePacket();
    }

    private void readCabinetIntoIndex(FilingCabinetBlockEntity cabinet, BlockPos cabinetPos) {
        for (int i = 0; i < 5; i++) {
            ItemStack folder = cabinet.inventory.getStackInSlot(i);
            if (!(folder.getItem() instanceof FilingFolderItem ff)) continue;
            FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) continue;
            ResourceLocation itemId = contents.storedItemId().get();
            itemIndex.putIfAbsent(itemId, new FolderRef(cabinetPos, i, contents.count(), ff.getCapacity()));
        }
    }

    private void rebuildIndexEntries() {
        indexEntries.clear();
        indexEntries.addAll(itemIndex.entrySet());
    }

    public List<Map.Entry<ResourceLocation, FolderRef>> getIndexEntries() {
        if (itemIndexDirty) rebuildItemIndex();
        return indexEntries;
    }

    public int getIndexSize() {
        if (itemIndexDirty) rebuildItemIndex();
        return indexEntries.size();
    }

    @Nullable
    public FolderRef getFolderRef(ResourceLocation itemId) {
        if (itemIndexDirty) rebuildItemIndex();
        return itemIndex.get(itemId);
    }

    public boolean isInRange(BlockPos cabinetPos) {
        int currentRange = getRange();
        long currentTime = System.currentTimeMillis();
        if (currentRange != lastKnownRange || currentTime - lastRangeCacheTime > RANGE_CACHE_DURATION_MS) {
            clearRangeCache();
            lastKnownRange = currentRange;
            lastRangeCacheTime = currentTime;
        }
        return rangeCache.computeIfAbsent(cabinetPos, pos -> {
            double rangeSq = (double) currentRange * currentRange;
            return getBlockPos().distSqr(pos) <= rangeSq;
        });
    }

    private void clearRangeCache() {
        rangeCache.clear();
        lastRangeCacheTime = 0L;
    }

    public int getRange() {
        ItemStack upgrade = inventory.getStackInSlot(0);
        if (upgrade.getItem() instanceof NetheriteRangeUpgradeItem) return Config.getNetheriteRangeUpgrade();
        if (upgrade.getItem() instanceof DiamondRangeUpgradeItem) return Config.getDiamondRangeUpgrade();
        if (upgrade.getItem() instanceof IronRangeUpgradeItem) return Config.getIronRangeUpgrade();
        return Config.getFilingIndexBaseRange();
    }

    public void addCabinet(BlockPos cabinetPos) {
        cabinetLock.writeLock().lock();
        try {
            boolean wasEmpty = linkedCabinets.isEmpty();
            if (linkedCabinets.add(cabinetPos)) {
                clearRangeCache();
                itemIndexDirty = true;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    scheduleBlockUpdate();
                    if (wasEmpty) scheduleConnectedStateUpdate();
                }
            }
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    public void removeCabinet(BlockPos cabinetPos) {
        cabinetLock.writeLock().lock();
        try {
            if (linkedCabinets.remove(cabinetPos)) {
                clearRangeCache();
                itemIndexDirty = true;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    scheduleBlockUpdate();
                    if (linkedCabinets.isEmpty()) scheduleConnectedStateUpdate();
                }
            }
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    public void addCabinets(Set<BlockPos> cabinets) {
        if (cabinets.isEmpty()) return;
        cabinetLock.writeLock().lock();
        try {
            boolean wasEmpty = linkedCabinets.isEmpty();
            boolean changed = false;
            for (BlockPos cabinet : cabinets) {
                if (linkedCabinets.add(cabinet)) changed = true;
            }
            if (changed) {
                clearRangeCache();
                itemIndexDirty = true;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    scheduleBlockUpdate();
                    if (wasEmpty && !linkedCabinets.isEmpty()) scheduleConnectedStateUpdate();
                }
            }
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    public void removeCabinets(Set<BlockPos> cabinets) {
        if (cabinets.isEmpty()) return;
        cabinetLock.writeLock().lock();
        try {
            boolean hadCabinets = !linkedCabinets.isEmpty();
            if (linkedCabinets.removeAll(cabinets)) {
                clearRangeCache();
                itemIndexDirty = true;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    scheduleBlockUpdate();
                    if (hadCabinets && linkedCabinets.isEmpty()) scheduleConnectedStateUpdate();
                }
            }
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    public boolean removeCabinetAt(BlockPos cabinetPos) {
        cabinetLock.writeLock().lock();
        try {
            if (!linkedCabinets.remove(cabinetPos)) return false;
            clearRangeCache();
            itemIndexDirty = true;
            if (level != null && !level.isClientSide()) {
                clearControllerPos(cabinetPos);
                scheduleBlockUpdate();
                if (linkedCabinets.isEmpty()) scheduleConnectedStateUpdate();
            }
            setChanged();
            return true;
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    public void clearAllLinkedCabinets() {
        cabinetLock.writeLock().lock();
        try {
            if (level != null && !level.isClientSide()) {
                linkedCabinets.forEach(this::clearControllerPos);
            }
            boolean hadCabinets = !linkedCabinets.isEmpty();
            linkedCabinets.clear();
            clearRangeCache();
            itemIndexDirty = true;
            setChanged();
            if (hadCabinets && level != null && !level.isClientSide()) {
                scheduleConnectedStateUpdate();
            }
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    private void clearControllerPos(BlockPos cabinetPos) {
        BlockEntity be = level.getBlockEntity(cabinetPos);
        if (be instanceof FilingCabinetBlockEntity cabinet) {
            cabinet.clearControllerPos();
        } else if (be instanceof FluidCabinetBlockEntity fluidCabinet) {
            fluidCabinet.clearControllerPos();
        }
    }

    public Set<BlockPos> getLinkedCabinets() {
        cabinetLock.readLock().lock();
        try {
            return new LinkedHashSet<>(linkedCabinets);
        } finally {
            cabinetLock.readLock().unlock();
        }
    }

    public int getLinkedCabinetCount() {
        cabinetLock.readLock().lock();
        try {
            return linkedCabinets.size();
        } finally {
            cabinetLock.readLock().unlock();
        }
    }

    @Nullable
    public IItemHandler getCapabilityHandler(@Nullable Direction side) {
        if (handlers.size() > MAX_HANDLER_CACHE_SIZE) handlers.clear();
        return handlers.computeIfAbsent(side != null ? side : Direction.UP, s -> new FilingIndexItemHandler(this));
    }

    @Nullable
    public IFluidHandler getFluidCapabilityHandler(@Nullable Direction side) {
        if (fluidHandlers.size() > MAX_HANDLER_CACHE_SIZE) fluidHandlers.clear();
        return fluidHandlers.computeIfAbsent(side != null ? side : Direction.UP, s -> new FilingIndexFluidHandler(this));
    }

    private void refreshHandlerSnapshots() {
        for (IItemHandler h : handlers.values()) {
            if (h instanceof FilingIndexItemHandler fh) fh.refreshSnapshot();
        }
        for (IFluidHandler h : fluidHandlers.values()) {
            if (h instanceof FilingIndexFluidHandler fh) fh.refreshSnapshot();
        }
    }

    private void invalidateHandlerRangeCaches() {
        for (IFluidHandler h : fluidHandlers.values()) {
            if (h instanceof FilingIndexFluidHandler fh) fh.invalidateRangeCache();
        }
    }

    private void scheduleBlockUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > MIN_UPDATE_INTERVAL_MS) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            lastUpdateTime = currentTime;
        }
    }

    private void scheduleConnectedStateUpdate() {
        if (!updateScheduled && level != null && !level.isClientSide()) {
            updateScheduled = true;
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
        }
    }

    public void updateConnectedState() {
        scheduleConnectedStateUpdate();
    }

    private void updateConnectedStateImmediate() {
        if (level == null || level.isClientSide()) return;
        BlockState currentState = getBlockState();
        if (!(currentState.getBlock() instanceof FilingIndexBlock)) return;

        cabinetLock.readLock().lock();
        try {
            boolean hasConnections = !linkedCabinets.isEmpty();
            if (hasConnections != currentState.getValue(FilingIndexBlock.CONNECTED)) {
                level.setBlock(getBlockPos(), currentState.setValue(FilingIndexBlock.CONNECTED, hasConnections), 3);
            }
        } finally {
            cabinetLock.readLock().unlock();
        }
    }

    public void drops() {
        clearAllLinkedCabinets();
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
        cabinetLock.readLock().lock();
        try {
            ListTag cabinetList = new ListTag();
            for (BlockPos cabinetPos : linkedCabinets) {
                cabinetList.add(LongTag.valueOf(cabinetPos.asLong()));
            }
            tag.put("linkedCabinets", cabinetList);
        } finally {
            cabinetLock.readLock().unlock();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        cabinetLock.writeLock().lock();
        try {
            linkedCabinets.clear();
            clearRangeCache();
            if (tag.contains("linkedCabinets")) {
                ListTag cabinetList = tag.getList("linkedCabinets", 4);
                for (int i = 0; i < cabinetList.size(); i++) {
                    linkedCabinets.add(BlockPos.of(((LongTag) cabinetList.get(i)).getAsLong()));
                }
            }
            itemIndexDirty = true;
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("blockentity.realfilingreborn.filing_index_name");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new FilingIndexMenu(id, playerInventory, this);
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