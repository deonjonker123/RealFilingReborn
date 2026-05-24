package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.gui.custom.FluidCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.util.Optional;

public class FluidCabinetBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final MapCodec<FluidCabinetBlock> CODEC = simpleCodec(FluidCabinetBlock::new);

    public FluidCabinetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidCabinetBlockEntity(pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    private void openFluidCabinetMenu(FluidCabinetBlockEntity blockEntity, ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, playerEntity) -> new FluidCabinetMenu(id, inventory, blockEntity),
                Component.translatable("menu.realfilingreborn.fluid_cabinet_menu_title")
        ), pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FluidCabinetBlockEntity cabinet)) return InteractionResult.FAIL;

        if (player.isCrouching()) {
            openFluidCabinetMenu(cabinet, (ServerPlayer) player, pos);
            level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        Direction facing = state.getValue(FACING);

        if (hitResult.getDirection() == facing && heldItem.getItem() == Items.BUCKET) {
            int targetSlot = getQuadFromHitResult(hitResult, facing);
            if (targetSlot >= 0 && targetSlot < 4) {
                return extractFromSlot(cabinet, targetSlot, player, level, pos, state);
            }
            return InteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof FluidCanisterItem) {
            for (int i = 0; i < 4; i++) {
                if (cabinet.getStack(i).isEmpty()) {
                    try (Transaction tx = Transaction.openRoot()) {
                        cabinet.inventory.insert(i, ItemResource.of(heldItem), 1, tx);
                        tx.commit();
                    }
                    heldItem.shrink(1);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
                    level.sendBlockUpdated(pos, state, state, 2);
                    cabinet.setChanged();
                    return InteractionResult.SUCCESS;
                }
            }
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.canisters_full"));
            return InteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY) {
            Fluid fluid = bucketItem.content;
            Identifier fluidId = fluid.builtInRegistryHolder().key().location();

            for (int i = 0; i < 4; i++) {
                ItemStack canisterStack = cabinet.getStack(i);
                if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;

                FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
                if (contents == null) continue;

                boolean isEmpty = contents.storedFluidId().isEmpty();
                boolean matchesFluid = !isEmpty && contents.storedFluidId().get().equals(fluidId);
                int canAdd = canister.getCapacity() - contents.amount();

                if ((isEmpty || matchesFluid) && canAdd >= 1000) {
                    Identifier storedId = isEmpty ? fluidId : contents.storedFluidId().get();
                    ItemStack updatedCanister = canisterStack.copy();
                    updatedCanister.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                            new FluidCanisterItem.CanisterContents(Optional.of(storedId), contents.amount() + 1000));
                    try (Transaction tx = Transaction.openRoot()) {
                        cabinet.inventory.extract(i, ItemResource.of(canisterStack), 1, tx);
                        cabinet.inventory.insert(i, ItemResource.of(updatedCanister), 1, tx);
                        tx.commit();
                    }
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                        if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket, false);
                    }
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.sendBlockUpdated(pos, state, state, 2);
                    cabinet.setChanged();
                    return InteractionResult.SUCCESS;
                }
            }
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_space_for_fluid"));
            return InteractionResult.SUCCESS;
        }

        openFluidCabinetMenu(cabinet, (ServerPlayer) player, pos);
        level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult extractFromSlot(FluidCabinetBlockEntity blockEntity, int slot, Player player, Level level, BlockPos pos, BlockState state) {
        ItemStack canisterStack = blockEntity.getStack(slot);
        if (!(canisterStack.getItem() instanceof FluidCanisterItem)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_fluid_in_slot"));
            return InteractionResult.SUCCESS;
        }

        FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
        if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() < 1000) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.not_enough_fluid_in_slot"));
            return InteractionResult.SUCCESS;
        }

        Identifier fluidId = contents.storedFluidId().get();
        ItemStack bucketToGive = FluidHelper.getBucketForFluid(fluidId);
        if (bucketToGive.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_bucket_for_fluid"));
            return InteractionResult.SUCCESS;
        }

        ItemStack updatedCanister = canisterStack.copy();
        updatedCanister.set(FluidCanisterItem.CANISTER_CONTENTS.value(),
                new FluidCanisterItem.CanisterContents(contents.storedFluidId(), contents.amount() - 1000));
        try (Transaction tx = Transaction.openRoot()) {
            blockEntity.inventory.extract(slot, ItemResource.of(canisterStack), 1, tx);
            blockEntity.inventory.insert(slot, ItemResource.of(updatedCanister), 1, tx);
            tx.commit();
        }

        player.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);
        if (!player.getInventory().add(bucketToGive)) player.drop(bucketToGive, false);

        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.sendBlockUpdated(pos, state, state, 2);
        blockEntity.setChanged();
        player.sendOverlayMessage(Component.translatable("message.realfilingreborn.fluid_extracted"));
        return InteractionResult.SUCCESS;
    }

    private int getQuadFromHitResult(BlockHitResult hitResult, Direction facing) {
        Vec3 hitPos = hitResult.getLocation();
        double relX = hitPos.x - Math.floor(hitPos.x);
        double relZ = hitPos.z - Math.floor(hitPos.z);
        double relY = hitPos.y - Math.floor(hitPos.y);

        double faceX = switch (facing) {
            case NORTH -> 1.0 - relX;
            case SOUTH -> relX;
            case EAST -> 1.0 - relZ;
            case WEST -> relZ;
            default -> -1;
        };

        if (faceX < 0) return -1;
        boolean left = faceX < 0.5;
        boolean top = relY > 0.5;
        if (top && left) return 0;
        if (top) return 1;
        if (left) return 2;
        return 3;
    }
}