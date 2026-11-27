package com.misterd.realfilingreborn.client.ber;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
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

public class FilingCabinetBlockEntityRenderer implements BlockEntityRenderer<FilingCabinetBlockEntity, FilingCabinetBlockEntityRenderState> {
    private static final float[][] SINGLE_OFFSETS = {{0.0f, 0.22f}};
    private static final float[][] DOUBLE_OFFSETS = {{0.0f, 0.47f}, {0.0f, -0.03f}};
    private static final float[][] QUAD_OFFSETS = {{-0.25f, 0.47f}, {0.25f, 0.47f}, {-0.25f, -0.03f}, {0.25f, -0.03f}};

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public FilingCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    @Override
    public FilingCabinetBlockEntityRenderState createRenderState() {
        return new FilingCabinetBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(FilingCabinetBlockEntity be, FilingCabinetBlockEntityRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facing = be.getBlockState().getValue(FilingCabinetBlock.FACING);
        state.light = 0xF000F0;

        float[][] offsets = switch (be.getSlotCount()) {
            case 1 -> SINGLE_OFFSETS;
            case 2 -> DOUBLE_OFFSETS;
            default -> QUAD_OFFSETS;
        };
        state.single = be.getSlotCount() == 1;

        for (int slot = 0; slot < state.slots.length; slot++) {
            FilingCabinetBlockEntityRenderState.SlotData sd = state.slots[slot];
            sd.active = false;
            sd.countText = "";
            sd.itemState.clear();
            if (slot >= offsets.length) continue;

            ItemStack folderStack = be.getStack(slot);
            if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) continue;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) continue;

            ItemStack storedItem = new ItemStack(BuiltInRegistries.ITEM.getValue(contents.storedItemId().get()));
            itemModelResolver.updateForTopItem(sd.itemState, storedItem, ItemDisplayContext.GUI, be.getLevel(), null, 0);
            sd.countText = FormattingCache.getFormattedItemCount(contents.count());
            sd.offsetX = offsets[slot][0];
            sd.offsetY = offsets[slot][1];
            sd.active = true;
        }
    }

    @Override
    public void submit(FilingCabinetBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        setupFaceTransform(poseStack, state.facing);

        for (FilingCabinetBlockEntityRenderState.SlotData sd : state.slots) {
            if (!sd.active) continue;

            poseStack.pushPose();
            poseStack.translate(sd.offsetX, -0.32f + sd.offsetY, 0.01f);

            poseStack.pushPose();
            float itemScale = state.single ? 0.5f : 0.26f;
            poseStack.scale(itemScale, itemScale, 0.01f);
            sd.itemState.submit(poseStack, collector, state.light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();

            if (!sd.countText.isEmpty()) {
                poseStack.pushPose();
                float textScale = state.single ? 0.01f : 0.005f;
                poseStack.translate(0.0f, state.single ? -0.27f : -0.14f, 0.001f);
                poseStack.scale(textScale, textScale, textScale);
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