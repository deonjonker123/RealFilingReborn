package com.misterd.realfilingreborn.client.ber;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class DoubleFilingCabinetBlockEntityRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public int light = 0;
    public final SlotData[] slots = new SlotData[2];

    {
        for (int i = 0; i < 2; i++) slots[i] = new SlotData();
    }

    public static class SlotData {
        public boolean active = false;
        public String countText = "";
        public float offsetX = 0f;
        public float offsetY = 0f;
        public final ItemStackRenderState itemState = new ItemStackRenderState();
    }
}