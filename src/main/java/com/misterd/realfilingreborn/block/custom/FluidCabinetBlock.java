package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.gui.custom.FluidCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;

public class FluidCabinetBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING;
    public static final VoxelShape SHAPE;
    public static final MapCodec<FluidCabinetBlock> CODEC;

    public FluidCabinetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidCabinetBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof FluidCabinetBlockEntity cabinet) {
                BlockPos controllerPos = cabinet.getControllerPos();
                if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index) {
                    index.removeCabinet(pos);
                }
                cabinet.drops();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void openFluidCabinetMenu(FluidCabinetBlockEntity blockEntity, ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, playerEntity) -> new FluidCabinetMenu(id, inventory, blockEntity),
                Component.translatable("menu.realfilingreborn.fluid_cabinet_menu_title")
        ), pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FluidCabinetBlockEntity cabinet)) {
            return ItemInteractionResult.FAIL;
        }

        if (player.isCrouching()) {
            if (!level.isClientSide()) {
                openFluidCabinetMenu(cabinet, (ServerPlayer) player, pos);
                level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        Direction facing = state.getValue(FACING);

        if (hitResult.getDirection() == facing && heldItem.getItem() == Items.BUCKET) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            int targetSlot = getQuadFromHitResult(hitResult, facing);
            if (targetSlot >= 0 && targetSlot < 4) {
                return extractFromSlot(cabinet, targetSlot, player, level, pos, state);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof FluidCanisterItem) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            for (int i = 0; i < 4; i++) {
                if (cabinet.inventory.getStackInSlot(i).isEmpty()) {
                    cabinet.inventory.setStackInSlot(i, heldItem.copyWithCount(1));
                    heldItem.shrink(1);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
                    level.sendBlockUpdated(pos, state, state, 2);
                    cabinet.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }
            player.displayClientMessage(Component.translatable("message.realfilingreborn.canisters_full"), true);
            return ItemInteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

            Fluid fluid = bucketItem.content;
            ResourceLocation fluidId = fluid.builtInRegistryHolder().key().location();

            for (int i = 0; i < 4; i++) {
                ItemStack canisterStack = cabinet.inventory.getStackInSlot(i);
                if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null) continue;

                boolean isEmpty = contents.storedFluidId().isEmpty();
                boolean matchesFluid = !isEmpty && contents.storedFluidId().get().equals(fluidId);
                int canAdd = canister.getCapacity() - contents.amount();

                if ((isEmpty || matchesFluid) && canAdd >= 1000) {
                    ResourceLocation storedId = isEmpty ? fluidId : contents.storedFluidId().get();
                    canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(Optional.of(storedId), contents.amount() + 1000));
                    heldItem.shrink(1);
                    ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                    if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket, false);
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.5F);
                    level.sendBlockUpdated(pos, state, state, 2);
                    cabinet.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }

            player.displayClientMessage(Component.translatable("message.realfilingreborn.no_compatible_canister"), true);
            return ItemInteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            openFluidCabinetMenu(cabinet, (ServerPlayer) player, pos);
            level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private int getQuadFromHitResult(BlockHitResult hitResult, Direction facing) {
        Vec3 hitPos = hitResult.getLocation();
        double relX = hitPos.x - Math.floor(hitPos.x);
        double relY = hitPos.y - Math.floor(hitPos.y);
        double relZ = hitPos.z - Math.floor(hitPos.z);

        double faceX = switch (facing) {
            case NORTH -> 1.0 - relX;
            case SOUTH -> relX;
            case EAST  -> 1.0 - relZ;
            case WEST  -> relZ;
            default    -> -1;
        };

        if (faceX < 0) return -1;
        boolean isLeft = faceX < 0.5;
        boolean isTop  = relY > 0.5;

        if (isTop  && isLeft)  return 0;
        if (isTop  && !isLeft) return 1;
        if (!isTop && isLeft)  return 2;
        return 3;
    }

    private ItemInteractionResult extractFromSlot(FluidCabinetBlockEntity blockEntity, int slot, Player player, Level level, BlockPos pos, BlockState state) {
        ItemStack canisterStack = blockEntity.inventory.getStackInSlot(slot);
        if (canisterStack.isEmpty() || !(canisterStack.getItem() instanceof FluidCanisterItem)) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.no_fluid_in_slot"), true);
            return ItemInteractionResult.SUCCESS;
        }

        FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() < 1000) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.not_enough_fluid_in_slot"), true);
            return ItemInteractionResult.SUCCESS;
        }

        ResourceLocation fluidId = contents.storedFluidId().get();
        ItemStack bucketToGive = FluidHelper.getBucketForFluid(fluidId);
        if (bucketToGive.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.no_bucket_for_fluid"), true);
            return ItemInteractionResult.SUCCESS;
        }

        canisterStack.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - 1000));
        player.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);
        if (!player.getInventory().add(bucketToGive)) player.drop(bucketToGive, false);

        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.sendBlockUpdated(pos, state, state, 2);
        blockEntity.setChanged();
        player.displayClientMessage(Component.translatable("message.realfilingreborn.fluid_extracted"), true);
        return ItemInteractionResult.SUCCESS;
    }

    static {
        FACING = HorizontalDirectionalBlock.FACING;
        SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
        CODEC = simpleCodec(FluidCabinetBlock::new);
    }
}