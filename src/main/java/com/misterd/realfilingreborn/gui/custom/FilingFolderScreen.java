package com.misterd.realfilingreborn.gui.custom;

import com.misterd.realfilingreborn.network.ExtractionPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

public class FilingFolderScreen extends AbstractContainerScreen<FilingFolderMenu> {

    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("realfilingreborn", "textures/gui/assignment_gui.png");

    private static final int EXTRACT_BUTTON_X = 154;
    private static final int EXTRACT_BUTTON_Y = 45;
    private static final int EXTRACT_BUTTON_SIZE = 12;

    public FilingFolderScreen(FilingFolderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 154);
        this.inventoryLabelY = 154 - 94;
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

        boolean hover = isOver(mouseX, mouseY, this.leftPos + EXTRACT_BUTTON_X, this.topPos + EXTRACT_BUTTON_Y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE,
                this.leftPos + EXTRACT_BUTTON_X, this.topPos + EXTRACT_BUTTON_Y,
                hover ? 188.0F : 176.0F, 0.0F,
                EXTRACT_BUTTON_SIZE, EXTRACT_BUTTON_SIZE, 256, 256);

        float scale = 0.8F;
        Component instruction = Component.translatable("gui.realfilingreborn.folder.instruction");
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, instruction,
                (int) ((this.leftPos + (this.imageWidth - this.font.width(instruction) * scale) / 2) / scale),
                (int) ((this.topPos + 23) / scale), 0xFF555555, false);
        graphics.pose().popMatrix();

        Component assignedText = this.menu.getAssignedItemText();
        Component countText = this.menu.getCurrentCountText();

        if (assignedText != null || countText != null) {
            Component line = assignedText != null && countText != null
                    ? assignedText.copy().append(countText)
                    : assignedText != null ? assignedText : countText;
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.text(this.font, line,
                    (int) ((this.leftPos + (this.imageWidth - this.font.width(line) * scale) / 2) / scale),
                    (int) ((this.topPos + 33) / scale), 0xFF555555, false);
            graphics.pose().popMatrix();
        }

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isOver(mouseX, mouseY, this.leftPos + EXTRACT_BUTTON_X, this.topPos + EXTRACT_BUTTON_Y)) {
            graphics.setComponentTooltipForNextFrame(this.font,
                    List.of(Component.translatable("gui.realfilingreborn.extract_items")), mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isOver(event.x(), event.y(), this.leftPos + EXTRACT_BUTTON_X, this.topPos + EXTRACT_BUTTON_Y)) {
            ClientPacketDistributor.sendToServer(new ExtractionPacket(ExtractionPacket.ExtractionType.FOLDER));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + EXTRACT_BUTTON_SIZE && mouseY >= y && mouseY < y + EXTRACT_BUTTON_SIZE;
    }
}