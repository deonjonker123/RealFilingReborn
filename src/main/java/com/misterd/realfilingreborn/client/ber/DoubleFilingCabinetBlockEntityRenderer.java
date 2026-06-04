package com.misterd.realfilingreborn.client.ber;

import com.misterd.realfilingreborn.block.custom.DoubleFilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.DoubleFilingCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.util.FormattingCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DoubleFilingCabinetBlockEntityRenderer implements BlockEntityRenderer<DoubleFilingCabinetBlockEntity, DoubleFilingCabinetBlockEntityRenderState> {

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public DoubleFilingCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    @Override
    public DoubleFilingCabinetBlockEntityRenderState createRenderState() {
        return new DoubleFilingCabinetBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(DoubleFilingCabinetBlockEntity be, DoubleFilingCabinetBlockEntityRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facing = be.getBlockState().getValue(DoubleFilingCabinetBlock.FACING);
        state.light = 0xF000F0;

        for (int slot = 0; slot < 2; slot++) {
            DoubleFilingCabinetBlockEntityRenderState.SlotData sd = state.slots[slot];
            sd.active = false;
            sd.countText = "";
            sd.itemState.clear();

            ItemStack folderStack = be.getStack(slot);
            if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) continue;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) continue;

            ItemStack storedItem = new ItemStack(BuiltInRegistries.ITEM.getValue(contents.storedItemId().get()));
            itemModelResolver.updateForTopItem(sd.itemState, storedItem, ItemDisplayContext.GUI, be.getLevel(), null, 0);
            sd.countText = FormattingCache.getFormattedItemCount(contents.count());

            sd.offsetX = 0.0f;
            sd.offsetY = slot == 0 ? 0.47f : -0.03f;
            sd.active = true;
        }
    }

    @Override
    public void submit(DoubleFilingCabinetBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        setupFaceTransform(poseStack, state.facing);

        for (DoubleFilingCabinetBlockEntityRenderState.SlotData sd : state.slots) {
            if (!sd.active) continue;

            poseStack.pushPose();
            poseStack.translate(sd.offsetX, -0.32f + sd.offsetY, 0.01f);

            poseStack.pushPose();
            poseStack.scale(0.26f, 0.26f, 0.01f);
            sd.itemState.submit(poseStack, collector, state.light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();

            if (!sd.countText.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(0.0f, -0.14f, 0.001f);
                poseStack.scale(0.005f, 0.005f, 0.005f);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
                float xOffset = -font.width(sd.countText) / 2.0f;
                collector.submitText(poseStack, xOffset, 0f,
                        Component.literal(sd.countText).getVisualOrderText(),
                        false, Font.DisplayMode.NORMAL, state.light, 0xFFFFFFFF, 0, 0);
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void setupFaceTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        switch (facing) {
            case NORTH -> poseStack.translate(0.0, 0.10, 0.5);
            case EAST -> { poseStack.translate(-0.5, 0.10, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f)); }
            case SOUTH -> { poseStack.translate(0.0, 0.10, -0.5); poseStack.mulPose(Axis.YP.rotationDegrees(180.0f)); }
            case WEST -> { poseStack.translate(0.5, 0.10, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(90.0f)); }
            default -> {}
        }
        poseStack.translate(0.0, 0.0, -0.03125);
    }
}