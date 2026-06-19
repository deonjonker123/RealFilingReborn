package com.misterd.realfilingreborn.client.renderer;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.SingleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.DoubleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.component.custom.LedgerData;
import com.misterd.realfilingreborn.item.custom.LedgerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = RealFilingReborn.MODID, value = Dist.CLIENT)
public class LedgerWireframeRenderer {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack ledgerStack = getLedgerStack(mc);
        if (ledgerStack == null) return;

        Level level = mc.level;
        if (level == null) return;

        LedgerData data = ledgerStack.getOrDefault(RFRDataComponents.LEDGER_DATA.get(), LedgerData.DEFAULT);

        if (data.selectedController() != null) {
            if (level.getBlockEntity(data.selectedController()) instanceof FilingIndexBlockEntity indexEntity) {
                Gizmos.cuboid(new AABB(data.selectedController()),
                        GizmoStyle.stroke(ARGB.colorFromFloat(0.8f, 0f, 1f, 0f)));

                Set<BlockPos> linked = indexEntity.getLinkedCabinets();
                for (BlockPos cabinetPos : linked) {
                    if (isLinkedCabinet(level.getBlockEntity(cabinetPos))) {
                        Gizmos.cuboid(new AABB(cabinetPos),
                                GizmoStyle.stroke(ARGB.colorFromFloat(0.6f, 1f, 1f, 1f)));
                    }
                }

                int range = indexEntity.getRange();
                BlockPos idx = data.selectedController();
                AABB rangeBox = new AABB(
                        idx.getX() - range, idx.getY() - range, idx.getZ() - range,
                        idx.getX() + range + 1.0, idx.getY() + range + 1.0, idx.getZ() + range + 1.0
                );
                Gizmos.cuboid(rangeBox, GizmoStyle.stroke(ARGB.colorFromFloat(0.3f, 1f, 1f, 0f)));
            }
        }

        if (data.firstMultiPos() != null && data.selectionMode() == LedgerData.SelectionMode.MULTI) {
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                BlockPos first = data.firstMultiPos();
                BlockPos current = blockHitResult.getBlockPos();
                AABB previewBox = new AABB(
                        Math.min(first.getX(), current.getX()),
                        Math.min(first.getY(), current.getY()),
                        Math.min(first.getZ(), current.getZ()),
                        Math.max(first.getX(), current.getX()) + 1.0,
                        Math.max(first.getY(), current.getY()) + 1.0,
                        Math.max(first.getZ(), current.getZ()) + 1.0
                );
                Gizmos.cuboid(previewBox, GizmoStyle.stroke(ARGB.colorFromFloat(0.6f, 1f, 1f, 1f)));
            }
        }
    }

    private static boolean isLinkedCabinet(BlockEntity be) {
        if (be instanceof FilingCabinetBlockEntity cab) return cab.isLinkedToController();
        if (be instanceof SingleFilingCabinetBlockEntity cab) return cab.isLinkedToController();
        if (be instanceof DoubleFilingCabinetBlockEntity cab) return cab.isLinkedToController();
        return false;
    }

    private static ItemStack getLedgerStack(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() instanceof LedgerItem) return main;
        ItemStack off = mc.player.getOffhandItem();
        if (off.getItem() instanceof LedgerItem) return off;
        return null;
    }
}