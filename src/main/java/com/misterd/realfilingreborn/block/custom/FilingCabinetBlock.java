package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.gui.custom.FilingCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;

public class FilingCabinetBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING;
    public static final VoxelShape SHAPE;
    public static final MapCodec<FilingCabinetBlock> CODEC;

    public FilingCabinetBlock(Properties properties) {
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
        return new FilingCabinetBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity cabinet) {
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

    private void openFilingCabinetMenu(FilingCabinetBlockEntity blockEntity, ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, playerEntity) -> new FilingCabinetMenu(id, inventory, blockEntity),
                Component.translatable("menu.realfilingreborn.menu_title")
        ), pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity cabinet)) {
            return ItemInteractionResult.FAIL;
        }

        if (player.isCrouching()) {
            if (!level.isClientSide()) {
                openFilingCabinetMenu(cabinet, (ServerPlayer) player, pos);
                level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.isEmpty()) {
            Direction facing = state.getValue(FACING);
            if (hitResult.getDirection() == facing) {
                if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
                int targetSlot = getSlotFromHitResult(hitResult, facing);
                if (targetSlot >= 0 && targetSlot < 5) {
                    InteractionResult result = extractFromSlot(cabinet, targetSlot, player, level, pos, state);
                    return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
                }
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof FilingFolderItem) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            for (int i = 0; i < 5; i++) {
                if (cabinet.inventory.getStackInSlot(i).isEmpty()) {
                    cabinet.inventory.setStackInSlot(i, heldItem.copyWithCount(1));
                    heldItem.shrink(1);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
                    level.sendBlockUpdated(pos, state, state, 2);
                    cabinet.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }
            player.displayClientMessage(Component.translatable("message.realfilingreborn.folders_full"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // Non-folder item — attempt to store into a folder
        ItemInteractionResult storageResult = handleItemStorage(heldItem, cabinet, player, level, pos, state);
        if (storageResult == ItemInteractionResult.FAIL) {
            if (!level.isClientSide()) {
                openFilingCabinetMenu(cabinet, (ServerPlayer) player, pos);
                level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return storageResult;
    }

    private ItemInteractionResult handleItemStorage(ItemStack heldItem, FilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

        for (int i = 0; i < 5; i++) {
            ItemStack folderStack = cabinet.inventory.getStackInSlot(i);
            if (!(folderStack.getItem() instanceof FilingFolderItem folder)) continue;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null) continue;

            if (contents.storedItemId().isEmpty()) {
                folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(Optional.of(itemId), heldItem.getCount()));
                heldItem.shrink(heldItem.getCount());
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
                level.sendBlockUpdated(pos, state, state, 2);
                cabinet.setChanged();
                return ItemInteractionResult.SUCCESS;
            }

            if (contents.storedItemId().get().equals(itemId)) {
                int toAdd = Math.min(heldItem.getCount(), folder.getCapacity() - contents.count());
                if (toAdd > 0) {
                    folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                            new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
                    heldItem.shrink(toAdd);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
                    level.sendBlockUpdated(pos, state, state, 2);
                    cabinet.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        return ItemInteractionResult.FAIL;
    }

    private int getSlotFromHitResult(BlockHitResult hitResult, Direction facing) {
        Vec3 hitPos = hitResult.getLocation();
        double relX = hitPos.x - Math.floor(hitPos.x);
        double relZ = hitPos.z - Math.floor(hitPos.z);

        double faceX = switch (facing) {
            case NORTH -> 1.0 - relX;
            case SOUTH -> relX;
            case EAST  -> 1.0 - relZ;
            case WEST  -> relZ;
            default    -> -1;
        };

        if (faceX < 0)   return -1;
        if (faceX < 0.2) return 0;
        if (faceX < 0.4) return 1;
        if (faceX < 0.6) return 2;
        if (faceX < 0.8) return 3;
        return 4;
    }

    private InteractionResult extractFromSlot(FilingCabinetBlockEntity blockEntity, int slot, Player player, Level level, BlockPos pos, BlockState state) {
        ItemStack folderStack = blockEntity.inventory.getStackInSlot(slot);
        if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) {
            return InteractionResult.SUCCESS;
        }

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.folder_empty"), true);
            return InteractionResult.SUCCESS;
        }

        ResourceLocation itemId = contents.storedItemId().get();
        Item item = BuiltInRegistries.ITEM.get(itemId);
        ItemStack extracted = new ItemStack(item);
        int extractAmount = Math.min(Math.min(contents.count(), item.getMaxStackSize(extracted)), 64);

        if (extractAmount <= 0) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.folder_empty"), true);
            return InteractionResult.SUCCESS;
        }

        ItemStack extractedStack = new ItemStack(item, extractAmount);
        folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                new FilingFolderItem.FolderContents(contents.storedItemId(), Math.max(0, contents.count() - extractAmount)));

        if (!player.getInventory().add(extractedStack)) {
            player.drop(extractedStack, false);
        }

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendBlockUpdated(pos, state, state, 2);
        blockEntity.setChanged();
        return InteractionResult.SUCCESS;
    }

    static {
        FACING = HorizontalDirectionalBlock.FACING;
        SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
        CODEC = simpleCodec(FilingCabinetBlock::new);
    }
}