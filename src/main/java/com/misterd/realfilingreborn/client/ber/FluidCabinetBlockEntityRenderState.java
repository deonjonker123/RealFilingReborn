package com.misterd.realfilingreborn.client.ber;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public class FluidCabinetBlockEntityRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public int light = 0;
    public final SlotData[] slots = new SlotData[4];

    {
        for (int i = 0; i < slots.length; i++) slots[i] = new SlotData();
    }

    public static class SlotData {
        public boolean active = false;
        public TextureAtlasSprite sprite = null;
        public int color = 0xFFFFFFFF;
        public float fill = 0f;
        public String amountText = "";
    }
}
