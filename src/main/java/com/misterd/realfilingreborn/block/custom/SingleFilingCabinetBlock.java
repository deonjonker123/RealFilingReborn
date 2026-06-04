package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.SingleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.gui.custom.SingleFilingCabinetMenu;
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

public class SingleFilingCabinetBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final MapCodec<SingleFilingCabinetBlock> CODEC = simpleCodec(SingleFilingCabinetBlock::new);

    public SingleFilingCabinetBlock(Properties properties) {
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
        return new SingleFilingCabinetBlockEntity(pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    private void openFilingCabinetMenu(SingleFilingCabinetBlockEntity blockEntity, ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, playerEntity) -> new SingleFilingCabinetMenu(id, inventory, blockEntity),
                Component.translatable("menu.realfilingreborn.menu_title")
        ), pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SingleFilingCabinetBlockEntity cabinet)) return InteractionResult.FAIL;

        Direction facing = state.getValue(FACING);
        boolean hittingFront = hitResult.getDirection() == facing;
        ItemStack heldItem = player.getItemInHand(hand);

        if (player.isCrouching() && hittingFront) {
            extractFromSlot(cabinet, 0, Integer.MAX_VALUE, player, level, pos, state);
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
            for (int i = 0; i < 1; i++) {
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
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.single_folders_full"));
            return InteractionResult.SUCCESS;
        }

        return handleItemStorage(heldItem, cabinet, player, level, pos, state);
    }

    private InteractionResult handleItemStorage(ItemStack heldItem, SingleFilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockState state) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

        for (int i = 0; i < 1; i++) {
            ItemStack folderStack = cabinet.getStack(i);
            if (!(folderStack.getItem() instanceof FilingFolderItem folder)) continue;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null) continue;

            if (contents.storedItemId().isEmpty()) {
                ItemStack updatedFolder = folderStack.copy();
                updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                        new FilingFolderItem.FolderContents(Optional.of(itemId), heldItem.getCount()));
                try (var tx = Transaction.openRoot()) {
                    cabinet.inventory.extract(i, ItemResource.of(folderStack), 1, tx);
                    cabinet.inventory.insert(i, ItemResource.of(updatedFolder), 1, tx);
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
                    ItemStack updatedFolder = folderStack.copy();
                    updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                            new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
                    try (var tx = Transaction.openRoot()) {
                        cabinet.inventory.extract(i, ItemResource.of(folderStack), 1, tx);
                        cabinet.inventory.insert(i, ItemResource.of(updatedFolder), 1, tx);
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

    private void extractFromSlot(SingleFilingCabinetBlockEntity blockEntity, int slot, int amount, Player player, Level level, BlockPos pos, BlockState state) {
        ItemStack folderStack = blockEntity.getStack(slot);
        if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) return;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.folder_empty"));
            return;
        }

        Identifier itemId = contents.storedItemId().get();
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        ItemStack dummy = new ItemStack(item);
        int extractAmount = Math.min(Math.min(contents.count(), item.getMaxStackSize(dummy)), amount);

        if (extractAmount <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.folder_empty"));
            return;
        }

        ItemStack extractedStack = new ItemStack(item, extractAmount);
        ItemStack updatedFolder = folderStack.copy();
        updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                new FilingFolderItem.FolderContents(contents.storedItemId(), Math.max(0, contents.count() - extractAmount)));

        try (var tx = Transaction.openRoot()) {
            blockEntity.inventory.extract(slot, ItemResource.of(folderStack), 1, tx);
            blockEntity.inventory.insert(slot, ItemResource.of(updatedFolder), 1, tx);
            tx.commit();
        }

        if (!player.getInventory().add(extractedStack)) {
            player.drop(extractedStack, false);
        }

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        blockEntity.setChanged();
    }
}