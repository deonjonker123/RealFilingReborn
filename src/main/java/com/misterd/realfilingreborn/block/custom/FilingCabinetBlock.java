package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.gui.custom.FilingCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.util.Optional;

public class FilingCabinetBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final MapCodec<FilingCabinetBlock> CODEC = simpleCodec(FilingCabinetBlock::new);

    public FilingCabinetBlock(Properties properties) {
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
        return new FilingCabinetBlockEntity(pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    private void openFilingCabinetMenu(FilingCabinetBlockEntity blockEntity, ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, playerEntity) -> new FilingCabinetMenu(id, inventory, blockEntity),
                Component.translatable("menu.realfilingreborn.menu_title")
        ), pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity cabinet)) return InteractionResult.FAIL;

        Direction facing = state.getValue(FACING);
        boolean hittingFront = hitResult.getDirection() == facing;
        ItemStack heldItem = player.getItemInHand(hand);

        if (player.isCrouching() && hittingFront) {
            int targetSlot = getSlotFromHitResult(hitResult, facing);
            if (targetSlot >= 0 && targetSlot < 4) {
                extractFromSlot(cabinet, targetSlot, Integer.MAX_VALUE, player, level, pos, state);
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isCrouching()) {
            openFilingCabinetMenu(cabinet, (ServerPlayer) player, pos);
            level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        if (heldItem.isEmpty()) {
            openFilingCabinetMenu(cabinet, (ServerPlayer) player, pos);
            level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        if (!hittingFront) {
            openFilingCabinetMenu(cabinet, (ServerPlayer) player, pos);
            level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        if (heldItem.getItem() instanceof FilingFolderItem) {
            for (int i = 0; i < 5; i++) {
                if (cabinet.getStack(i).isEmpty()) {
                    try (var tx = Transaction.openRoot()) {
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
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.folders_full"));
            return InteractionResult.SUCCESS;
        }

        return handleItemStorage(heldItem, cabinet, player, level, pos, state);
    }

    private InteractionResult handleItemStorage(ItemStack heldItem, FilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockState state) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

        for (int i = 0; i < 4; i++) {
            ItemStack folderStack = cabinet.getStack(i);
            if (!(folderStack.getItem() instanceof FilingFolderItem folder)) continue;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null) continue;

            if (contents.storedItemId().isEmpty()) {
                folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(Optional.of(itemId), heldItem.getCount()));
                try (var tx = Transaction.openRoot()) {
                    cabinet.inventory.extract(i, ItemResource.of(cabinet.getStack(i)), cabinet.getStack(i).getCount(), tx);
                    cabinet.inventory.insert(i, ItemResource.of(folderStack), 1, tx);
                    tx.commit();
                }
                heldItem.shrink(heldItem.getCount());
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
                cabinet.setChanged();
                return InteractionResult.SUCCESS;
            }

            if (contents.storedItemId().get().equals(itemId)) {
                int toAdd = Math.min(heldItem.getCount(), folder.getCapacity() - contents.count());
                if (toAdd > 0) {
                    folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                            new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
                    try (var tx = Transaction.openRoot()) {
                        cabinet.inventory.extract(i, ItemResource.of(cabinet.getStack(i)), cabinet.getStack(i).getCount(), tx);
                        cabinet.inventory.insert(i, ItemResource.of(folderStack), 1, tx);
                        tx.commit();
                    }
                    heldItem.shrink(toAdd);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
                    cabinet.setChanged();
                    return InteractionResult.SUCCESS;
                }
            }
        }

        openFilingCabinetMenu(cabinet, (ServerPlayer) player, pos);
        level.playSound(player, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private int getSlotFromHitResult(BlockHitResult hitResult, Direction facing) {
        Vec3 hitPos = hitResult.getLocation();
        double relX = hitPos.x - Math.floor(hitPos.x);
        double relZ = hitPos.z - Math.floor(hitPos.z);

        double faceX = switch (facing) {
            case NORTH -> 1.0 - relX;
            case SOUTH -> relX;
            case EAST -> 1.0 - relZ;
            case WEST -> relZ;
            default -> -1;
        };

        if (faceX < 0) return -1;
        if (faceX < 0.2) return 0;
        if (faceX < 0.4) return 1;
        if (faceX < 0.6) return 2;
        if (faceX < 0.8) return 3;
        return 4;
    }

    private void extractFromSlot(FilingCabinetBlockEntity blockEntity, int slot, int amount, Player player, Level level, BlockPos pos, BlockState state) {
        ItemStack folderStack = blockEntity.getStack(slot);
        if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) return;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.folder_empty"));
            return;
        }

        Identifier itemId = contents.storedItemId().get();
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        ItemStack extracted = new ItemStack(item);
        int extractAmount = Math.min(Math.min(contents.count(), item.getMaxStackSize(extracted)), amount);

        if (extractAmount <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.folder_empty"));
            return;
        }

        ItemStack extractedStack = new ItemStack(item, extractAmount);
        folderStack.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                new FilingFolderItem.FolderContents(contents.storedItemId(), Math.max(0, contents.count() - extractAmount)));
        try (var tx = Transaction.openRoot()) {
            blockEntity.inventory.extract(slot, ItemResource.of(blockEntity.getStack(slot)), blockEntity.getStack(slot).getCount(), tx);
            blockEntity.inventory.insert(slot, ItemResource.of(folderStack), 1, tx);
            tx.commit();
        }

        if (!player.getInventory().add(extractedStack)) {
            player.drop(extractedStack, false);
        }

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        blockEntity.setChanged();
    }
}