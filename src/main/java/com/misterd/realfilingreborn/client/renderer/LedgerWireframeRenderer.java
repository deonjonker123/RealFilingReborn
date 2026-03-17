package com.misterd.realfilingreborn.client.renderer;

import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FilingIndexBlockEntity;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.component.RFRDataComponents;
import com.misterd.realfilingreborn.component.custom.LedgerData;
import com.misterd.realfilingreborn.item.custom.LedgerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Set;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class LedgerWireframeRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack ledgerStack = null;
        if (mainHand.getItem() instanceof LedgerItem) {
            ledgerStack = mainHand;
        } else if (offHand.getItem() instanceof LedgerItem) {
            ledgerStack = offHand;
        }

        if (ledgerStack == null) return;

        LedgerData data = ledgerStack.getOrDefault(RFRDataComponents.LEDGER_DATA.get(), LedgerData.DEFAULT);
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        if (data.selectedController() != null) {
            renderIndexWireframe(poseStack, cameraPos, data.selectedController(), level);

            if (level.getBlockEntity(data.selectedController()) instanceof FilingIndexBlockEntity indexEntity) {
                renderConnectedCabinetsWireframe(poseStack, cameraPos, indexEntity.getLinkedCabinets(), level);
                renderRangeWireframe(poseStack, cameraPos, data.selectedController(), indexEntity.getRange());
            }
        }

        if (data.firstMultiPos() != null && data.selectionMode() == LedgerData.SelectionMode.MULTI) {
            renderMultiSelectionWireframe(poseStack, cameraPos, data.firstMultiPos(), level, minecraft);
        }
    }

    private static void renderIndexWireframe(PoseStack poseStack, Vec3 cameraPos, BlockPos indexPos, Level level) {
        if (level.getBlockEntity(indexPos) instanceof FilingIndexBlockEntity) {
            renderWireframeBox(poseStack, cameraPos, new AABB(indexPos), 0.0F, 1.0F, 0.0F, 0.8F);
        }
    }

    private static void renderConnectedCabinetsWireframe(PoseStack poseStack, Vec3 cameraPos, Set<BlockPos> linkedCabinets, Level level) {
        for (BlockPos cabinetPos : linkedCabinets) {
            boolean isValidCabinet = false;

            if (level.getBlockEntity(cabinetPos) instanceof FilingCabinetBlockEntity cabinet && cabinet.isLinkedToController()) {
                isValidCabinet = true;
            } else if (level.getBlockEntity(cabinetPos) instanceof FluidCabinetBlockEntity fluidCabinet && fluidCabinet.isLinkedToController()) {
                isValidCabinet = true;
            }

            if (isValidCabinet) {
                renderWireframeBox(poseStack, cameraPos, new AABB(cabinetPos), 1.0F, 1.0F, 1.0F, 0.6F);
            }
        }
    }

    private static void renderRangeWireframe(PoseStack poseStack, Vec3 cameraPos, BlockPos indexPos, int range) {
        AABB rangeAABB = new AABB(
                indexPos.getX() - range,       indexPos.getY() - range,       indexPos.getZ() - range,
                indexPos.getX() + range + 1.0, indexPos.getY() + range + 1.0, indexPos.getZ() + range + 1.0
        );
        renderWireframeBox(poseStack, cameraPos, rangeAABB, 1.0F, 1.0F, 0.0F, 0.3F);
    }

    private static void renderMultiSelectionWireframe(PoseStack poseStack, Vec3 cameraPos, BlockPos firstPos, Level level, Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult blockHitResult)) return;

        BlockPos currentPos = blockHitResult.getBlockPos();
        AABB selectionArea = new AABB(
                Math.min(firstPos.getX(), currentPos.getX()),
                Math.min(firstPos.getY(), currentPos.getY()),
                Math.min(firstPos.getZ(), currentPos.getZ()),
                Math.max(firstPos.getX(), currentPos.getX()) + 1.0,
                Math.max(firstPos.getY(), currentPos.getY()) + 1.0,
                Math.max(firstPos.getZ(), currentPos.getZ()) + 1.0
        );
        renderWireframeBox(poseStack, cameraPos, selectionArea, 1.0F, 1.0F, 1.0F, 0.6F);
    }

    private static void renderWireframeBox(PoseStack poseStack, Vec3 cameraPos, AABB aabb, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, buffer, aabb, red, green, blue, alpha);
        bufferSource.endBatch(RenderType.lines());
        poseStack.popPose();
    }
}
