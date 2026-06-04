package com.misterd.realfilingreborn.client.renderer;

import com.misterd.realfilingreborn.RealFilingReborn;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.SingleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.DoubleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.component.custom.LedgerData;
import com.misterd.realfilingreborn.item.custom.LedgerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = RealFilingReborn.MODID, value = Dist.CLIENT)
public class LedgerWireframeRenderer {

    private record WireframeBox(AABB aabb, int color) {}

    private static final ContextKey<List<WireframeBox>> WIREFRAME_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath("realfilingreborn", "ledger_wireframe")
    );

    @SubscribeEvent
    public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack ledgerStack = getLedgerStack(mc);
        if (ledgerStack == null) return;

        Level level = event.getLevel();
        LedgerData data = ledgerStack.getOrDefault(RFRDataComponents.LEDGER_DATA.get(), LedgerData.DEFAULT);
        List<WireframeBox> boxes = new ArrayList<>();

        if (data.selectedController() != null) {
            if (level.getBlockEntity(data.selectedController()) instanceof FilingIndexBlockEntity indexEntity) {
                boxes.add(new WireframeBox(new AABB(data.selectedController()), ARGB.colorFromFloat(0.8f, 0f, 1f, 0f)));

                Set<BlockPos> linked = indexEntity.getLinkedCabinets();
                for (BlockPos cabinetPos : linked) {
                    if (isLinkedCabinet(level.getBlockEntity(cabinetPos))) {
                        boxes.add(new WireframeBox(new AABB(cabinetPos), ARGB.colorFromFloat(0.6f, 1f, 1f, 1f)));
                    }
                }

                int range = indexEntity.getRange();
                BlockPos idx = data.selectedController();
                boxes.add(new WireframeBox(new AABB(
                        idx.getX() - range, idx.getY() - range, idx.getZ() - range,
                        idx.getX() + range + 1.0, idx.getY() + range + 1.0, idx.getZ() + range + 1.0
                ), ARGB.colorFromFloat(0.3f, 1f, 1f, 0f)));
            }
        }

        if (data.firstMultiPos() != null && data.selectionMode() == LedgerData.SelectionMode.MULTI) {
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                BlockPos first = data.firstMultiPos();
                BlockPos current = blockHitResult.getBlockPos();
                boxes.add(new WireframeBox(new AABB(
                        Math.min(first.getX(), current.getX()),
                        Math.min(first.getY(), current.getY()),
                        Math.min(first.getZ(), current.getZ()),
                        Math.max(first.getX(), current.getX()) + 1.0,
                        Math.max(first.getY(), current.getY()) + 1.0,
                        Math.max(first.getZ(), current.getZ()) + 1.0
                ), ARGB.colorFromFloat(0.6f, 1f, 1f, 1f)));
            }
        }

        if (!boxes.isEmpty()) event.getRenderState().setRenderData(WIREFRAME_KEY, boxes);
    }

    private static boolean isLinkedCabinet(BlockEntity be) {
        if (be instanceof FilingCabinetBlockEntity cab) return cab.isLinkedToController();
        if (be instanceof SingleFilingCabinetBlockEntity cab) return cab.isLinkedToController();
        if (be instanceof DoubleFilingCabinetBlockEntity cab) return cab.isLinkedToController();
        return false;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        LevelRenderState renderState = event.getLevelRenderState();
        List<WireframeBox> boxes = renderState.getRenderData(WIREFRAME_KEY);
        if (boxes == null || boxes.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        PoseStack poseStack = event.getPoseStack();
        var bufferSource = mc.renderBuffers().bufferSource();
        float lineWidth = mc.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;

        poseStack.pushPose();
        poseStack.translate(-camPos.x(), -camPos.y(), -camPos.z());

        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.lines());
        for (WireframeBox box : boxes) {
            ShapeRenderer.renderShape(
                    poseStack, buffer,
                    Shapes.create(box.aabb()),
                    0.0, 0.0, 0.0,
                    box.color(),
                    lineWidth
            );
        }
        bufferSource.endLastBatch();
        poseStack.popPose();
    }

    private static ItemStack getLedgerStack(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() instanceof LedgerItem) return main;
        ItemStack off = mc.player.getOffhandItem();
        if (off.getItem() instanceof LedgerItem) return off;
        return null;
    }
}