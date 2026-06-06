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
import net.minecraft.world.entity.player.Inventory;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.NonNull;

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
    protected @NonNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SingleFilingCabinetBlockEntity cabinet)) return InteractionResult.FAIL;

        Direction facing = state.getValue(FACING);
        boolean hittingFront = hitResult.getDirection() == facing;
        ItemStack heldItem = player.getItemInHand(hand);

        if (player.isCrouching()) {
            return trySneakItemInteraction(heldItem, cabinet, player, level, pos, state, hitResult, facing, hittingFront);
        }

        if (!hittingFront) {
            return InteractionResult.PASS;
        }

        InteractionResult result = tryItemInteraction(heldItem, cabinet, player, hand, level, pos, state);
        if (result.consumesAction()) {
            return result;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult tryItemInteraction(ItemStack heldItem, SingleFilingCabinetBlockEntity cabinet, Player player, InteractionHand hand, Level level, BlockPos pos, BlockState state) {
        if (heldItem.getItem() instanceof FilingFolderItem) {
            return tryInsertFolder(heldItem, cabinet, player, level, pos, state);
        }

        if (depositItem(player, hand, heldItem, 0, cabinet, level, pos, state)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }

    private InteractionResult trySneakItemInteraction(ItemStack heldItem, SingleFilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockState state, BlockHitResult hitResult, Direction facing, boolean hittingFront) {
        if (hittingFront) {
            if (heldItem.isEmpty()) {
                openMenu(cabinet, (ServerPlayer) player, pos, level);
                return InteractionResult.SUCCESS;
            }
            extractFromSlot(cabinet, 0, Integer.MAX_VALUE, player, level, pos, state);
            return InteractionResult.SUCCESS;
        }

        openMenu(cabinet, (ServerPlayer) player, pos, level);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof SingleFilingCabinetBlockEntity cabinet)) return;

        Direction facing = state.getValue(FACING);
        HitResult result = player.pick(player.blockInteractionRange(), 0, false);
        if (!(result instanceof BlockHitResult blockHit)) return;
        if (!blockHit.getBlockPos().equals(pos) || blockHit.getDirection() != facing) return;

        int amount = player.isCrouching() ? getMaxStackSizeForSlot(cabinet, 0) : 1;

        extractFromSlot(cabinet, 0, amount, player, level, pos, state);
    }

    private boolean depositItem(Player player, InteractionHand hand, ItemStack heldItem, int slot, SingleFilingCabinetBlockEntity cabinet, Level level, BlockPos pos, BlockState state) {
        long gameTime = level.getGameTime();
        boolean doubleClick = gameTime - cabinet.getLastDepositTime() < 10;
        cabinet.setLastDepositTime(gameTime);

        ItemStack folderStack = cabinet.getStack(slot);

        if (doubleClick) {
            if (!heldItem.isEmpty()) {
                depositStackIntoFolder(heldItem, slot, folderStack, cabinet, player, hand, level, pos, state);
                folderStack = cabinet.getStack(slot);
            }
            return depositFromAllOfPlayersInventory(player, slot, folderStack, cabinet, level, pos, state);
        }

        if (heldItem.isEmpty()) {
            return false;
        }

        return depositStackIntoFolder(heldItem, slot, folderStack, cabinet, player, hand, level, pos, state);
    }

    private boolean depositStackIntoFolder(ItemStack heldItem, int slot, ItemStack folderStack, SingleFilingCabinetBlockEntity cabinet, Player player, InteractionHand hand, Level level, BlockPos pos, BlockState state) {
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return false;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null) return false;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

        if (contents.storedItemId().isEmpty()) {
            ItemStack updatedFolder = folderStack.copy();
            updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(Optional.of(itemId), heldItem.getCount()));
            swapFolder(cabinet, slot, folderStack, updatedFolder);
            player.setItemInHand(hand, ItemStack.EMPTY);
            playDepositSound(level, pos);
            cabinet.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
            return true;
        }

        if (contents.storedItemId().get().equals(itemId)) {
            int toAdd = Math.min(heldItem.getCount(), folder.getCapacity() - contents.count());
            if (toAdd <= 0) return false;

            ItemStack updatedFolder = folderStack.copy();
            updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(),
                    new FilingFolderItem.FolderContents(contents.storedItemId(), contents.count() + toAdd));
            swapFolder(cabinet, slot, folderStack, updatedFolder);
            heldItem.shrink(toAdd);
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            playDepositSound(level, pos);
            cabinet.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
            return true;
        }

        return false;
    }

    private boolean depositFromAllOfPlayersInventory(Player player, int slot, ItemStack folderStack, SingleFilingCabinetBlockEntity cabinet, Level level, BlockPos pos, BlockState state) {
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return false;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return false;

        Identifier targetId = contents.storedItemId().get();
        int capacity = folder.getCapacity();
        int[] currentCount = { contents.count() };
        boolean[] changed = { false };

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack playerStack = inv.getItem(i);
            if (playerStack.isEmpty()) continue;

            Identifier stackId = BuiltInRegistries.ITEM.getKey(playerStack.getItem());
            if (!stackId.equals(targetId)) continue;

            int toAdd = Math.min(playerStack.getCount(), capacity - currentCount[0]);
            if (toAdd <= 0) break;

            inv.removeItem(i, toAdd);
            currentCount[0] += toAdd;
            changed[0] = true;
        }

        if (!changed[0]) return false;

        ItemStack updatedFolder = folderStack.copy();
        updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(contents.storedItemId(), currentCount[0]));
        swapFolder(cabinet, slot, folderStack, updatedFolder);
        playDepositSound(level, pos);
        cabinet.setChanged();
        level.sendBlockUpdated(pos, state, state, 2);
        return true;
    }

    private InteractionResult tryInsertFolder(ItemStack heldItem, SingleFilingCabinetBlockEntity cabinet,
                                              Player player, Level level, BlockPos pos, BlockState state) {
        if (cabinet.getStack(0).isEmpty()) {
            try (var tx = Transaction.openRoot()) {
                cabinet.inventory.insert(0, ItemResource.of(heldItem), 1, tx);
                tx.commit();
            }
            heldItem.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
            level.sendBlockUpdated(pos, state, state, 2);
            cabinet.setChanged();
            return InteractionResult.SUCCESS;
        }
        player.sendOverlayMessage(Component.translatable("message.realfilingreborn.single_folders_full"));
        return InteractionResult.SUCCESS;
    }

    private void extractFromSlot(SingleFilingCabinetBlockEntity cabinet, int slot, int amount, Player player, Level level, BlockPos pos, BlockState state) {
        ItemStack folderStack = cabinet.getStack(slot);
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
        updatedFolder.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(contents.storedItemId(), Math.max(0, contents.count() - extractAmount)));

        swapFolder(cabinet, slot, folderStack, updatedFolder);

        if (!player.getInventory().add(extractedStack)) {
            player.drop(extractedStack, false);
        }

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        cabinet.setChanged();
    }

    private int getMaxStackSizeForSlot(SingleFilingCabinetBlockEntity cabinet, int slot) {
        ItemStack folderStack = cabinet.getStack(slot);
        if (!(folderStack.getItem() instanceof FilingFolderItem)) return 1;
        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return 1;
        Item item = BuiltInRegistries.ITEM.getValue(contents.storedItemId().get());
        return item.getMaxStackSize(new ItemStack(item));
    }

    private void swapFolder(SingleFilingCabinetBlockEntity cabinet, int slot, ItemStack oldFolder, ItemStack newFolder) {
        try (var tx = Transaction.openRoot()) {
            cabinet.inventory.extract(slot, ItemResource.of(oldFolder), 1, tx);
            cabinet.inventory.insert(slot, ItemResource.of(newFolder), 1, tx);
            tx.commit();
        }
    }

    private void openMenu(SingleFilingCabinetBlockEntity cabinet, ServerPlayer player, BlockPos pos, Level level) {
        player.openMenu(new SimpleMenuProvider((id, inventory, playerEntity) -> new SingleFilingCabinetMenu(id, inventory, cabinet), Component.translatable("menu.realfilingreborn.menu_title")), pos);
        level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void playDepositSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
    }
}