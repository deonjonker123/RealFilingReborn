package com.misterd.realfilingreborn.client.ber;

import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FluidCabinetBlockEntityRenderer implements BlockEntityRenderer<FluidCabinetBlockEntity> {

    private static final Map<Fluid, IClientFluidTypeExtensions> FLUID_EXTENSIONS_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITE_CACHE = new ConcurrentHashMap<>();
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Font FONT = MC.font;

    private static final float[][] POSITIONS = {
            {-0.188f,  0.188f},
            { 0.188f,  0.188f},
            {-0.188f, -0.188f},
            { 0.188f, -0.188f}
    };

    public FluidCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FluidCabinetBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) return;

        Direction facing = blockEntity.getBlockState().getValue(FluidCabinetBlock.FACING);

        BlockPos renderPos = blockEntity.getBlockPos().relative(facing);
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(blockEntity.getLevel(), renderPos);

        poseStack.pushPose();
        setupFaceTransform(poseStack, facing);
        renderFluidGrid(blockEntity, poseStack, bufferSource, light);
        poseStack.popPose();
    }

    private void setupFaceTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        switch (facing) {
            case NORTH -> poseStack.translate(0.0, 0.0, 0.475);
            case EAST  -> { poseStack.translate(-0.475, 0.0, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f)); }
            case SOUTH -> { poseStack.translate(0.0, 0.0, -0.475); poseStack.mulPose(Axis.YP.rotationDegrees(180.0f)); }
            case WEST  -> { poseStack.translate(0.475, 0.0, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(90.0f)); }
            default    -> {}
        }
        poseStack.translate(0.0, 0.0, -0.03125);
    }

    private void renderFluidGrid(FluidCabinetBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        int slots = Math.min(4, blockEntity.inventory.getSlots());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack canisterStack = blockEntity.inventory.getStackInSlot(slot);
            if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;

            FluidCanisterItem.CanisterContents contents = canisterStack.get(FluidCanisterItem.CANISTER_CONTENTS.value());
            if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;

            Fluid fluid = FluidHelper.getFluidFromId(contents.storedFluidId().get());
            if (fluid == null || fluid == Fluids.EMPTY) continue;

            float offsetX = POSITIONS[slot][0];
            float offsetY = POSITIONS[slot][1];

            renderFluidQuad(fluid, contents.amount(), canister.getCapacity(), offsetX, offsetY, 0.25f, 0.25f, poseStack, bufferSource, light);
            renderFluidText(formatFluidAmount(contents.amount()), offsetX, offsetY - 0.15f, poseStack, bufferSource, light);
        }
    }

    private static String formatFluidAmount(int amount) {
        int buckets = amount / 1000;
        if (buckets >= 1_000_000) {
            return (buckets / 1_000_000) + "MB";
        } else if (buckets >= 1_000) {
            return (buckets / 1_000) + "KB";
        } else if (buckets > 0) {
            return buckets + "B";
        } else {
            return (amount % 1000) + "mB";
        }
    }

    private void renderFluidQuad(Fluid fluid, int amount, int capacity, float offsetX, float offsetY, float width, float height, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        try {
            IClientFluidTypeExtensions ext = FLUID_EXTENSIONS_CACHE.computeIfAbsent(fluid, IClientFluidTypeExtensions::of);
            ResourceLocation stillTexture = ext.getStillTexture();
            if (stillTexture == null) return;

            TextureAtlasSprite sprite = SPRITE_CACHE.computeIfAbsent(stillTexture,
                    tex -> MC.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(tex));

            int color = ext.getTintColor(new FluidStack(fluid, 1000));
            float r = (color >> 16 & 0xFF) / 255.0f;
            float g = (color >>  8 & 0xFF) / 255.0f;
            float b = (color       & 0xFF) / 255.0f;
            float a = (color >> 24 & 0xFF) / 255.0f;
            if (a == 0.0f) a = 1.0f;

            float fillPct = capacity > 0 ? Math.min((float) amount / capacity, 1.0f) : 0.0f;
            if (fillPct <= 0.0f) return;

            float halfW = width / 2.0f;
            float halfH = height / 2.0f;
            float left   = offsetX - halfW;
            float right  = offsetX + halfW;
            float bottom = offsetY - halfH;
            float top    = bottom + height * fillPct;

            float minU = sprite.getU0(), maxU = sprite.getU1();
            float minV = sprite.getV0(), maxV = sprite.getV1();
            float adjMinV = maxV - (maxV - minV) * fillPct;

            VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
            Matrix4f m = poseStack.last().pose();
            vc.addVertex(m, left,  bottom, 0.001f).setColor(r, g, b, a).setUv(minU, maxV).setLight(light).setNormal(0, 0, 1);
            vc.addVertex(m, right, bottom, 0.001f).setColor(r, g, b, a).setUv(maxU, maxV).setLight(light).setNormal(0, 0, 1);
            vc.addVertex(m, right, top,    0.001f).setColor(r, g, b, a).setUv(maxU, adjMinV).setLight(light).setNormal(0, 0, 1);
            vc.addVertex(m, left,  top,    0.001f).setColor(r, g, b, a).setUv(minU, adjMinV).setLight(light).setNormal(0, 0, 1);

        } catch (Exception e) {
            renderSolidQuad(0.2f, 0.5f, 1.0f, 0.8f, amount, capacity, offsetX, offsetY, width, height, poseStack, bufferSource, light);
        }
    }

    private void renderSolidQuad(float r, float g, float b, float a, int amount, int capacity, float offsetX, float offsetY, float width, float height, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        float fillPct = capacity > 0 ? Math.min((float) amount / capacity, 1.0f) : 0.0f;
        if (fillPct <= 0.0f) return;

        float halfW = width / 2.0f;
        float halfH = height / 2.0f;
        float left   = offsetX - halfW;
        float right  = offsetX + halfW;
        float bottom = offsetY - halfH;
        float top    = bottom + height * fillPct;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f m = poseStack.last().pose();
        vc.addVertex(m, left,  bottom, 0.001f).setColor(r, g, b, a).setUv(0, 1).setLight(light).setNormal(0, 0, 1);
        vc.addVertex(m, right, bottom, 0.001f).setColor(r, g, b, a).setUv(1, 1).setLight(light).setNormal(0, 0, 1);
        vc.addVertex(m, right, top,    0.001f).setColor(r, g, b, a).setUv(1, 0).setLight(light).setNormal(0, 0, 1);
        vc.addVertex(m, left,  top,    0.001f).setColor(r, g, b, a).setUv(0, 0).setLight(light).setNormal(0, 0, 1);
    }

    private void renderFluidText(String text, float offsetX, float offsetY, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (text.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, 0.002f);
        poseStack.scale(0.004f, 0.004f, 0.004f);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        FONT.drawInBatch(text, -FONT.width(text) / 2.0f, 0.0f, 0xFFFFFF, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        poseStack.popPose();
    }
}