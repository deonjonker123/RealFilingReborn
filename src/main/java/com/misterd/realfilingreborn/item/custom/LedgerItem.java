package com.misterd.realfilingreborn.item.custom;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.block.custom.FilingIndexBlock;
import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.component.custom.LedgerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class LedgerItem extends Item {

    private static final int MAX_SELECTION_SIZE = 1000;
    private static final int MAX_SELECTION_DIMENSION = 32;

    public LedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            toggleOperationMode(stack, player);
        } else {
            toggleSelectionMode(stack, player);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        LedgerData data = stack.getOrDefault(RFRDataComponents.LEDGER_DATA.get(), LedgerData.DEFAULT);

        if (state.getBlock() instanceof FilingIndexBlock && player.isShiftKeyDown()) {
            selectController(stack, pos, player);
            return InteractionResult.SUCCESS;
        }

        if ((state.getBlock() instanceof FilingCabinetBlock || state.getBlock() instanceof FluidCabinetBlock) && player.isShiftKeyDown()) {
            if (data.selectionMode() == LedgerData.SelectionMode.SINGLE) {
                handleSingleCabinetAction(level, pos, stack, player);
            } else {
                handleMultiCabinetAction(level, pos, stack, player);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private LedgerData getData(ItemStack stack) {
        return stack.getOrDefault(RFRDataComponents.LEDGER_DATA.get(), LedgerData.DEFAULT);
    }

    private void setData(ItemStack stack, LedgerData data) {
        stack.set(RFRDataComponents.LEDGER_DATA.get(), data);
    }

    private void toggleOperationMode(ItemStack stack, Player player) {
        LedgerData data = getData(stack);
        LedgerData.OperationMode newMode = data.operationMode() == LedgerData.OperationMode.ADD
                ? LedgerData.OperationMode.REMOVE : LedgerData.OperationMode.ADD;
        setData(stack, data.withOperationMode(newMode));
        player.displayClientMessage(Component.translatable(newMode == LedgerData.OperationMode.ADD
                ? "item.realfilingreborn.ledger.mode.add"
                : "item.realfilingreborn.ledger.mode.remove"), true);
    }

    private void toggleSelectionMode(ItemStack stack, Player player) {
        LedgerData data = getData(stack);
        LedgerData.SelectionMode newMode = data.selectionMode() == LedgerData.SelectionMode.SINGLE
                ? LedgerData.SelectionMode.MULTI : LedgerData.SelectionMode.SINGLE;
        setData(stack, data.withSelectionMode(newMode));
        player.displayClientMessage(Component.translatable(newMode == LedgerData.SelectionMode.SINGLE
                ? "item.realfilingreborn.ledger.selection.single"
                : "item.realfilingreborn.ledger.selection.multi"), true);
    }

    private void selectController(ItemStack stack, BlockPos controllerPos, Player player) {
        LedgerData data = getData(stack);
        setData(stack, data.withSelectedController(controllerPos));
        player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.controller.selected",
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()), true);
    }

    private void handleSingleCabinetAction(Level level, BlockPos cabinetPos, ItemStack stack, Player player) {
        LedgerData data = getData(stack);

        if (data.selectedController() == null) {
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.error.no_controller"), true);
            return;
        }

        if (!(level.getBlockEntity(data.selectedController()) instanceof FilingIndexBlockEntity indexEntity)) {
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.error.no_controller"), true);
            return;
        }

        if (!isInRange(data.selectedController(), cabinetPos, indexEntity.getRange())) {
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.cabinet.out_of_range"), true);
            return;
        }

        boolean adding = data.operationMode() == LedgerData.OperationMode.ADD;
        BlockEntity be = level.getBlockEntity(cabinetPos);

        if (be instanceof FilingCabinetBlockEntity cabinet) {
            linkOrUnlink(level, cabinetPos, cabinet::setControllerPos, cabinet::getControllerPos, cabinet::clearControllerPos, indexEntity, adding, player);
        } else if (be instanceof FluidCabinetBlockEntity fluidCabinet) {
            linkOrUnlink(level, cabinetPos, fluidCabinet::setControllerPos, fluidCabinet::getControllerPos, fluidCabinet::clearControllerPos, indexEntity, adding, player);
        }
    }

    private void linkOrUnlink(Level level, BlockPos cabinetPos,
                              java.util.function.Consumer<BlockPos> setController,
                              java.util.function.Supplier<BlockPos> getController,
                              Runnable clearController,
                              FilingIndexBlockEntity indexEntity,
                              boolean adding, Player player) {
        if (adding) {
            setController.accept(indexEntity.getBlockPos());
            indexEntity.addCabinet(cabinetPos);
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.cabinet.linked"), true);
        } else {
            BlockPos oldControllerPos = getController.get();
            clearController.run();
            if (oldControllerPos != null && level.getBlockEntity(oldControllerPos) instanceof FilingIndexBlockEntity oldIndex) {
                oldIndex.removeCabinet(cabinetPos);
            }
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.cabinet.unlinked"), true);
        }
    }

    private void handleMultiCabinetAction(Level level, BlockPos cabinetPos, ItemStack stack, Player player) {
        LedgerData data = getData(stack);
        if (data.firstMultiPos() == null) {
            setData(stack, data.withFirstMultiPos(cabinetPos));
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.multi.start",
                    cabinetPos.getX(), cabinetPos.getY(), cabinetPos.getZ()), true);
        } else {
            processMultiSelection(level, data.firstMultiPos(), cabinetPos, stack, player);
            setData(stack, data.withFirstMultiPos(null));
        }
    }

    private void processMultiSelection(Level level, BlockPos pos1, BlockPos pos2, ItemStack stack, Player player) {
        LedgerData data = getData(stack);

        if (data.selectedController() == null && data.operationMode() == LedgerData.OperationMode.ADD) {
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.error.no_controller"), true);
            return;
        }

        int minX = Math.min(pos1.getX(), pos2.getX()), maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY()), maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ()), maxZ = Math.max(pos1.getZ(), pos2.getZ());
        int sizeX = maxX - minX + 1, sizeY = maxY - minY + 1, sizeZ = maxZ - minZ + 1;

        if (sizeX > MAX_SELECTION_DIMENSION || sizeY > MAX_SELECTION_DIMENSION || sizeZ > MAX_SELECTION_DIMENSION) {
            player.displayClientMessage(Component.literal("Selection too large! Maximum size: " + MAX_SELECTION_DIMENSION + " blocks per dimension")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        long totalBlocks = (long) sizeX * sizeY * sizeZ;
        if (totalBlocks > MAX_SELECTION_SIZE) {
            player.displayClientMessage(Component.literal("Selection too large! Maximum total blocks: " + MAX_SELECTION_SIZE)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        boolean adding = data.operationMode() == LedgerData.OperationMode.ADD;

        FilingIndexBlockEntity indexEntity = null;
        if (data.selectedController() != null &&
                level.getBlockEntity(data.selectedController()) instanceof FilingIndexBlockEntity idx) {
            indexEntity = idx;
        }

        // For REMOVE mode, scan nearby controllers
        Map<BlockPos, FilingIndexBlockEntity> controllerLookup = new HashMap<>();
        if (!adding) {
            int searchRadius = Math.min(64, Math.max(sizeX, Math.max(sizeY, sizeZ)) * 2);
            BlockPos center = new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
            for (int dx = -searchRadius; dx <= searchRadius; dx += 8) {
                for (int dy = -searchRadius; dy <= searchRadius; dy += 8) {
                    for (int dz = -searchRadius; dz <= searchRadius; dz += 8) {
                        BlockPos checkPos = center.offset(dx, dy, dz);
                        if (level.getBlockEntity(checkPos) instanceof FilingIndexBlockEntity ctrl) {
                            controllerLookup.put(checkPos, ctrl);
                        }
                    }
                }
            }
        }

        Set<BlockPos> cabinetsToAdd = new HashSet<>();
        Set<BlockPos> cabinetsToRemove = new HashSet<>();
        int processedCount = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(currentPos);
                    if (!(state.getBlock() instanceof FilingCabinetBlock) && !(state.getBlock() instanceof FluidCabinetBlock)) continue;
                    if (adding && (indexEntity == null || !isInRange(data.selectedController(), currentPos, indexEntity.getRange()))) continue;

                    BlockEntity be = level.getBlockEntity(currentPos);
                    boolean processed = false;

                    if (be instanceof FilingCabinetBlockEntity cabinet) {
                        if (adding) {
                            cabinet.setControllerPos(data.selectedController());
                            cabinetsToAdd.add(currentPos);
                        } else {
                            BlockPos old = cabinet.getControllerPos();
                            cabinet.clearControllerPos();
                            if (old != null && controllerLookup.containsKey(old)) cabinetsToRemove.add(currentPos);
                        }
                        processed = true;
                    } else if (be instanceof FluidCabinetBlockEntity fluidCabinet) {
                        if (adding) {
                            fluidCabinet.setControllerPos(data.selectedController());
                            cabinetsToAdd.add(currentPos);
                        } else {
                            BlockPos old = fluidCabinet.getControllerPos();
                            fluidCabinet.clearControllerPos();
                            if (old != null && controllerLookup.containsKey(old)) cabinetsToRemove.add(currentPos);
                        }
                        processed = true;
                    }

                    if (processed) processedCount++;
                }
            }
        }

        if (!cabinetsToAdd.isEmpty() && indexEntity != null) {
            indexEntity.addCabinets(cabinetsToAdd);
        }

        for (BlockPos cabinetPos : cabinetsToRemove) {
            for (FilingIndexBlockEntity ctrl : controllerLookup.values()) {
                if (ctrl.getLinkedCabinets().contains(cabinetPos)) {
                    ctrl.removeCabinet(cabinetPos);
                    break;
                }
            }
        }

        player.displayClientMessage(Component.translatable(adding
                ? "item.realfilingreborn.ledger.multi.linked"
                : "item.realfilingreborn.ledger.multi.unlinked", processedCount), true);
    }

    private boolean isInRange(BlockPos controllerPos, BlockPos cabinetPos, int range) {
        return controllerPos.distSqr(cabinetPos) <= (double) range * range;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LedgerData data = getData(stack);

        tooltip.add(Component.translatable("item.realfilingreborn.ledger.subtitle").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(data.operationMode() == LedgerData.OperationMode.ADD
                ? Component.translatable("item.realfilingreborn.ledger.tooltip.operation.add").withStyle(ChatFormatting.GREEN)
                : Component.translatable("item.realfilingreborn.ledger.tooltip.operation.remove").withStyle(ChatFormatting.RED));
        tooltip.add(data.selectionMode() == LedgerData.SelectionMode.SINGLE
                ? Component.translatable("item.realfilingreborn.ledger.tooltip.selection.single").withStyle(ChatFormatting.AQUA)
                : Component.translatable("item.realfilingreborn.ledger.tooltip.selection.multi").withStyle(ChatFormatting.LIGHT_PURPLE));

        if (data.selectedController() != null) {
            tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.controller.selected",
                            data.selectedController().getX(), data.selectedController().getY(), data.selectedController().getZ())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.controller.none").withStyle(ChatFormatting.RED));
        }

        if (data.firstMultiPos() != null) {
            tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.multi.active",
                            data.firstMultiPos().getX(), data.firstMultiPos().getY(), data.firstMultiPos().getZ())
                    .withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.selection").withColor(11184810));
        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.operation").withColor(11184810));
        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.controller").withColor(11184810));
        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.cabinet").withColor(11184810));
    }
}
