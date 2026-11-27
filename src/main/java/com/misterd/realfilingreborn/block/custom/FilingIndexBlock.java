package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.component.custom.LedgerData;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.item.custom.LedgerItem;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FilingIndexBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final MapCodec<FilingIndexBlock> CODEC = simpleCodec(FilingIndexBlock::new);
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public FilingIndexBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CONNECTED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilingIndexBlockEntity(pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FilingIndexBlockEntity index) {
            index.performScheduledUpdate();
        }
    }

    public static void updateConnectedState(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockState currentState = level.getBlockState(pos);
        if (!(currentState.getBlock() instanceof FilingIndexBlock)) return;
        if (!(level.getBlockEntity(pos) instanceof FilingIndexBlockEntity index)) return;
        boolean hasConnections = index.getLinkedCabinetCount() > 0;
        if (hasConnections != currentState.getValue(CONNECTED)) {
            level.setBlock(pos, currentState.setValue(CONNECTED, hasConnections), 3);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        clearControllerFromNearbyLedgers(level, pos);
        level.updateNeighbourForOutputSignal(pos, this);
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    private void clearControllerFromNearbyLedgers(Level level, BlockPos controllerPos) {
        if (level.isClientSide()) return;
        level.players().forEach(player -> {
            if (player.distanceToSqr(controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()) <= 4096.0) {
                clearControllerFromPlayerLedgers(player, controllerPos);
            }
        });
    }

    private void clearControllerFromPlayerLedgers(Player player, BlockPos controllerPos) {
        clearControllerFromLedger(player.getMainHandItem(), controllerPos, player);
        clearControllerFromLedger(player.getOffhandItem(), controllerPos, player);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            clearControllerFromLedger(player.getInventory().getItem(i), controllerPos, player);
        }
    }

    private void clearControllerFromLedger(ItemStack ledgerStack, BlockPos controllerPos, Player player) {
        if (!(ledgerStack.getItem() instanceof LedgerItem)) return;
        LedgerData data = ledgerStack.getOrDefault(RFRDataComponents.LEDGER_DATA.get(), LedgerData.DEFAULT);
        if (data.selectedController() != null && data.selectedController().equals(controllerPos)) {
            ledgerStack.set(RFRDataComponents.LEDGER_DATA.get(), data.withSelectedController(null));
            player.sendOverlayMessage(Component.translatable("item.realfilingreborn.ledger.controller.cleared"));
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FilingIndexBlockEntity index))
            return InteractionResult.FAIL;

        if (player.isCrouching()) {
            openMenu(index, (ServerPlayer) player, pos, level);
            return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        depositItem(player, hand, heldItem, index, level, pos);

        return InteractionResult.SUCCESS;
    }

    private boolean depositItem(Player player, InteractionHand hand, ItemStack heldItem, FilingIndexBlockEntity index, Level level, BlockPos pos) {
        boolean doubleClick = index.registerDepositClick(player, level.getGameTime());
        ResourceHandler<ItemResource> network = index.getCapabilityHandler(null);
        if (network == null) return false;

        if (doubleClick) {
            boolean deposited = false;
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack playerStack = inv.getItem(i);
                if (playerStack.isEmpty() || FilingFolderItem.hasSignificantNBT(playerStack)) continue;
                deposited |= insertInto(network, playerStack) > 0;
            }
            if (deposited) level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
            return deposited;
        }

        if (heldItem.isEmpty()) return false;
        if (FilingFolderItem.hasSignificantNBT(heldItem)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.standard_folder_no_nbt").withStyle(ChatFormatting.RED));
            return false;
        }
        if (insertInto(network, heldItem) <= 0) return false;
        if (heldItem.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.5F);
        return true;
    }

    private int insertInto(ResourceHandler<ItemResource> network, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = network.insert(0, ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            stack.shrink(inserted);
            return inserted;
        }
    }

    private void openMenu(FilingIndexBlockEntity index, ServerPlayer player, BlockPos pos, Level level) {
        player.openMenu(new SimpleMenuProvider(index, Component.translatable("menu.realfilingreborn.filing_index")), pos);
    }
}