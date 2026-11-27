package com.misterd.realfilingreborn.gui.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FilingCabinetScreen extends AbstractContainerScreen<FilingCabinetMenu> {
    private static final ResourceLocation SINGLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("realfilingreborn", "textures/gui/single_cabinet_gui.png");
    private static final ResourceLocation DOUBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("realfilingreborn", "textures/gui/double_cabinet_gui.png");
    private static final ResourceLocation QUAD_TEXTURE = ResourceLocation.fromNamespaceAndPath("realfilingreborn", "textures/gui/quad_cabinet_gui.png");

    private final ResourceLocation guiTexture;

    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    public FilingCabinetScreen(FilingCabinetMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.guiTexture = switch (menu.getCabinetSlotCount()) {
            case 1 -> SINGLE_TEXTURE;
            case 2 -> DOUBLE_TEXTURE;
            default -> QUAD_TEXTURE;
        };
        this.imageHeight = 165;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, guiTexture);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(guiTexture, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}