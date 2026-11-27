package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.gui.custom.FilingCabinetMenu;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;

public class FilingCabinetBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final MapCodec<FilingCabinetBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            Codec.INT.fieldOf("slots").forGetter(FilingCabinetBlock::getSlotCount)
    ).apply(instance, FilingCabinetBlock::new));

    private final int slotCount;

    public FilingCabinetBlock(Properties properties, int slotCount) {
        super(properties);
        this.slotCount = slotCount;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public int getSlotCount() {
        return slotCount;
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

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity cabinet)) return InteractionResult.FAIL;

        if (cabinet.hasOverflow()) {
            returnOverflow(cabinet, player);
        }

        Direction facing = state.getValue(FACING);
        boolean hittingFront = hitResult.getDirection() == facing;
        ItemStack heldItem = player.getItemInHand(hand);

        if (!hittingFront) {
            openMenu(cabinet, player, pos, level);
            return InteractionResult.SUCCESS;
        }

        if (player.isCrouching()) {
            return trySneakInteraction(heldItem, cabinet, player, level, pos, hitResult, facing);
        }

        if (heldItem.getItem() instanceof FilingFolderItem) {
            return tryInsertFolder(heldItem, cabinet, player, level, pos, state);
        }

        int slot = getSlotFromHitResult(hitResult, facing);
        if (slot >= 0 && slot < slotCount && depositItem(player, hand, heldItem, slot, cabinet, level, pos)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult trySneakInteraction(ItemStack heldItem, FilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockHitResult hitResult, Direction facing) {
        if (heldItem.isEmpty()) {
            openMenu(cabinet, player, pos, level);
            return InteractionResult.SUCCESS;
        }
        int slot = getSlotFromHitResult(hitResult, facing);
        if (slot >= 0 && slot < slotCount) {
            extractFromSlot(cabinet, slot, Integer.MAX_VALUE, player, level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity cabinet)) return;

        Direction facing = state.getValue(FACING);
        HitResult result = player.pick(player.blockInteractionRange(), 0, false);
        if (!(result instanceof BlockHitResult blockHit)) return;
        if (!blockHit.getBlockPos().equals(pos) || blockHit.getDirection() != facing) return;

        int slot = getSlotFromHitResult(blockHit, facing);
        if (slot < 0 || slot >= slotCount) return;

        int amount = player.isCrouching() ? getMaxStackSizeForSlot(cabinet, slot) : 1;
        extractFromSlot(cabinet, slot, amount, player, level, pos);
    }

    private void returnOverflow(FilingCabinetBlockEntity cabinet, Player player) {
        int given = cabinet.giveOverflowTo(player);
        if (cabinet.hasOverflow()) {
            player.sendSystemMessage(Component.translatable("message.realfilingreborn.overflow_inventory_full", cabinet.getOverflowCount()).withStyle(ChatFormatting.GOLD));
        } else if (given > 0) {
            player.sendSystemMessage(Component.translatable("message.realfilingreborn.overflow_returned", given).withStyle(ChatFormatting.GREEN));
        }
    }

    private InteractionResult tryInsertFolder(ItemStack heldItem, FilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockState state) {
        for (int i = 0; i < slotCount; i++) {
            if (cabinet.getStack(i).isEmpty()) {
                cabinet.setStack(i, heldItem.copyWithCount(1));
                heldItem.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
                level.sendBlockUpdated(pos, state, state, 2);
                return InteractionResult.SUCCESS;
            }
        }
        player.sendOverlayMessage(Component.translatable(switch (slotCount) {
            case 1 -> "message.realfilingreborn.single_folders_full";
            case 2 -> "message.realfilingreborn.double_folders_full";
            default -> "message.realfilingreborn.folders_full";
        }));
        return InteractionResult.SUCCESS;
    }

    private boolean depositItem(Player player, InteractionHand hand, ItemStack heldItem, int slot, FilingCabinetBlockEntity cabinet, Level level, BlockPos pos) {
        boolean doubleClick = cabinet.registerDepositClick(player, level.getGameTime());

        if (doubleClick) {
            if (!heldItem.isEmpty()) {
                depositStackIntoFolder(heldItem, slot, cabinet, player, hand, level, pos);
            }
            return depositFromInventory(player, slot, cabinet, level, pos);
        }

        if (heldItem.isEmpty()) return false;
        return depositStackIntoFolder(heldItem, slot, cabinet, player, hand, level, pos);
    }

    private boolean depositStackIntoFolder(ItemStack heldItem, int slot, FilingCabinetBlockEntity cabinet, Player player, InteractionHand hand, Level level, BlockPos pos) {
        ItemStack folderStack = cabinet.getStack(slot);
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return false;

        if (FilingFolderItem.hasSignificantNBT(heldItem)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.standard_folder_no_nbt").withStyle(ChatFormatting.RED));
            return false;
        }

        FilingFolderItem.FolderContents contents = folderStack.getOrDefault(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(Optional.empty(), 0));
        Identifier itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        if (contents.storedItemId().isPresent() && !contents.storedItemId().get().equals(itemId)) return false;

        int toAdd = Math.min(heldItem.getCount(), folder.getCapacity() - contents.count());
        if (toAdd <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.folder_full"));
            return false;
        }

        setFolderCount(cabinet, slot, itemId, contents.count() + toAdd);
        heldItem.shrink(toAdd);
        if (heldItem.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        playDepositSound(level, pos);
        return true;
    }

    private boolean depositFromInventory(Player player, int slot, FilingCabinetBlockEntity cabinet, Level level, BlockPos pos) {
        ItemStack folderStack = cabinet.getStack(slot);
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return false;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return false;

        Identifier targetId = contents.storedItemId().get();
        int count = contents.count();
        boolean changed = false;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && count < folder.getCapacity(); i++) {
            ItemStack playerStack = inv.getItem(i);
            if (playerStack.isEmpty() || FilingFolderItem.hasSignificantNBT(playerStack)) continue;
            if (!BuiltInRegistries.ITEM.getKey(playerStack.getItem()).equals(targetId)) continue;

            int toAdd = Math.min(playerStack.getCount(), folder.getCapacity() - count);
            inv.removeItem(i, toAdd);
            count += toAdd;
            changed = true;
        }

        if (!changed) return false;
        setFolderCount(cabinet, slot, targetId, count);
        playDepositSound(level, pos);
        return true;
    }

    private void setFolderCount(FilingCabinetBlockEntity cabinet, int slot, Identifier itemId, int count) {
        ItemStack updated = cabinet.getStack(slot).copyWithCount(1);
        updated.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(Optional.of(itemId), count));
        cabinet.setStack(slot, updated);
    }

    private void extractFromSlot(FilingCabinetBlockEntity cabinet, int slot, int amount, Player player, Level level, BlockPos pos) {
        ItemStack folderStack = cabinet.getStack(slot);
        if (!(folderStack.getItem() instanceof FilingFolderItem)) return;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.folder_empty"));
            return;
        }

        Item item = BuiltInRegistries.ITEM.getValue(contents.storedItemId().get());
        int extractAmount = Math.min(Math.min(contents.count(), new ItemStack(item).getMaxStackSize()), amount);
        if (extractAmount <= 0) return;

        setFolderCount(cabinet, slot, contents.storedItemId().get(), contents.count() - extractAmount);

        ItemStack extracted = new ItemStack(item, extractAmount);
        if (!player.getInventory().add(extracted)) {
            player.drop(extracted, false);
        }
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private int getMaxStackSizeForSlot(FilingCabinetBlockEntity cabinet, int slot) {
        FilingFolderItem.FolderContents contents = cabinet.getStack(slot).get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return 1;
        return new ItemStack(BuiltInRegistries.ITEM.getValue(contents.storedItemId().get())).getMaxStackSize();
    }

    private void openMenu(FilingCabinetBlockEntity cabinet, Player player, BlockPos pos, Level level) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, playerEntity) -> new FilingCabinetMenu(id, inventory, cabinet),
                Component.translatable("menu.realfilingreborn.menu_title")), pos);
    }

    private void playDepositSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
    }

    private int getSlotFromHitResult(BlockHitResult hitResult, Direction facing) {
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

        return switch (slotCount) {
            case 1 -> 0;
            case 2 -> relY >= 0.5 ? 0 : 1;
            default -> (relY >= 0.5 ? 0 : 2) + (faceX < 0.5 ? 0 : 1);
        };
    }
}
