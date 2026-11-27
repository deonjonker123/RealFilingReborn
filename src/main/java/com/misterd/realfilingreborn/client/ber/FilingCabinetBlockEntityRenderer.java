package com.misterd.realfilingreborn.client.ber;

import com.misterd.realfilingreborn.block.custom.FilingCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FilingCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FilingFolderItem;
import com.misterd.realfilingreborn.util.FormattingCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FilingCabinetBlockEntityRenderer implements BlockEntityRenderer<FilingCabinetBlockEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;

    private record Layout(float itemScale, float textOffsetY, float textScale, float[][] offsets) {}

    private static final Layout SINGLE = new Layout(0.5f, -0.27f, 0.01f, new float[][]{{0.0f, 0.22f}});
    private static final Layout DOUBLE = new Layout(0.26f, -0.14f, 0.005f, new float[][]{{0.0f, 0.47f}, {0.0f, -0.03f}});
    private static final Layout QUAD = new Layout(0.26f, -0.14f, 0.005f,
            new float[][]{{-0.25f, 0.47f}, {0.25f, 0.47f}, {-0.25f, -0.03f}, {0.25f, -0.03f}});

    private final ItemRenderer itemRenderer;
    private final Font font;

    public FilingCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    private static Layout layoutFor(int slots) {
        return switch (slots) {
            case 1 -> SINGLE;
            case 2 -> DOUBLE;
            default -> QUAD;
        };
    }

    @Override
    public void render(FilingCabinetBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) return;

        Direction facing = blockEntity.getBlockState().getValue(FilingCabinetBlock.FACING);
        Layout layout = layoutFor(blockEntity.getSlotCount());

        poseStack.pushPose();
        setupFaceTransform(poseStack, facing);

        int slots = Math.min(blockEntity.inventory.getSlots(), layout.offsets().length);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack folderStack = blockEntity.inventory.getStackInSlot(slot);
            if (folderStack.isEmpty() || !(folderStack.getItem() instanceof FilingFolderItem)) continue;

            FilingFolderItem.FolderContents contents = folderStack.get(FilingFolderItem.FOLDER_CONTENTS.value());
            if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) continue;

            ItemStack storedItem = new ItemStack(BuiltInRegistries.ITEM.get(contents.storedItemId().get()));
            String countText = FormattingCache.getFormattedItemCount(contents.count());
            float[] offset = layout.offsets()[slot];

            poseStack.pushPose();
            poseStack.translate(offset[0], -0.32f + offset[1], 0.01f);

            poseStack.pushPose();
            poseStack.scale(layout.itemScale(), layout.itemScale(), 0.01f);
            itemRenderer.renderStatic(storedItem, ItemDisplayContext.GUI, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource, blockEntity.getLevel(), 0);
            poseStack.popPose();

            if (!countText.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(0.0f, layout.textOffsetY(), 0.001f);
                poseStack.scale(layout.textScale(), layout.textScale(), layout.textScale());
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
                float xOffset = -font.width(countText) / 2.0f;
                font.drawInBatch(countText, xOffset, 0.0f, 0xFFFFFFFF, false, poseStack.last().pose(),
                        bufferSource, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
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
