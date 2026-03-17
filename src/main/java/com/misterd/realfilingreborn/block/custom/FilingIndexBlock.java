package com.misterd.realfilingreborn.block.custom;

import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.component.custom.LedgerData;
import com.misterd.realfilingreborn.item.custom.LedgerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class FilingIndexBlock extends BaseEntityBlock {

    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final MapCodec<FilingIndexBlock> CODEC = simpleCodec(FilingIndexBlock::new);
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public FilingIndexBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CONNECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED);
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof FilingIndexBlockEntity index) {
                index.clearAllLinkedCabinets();
                clearControllerFromNearbyLedgers(level, pos);
                index.drops();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.controller.cleared"), true);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FilingIndexBlockEntity index) {
            ((ServerPlayer) player).openMenu(new SimpleMenuProvider(index,
                    Component.translatable("menu.realfilingreborn.filing_index")), pos);
        }
        return ItemInteractionResult.SUCCESS;
    }
}