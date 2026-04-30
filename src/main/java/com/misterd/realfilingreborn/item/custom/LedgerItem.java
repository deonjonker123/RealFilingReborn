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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
        LedgerData data = getData(stack);

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

    private void selectController(ItemStack stack, BlockPos pos, Player player) {
        LedgerData data = getData(stack);
        setData(stack, data.withSelectedController(pos).withFirstMultiPos(null));
        player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.controller.selected",
                pos.getX(), pos.getY(), pos.getZ()), true);
    }

    private void handleSingleCabinetAction(Level level, BlockPos cabinetPos, ItemStack stack, Player player) {
        if (level.isClientSide()) return;

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
        if (level.isClientSide()) return;

        LedgerData data = getData(stack);
        if (data.firstMultiPos() == null) {
            setData(stack, data.withFirstMultiPos(cabinetPos));
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.multi.start",
                    cabinetPos.getX(), cabinetPos.getY(), cabinetPos.getZ()), true);
            return;
        }

        BlockPos first = data.firstMultiPos();
        BlockPos second = cabinetPos;
        setData(stack, data.withFirstMultiPos(null));

        if (data.selectedController() == null && data.operationMode() == LedgerData.OperationMode.ADD) {
            player.displayClientMessage(Component.translatable("item.realfilingreborn.ledger.error.no_controller"), true);
            return;
        }

        FilingIndexBlockEntity indexEntity = null;
        if (data.selectedController() != null &&
                level.getBlockEntity(data.selectedController()) instanceof FilingIndexBlockEntity idx) {
            indexEntity = idx;
        }

        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());

        // Clamp to dimension limit
        maxX = Math.min(maxX, minX + MAX_SELECTION_DIMENSION);
        maxY = Math.min(maxY, minY + MAX_SELECTION_DIMENSION);
        maxZ = Math.min(maxZ, minZ + MAX_SELECTION_DIMENSION);

        boolean adding = data.operationMode() == LedgerData.OperationMode.ADD;

        Set<BlockPos> itemCabinetsToAdd = new LinkedHashSet<>();
        Set<BlockPos> fluidCabinetsToAdd = new LinkedHashSet<>();
        Set<BlockPos> cabinetsToRemove = new LinkedHashSet<>();
        Map<BlockPos, FilingIndexBlockEntity> controllerLookup = new HashMap<>();
        int processedCount = 0;

        for (int x = minX; x <= maxX && processedCount < MAX_SELECTION_SIZE; x++) {
            for (int y = minY; y <= maxY && processedCount < MAX_SELECTION_SIZE; y++) {
                for (int z = minZ; z <= maxZ && processedCount < MAX_SELECTION_SIZE; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(currentPos);

                    boolean isItemCabinet = state.getBlock() instanceof FilingCabinetBlock;
                    boolean isFluidCabinet = state.getBlock() instanceof FluidCabinetBlock;
                    if (!isItemCabinet && !isFluidCabinet) continue;

                    // Bug 5 fix: when adding, validate that the cabinet type is compatible
                    // with the index. Filing Index only manages FilingCabinetBlockEntity for
                    // item storage; fluid cabinets link separately. We allow both to be linked
                    // to the same index (the index tracks them all for range/connected state)
                    // but we flag a mismatch if the player tries to link a fluid cabinet
                    // when the first selected block was an item cabinet, or vice versa.
                    // Concretely: item cabinets and fluid cabinets are both valid to link,
                    // but we skip blocks that don't have a matching BE to avoid slot count corruption.
                    BlockEntity be = level.getBlockEntity(currentPos);
                    if (be == null) continue;
                    if (isItemCabinet && !(be instanceof FilingCabinetBlockEntity)) continue;
                    if (isFluidCabinet && !(be instanceof FluidCabinetBlockEntity)) continue;

                    if (adding) {
                        if (indexEntity == null || !isInRange(data.selectedController(), currentPos, indexEntity.getRange())) continue;

                        if (be instanceof FilingCabinetBlockEntity cabinet) {
                            cabinet.setControllerPos(data.selectedController());
                            itemCabinetsToAdd.add(currentPos);
                        } else if (be instanceof FluidCabinetBlockEntity fluidCabinet) {
                            fluidCabinet.setControllerPos(data.selectedController());
                            fluidCabinetsToAdd.add(currentPos);
                        }
                    } else {
                        BlockPos oldController = null;
                        if (be instanceof FilingCabinetBlockEntity cabinet) {
                            oldController = cabinet.getControllerPos();
                            cabinet.clearControllerPos();
                        } else if (be instanceof FluidCabinetBlockEntity fluidCabinet) {
                            oldController = fluidCabinet.getControllerPos();
                            fluidCabinet.clearControllerPos();
                        }
                        if (oldController != null) {
                            cabinetsToRemove.add(currentPos);
                            if (!controllerLookup.containsKey(oldController) &&
                                    level.getBlockEntity(oldController) instanceof FilingIndexBlockEntity ctrl) {
                                controllerLookup.put(oldController, ctrl);
                            }
                        }
                    }
                    processedCount++;
                }
            }
        }

        if (adding && indexEntity != null) {
            Set<BlockPos> allToAdd = new LinkedHashSet<>();
            allToAdd.addAll(itemCabinetsToAdd);
            allToAdd.addAll(fluidCabinetsToAdd);
            if (!allToAdd.isEmpty()) indexEntity.addCabinets(allToAdd);
        }

        if (!cabinetsToRemove.isEmpty()) {
            for (BlockPos removedPos : cabinetsToRemove) {
                for (FilingIndexBlockEntity ctrl : controllerLookup.values()) {
                    if (ctrl.getLinkedCabinets().contains(removedPos)) {
                        ctrl.removeCabinet(removedPos);
                        break;
                    }
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
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        LedgerData data = getData(stack);

        tooltip.add(Component.translatable("item.realfilingreborn.ledger.subtitle").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(data.operationMode() == LedgerData.OperationMode.ADD
                ? Component.translatable("item.realfilingreborn.ledger.tooltip.operation.add").withStyle(ChatFormatting.GREEN)
                : Component.translatable("item.realfilingreborn.ledger.tooltip.operation.remove").withStyle(ChatFormatting.RED));
        tooltip.add(data.selectionMode() == LedgerData.SelectionMode.SINGLE
                ? Component.translatable("item.realfilingreborn.ledger.tooltip.selection.single").withStyle(ChatFormatting.AQUA)
                : Component.translatable("item.realfilingreborn.ledger.tooltip.selection.multi").withStyle(ChatFormatting.AQUA));

        if (data.selectedController() != null) {
            BlockPos c = data.selectedController();
            tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.controller.selected",
                    c.getX(), c.getY(), c.getZ()).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.controller.none").withStyle(ChatFormatting.GRAY));
        }

        if (data.firstMultiPos() != null) {
            BlockPos m = data.firstMultiPos();
            tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.multi.active",
                    m.getX(), m.getY(), m.getZ()).withStyle(ChatFormatting.WHITE));
        }

        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.selection").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.operation").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.controller").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.realfilingreborn.ledger.tooltip.usage.cabinet").withStyle(ChatFormatting.GRAY));
    }
}