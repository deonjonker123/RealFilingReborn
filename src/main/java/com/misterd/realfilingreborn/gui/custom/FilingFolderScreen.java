package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.network.ExtractionPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class FilingFolderScreen extends AbstractContainerScreen<FilingFolderMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("realfilingreborn", "textures/gui/assignment_gui.png");

    private static final int EXTRACT_BUTTON_X = 154;
    private static final int EXTRACT_BUTTON_Y = 45;
    private static final int EXTRACT_BUTTON_SIZE = 12;

    public FilingFolderScreen(FilingFolderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 154;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        boolean hover = isMouseOverButton(mouseX, mouseY, x + EXTRACT_BUTTON_X, y + EXTRACT_BUTTON_Y, EXTRACT_BUTTON_SIZE, EXTRACT_BUTTON_SIZE);
        renderExtractButton(guiGraphics, x + EXTRACT_BUTTON_X, y + EXTRACT_BUTTON_Y, hover);
    }

    private void renderExtractButton(GuiGraphics guiGraphics, int x, int y, boolean hover) {
        guiGraphics.blit(GUI_TEXTURE, x, y, hover ? 188 : 176, 0, EXTRACT_BUTTON_SIZE, EXTRACT_BUTTON_SIZE);
    }

    private boolean isMouseOverButton(double mouseX, double mouseY, int buttonX, int buttonY, int width, int height) {
        return mouseX >= buttonX && mouseX < buttonX + width && mouseY >= buttonY && mouseY < buttonY + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (isMouseOverButton(mouseX, mouseY, x + EXTRACT_BUTTON_X, y + EXTRACT_BUTTON_Y, EXTRACT_BUTTON_SIZE, EXTRACT_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new ExtractionPacket(ExtractionPacket.ExtractionType.FOLDER), new CustomPacketPayload[0]);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (isMouseOverButton(mouseX, mouseY, x + EXTRACT_BUTTON_X, y + EXTRACT_BUTTON_Y, EXTRACT_BUTTON_SIZE, EXTRACT_BUTTON_SIZE)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.realfilingreborn.extract_items"), mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        Component instruction = Component.translatable("gui.realfilingreborn.folder.instruction");
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8F, 0.8F, 1.0F);
        int scaledX = (int)(((float) this.imageWidth - (float) this.font.width(instruction) * 0.8F) / 2.0F / 0.8F);
        guiGraphics.drawString(this.font, instruction, scaledX, 25, 10588695, false);
        guiGraphics.pose().popPose();

        Component countText = this.menu.getCurrentCountText();
        if (countText != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.8F, 0.8F, 1.0F);
            int scaledCountX = (int)(((float) this.imageWidth - (float) this.font.width(countText) * 0.8F) / 2.0F / 0.8F);
            guiGraphics.drawString(this.font, countText, scaledCountX, 37, 4210752, false);
            guiGraphics.pose().popPose();
        }
    }
}
