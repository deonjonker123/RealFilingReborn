package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.Config;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.capability.FilingIndexFluidHandler;
import com.misterd.realfilingreborn.capability.FilingIndexItemHandler;
import com.misterd.realfilingreborn.gui.custom.FilingIndexMenu;
import com.misterd.realfilingreborn.item.custom.DiamondRangeUpgradeItem;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FilingIndexBlockEntity extends BlockEntity implements MenuProvider {

    private final Set<BlockPos> linkedCabinets = ConcurrentHashMap.newKeySet();
    private final ReentrantReadWriteLock cabinetLock = new ReentrantReadWriteLock();
    private final Map<Direction, IItemHandler> handlers = new ConcurrentHashMap<>();
    private final Map<Direction, IFluidHandler> fluidHandlers = new ConcurrentHashMap<>();
    private static final int MAX_HANDLER_CACHE_SIZE = 16;

    private final Map<BlockPos, Boolean> rangeCache = new ConcurrentHashMap<>();
    private volatile int lastKnownRange = -1;
    private volatile long lastRangeCacheTime = 0L;
    private static final long RANGE_CACHE_DURATION_MS = 5000L;

    private volatile boolean updateScheduled = false;
    private volatile long lastUpdateTime = 0L;
    private static final long MIN_UPDATE_INTERVAL_MS = 100L;

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            clearRangeCache();
            setChanged();
            if (level != null && !level.isClientSide()) {
                scheduleBlockUpdate();
            }
        }
    };

    public FilingIndexBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.FILING_INDEX_BE.get(), pos, blockState);
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

    public void drops() {
        clearAllLinkedCabinets();
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    public void addCabinet(BlockPos cabinetPos) {
        cabinetLock.writeLock().lock();
        try {
            boolean wasEmpty = linkedCabinets.isEmpty();
            if (linkedCabinets.add(cabinetPos)) {
                clearRangeCache();
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

    public void performScheduledUpdate() {
        if (updateScheduled) {
            updateScheduled = false;
            updateConnectedStateImmediate();
        }
    }

    public Set<BlockPos> getLinkedCabinets() {
        cabinetLock.readLock().lock();
        try {
            return new HashSet<>(linkedCabinets);
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

    public boolean removeCabinetAt(BlockPos cabinetPos) {
        cabinetLock.writeLock().lock();
        try {
            if (!linkedCabinets.remove(cabinetPos)) return false;

            clearRangeCache();
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

    public int getRange() {
        ItemStack upgrade = inventory.getStackInSlot(0);
        if (upgrade.getItem() instanceof NetheriteRangeUpgradeItem) return Config.getNetheriteRangeUpgrade();
        if (upgrade.getItem() instanceof DiamondRangeUpgradeItem)  return Config.getDiamondRangeUpgrade();
        if (upgrade.getItem() instanceof IronRangeUpgradeItem)     return Config.getIronRangeUpgrade();
        return Config.getFilingIndexBaseRange();
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
