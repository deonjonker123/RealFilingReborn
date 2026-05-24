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

public class FluidCabinetBlockEntity extends BlockEntity implements MenuProvider {

    @Nullable
    private BlockPos controllerPos = null;

    public final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(4) {
        @Override
        protected void onContentsChanged(int slot, ItemStack previous) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final Map<Direction, ResourceHandler<FluidResource>> fluidHandlers = new HashMap<>();

    public FluidCabinetBlockEntity(BlockPos pos, BlockState blockState) {
        super(RFRBlockEntities.FLUID_CABINET_BE.get(), pos, blockState);
    }

    @Nullable
    public ResourceHandler<ItemResource> getCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FluidCabinetBlock.FACING) == side) return null;
        return null;
    }

    @Nullable
    public ResourceHandler<FluidResource> getFluidCapabilityHandler(@Nullable Direction side) {
        if (side != null && getBlockState().getValue(FluidCabinetBlock.FACING) == side) return null;
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getResource(i).isEmpty()) {
                return fluidHandlers.computeIfAbsent(side != null ? side : Direction.UP,
                        s -> new FluidCabinetFluidHandler(this, s));
            }
        }
        return null;
    }

    public ItemStack getStack(int slot) {
        ItemResource res = inventory.getResource(slot);
        if (res.isEmpty()) return ItemStack.EMPTY;
        return res.toStack(inventory.getAmountAsInt(slot));
    }

    public void notifyCanisterContentsChanged() {
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
        return Component.translatable("blockentity.realfilingreborn.fluid_cabinet_name");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new FluidCabinetMenu(id, playerInventory, this);
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

    private static class FluidCabinetFluidHandler implements ResourceHandler<FluidResource> {

        private final FluidCabinetBlockEntity cabinet;
        private final Direction side;

        public FluidCabinetFluidHandler(FluidCabinetBlockEntity cabinet, @Nullable Direction side) {
            this.cabinet = cabinet;
            this.side = side;
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public FluidResource getResource(int slot) {
            if (slot < 0 || slot >= 4) return FluidResource.EMPTY;
            ItemStack canisterStack = cabinet.getStack(slot);
            if (!(canisterStack.getItem() instanceof FluidCanisterItem)) return FluidResource.EMPTY;
            FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
            if (contents == null || contents.storedFluidId().isEmpty()) return FluidResource.EMPTY;
            Fluid fluid = FluidHelper.getFluidFromId(contents.storedFluidId().get());
            return fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY
                    ? FluidResource.of(fluid) : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int slot) {
            if (slot < 0 || slot >= 4) return 0;
            ItemStack canisterStack = cabinet.getStack(slot);
            if (!(canisterStack.getItem() instanceof FluidCanisterItem)) return 0;
            FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
            return contents != null ? contents.amount() : 0;
        }

        @Override
        public long getCapacityAsLong(int slot, FluidResource resource) {
            if (slot < 0 || slot >= 4) return 0;
            ItemStack canisterStack = cabinet.getStack(slot);
            if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) return 0;
            return canister.getCapacity();
        }

        @Override
        public boolean isValid(int slot, FluidResource resource) {
            return !resource.isEmpty() && FluidHelper.isValidFluid(resource.getFluid());
        }

        @Override
        public int insert(int slot, FluidResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0 || slot < 0 || slot >= 4) return 0;
            if (!FluidHelper.isValidFluid(resource.getFluid())) return 0;

            Identifier fluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));
            ItemStack canisterStack = cabinet.getStack(slot);
            if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) return 0;

            FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
            if (contents == null) return 0;
            if (!contents.storedFluidId().isEmpty() && !FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), fluidId)) return 0;

            int toAdd = (int) Math.min(amount, canister.getCapacity() - contents.amount());
            if (toAdd <= 0) return 0;

            ItemStack updated = canisterStack.copy();
            updated.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                    new FluidCanisterItem.CanisterContents(Optional.of(fluidId), contents.amount() + toAdd));
            try (Transaction innerTx = Transaction.open(tx)) {
                cabinet.inventory.extract(slot, ItemResource.of(canisterStack), 1, innerTx);
                cabinet.inventory.insert(slot, ItemResource.of(updated), 1, innerTx);
                innerTx.commit();
            }
            cabinet.notifyCanisterContentsChanged();
            return toAdd;
        }

        @Override
        public int extract(int slot, FluidResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0 || slot < 0 || slot >= 4) return 0;

            Identifier fluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(resource.getFluid()));
            ItemStack canisterStack = cabinet.getStack(slot);
            if (!(canisterStack.getItem() instanceof FluidCanisterItem)) return 0;

            FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
            if (contents == null || contents.storedFluidId().isEmpty()) return 0;
            if (!FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), fluidId)) return 0;

            int toDrain = Math.min(amount, contents.amount());
            if (toDrain <= 0) return 0;

            ItemStack updated = canisterStack.copy();
            updated.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                    new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - toDrain));
            try (Transaction innerTx = Transaction.open(tx)) {
                cabinet.inventory.extract(slot, ItemResource.of(canisterStack), 1, innerTx);
                cabinet.inventory.insert(slot, ItemResource.of(updated), 1, innerTx);
                innerTx.commit();
            }
            cabinet.notifyCanisterContentsChanged();
            return toDrain;
        }
    }
}