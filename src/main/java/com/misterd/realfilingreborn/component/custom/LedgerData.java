package com.misterd.realfilingreborn.component.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Optional;

public record LedgerData(
        OperationMode operationMode,
        SelectionMode selectionMode,
        @Nullable BlockPos selectedController,
        @Nullable BlockPos firstMultiPos
) {
    public static final Codec<LedgerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    OperationMode.CODEC.optionalFieldOf("operationMode", OperationMode.ADD).forGetter(LedgerData::operationMode),
                    SelectionMode.CODEC.optionalFieldOf("selectionMode", SelectionMode.SINGLE).forGetter(LedgerData::selectionMode),
                    BlockPos.CODEC.optionalFieldOf("selectedController").forGetter(data -> Optional.ofNullable(data.selectedController())),
                    BlockPos.CODEC.optionalFieldOf("firstMultiPos").forGetter(data -> Optional.ofNullable(data.firstMultiPos()))
            ).apply(instance, (opMode, selMode, controller, multiPos) ->
                    new LedgerData(opMode, selMode, controller.orElse(null), multiPos.orElse(null))));

    public static final LedgerData DEFAULT = new LedgerData(OperationMode.ADD, SelectionMode.SINGLE, null, null);

    public LedgerData withOperationMode(OperationMode operationMode) {
        return new LedgerData(operationMode, selectionMode, selectedController, firstMultiPos);
    }

    public LedgerData withSelectionMode(SelectionMode selectionMode) {
        return new LedgerData(operationMode, selectionMode, selectedController, firstMultiPos);
    }

    public LedgerData withSelectedController(@Nullable BlockPos selectedController) {
        return new LedgerData(operationMode, selectionMode, selectedController, firstMultiPos);
    }

    public LedgerData withFirstMultiPos(@Nullable BlockPos firstMultiPos) {
        return new LedgerData(operationMode, selectionMode, selectedController, firstMultiPos);
    }

    public enum OperationMode {
        ADD, REMOVE;

        public static final Codec<OperationMode> CODEC = Codec.stringResolver(Enum::name, name -> {
            try {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ADD;
            }
        });
    }

    public enum SelectionMode {
        SINGLE, MULTI;

        public static final Codec<SelectionMode> CODEC = Codec.stringResolver(Enum::name, name -> {
            try {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return SINGLE;
            }
        });
    }
}