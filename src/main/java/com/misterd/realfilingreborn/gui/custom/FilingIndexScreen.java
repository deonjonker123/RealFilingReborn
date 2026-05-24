package com.misterd.realfilingreborn.gui.custom;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FilingIndexScreen extends AbstractContainerScreen<FilingIndexMenu> {
    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("realfilingreborn", "textures/gui/filing_index_gui.png");

    public FilingIndexScreen(FilingIndexMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 145);
        this.inventoryLabelY = 145 - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE,
                this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }
}