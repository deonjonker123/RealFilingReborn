package com.misterd.realfilingreborn.blockentity.custom;

import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
import com.misterd.realfilingreborn.blockentity.RFRBlockEntities;
import com.misterd.realfilingreborn.gui.custom.FluidCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FluidCabinetBlockEntity extends BlockEntity implements MenuProvider, LinkableCabinet {
    public static final int SLOTS = 4;

    @Nullable
    private BlockPos controllerPos = null;

    public final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(SLOTS) {
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return 1;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && resource.getItem() instanceof FluidCanisterItem;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
            if (level == null || level.isClientSide()) return;
            if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index2) {
                index2.scheduleFlush(getBlockPos());
            }
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    };

    private final Map<Direction, ResourceHandler<FluidResource>> fluidHandlers = new HashMap<>();

    public FluidCabinetBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.FLUID_CABINET_BE.get(), pos, blockState);
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

    public void setCanisterAmount(int slot, Identifier fluidId, int amount) {
        ItemStack updated = getStack(slot).copyWithCount(1);
        updated.set(FluidCanisterItem.CANISTER_CONTENTS.value(), new FluidCanisterItem.CanisterContents(Optional.of(fluidId), amount));
        setStack(slot, updated);
    }

    @Nullable
    public ResourceHandler<FluidResource> getFluidCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FluidCabinetBlock.FACING) == side) return null;
        return fluidHandlers.computeIfAbsent(side != null ? side : Direction.UP, s -> new CanisterTanks(this));
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
        return Component.translatable("blockentity.realfilingreborn.fluid_cabinet_name");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new FluidCabinetMenu(id, playerInventory, this);
    }

    @Override
    public void setControllerPos(BlockPos pos) {
        controllerPos = pos;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void clearControllerPos() {
        controllerPos = null;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public BlockPos getControllerPos() {
        return controllerPos;
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

    public static boolean swapCanister(ItemStacksResourceHandler inventory, int slot, ItemStack oldCanister, ItemStack newCanister, TransactionContext tx) {
        try (Transaction inner = Transaction.open(tx)) {
            if (inventory.extract(slot, ItemResource.of(oldCanister), 1, inner) != 1) return false;
            if (inventory.insert(slot, ItemResource.of(newCanister), 1, inner) != 1) return false;
            inner.commit();
            return true;
        }
    }

    public static int changeCanister(ItemStacksResourceHandler inventory, int slot, ItemStack canisterStack, Identifier fluidId, int delta, TransactionContext tx) {
        if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) return 0;
        FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canisterStack);
        if (contents.storedFluidId().isPresent() && !FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), fluidId)) return 0;

        int moved = delta > 0
                ? Math.min(delta, canister.getCapacity() - contents.amount())
                : Math.min(-delta, contents.amount());
        if (moved <= 0) return 0;

        int newAmount = delta > 0 ? contents.amount() + moved : contents.amount() - moved;
        ItemStack updated = canisterStack.copyWithCount(1);
        updated.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                new FluidCanisterItem.CanisterContents(Optional.of(contents.storedFluidId().orElse(fluidId)), newAmount));
        return swapCanister(inventory, slot, canisterStack, updated, tx) ? moved : 0;
    }

    private static class CanisterTanks implements ResourceHandler<FluidResource> {
        private final FluidCabinetBlockEntity cabinet;

        CanisterTanks(FluidCabinetBlockEntity cabinet) {
            this.cabinet = cabinet;
        }

        @Nullable
        private Identifier idOf(FluidResource resource) {
            if (resource.isEmpty() || !FluidHelper.isValidFluid(resource.getFluid())) return null;
            return FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));
        }

        private boolean accepts(int index, FluidResource resource) {
            Identifier id = idOf(resource);
            if (id == null || index < 0 || index >= SLOTS) return false;
            ItemStack canister = cabinet.getStack(index);
            if (!(canister.getItem() instanceof FluidCanisterItem)) return false;
            FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canister);
            return contents.storedFluidId().isEmpty() || FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), id);
        }

        @Override
        public int size() {
            return SLOTS;
        }

        @Override
        public FluidResource getResource(int index) {
            ItemStack canister = cabinet.getStack(index);
            if (!(canister.getItem() instanceof FluidCanisterItem)) return FluidResource.EMPTY;
            FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canister);
            if (contents.storedFluidId().isEmpty() || contents.amount() <= 0) return FluidResource.EMPTY;
            Fluid fluid = FluidHelper.getFluidFromId(contents.storedFluidId().get());
            return FluidHelper.isValidFluid(fluid) ? FluidResource.of(fluid) : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            ItemStack canister = cabinet.getStack(index);
            if (!(canister.getItem() instanceof FluidCanisterItem)) return 0;
            return FluidCanisterItem.contentsOf(canister).amount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            ItemStack canister = cabinet.getStack(index);
            if (!(canister.getItem() instanceof FluidCanisterItem item)) return 0;
            if (!resource.isEmpty() && !accepts(index, resource)) return 0;
            return item.getCapacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return accepts(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || !accepts(index, resource)) return 0;
            return changeCanister(cabinet.inventory, index, cabinet.getStack(index), idOf(resource), amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            Identifier id = idOf(resource);
            if (id == null || amount <= 0) return 0;
            int inserted = 0;
            for (int pass = 0; pass < 2 && inserted < amount; pass++) {
                for (int i = 0; i < SLOTS && inserted < amount; i++) {
                    ItemStack canister = cabinet.getStack(i);
                    if (!(canister.getItem() instanceof FluidCanisterItem)) continue;
                    boolean assigned = FluidCanisterItem.contentsOf(canister).storedFluidId().isPresent();
                    if (assigned != (pass == 0)) continue;
                    inserted += insert(i, resource, amount - inserted, transaction);
                }
            }
            return inserted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || !accepts(index, resource)) return 0;
            ItemStack canister = cabinet.getStack(index);
            if (FluidCanisterItem.contentsOf(canister).storedFluidId().isEmpty()) return 0;
            return changeCanister(cabinet.inventory, index, canister, idOf(resource), -amount, transaction);
        }
    }
}
