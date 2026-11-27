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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
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

    private static final Codec<CanisterContents> CANISTER_CONTENTS_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.optionalFieldOf("storedFluidId").forGetter(CanisterContents::storedFluidId),
                    Codec.INT.fieldOf("amount").forGetter(CanisterContents::amount)
            ).apply(instance, CanisterContents::new));

    private static final StreamCodec<ByteBuf, CanisterContents> CANISTER_CONTENTS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(FilingFolderItem.IDENTIFIER_STREAM_CODEC), CanisterContents::storedFluidId,
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

    private final CanisterTier tier;

    public FluidCanisterItem(CanisterTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getCapacity() {
        return tier.getCapacity();
    }

    public static int getCapacity(ItemStack stack) {
        if (stack.getItem() instanceof FluidCanisterItem canister) return canister.getCapacity();
        return CanisterTier.BASE.getCapacity();
    }

    public static CanisterContents contentsOf(ItemStack stack) {
        return stack.getOrDefault(CANISTER_CONTENTS.value(), new CanisterContents(Optional.empty(), 0));
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
        if (offhand.isEmpty() || !(offhand.getItem() instanceof BucketItem bucketItem)) return InteractionResult.PASS;

        if (!FluidHelper.isValidFluid(bucketItem.content)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.invalid_fluid"));
            return InteractionResult.PASS;
        }

        if (canisterStack.getCount() > 1) {
            ItemStack single = canisterStack.copyWithCount(1);
            if (storeFluid(player, single, offhand)) {
                canisterStack.shrink(1);
                if (!player.getInventory().add(single)) player.drop(single, false);
            }
            return InteractionResult.SUCCESS;
        }

        storeFluid(player, canisterStack, offhand);
        return InteractionResult.SUCCESS;
    }

    private boolean storeFluid(Player player, ItemStack canisterStack, ItemStack bucketStack) {
        if (!(bucketStack.getItem() instanceof BucketItem bucketItem)) return false;
        Fluid fluid = bucketItem.content;
        if (!FluidHelper.isValidFluid(fluid)) return false;

        CanisterContents contents = contentsOf(canisterStack);
        Identifier newFluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(fluid));
        if (contents.storedFluidId().isPresent() && !FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), newFluidId)) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.wrong_fluid_type",
                    Component.literal(FluidHelper.getFluidDisplayName(contents.storedFluidId().get())).withStyle(ChatFormatting.YELLOW)));
            return false;
        }
        if (getCapacity() - contents.amount() < 1000) {
            player.sendOverlayMessage(Component.translatable("message.realfilingreborn.canister_full"));
            return false;
        }

        canisterStack.set(CANISTER_CONTENTS.value(), new CanisterContents(Optional.of(contents.storedFluidId().orElse(newFluidId)), contents.amount() + 1000));
        bucketStack.shrink(1);
        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
        if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket, false);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack canisterStack, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY || carried.isEmpty() || !slot.allowModification(player)) return false;
        if (!(carried.getItem() instanceof BucketItem bucketItem)) return false;

        boolean server = !player.level().isClientSide();
        CanisterContents contents = contentsOf(canisterStack);

        if (bucketItem.content == Fluids.EMPTY) {
            if (contents.storedFluidId().isEmpty() || contents.amount() < 1000) {
                if (server) player.sendOverlayMessage(Component.translatable("message.realfilingreborn.not_enough_fluid"));
                return true;
            }
            ItemStack filled = FluidHelper.getBucketForFluid(contents.storedFluidId().get());
            if (filled.isEmpty()) {
                if (server) player.sendOverlayMessage(Component.translatable("message.realfilingreborn.no_bucket_for_fluid"));
                return true;
            }
            ItemStack updated = canisterStack.copyWithCount(1);
            updated.set(CANISTER_CONTENTS.value(), new CanisterContents(contents.storedFluidId(), contents.amount() - 1000));
            FilingFolderItem.replaceOne(canisterStack, updated, slot, player);
            swapCarried(carried, filled, carriedAccess, player);
            player.playSound(SoundEvents.BUCKET_FILL, 0.8F, 1.0F);
            return true;
        }

        if (!FluidHelper.isValidFluid(bucketItem.content)) return false;
        Identifier fluidId = FluidHelper.getStillFluid(FluidHelper.getFluidId(bucketItem.content));

        if (contents.storedFluidId().isPresent() && !FluidHelper.areFluidsCompatible(contents.storedFluidId().get(), fluidId)) {
            if (server) player.sendOverlayMessage(Component.translatable("message.realfilingreborn.wrong_fluid_type",
                    Component.literal(FluidHelper.getFluidDisplayName(contents.storedFluidId().get())).withStyle(ChatFormatting.YELLOW)));
            return true;
        }
        if (getCapacity() - contents.amount() < 1000) {
            if (server) player.sendOverlayMessage(Component.translatable("message.realfilingreborn.canister_full"));
            return true;
        }

        ItemStack updated = canisterStack.copyWithCount(1);
        updated.set(CANISTER_CONTENTS.value(), new CanisterContents(Optional.of(contents.storedFluidId().orElse(fluidId)), contents.amount() + 1000));
        FilingFolderItem.replaceOne(canisterStack, updated, slot, player);
        swapCarried(carried, new ItemStack(Items.BUCKET), carriedAccess, player);
        player.playSound(SoundEvents.BUCKET_EMPTY, 0.8F, 1.0F);
        return true;
    }

    private static void swapCarried(ItemStack carried, ItemStack result, SlotAccess carriedAccess, Player player) {
        if (carried.getCount() <= 1) {
            carriedAccess.set(result);
            return;
        }
        carried.shrink(1);
        if (!player.getInventory().add(result) && !player.level().isClientSide()) {
            player.drop(result, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        CanisterContents contents = stack.get(CANISTER_CONTENTS.value());
        if (contents != null && contents.storedFluidId().isPresent()) {
            adder.accept(Component.translatable("tooltip.realfilingreborn.stored_fluid",
                            Component.literal(FluidHelper.getFluidDisplayName(contents.storedFluidId().get())).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.GRAY));
            if (contents.amount() > 0) {
                adder.accept(Component.translatable("tooltip.realfilingreborn.fluid_amount",
                                Component.literal(FluidHelper.formatAmount(contents.amount())).withStyle(ChatFormatting.BLUE))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                adder.accept(Component.translatable("tooltip.realfilingreborn.empty_canister").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
        } else {
            adder.accept(Component.translatable("tooltip.realfilingreborn.unregistered_canister").withStyle(ChatFormatting.GRAY));
        }

        adder.accept(Component.translatable("tooltip.realfilingreborn.canister_capacity",
                        Component.literal(String.format("%,d", getCapacity() / 1000) + "B").withStyle(ChatFormatting.BLUE))
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("tooltip.realfilingreborn.canister_info").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        adder.accept(Component.translatable("tooltip.realfilingreborn.canister_gui_hint").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, display, adder, flag);
    }

    public record CanisterContents(Optional<Identifier> storedFluidId, int amount) {}
}
