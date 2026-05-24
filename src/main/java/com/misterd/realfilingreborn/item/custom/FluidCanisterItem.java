package com.misterd.realfilingreborn.item.custom;

import com.misterd.realfilingreborn.gui.custom.FluidCanisterMenu;
import com.misterd.realfilingreborn.util.FluidHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Consumer;

public class FluidCanisterItem extends Item {

    public enum CanisterTier {
        BASE(64_000),
        COPPER(512_000),
        IRON(4_096_000),
        GOLD(32_768_000),
        DIAMOND(262_144_000),
        NETHERITE(2_097_152_000);

        private final int capacity;

        CanisterTier(int capacity) {
            this.capacity = capacity;
        }

        public int getCapacity() {
            return capacity;
        }
    }

    private final CanisterTier tier;

    private static final Codec<CanisterContents> CANISTER_CONTENTS_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.optionalFieldOf("storedFluidId").forGetter(CanisterContents::storedFluidId),
                    Codec.INT.fieldOf("amount").forGetter(CanisterContents::amount)
            ).apply(instance, CanisterContents::new));

    public static final StreamCodec<ByteBuf, Identifier> IDENTIFIER_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(Identifier::parse, Identifier::toString);

    private static final StreamCodec<ByteBuf, CanisterContents> CANISTER_CONTENTS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(IDENTIFIER_STREAM_CODEC), CanisterContents::storedFluidId,
                    ByteBufCodecs.INT, CanisterContents::amount,
                    CanisterContents::new);

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "realfilingreborn");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CanisterContents>> CANISTER_CONTENTS =
            DATA_COMPONENTS.register("canister_contents", () ->
                    DataComponentType.<CanisterContents>builder()
                            .persistent(CANISTER_CONTENTS_CODEC)
                            .networkSynchronized(CANISTER_CONTENTS_STREAM_CODEC)
                            .build());

    public FluidCanisterItem(CanisterTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        properties.component(CANISTER_CONTENTS.value(), new CanisterContents(Optional.empty(), 0));
    }

    public int getCapacity() {
        return tier.getCapacity();
    }

    public static int getCapacity(ItemStack stack) {
        if (stack.getItem() instanceof FluidCanisterItem canister) {
            return canister.getCapacity();
        }
        return CanisterTier.BASE.getCapacity();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack canisterStack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                int foundSlot = -1;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i) == canisterStack) {
                        foundSlot = i;
                        break;
                    }
                }
                if (foundSlot != -1) {
                    final int slotIndex = foundSlot;
                    serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inventory, p) -> new FluidCanisterMenu(id, inventory, slotIndex),
                            Component.translatable("gui.realfilingreborn.canister.title")
                    ), buf -> buf.writeInt(slotIndex));
                }
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offhand.isEmpty() || !(offhand.getItem() instanceof BucketItem bucketItem)) {
            return InteractionResult.PASS;
        }

        if (!FluidHelper.isValidFluid(bucketItem.content)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.invalid_fluid"));
            return InteractionResult.PASS;
        }

        if (canisterStack.getCount() > 1) {
            ItemStack singleCanister = canisterStack.copyWithCount(1);
            CanisterContents contents = singleCanister.getOrDefault(CANISTER_CONTENTS.value(), new CanisterContents(Optional.empty(), 0));
            boolean stored = storeFluid(player, singleCanister, offhand, contents);
            if (stored) {
                canisterStack.shrink(1);
                if (!player.getInventory().add(singleCanister)) player.drop(singleCanister, false);
            }
            return InteractionResult.SUCCESS;
        }

        CanisterContents contents = canisterStack.getOrDefault(CANISTER_CONTENTS.value(), new CanisterContents(Optional.empty(), 0));
        canisterStack.set(CANISTER_CONTENTS.value(), contents);
        storeFluid(player, canisterStack, offhand, contents);
        return InteractionResult.SUCCESS;
    }

    private void extractFluid(Player player, ItemStack canisterStack, CanisterContents contents) {
        if (contents == null || contents.storedFluidId().isEmpty() || contents.amount() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.canister_empty"));
            return;
        }

        if (contents.amount() < 1000) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.not_enough_fluid"));
            return;
        }

        Identifier fluidId = contents.storedFluidId().get();
        ItemStack bucketToGive = FluidHelper.getBucketForFluid(fluidId);
        if (bucketToGive.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_bucket_for_fluid"));
            return;
        }

        boolean bucketRemoved = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.BUCKET) {
                stack.shrink(1);
                bucketRemoved = true;
                break;
            }
        }

        if (!bucketRemoved) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.need_empty_bucket"));
            return;
        }

        canisterStack.set(CANISTER_CONTENTS.value(),
                new CanisterContents(contents.storedFluidId(), Math.max(0, contents.amount() - 1000)));
        if (!player.getInventory().add(bucketToGive)) player.drop(bucketToGive, false);
    }

    private boolean storeFluid(Player player, ItemStack canisterStack, ItemStack bucketStack, CanisterContents contents) {
        if (!(bucketStack.getItem() instanceof BucketItem bucketItem)) return false;

        Fluid fluid = bucketItem.content;
        if (!FluidHelper.isValidFluid(fluid)) return false;

        Identifier newFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(fluid));
        Identifier effectiveFluidId;

        if (contents.storedFluidId().isEmpty()) {
            effectiveFluidId = newFluidId;
        } else {
            effectiveFluidId = contents.storedFluidId().get();
            if (!FluidHelper.areFluidsCompatible(effectiveFluidId, newFluidId)) {
                player.sendOverlayMessage(Component.translatable("message.realfilingreborn.wrong_fluid_type",
                        Component.literal(FluidHelper.getFluidDisplayName(effectiveFluidId)).withStyle(ChatFormatting.YELLOW)));
                return false;
            }
        }

        int toAdd = Math.min(1000, getCapacity() - contents.amount());
        if (toAdd <= 0) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.canister_full"));
            return false;
        }

        canisterStack.set(CANISTER_CONTENTS.value(),
                new CanisterContents(Optional.of(effectiveFluidId), contents.amount() + toAdd));
        bucketStack.shrink(1);
        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
        if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket, false);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        CanisterContents contents = stack.get(CANISTER_CONTENTS.value());
        if (contents != null && contents.storedFluidId().isPresent()) {
            String fluidName = FluidHelper.getFluidDisplayName(contents.storedFluidId().get());
            adder.accept(Component.translatable("tooltip.realfilingreborn.stored_fluid",
                            Component.literal(fluidName).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.GRAY));
            if (contents.amount() > 0) {
                int buckets = contents.amount() / 1000;
                int mb = contents.amount() % 1000;
                String amountText = buckets > 0
                        ? (mb > 0 ? buckets + "." + mb / 100 + "B" : buckets + "B")
                        : mb + "mB";
                adder.accept(Component.translatable("tooltip.realfilingreborn.fluid_amount",
                                Component.literal(amountText).withStyle(ChatFormatting.BLUE))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                adder.accept(Component.translatable("tooltip.realfilingreborn.empty_canister")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
        } else {
            adder.accept(Component.translatable("tooltip.realfilingreborn.unregistered_canister")
                    .withStyle(ChatFormatting.GRAY));
        }

        adder.accept(Component.translatable("tooltip.realfilingreborn.canister_capacity",
                        Component.literal(String.format("%,d", getCapacity() / 1000) + "B").withStyle(ChatFormatting.BLUE))
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("tooltip.realfilingreborn.canister_info")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        adder.accept(Component.translatable("tooltip.realfilingreborn.canister_gui_hint")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, display, adder, flag);
    }

    public record CanisterContents(Optional<Identifier> storedFluidId, int amount) {}
}