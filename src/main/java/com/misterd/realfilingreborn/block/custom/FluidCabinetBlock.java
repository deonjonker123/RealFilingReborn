package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.gui.custom.FluidCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

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

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FluidCabinetBlockEntity cabinet)) return InteractionResult.FAIL;

        Direction facing = state.getValue(FACING);
        ItemStack heldItem = player.getItemInHand(hand);

        if (player.isCrouching() || hitResult.getDirection() != facing || heldItem.isEmpty()) {
            openMenu(cabinet, player, pos, level);
            return InteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof FluidCanisterItem) {
            for (int i = 0; i < FluidCabinetBlockEntity.SLOTS; i++) {
                if (cabinet.getStack(i).isEmpty()) {
                    cabinet.setStack(i, heldItem.copyWithCount(1));
                    heldItem.shrink(1);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
                    return InteractionResult.SUCCESS;
                }
            }
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.canisters_full"));
            return InteractionResult.SUCCESS;
        }

        if (heldItem.getItem() == Items.BUCKET) {
            int slot = getQuadFromHitResult(hitResult, facing);
            if (slot >= 0) extractToBucket(cabinet, slot, player, hand, level, pos);
            return InteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof BucketItem bucketItem && bucketItem.content != Fluids.EMPTY) {
            fillFromBucket(cabinet, bucketItem, player, hand, level, pos);
            return InteractionResult.SUCCESS;
        }

        openMenu(cabinet, player, pos, level);
        return InteractionResult.SUCCESS;
    }

    private void fillFromBucket(FluidCabinetBlockEntity cabinet, BucketItem bucketItem, Player player, InteractionHand hand, Level level, BlockPos pos) {
        if (!FluidHelper.isValidFluid(bucketItem.content)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.invalid_fluid"));
            return;
        }
        ResourceHandler<FluidResource> tanks = cabinet.getFluidCapabilityHandler(null);
        try (Transaction tx = Transaction.openRoot()) {
            if (tanks == null || tanks.insert(FluidResource.of(bucketItem.content), 1000, tx) != 1000) {
                player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_compatible_canister"));
                return;
            }
            tx.commit();
        }
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
        if (held.isEmpty()) player.setItemInHand(hand, emptyBucket);
        else if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket, false);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.5F);
    }

    private void extractToBucket(FluidCabinetBlockEntity cabinet, int slot, Player player, InteractionHand hand, Level level, BlockPos pos) {
        ItemStack canisterStack = cabinet.getStack(slot);
        if (!(canisterStack.getItem() instanceof FluidCanisterItem)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_fluid_in_slot"));
            return;
        }

        FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canisterStack);
        if (contents.storedFluidId().isEmpty() || contents.amount() < 1000) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.not_enough_fluid_in_slot"));
            return;
        }

        ItemStack bucketToGive = FluidHelper.getBucketForFluid(contents.storedFluidId().get());
        if (bucketToGive.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_bucket_for_fluid"));
            return;
        }

        cabinet.setCanisterAmount(slot, contents.storedFluidId().get(), contents.amount() - 1000);
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, bucketToGive);
        else if (!player.getInventory().add(bucketToGive)) player.drop(bucketToGive, false);
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.5F);
    }

    private int getQuadFromHitResult(BlockHitResult hitResult, Direction facing) {
        Vec3 hitPos = hitResult.getLocation();
        double relX = hitPos.x - Math.floor(hitPos.x);
        double relY = hitPos.y - Math.floor(hitPos.y);
        double relZ = hitPos.z - Math.floor(hitPos.z);

        double faceX = switch (facing) {
            case NORTH -> 1.0 - relX;
            case SOUTH -> relX;
            case EAST -> 1.0 - relZ;
            case WEST -> relZ;
            default -> -1;
        };

        if (faceX < 0) return -1;
        return (relY > 0.5 ? 0 : 2) + (faceX < 0.5 ? 0 : 1);
    }

    private void openMenu(FluidCabinetBlockEntity cabinet, Player player, BlockPos pos, Level level) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, playerEntity) -> new FluidCabinetMenu(id, inventory, cabinet),
                Component.translatable("menu.realfilingreborn.fluid_cabinet_menu_title")), pos);
    }
}
