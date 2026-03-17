package com.misterd.realfilingreborn.client.ber;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.util.FormattingCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class FilingCabinetBlockEntityRenderer implements BlockEntityRenderer<FilingCabinetBlockEntity> {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Font FONT = MC.font;

    public FilingCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FilingCabinetBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) return;

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(FilingCabinetBlock.FACING);

        BlockPos renderPos = blockEntity.getBlockPos().relative(facing);
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(blockEntity.getLevel(), renderPos);

        poseStack.pushPose();
        setupFaceTransform(poseStack, facing);

        for (int slot = 0; slot < blockEntity.inventory.getSlots(); slot++) {
            ItemStack folderStack = blockEntity.inventory.getStackInSlot(slot);
            if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) continue;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) continue;

            ItemStack storedItem = new ItemStack(BuiltInRegistries.ITEM.get(contents.storedItemId().get()));
            String countText = FormattingCache.getFormattedItemCount(contents.count());
            float offsetX = (slot - 2) * 0.15f;

            renderSlotContent(storedItem, countText, offsetX, poseStack, bufferSource, light);
        }

        poseStack.popPose();
    }

    private void setupFaceTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        switch (facing) {
            case NORTH -> poseStack.translate(0.0, 0.025, 0.475);
            case EAST  -> { poseStack.translate(-0.475, 0.025, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f)); }
            case SOUTH -> { poseStack.translate(0.0, 0.025, -0.475); poseStack.mulPose(Axis.YP.rotationDegrees(180.0f)); }
            case WEST  -> { poseStack.translate(0.475, 0.025, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(90.0f)); }
            default    -> {}
        }
        poseStack.translate(0.0, 0.0, -0.03125);
    }

    private void renderSlotContent(ItemStack stack, String countText, float offsetX, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(offsetX, -0.2f, 0.0f);

        poseStack.pushPose();
        poseStack.scale(0.15f, 0.15f, 0.15f);
        MC.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
        poseStack.popPose();

        if (!countText.isEmpty()) {
            renderText(countText, poseStack, bufferSource, packedLight);
        }

        poseStack.popPose();
    }

    private void renderText(String text, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0f, -0.08f, 0.001f);
        poseStack.scale(0.004f, 0.0045f, 0.004f);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        float xOffset = -FONT.width(text) / 2.0f;
        FONT.drawInBatch(text, xOffset, 0.0f, 0xFFFFFF, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }
}
