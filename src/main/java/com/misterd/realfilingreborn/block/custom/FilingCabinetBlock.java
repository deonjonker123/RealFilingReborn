package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;

public class FilingCabinetBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING;
    public static final VoxelShape SHAPE;
    public static final MapCodec<FilingCabinetBlock> CODEC;

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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FilingCabinetBlockEntity cabinet)) return ItemInteractionResult.FAIL;

        if (cabinet.hasOverflow()) {
            returnOverflow(cabinet, player);
        }

        Direction facing = state.getValue(FACING);
        boolean hittingFront = hitResult.getDirection() == facing;
        ItemStack heldItem = player.getItemInHand(hand);

        if (!hittingFront) {
            openMenu(cabinet, player, pos, level);
            return ItemInteractionResult.SUCCESS;
        }

        if (player.isCrouching()) {
            return trySneakInteraction(heldItem, cabinet, player, level, pos, hitResult, facing);
        }

        if (heldItem.getItem() instanceof FilingFolderItem) {
            return tryInsertFolder(heldItem, cabinet, player, level, pos, state);
        }

        int slot = getSlotFromHitResult(hitResult, facing);
        if (slot >= 0 && slot < slotCount && depositItem(player, hand, heldItem, slot, cabinet, level, pos)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.CONSUME;
    }

    private ItemInteractionResult trySneakInteraction(ItemStack heldItem, FilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockHitResult hitResult, Direction facing) {
        if (heldItem.isEmpty()) {
            openMenu(cabinet, player, pos, level);
            return ItemInteractionResult.SUCCESS;
        }
        int slot = getSlotFromHitResult(hitResult, facing);
        if (slot >= 0 && slot < slotCount) {
            extractFromSlot(cabinet, slot, Integer.MAX_VALUE, player, level, pos);
        }
        return ItemInteractionResult.SUCCESS;
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
            player.displayClientMessage(Component.translatable("message.realfilingreborn.overflow_inventory_full", cabinet.getOverflowCount()).withStyle(ChatFormatting.GOLD), false);
        } else if (given > 0) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.overflow_returned", given).withStyle(ChatFormatting.GREEN), false);
        }
    }

    private ItemInteractionResult tryInsertFolder(ItemStack heldItem, FilingCabinetBlockEntity cabinet, Player player, Level level, BlockPos pos, BlockState state) {
        for (int i = 0; i < slotCount; i++) {
            if (cabinet.inventory.getStackInSlot(i).isEmpty()) {
                cabinet.inventory.setStackInSlot(i, heldItem.copyWithCount(1));
                heldItem.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 2.0F);
                level.sendBlockUpdated(pos, state, state, 2);
                return ItemInteractionResult.SUCCESS;
            }
        }
        player.displayClientMessage(Component.translatable(switch (slotCount) {
            case 1 -> "message.realfilingreborn.single_folders_full";
            case 2 -> "message.realfilingreborn.double_folders_full";
            default -> "message.realfilingreborn.folders_full";
        }), true);
        return ItemInteractionResult.SUCCESS;
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
        ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return false;

        if (FilingFolderItem.hasSignificantNBT(heldItem)) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.standard_folder_no_nbt").withStyle(ChatFormatting.RED), true);
            return false;
        }

        FilingFolderItem.FolderContents contents = folderStack.getOrDefault(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(Optional.empty(), 0));
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        if (contents.storedItemId().isPresent() && !contents.storedItemId().get().equals(itemId)) return false;

        int toAdd = Math.min(heldItem.getCount(), folder.getCapacity() - contents.count());
        if (toAdd <= 0) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.folder_full"), true);
            return false;
        }

        setFolderCount(cabinet, slot, itemId, contents.count() + toAdd);
        heldItem.shrink(toAdd);
        if (heldItem.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        playDepositSound(level, pos);
        return true;
    }

    private boolean depositFromInventory(Player player, int slot, FilingCabinetBlockEntity cabinet, Level level, BlockPos pos) {
        ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
        if (!(folderStack.getItem() instanceof FilingFolderItem folder)) return false;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return false;

        ResourceLocation targetId = contents.storedItemId().get();
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

    private void setFolderCount(FilingCabinetBlockEntity cabinet, int slot, ResourceLocation itemId, int count) {
        ItemStack updated = cabinet.inventory.getStackInSlot(slot).copyWithCount(1);
        updated.set(FilingFolderItem.FOLDER_CONTENTS.value(), new FilingFolderItem.FolderContents(Optional.of(itemId), count));
        cabinet.inventory.setStackInSlot(slot, updated);
    }

    private void extractFromSlot(FilingCabinetBlockEntity cabinet, int slot, int amount, Player player, Level level, BlockPos pos) {
        ItemStack folderStack = cabinet.inventory.getStackInSlot(slot);
        if (!(folderStack.getItem() instanceof FilingFolderItem)) return;

        FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.folder_empty"), true);
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
        int extractAmount = Math.min(Math.min(contents.count(), item.getDefaultMaxStackSize()), amount);
        if (extractAmount <= 0) return;

        setFolderCount(cabinet, slot, contents.storedItemId().get(), contents.count() - extractAmount);

        ItemStack extracted = new ItemStack(item, extractAmount);
        if (!player.getInventory().add(extracted)) {
            player.drop(extracted, false);
        }
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private int getMaxStackSizeForSlot(FilingCabinetBlockEntity cabinet, int slot) {
        FilingFolderItem.FolderContents contents = cabinet.inventory.getStackInSlot(slot).get(FilingFolderItem.FOLDER_CONTENTS.value());
        if (contents == null || contents.storedItemId().isEmpty()) return 1;
        return BuiltInRegistries.ITEM.get(contents.storedItemId().get()).getDefaultMaxStackSize();
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

    static {
        FACING = HorizontalDirectionalBlock.FACING;
        SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
        CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                propertiesCodec(),
                Codec.INT.fieldOf("slots").forGetter(FilingCabinetBlock::getSlotCount)
        ).apply(instance, FilingCabinetBlock::new));
    }
}