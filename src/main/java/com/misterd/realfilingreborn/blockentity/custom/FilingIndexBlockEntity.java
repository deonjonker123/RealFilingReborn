package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.Config;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
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

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FilingIndexBlockEntity extends BlockEntity implements MenuProvider {

    private long lastDepositTime = -100;

    public record FolderRef(BlockPos cabinetPos, int slot, int count, int capacity) {}

    private final LinkedHashMap<Identifier, List<FolderRef>> itemIndex = new LinkedHashMap<>();
    private final List<Map.Entry<Identifier, FolderRef>> indexEntries = new ArrayList<>();
    public boolean itemIndexDirty = true;

    private final Set<BlockPos> pendingFlush = Collections.synchronizedSet(new LinkedHashSet<>());
    private boolean flushScheduled = false;

    private final Set<BlockPos> linkedCabinets = new LinkedHashSet<>();
    private final ReentrantReadWriteLock cabinetLock = new ReentrantReadWriteLock();

    public final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(1) {
        @Override
        public long getCapacityAsLong(int slot, ItemResource resource) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack previous) {
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

    private final Map<Direction, ResourceHandler<ItemResource>> handlers = new HashMap<>();
    private static final int MAX_HANDLER_CACHE_SIZE = 8;

    private long lastUpdateTime = 0L;
    private static final long MIN_UPDATE_INTERVAL_MS = 100L;
    private boolean updateScheduled = false;

    public FilingIndexBlockEntity(BlockPos pos, BlockState state) {
        super(RFRBlockEntities.FILING_INDEX_BE.get(), pos, state);
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
            for (BlockPos pos : toFlush) {
                patchCabinetInIndex(pos);
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
            for (BlockPos pos : linkedCabinets) {
                if (!isInRange(pos)) continue;

                BlockEntity be = level.getBlockEntity(pos);

                if (be instanceof FilingCabinetBlockEntity cabinet) {
                    if (!cabinet.isLinkedToController()) continue;
                    readCabinetIntoIndex(cabinet, pos);
                    cabinet.sendUpdatePacket();
                } else if (be instanceof SingleFilingCabinetBlockEntity cabinet) {
                    if (!cabinet.isLinkedToController()) continue;
                    readSingleCabinetIntoIndex(cabinet, pos);
                    cabinet.sendUpdatePacket();
                } else if (be instanceof DoubleFilingCabinetBlockEntity cabinet) {
                    if (!cabinet.isLinkedToController()) continue;
                    readDoubleCabinetIntoIndex(cabinet, pos);
                    cabinet.sendUpdatePacket();
                }
            }
        } finally {
            cabinetLock.readLock().unlock();
        }

        pendingFlush.clear();
        itemIndexDirty = false;
        rebuildIndexEntries();
    }

    private void patchCabinetInIndex(BlockPos cabinetPos) {
        itemIndex.forEach((id, refs) -> refs.removeIf(r -> r.cabinetPos().equals(cabinetPos)));
        itemIndex.entrySet().removeIf(e -> e.getValue().isEmpty());

        BlockEntity be = level.getBlockEntity(cabinetPos);

        if (be instanceof FilingCabinetBlockEntity cabinet) {
            if (!cabinet.isLinkedToController()) return;
            readCabinetIntoIndex(cabinet, cabinetPos);
            cabinet.sendUpdatePacket();
        } else if (be instanceof SingleFilingCabinetBlockEntity cabinet) {
            if (!cabinet.isLinkedToController()) return;
            readSingleCabinetIntoIndex(cabinet, cabinetPos);
            cabinet.sendUpdatePacket();
        } else if (be instanceof DoubleFilingCabinetBlockEntity cabinet) {
            if (!cabinet.isLinkedToController()) return;
            readDoubleCabinetIntoIndex(cabinet, cabinetPos);
            cabinet.sendUpdatePacket();
        }
    }

    private void readCabinetIntoIndex(FilingCabinetBlockEntity cabinet, BlockPos pos) {
        for (int i = 0; i < 4; i++) {
            readFolderIntoIndex(cabinet.getStack(i), pos, i);
        }
    }

    private void readSingleCabinetIntoIndex(SingleFilingCabinetBlockEntity cabinet, BlockPos pos) {
        readFolderIntoIndex(cabinet.getStack(0), pos, 0);
    }

    private void readDoubleCabinetIntoIndex(DoubleFilingCabinetBlockEntity cabinet, BlockPos pos) {
        for (int i = 0; i < 2; i++) {
            readFolderIntoIndex(cabinet.getStack(i), pos, i);
        }
    }

    private void rebuildIndexEntries() {
        indexEntries.clear();
        for (Map.Entry<Identifier, List<FolderRef>> e : itemIndex.entrySet()) {
            int totalCount = e.getValue().stream().mapToInt(FolderRef::count).sum();
            int totalCapacity = e.getValue().stream().mapToInt(FolderRef::capacity).sum();
            FolderRef synthetic = new FolderRef(e.getValue().get(0).cabinetPos(),
                    e.getValue().get(0).slot(),
                    totalCount, totalCapacity);
            indexEntries.add(Map.entry(e.getKey(), synthetic));
        }
    }

    private void readFolderIntoIndex(ItemStack folder, BlockPos pos, int slot) {
        if (!(folder.getItem() instanceof FilingFolderItem ff)) return;
        FilingFolderItem.FolderContents contents = folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return;
        Identifier id = contents.storedItemId().get();
        itemIndex.computeIfAbsent(id, k -> new ArrayList<>())
                .add(new FolderRef(pos, slot, contents.count(), ff.getCapacity()));
    }

    public List<Map.Entry<Identifier, FolderRef>> getIndexEntries() {
        if (itemIndexDirty) rebuildItemIndex();
        return indexEntries;
    }

    public int getIndexSize() {
        if (itemIndexDirty) rebuildItemIndex();
        return indexEntries.size();
    }

    @Nullable
    public List<FolderRef> getFolderRefs(Identifier id) {
        if (itemIndexDirty) rebuildItemIndex();
        return itemIndex.get(id);
    }

    @Nullable
    public FolderRef getFolderRef(Identifier id) {
        List<FolderRef> refs = getFolderRefs(id);
        if (refs == null || refs.isEmpty()) return null;
        for (FolderRef ref : refs) {
            if (ref.count() < ref.capacity()) return ref;
        }
        return refs.get(0);
    }

    public boolean isInRange(BlockPos pos) {
        int range = getRange();
        long now = System.currentTimeMillis();

        if (range != lastKnownRange || now - lastRangeCacheTime > RANGE_CACHE_DURATION_MS) {
            clearRangeCache();
            lastKnownRange = range;
            lastRangeCacheTime = now;
        }

        return rangeCache.computeIfAbsent(pos,
                p -> getBlockPos().distSqr(p) <= (long) range * range);
    }

    private void clearRangeCache() {
        rangeCache.clear();
        lastRangeCacheTime = 0L;
    }

    public int getRange() {
        ItemStack upgrade = inventory.getResource(0).toStack(inventory.getAmountAsInt(0));

        if (upgrade.getItem() instanceof NetheriteRangeUpgradeItem)
            return Config.getNetheriteRangeUpgrade();
        if (upgrade.getItem() instanceof DiamondRangeUpgradeItem)
            return Config.getDiamondRangeUpgrade();
        if (upgrade.getItem() instanceof IronRangeUpgradeItem)
            return Config.getIronRangeUpgrade();

        return Config.getFilingIndexBaseRange();
    }

    public ItemStack getUpgradeStack() {
        ItemResource res = inventory.getResource(0);
        if (res.isEmpty()) return ItemStack.EMPTY;
        return res.toStack(inventory.getAmountAsInt(0));
    }

    public void addCabinet(BlockPos pos) {
        cabinetLock.writeLock().lock();
        try {
            boolean wasEmpty = linkedCabinets.isEmpty();

            if (linkedCabinets.add(pos)) {
                clearRangeCache();
                itemIndexDirty = true;
                setChanged();

                if (level != null && !level.isClientSide()) {
                    scheduleFlush();
                    scheduleBlockUpdate();
                    if (wasEmpty) scheduleConnectedStateUpdate();
                }
            }
        } finally {
            cabinetLock.writeLock().unlock();
        }
    }

    public void removeCabinet(BlockPos pos) {
        cabinetLock.writeLock().lock();
        try {
            if (linkedCabinets.remove(pos)) {
                clearRangeCache();
                itemIndexDirty = true;
                setChanged();

                if (level != null && !level.isClientSide()) {
                    scheduleFlush();
                    scheduleBlockUpdate();
                    if (linkedCabinets.isEmpty()) scheduleConnectedStateUpdate();
                }
            }
        } finally {
            cabinetLock.writeLock().unlock();
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
    public ResourceHandler<ItemResource> getCapabilityHandler(@Nullable Direction side) {
        if (handlers.size() > MAX_HANDLER_CACHE_SIZE) handlers.clear();

        return handlers.computeIfAbsent(
                side != null ? side : Direction.UP,
                s -> new FilingIndexItemHandler(this)
        );
    }

    private void refreshHandlerSnapshots() {
        for (ResourceHandler<ItemResource> h : handlers.values()) {
            if (h instanceof FilingIndexItemHandler fh) {
                fh.refreshSnapshot();
            }
        }
    }

    private void invalidateHandlerRangeCaches() {
        handlers.values().forEach(h -> {
            if (h instanceof FilingIndexItemHandler fh) {
                fh.refreshSnapshot();
            }
        });
    }

    private void scheduleBlockUpdate() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime > MIN_UPDATE_INTERVAL_MS) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            lastUpdateTime = now;
        }
    }

    private void scheduleConnectedStateUpdate() {
        if (!updateScheduled && level != null && !level.isClientSide()) {
            updateScheduled = true;
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
        }
    }

    private void updateConnectedStateImmediate() {
        if (level == null || level.isClientSide()) return;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof FilingIndexBlock)) return;

        cabinetLock.readLock().lock();
        try {
            boolean connected = !linkedCabinets.isEmpty();

            if (connected != state.getValue(FilingIndexBlock.CONNECTED)) {
                level.setBlock(
                        getBlockPos(),
                        state.setValue(FilingIndexBlock.CONNECTED, connected),
                        3
                );
            }
        } finally {
            cabinetLock.readLock().unlock();
        }
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
            ItemResource res = inventory.getResource(i);
            if (!res.isEmpty()) {
                inv.setItem(i, res.toStack(inventory.getAmountAsInt(i)));
            }
        }

        Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        inventory.serialize(output);

        cabinetLock.readLock().lock();
        try {
            output.putInt("linkedCabinetCount", linkedCabinets.size());
            int i = 0;
            for (BlockPos pos : linkedCabinets) {
                output.putLong("linkedCabinet_" + i++, pos.asLong());
            }
        } finally {
            cabinetLock.readLock().unlock();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        inventory.deserialize(input);

        cabinetLock.writeLock().lock();
        try {
            linkedCabinets.clear();
            clearRangeCache();

            int count = input.getIntOr("linkedCabinetCount", 0);
            for (int i = 0; i < count; i++) {
                input.getLong("linkedCabinet_" + i)
                        .ifPresent(l -> linkedCabinets.add(BlockPos.of(l)));
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
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FilingIndexMenu(id, inv, this);
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