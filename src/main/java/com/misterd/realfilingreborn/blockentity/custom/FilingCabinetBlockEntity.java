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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FilingCabinetBlockEntity extends BlockEntity implements MenuProvider, LinkableCabinet {
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

    private final Map<Direction, ResourceHandler<ItemResource>> handlers = new HashMap<>();

    public FilingCabinetBlockEntity(BlockPos pos, BlockState blockState) {
        this(typeFor(blockState), pos, blockState);
    }

    public FilingCabinetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.slotCount = slotCountOf(blockState);
        this.dirtySlots = new boolean[slotCount];
        this.inventory = new CabinetInventory(slotCount);
    }

    private static int slotCountOf(BlockState state) {
        return state.getBlock() instanceof FilingCabinetBlock cabinet ? cabinet.getSlotCount() : 4;
    }

    public static BlockEntityType<FilingCabinetBlockEntity> typeFor(BlockState state) {
        return switch (slotCountOf(state)) {
            case 1 -> RFRBlockEntities.SINGLE_FILING_CABINET_BE.get();
            case 2 -> RFRBlockEntities.DOUBLE_FILING_CABINET_BE.get();
            default -> RFRBlockEntities.FILING_CABINET_BE.get();
        };
    }

    public int getSlotCount() {
        return slotCount;
    }

    public class CabinetInventory extends ItemStacksResourceHandler {
        CabinetInventory(int size) {
            super(size);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return 1;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && resource.getItem() instanceof FilingFolderItem;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
            if (index >= 0 && index < dirtySlots.length) {
                dirtySlots[index] = true;
                anySlotDirty = true;
            }
            if (level == null || level.isClientSide()) return;
            if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity indexBe) {
                indexBe.scheduleFlush(getBlockPos());
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
            setStacks(resized);
        }
    }

    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= inventory.size()) return ItemStack.EMPTY;
        ItemResource res = inventory.getResource(slot);
        if (res.isEmpty()) return ItemStack.EMPTY;
        return res.toStack(inventory.getAmountAsInt(slot));
    }

    public void setStack(int slot, ItemStack stack) {
        if (stack.isEmpty()) inventory.set(slot, ItemResource.EMPTY, 0);
        else inventory.set(slot, ItemResource.of(stack), 1);
    }

    @Nullable
    public ResourceHandler<ItemResource> getCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FilingCabinetBlock.FACING) == side) return null;
        return handlers.computeIfAbsent(side != null ? side : Direction.UP, s -> new FolderContentsHandler(this));
    }

    public boolean[] consumeDirtySlots() {
        if (!anySlotDirty) return null;
        boolean[] snapshot = dirtySlots.clone();
        Arrays.fill(dirtySlots, false);
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

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.size() + overflow.size());
        int i = 0;
        for (; i < inventory.size(); i++) {
            inv.setItem(i, getStack(i));
        }
        for (ItemStack extra : overflow) inv.setItem(i++, extra);
        overflow.clear();
        Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && controllerPos != null && level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index) {
            index.removeCabinet(pos);
        }
        drops();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && controllerPos != null) {
            var server = serverLevel.getServer();
            server.schedule(new TickTask(server.getTickCount() + 1, this::validateController));
        }
    }

    private void validateController() {
        if (isRemoved() || level == null || controllerPos == null || !level.isLoaded(controllerPos)) return;
        if (!(level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index)
                || !index.getLinkedCabinets().contains(worldPosition)) {
            clearControllerPos();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        if (controllerPos != null) {
            output.putLong("controllerPos", controllerPos.asLong());
        }
        if (!overflow.isEmpty()) {
            output.store("overflow", ItemStack.CODEC.listOf(), overflow);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        overflow.clear();
        inventory.deserialize(input);
        inventory.normalize(slotCount);
        long packed = input.getLongOr("controllerPos", Long.MIN_VALUE);
        controllerPos = packed != Long.MIN_VALUE ? BlockPos.of(packed) : null;
        input.read("overflow", ItemStack.CODEC.listOf()).ifPresent(overflow::addAll);
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

    public static boolean swapFolder(ItemStacksResourceHandler inventory, int slot, ItemStack oldFolder, ItemStack newFolder, TransactionContext tx) {
        try (Transaction inner = Transaction.open(tx)) {
            if (inventory.extract(slot, ItemResource.of(oldFolder), 1, inner) != 1) return false;
            if (inventory.insert(slot, ItemResource.of(newFolder), 1, inner) != 1) return false;
            inner.commit();
            return true;
        }
    }

    public static boolean swapFolder(ItemStacksResourceHandler inventory, int slot, ItemStack oldFolder, ItemStack newFolder) {
        try (Transaction tx = Transaction.openRoot()) {
            if (!swapFolder(inventory, slot, oldFolder, newFolder, tx)) return false;
            tx.commit();
            return true;
        }
    }

    private static class FolderContentsHandler implements ResourceHandler<ItemResource> {
        private final FilingCabinetBlockEntity cabinet;

        FolderContentsHandler(FilingCabinetBlockEntity cabinet) {
            this.cabinet = cabinet;
        }

        @Nullable
        private FilingFolderItem.FolderContents contents(int slot) {
            ItemStack folder = cabinet.getStack(slot);
            if (!(folder.getItem() instanceof FilingFolderItem)) return null;
            return folder.get(FilingFolderItem.FOLDER_CONTENTS.value());
        }

        private boolean accepts(int slot, ItemResource resource) {
            if (resource.isEmpty() || FilingFolderItem.hasSignificantNBT(resource.toStack())) return false;
            FilingFolderItem.FolderContents contents = contents(slot);
            if (contents == null || contents.storedItemId().isEmpty()) return false;
            return contents.storedItemId().get().equals(BuiltInRegistries.ITEM.getKey(resource.getItem()));
        }

        @Override
        public int size() {
            return cabinet.slotCount;
        }

        @Override
        public ItemResource getResource(int index) {
            FilingFolderItem.FolderContents contents = contents(index);
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) return ItemResource.EMPTY;
            return ItemResource.of(BuiltInRegistries.ITEM.getValue(contents.storedItemId().get()));
        }

        @Override
        public long getAmountAsLong(int index) {
            FilingFolderItem.FolderContents contents = contents(index);
            return contents == null ? 0 : contents.count();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            if (!resource.isEmpty() && !accepts(index, resource)) return 0;
            return contents(index) == null ? 0 : FilingFolderItem.getCapacity(cabinet.getStack(index));
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return accepts(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || index < 0 || index >= cabinet.slotCount || !accepts(index, resource)) return 0;
            ItemStack folderStack = cabinet.getStack(index);
            FilingFolderItem.FolderContents contents = contents(index);
            int toAdd = Math.min(amount, FilingFolderItem.getCapacity(folderStack) - contents.count());
            if (toAdd <= 0) return 0;
            ItemStack updated = folderStack.copyWithCount(1);
            updated.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
            return swapFolder(cabinet.inventory, index, folderStack, updated, transaction) ? toAdd : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || index < 0 || index >= cabinet.slotCount || !accepts(index, resource)) return 0;
            ItemStack folderStack = cabinet.getStack(index);
            FilingFolderItem.FolderContents contents = contents(index);
            int toExtract = Math.min(amount, contents.count());
            if (toExtract <= 0) return 0;
            ItemStack updated = folderStack.copyWithCount(1);
            updated.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() - toExtract));
            return swapFolder(cabinet.inventory, index, folderStack, updated, transaction) ? toExtract : 0;
        }
    }
}
