package com.misterd.realfilingreborn.network;

import com.misterd.realfilingreborn.gui.custom.FilingFolderMenu;
import com.misterd.realfilingreborn.gui.custom.FluidCanisterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExtractionPacket(ExtractionType extractionType) implements CustomPacketPayload {

    public static final Type<ExtractionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("realfilingreborn", "extraction"));

    public static final StreamCodec<FriendlyByteBuf, ExtractionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt),
                    ExtractionPacket::typeOrdinal,
                    ExtractionPacket::fromOrdinal);

    public int typeOrdinal() {
        return extractionType.ordinal();
    }

    public static ExtractionPacket fromOrdinal(int ordinal) {
        return new ExtractionPacket(ExtractionType.values()[ordinal]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ExtractionPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

        AbstractContainerMenu menu = serverPlayer.containerMenu;
        switch (packet.extractionType) {
            case FOLDER   -> { if (menu instanceof FilingFolderMenu m)  m.extractItems(); }
            case CANISTER -> { if (menu instanceof FluidCanisterMenu m) m.extractFluid(); }
        }
    }

    public enum ExtractionType {
        FOLDER,
        CANISTER
    }
}
