package com.misterd.realfilingreborn.client.ber;

import com.misterd.realfilingreborn.block.custom.FluidCabinetBlock;
import com.misterd.realfilingreborn.blockentity.custom.FluidCabinetBlockEntity;
import com.misterd.realfilingreborn.item.custom.FluidCanisterItem;
import com.misterd.realfilingreborn.util.FluidHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FluidCabinetBlockEntityRenderer implements BlockEntityRenderer<FluidCabinetBlockEntity, FluidCabinetBlockEntityRenderState> {
    private static final float[][] POSITIONS = {{-0.188f, 0.188f}, {0.188f, 0.188f}, {-0.188f, -0.188f}, {0.188f, -0.188f}};
    private static final float SIZE = 0.25f;
    private static final RenderType FLUID_TYPE = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

    private final Font font;

    public FluidCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public FluidCabinetBlockEntityRenderState createRenderState() {
        return new FluidCabinetBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(FluidCabinetBlockEntity be, FluidCabinetBlockEntityRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facing = be.getBlockState().getValue(FluidCabinetBlock.FACING);
        state.light = 0xF000F0;

        for (int slot = 0; slot < state.slots.length; slot++) {
            FluidCabinetBlockEntityRenderState.SlotData sd = state.slots[slot];
            sd.active = false;

            ItemStack canisterStack = be.getStack(slot);
            if (!(canisterStack.getItem() instanceof FluidCanisterItem canister)) continue;

            FluidCanisterItem.CanisterContents contents = FluidCanisterItem.contentsOf(canisterStack);
            if (contents.storedFluidId().isEmpty() || contents.amount() <= 0) continue;

            Fluid fluid = FluidHelper.getFluidFromId(contents.storedFluidId().get());
            if (!FluidHelper.isValidFluid(fluid)) continue;

            FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
            if (model == null) continue;

            sd.sprite = model.stillMaterial().sprite();
            sd.color = model.fluidTintSource() != null ? model.fluidTintSource().color(fluid.defaultFluidState()) | 0xFF000000 : 0xFFFFFFFF;
            sd.fill = Math.min((float) contents.amount() / canister.getCapacity(), 1.0f);
            sd.amountText = FluidHelper.formatShortAmount(contents.amount());
            sd.active = true;
        }
    }

    @Override
    public void submit(FluidCabinetBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        setupFaceTransform(poseStack, state.facing);

        for (int slot = 0; slot < state.slots.length; slot++) {
            FluidCabinetBlockEntityRenderState.SlotData sd = state.slots[slot];
            if (!sd.active) continue;

            float x = POSITIONS[slot][0];
            float y = POSITIONS[slot][1];
            if (sd.fill > 0f) submitFluidQuad(sd, x, y, state.light, poseStack, collector);

            poseStack.pushPose();
            poseStack.translate(x, y - 0.15f, 0.002f);
            poseStack.scale(0.004f, 0.004f, 0.004f);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            collector.submitText(poseStack, -font.width(sd.amountText) / 2.0f, 0f,
                    Component.literal(sd.amountText).getVisualOrderText(),
                    false, Font.DisplayMode.NORMAL, state.light, 0xFFFFFFFF, 0, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void submitFluidQuad(FluidCabinetBlockEntityRenderState.SlotData sd, float x, float y, int light, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = sd.sprite;
        float left = x - SIZE / 2f;
        float right = x + SIZE / 2f;
        float bottom = y - SIZE / 2f;
        float top = bottom + SIZE * sd.fill;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();
        float vTop = v1 - (v1 - sprite.getV0()) * sd.fill;
        int color = sd.color;

        collector.submitCustomGeometry(poseStack, FLUID_TYPE, (pose, buffer) -> {
            buffer.addVertex(pose, left, bottom, 0.001f).setColor(color).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            buffer.addVertex(pose, right, bottom, 0.001f).setColor(color).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            buffer.addVertex(pose, right, top, 0.001f).setColor(color).setUv(u1, vTop).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            buffer.addVertex(pose, left, top, 0.001f).setColor(color).setUv(u0, vTop).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        });
    }

    private static void setupFaceTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        switch (facing) {
            case NORTH -> poseStack.translate(0.0, 0.0, 0.5);
            case EAST -> { poseStack.translate(-0.5, 0.0, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f)); }
            case SOUTH -> { poseStack.translate(0.0, 0.0, -0.5); poseStack.mulPose(Axis.YP.rotationDegrees(180.0f)); }
            case WEST -> { poseStack.translate(0.5, 0.0, 0.0); poseStack.mulPose(Axis.YP.rotationDegrees(90.0f)); }
            default -> {}
        }
        poseStack.translate(0.0, 0.0, -0.03125);
    }
}
