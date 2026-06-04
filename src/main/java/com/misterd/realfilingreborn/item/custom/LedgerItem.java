package com.misterd.realfilingreborn.item.custom;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.block.custom.SingleFilingCabinetBlock;
import com.misterd.realfilingreborn.block.custom.DoubleFilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.SingleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.DoubleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.component.custom.LedgerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public class LedgerItem extends Item {

    private static final int MAX_SELECTION_SIZE = 1000;
    private static final int MAX_SELECTION_DIMENSION = 32;

    public LedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            toggleOperationMode(stack, player);
        } else {
            toggleSelectionMode(stack, player);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        LedgerData data = getData(stack);

        if (state.getBlock() instanceof FilingIndexBlock && player.isShiftKeyDown()) {
            selectController(stack, pos, player);
            return InteractionResult.SUCCESS;
        }

        boolean isAnyCabinet = state.getBlock() instanceof FilingCabinetBlock
                || state.getBlock() instanceof SingleFilingCabinetBlock
                || state.getBlock() instanceof DoubleFilingCabinetBlock;

        if (isAnyCabinet && player.isShiftKeyDown()) {
            if (data.selectionMode() == LedgerData.SelectionMode.SINGLE) {
                handleSingleCabinetAction(level, pos, stack, player);
            } else {
                handleMultiCabinetAction(level, pos, stack, player);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // --- Helper to get controller pos from any supported BE ---

    private BlockPos getControllerPos(BlockEntity be) {
        if (be instanceof FilingCabinetBlockEntity e) return e.getControllerPos();
        if (be instanceof SingleFilingCabinetBlockEntity e) return e.getControllerPos();
        if (be instanceof DoubleFilingCabinetBlockEntity e) return e.getControllerPos();
        return null;
    }

    private void setControllerPos(BlockEntity be, BlockPos pos) {
        if (be instanceof FilingCabinetBlockEntity e) e.setControllerPos(pos);
        else if (be instanceof SingleFilingCabinetBlockEntity e) e.setControllerPos(pos);
        else if (be instanceof DoubleFilingCabinetBlockEntity e) e.setControllerPos(pos);
    }

    private void clearControllerPos(BlockEntity be) {
        if (be instanceof FilingCabinetBlockEntity e) e.clearControllerPos();
        else if (be instanceof SingleFilingCabinetBlockEntity e) e.clearControllerPos();
        else if (be instanceof DoubleFilingCabinetBlockEntity e) e.clearControllerPos();
    }

    private boolean isSupportedCabinet(BlockEntity be) {
        return be instanceof FilingCabinetBlockEntity
                || be instanceof SingleFilingCabinetBlockEntity
                || be instanceof DoubleFilingCabinetBlockEntity;
    }

    // --- Data helpers ---

    private LedgerData getData(ItemStack stack) {
        return stack.getOrDefault(RFRDataComponents.LEDGER_DATA.get(), LedgerData.DEFAULT);
    }

    private void setData(ItemStack stack, LedgerData data) {
        stack.set(RFRDataComponents.LEDGER_DATA.get(), data);
    }

    // --- Mode toggles ---

    private void toggleOperationMode(ItemStack stack, Player player) {
        LedgerData data = getData(stack);

        LedgerData.OperationMode newMode = data.operationMode() == LedgerData.OperationMode.ADD
                ? LedgerData.OperationMode.REMOVE
                : LedgerData.OperationMode.ADD;

        setData(stack, data.withOperationMode(newMode));

        player.sendOverlayMessage(Component.translatable(
                newMode == LedgerData.OperationMode.ADD
                        ? "item.realfilingreborn.ledger.mode.add"
                        : "item.realfilingreborn.ledger.mode.remove"
        ));
    }

    private void toggleSelectionMode(ItemStack stack, Player player) {
        LedgerData data = getData(stack);

        LedgerData.SelectionMode newMode = data.selectionMode() == LedgerData.SelectionMode.SINGLE
                ? LedgerData.SelectionMode.MULTI
                : LedgerData.SelectionMode.SINGLE;

        setData(stack, data.withSelectionMode(newMode));

        player.sendOverlayMessage(Component.translatable(
                newMode == LedgerData.SelectionMode.SINGLE
                        ? "item.realfilingreborn.ledger.selection.single"
                        : "item.realfilingreborn.ledger.selection.multi"
        ));
    }

    private void selectController(ItemStack stack, BlockPos pos, Player player) {
        LedgerData data = getData(stack);

        setData(stack, data.withSelectedController(pos).withFirstMultiPos(null));

        player.sendOverlayMessage(Component.translatable(
                "item.realfilingreborn.ledger.controller.selected",
                pos.getX(), pos.getY(), pos.getZ()
        ));
    }

    // --- Single cabinet action ---

    private void handleSingleCabinetAction(Level level, BlockPos cabinetPos, ItemStack stack, Player player) {
        if (level.isClientSide()) return;

        LedgerData data = getData(stack);

        if (data.selectedController() == null) {
            player.sendOverlayMessage(Component.translatable("item.realfilingreborn.ledger.error.no_controller"));
            return;
        }

        BlockEntity controllerBE = level.getBlockEntity(data.selectedController());
        if (!(controllerBE instanceof FilingIndexBlockEntity index)) {
            player.sendOverlayMessage(Component.translatable("item.realfilingreborn.ledger.error.no_controller"));
            return;
        }

        BlockEntity cabinetBE = level.getBlockEntity(cabinetPos);
        if (!isSupportedCabinet(cabinetBE)) return;

        if (data.operationMode() == LedgerData.OperationMode.ADD) {
            setControllerPos(cabinetBE, data.selectedController());
            index.addCabinet(cabinetPos);
            player.sendOverlayMessage(Component.translatable("item.realfilingreborn.ledger.cabinet.linked"));
        } else {
            clearControllerPos(cabinetBE);
            index.removeCabinet(cabinetPos);
            player.sendOverlayMessage(Component.translatable("item.realfilingreborn.ledger.cabinet.unlinked"));
        }
    }

    // --- Multi cabinet action ---

    private void handleMultiCabinetAction(Level level, BlockPos pos, ItemStack stack, Player player) {
        if (level.isClientSide()) return;

        LedgerData data = getData(stack);

        if (data.firstMultiPos() == null) {
            setData(stack, data.withFirstMultiPos(pos));
            player.sendOverlayMessage(Component.translatable(
                    "item.realfilingreborn.ledger.multi.start",
                    pos.getX(), pos.getY(), pos.getZ()
            ));
        } else {
            processMultiSelection(level, data.firstMultiPos(), pos, stack, player);
            setData(stack, data.withFirstMultiPos(null));
        }
    }

    private void processMultiSelection(Level level, BlockPos pos1, BlockPos pos2, ItemStack stack, Player player) {
        LedgerData data = getData(stack);

        if (data.selectedController() == null && data.operationMode() == LedgerData.OperationMode.ADD) {
            player.sendOverlayMessage(Component.translatable("item.realfilingreborn.ledger.error.no_controller"));
            return;
        }

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.min(Math.max(pos1.getX(), pos2.getX()), minX + MAX_SELECTION_DIMENSION);
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.min(Math.max(pos1.getY(), pos2.getY()), minY + MAX_SELECTION_DIMENSION);
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.min(Math.max(pos1.getZ(), pos2.getZ()), minZ + MAX_SELECTION_DIMENSION);

        FilingIndexBlockEntity index = null;
        if (data.selectedController() != null &&
                level.getBlockEntity(data.selectedController()) instanceof FilingIndexBlockEntity be) {
            index = be;
        }

        Set<BlockPos> cabinetsToAdd = new LinkedHashSet<>();
        int processedCount = 0;

        for (int x = minX; x <= maxX && processedCount < MAX_SELECTION_SIZE; x++) {
            for (int y = minY; y <= maxY && processedCount < MAX_SELECTION_SIZE; y++) {
                for (int z = minZ; z <= maxZ && processedCount < MAX_SELECTION_SIZE; z++) {

                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockEntity be = level.getBlockEntity(currentPos);
                    if (!isSupportedCabinet(be)) continue;

                    if (data.operationMode() == LedgerData.OperationMode.ADD) {
                        if (index == null) continue;
                        if (!isInRange(data.selectedController(), currentPos,
                                getControllerRange(level, data.selectedController()))) continue;

                        setControllerPos(be, data.selectedController());
                        cabinetsToAdd.add(currentPos);
                    } else {
                        BlockPos oldController = getControllerPos(be);
                        clearControllerPos(be);

                        if (oldController != null &&
                                level.getBlockEntity(oldController) instanceof FilingIndexBlockEntity oldIndex) {
                            oldIndex.removeCabinet(currentPos);
                        }
                    }

                    processedCount++;
                }
            }
        }

        if (index != null) {
            for (BlockPos pos : cabinetsToAdd) {
                index.addCabinet(pos);
            }
        }

        player.sendOverlayMessage(Component.translatable(
                data.operationMode() == LedgerData.OperationMode.ADD
                        ? "item.realfilingreborn.ledger.multi.linked"
                        : "item.realfilingreborn.ledger.multi.unlinked",
                processedCount
        ));
    }

    private int getControllerRange(Level level, BlockPos controllerPos) {
        if (level.getBlockEntity(controllerPos) instanceof FilingIndexBlockEntity index) {
            return index.getRange();
        }
        return 8;
    }

    private boolean isInRange(BlockPos controllerPos, BlockPos cabinetPos, int range) {
        return controllerPos.distSqr(cabinetPos) <= (double) range * range;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {

        LedgerData data = getData(stack);

        adder.accept(Component.translatable("item.realfilingreborn.ledger.subtitle")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        adder.accept(data.operationMode() == LedgerData.OperationMode.ADD
                ? Component.translatable("item.realfilingreborn.ledger.tooltip.operation.add").withStyle(ChatFormatting.GREEN)
                : Component.translatable("item.realfilingreborn.ledger.tooltip.operation.remove").withStyle(ChatFormatting.RED));

        adder.accept(data.selectionMode() == LedgerData.SelectionMode.SINGLE
                ? Component.translatable("item.realfilingreborn.ledger.tooltip.selection.single").withStyle(ChatFormatting.AQUA)
                : Component.translatable("item.realfilingreborn.ledger.tooltip.selection.multi").withStyle(ChatFormatting.AQUA));

        if (data.selectedController() != null) {
            BlockPos c = data.selectedController();
            adder.accept(Component.translatable(
                    "item.realfilingreborn.ledger.tooltip.controller.selected",
                    c.getX(), c.getY(), c.getZ()
            ).withStyle(ChatFormatting.YELLOW));
        } else {
            adder.accept(Component.translatable(
                    "item.realfilingreborn.ledger.tooltip.controller.none"
            ).withStyle(ChatFormatting.GRAY));
        }

        if (data.firstMultiPos() != null) {
            BlockPos m = data.firstMultiPos();
            adder.accept(Component.translatable(
                    "item.realfilingreborn.ledger.tooltip.multi.active",
                    m.getX(), m.getY(), m.getZ()
            ).withStyle(ChatFormatting.GREEN));
        }

        adder.accept(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.selection")
                .withStyle(ChatFormatting.GOLD));
        adder.accept(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.operation")
                .withStyle(ChatFormatting.GREEN));
        adder.accept(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.controller")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.cabinet")
                .withStyle(ChatFormatting.GRAY));
    }
}