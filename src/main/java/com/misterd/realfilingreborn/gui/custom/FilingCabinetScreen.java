package com.misterd.realfilingreborn.gui.custom;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FilingCabinetScreen extends AbstractContainerScreen<FilingCabinetMenu> {
    private static final Identifier SINGLE_TEXTURE = Identifier.fromNamespaceAndPath("realfilingreborn", "textures/gui/single_cabinet_gui.png");
    private static final Identifier DOUBLE_TEXTURE = Identifier.fromNamespaceAndPath("realfilingreborn", "textures/gui/double_cabinet_gui.png");
    private static final Identifier QUAD_TEXTURE = Identifier.fromNamespaceAndPath("realfilingreborn", "textures/gui/quad_cabinet_gui.png");

    private final Identifier guiTexture;

    public FilingCabinetScreen(FilingCabinetMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 165);
        this.guiTexture = switch (menu.getCabinetSlotCount()) {
            case 1 -> SINGLE_TEXTURE;
            case 2 -> DOUBLE_TEXTURE;
            default -> QUAD_TEXTURE;
        };
        this.inventoryLabelY = 165 - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, guiTexture,
                this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }
}