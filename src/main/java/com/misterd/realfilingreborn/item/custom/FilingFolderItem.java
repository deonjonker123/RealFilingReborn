package com.misterd.realfilingreborn.item.custom;

import com.misterd.realfilingreborn.Config;
import com.misterd.realfilingreborn.gui.custom.FilingFolderMenu;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public class FilingFolderItem extends Item {

    private static final Codec<FolderContents> FOLDER_CONTENTS_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("storedItemId").forGetter(FolderContents::storedItemId),
                    Codec.INT.fieldOf("count").forGetter(FolderContents::count)
            ).apply(instance, FolderContents::new));

    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString);

    private static final StreamCodec<ByteBuf, FolderContents> FOLDER_CONTENTS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(RESOURCE_LOCATION_STREAM_CODEC), FolderContents::storedItemId,
                    ByteBufCodecs.INT, FolderContents::count,
                    FolderContents::new);

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "realfilingreborn");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FolderContents>> FOLDER_CONTENTS =
            DATA_COMPONENTS.register("folder_contents", () ->
                    DataComponentType.<FolderContents>builder()
                            .persistent(FOLDER_CONTENTS_CODEC)
                            .networkSynchronized(FOLDER_CONTENTS_STREAM_CODEC)
                            .build());

    public FilingFolderItem(Properties properties) {
        super(properties);
        properties.component(FOLDER_CONTENTS.value(), new FolderContents(Optional.empty(), 0));
    }

    public static boolean hasSignificantNBT(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isDamaged()) return true;

        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) return true;

        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) return true;

        if (stack.get(DataComponents.CUSTOM_NAME) != null) return true;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) return true;

        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        return potion != null && (!potion.customEffects().isEmpty() || !potion.potion().isEmpty());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack folderStack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(folderStack);

        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                int foundSlot = -1;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i) == folderStack) {
                        foundSlot = i;
                        break;
                    }
                }
                if (foundSlot != -1) {
                    final int slotIndex = foundSlot;
                    serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inventory, p) -> new FilingFolderMenu(id, inventory, slotIndex),
                            Component.translatable("gui.realfilingreborn.folder.title")
                    ), buf -> buf.writeInt(slotIndex));
                }
            }
            return InteractionResultHolder.success(folderStack);
        }

        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (offhand.isEmpty() || offhand.getItem() instanceof FilingFolderItem) {
            if (!offhand.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.realfilingreborn.no_folder_ception"), true);
            }
            return InteractionResultHolder.pass(folderStack);
        }

        if (hasSignificantNBT(offhand)) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.standard_folder_no_nbt"), true);
            return InteractionResultHolder.fail(folderStack);
        }

        if (folderStack.getCount() > 1) {
            ItemStack singleFolder = folderStack.copyWithCount(1);
            FolderContents contents = singleFolder.getOrDefault(FOLDER_CONTENTS.value(), new FolderContents(Optional.empty(), 0));
            InteractionResultHolder<ItemStack> result = storeItems(level, player, singleFolder, offhand, contents);
            folderStack.shrink(1);
            ItemStack modified = result.getObject();
            if (!player.getInventory().add(modified)) {
                player.drop(modified, false);
            }
            return InteractionResultHolder.success(folderStack);
        }

        FolderContents contents = folderStack.getOrDefault(FOLDER_CONTENTS.value(), new FolderContents(Optional.empty(), 0));
        folderStack.set(FOLDER_CONTENTS.value(), contents);
        return storeItems(level, player, folderStack, offhand, contents);
    }

    private InteractionResultHolder<ItemStack> extractItems(Level level, Player player, ItemStack folderStack, FolderContents contents) {
        if (contents == null || contents.storedItemId().isEmpty() || contents.count() <= 0) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.folder_empty"), true);
            return InteractionResultHolder.fail(folderStack);
        }

        ResourceLocation itemId = contents.storedItemId().get();
        Item item = BuiltInRegistries.ITEM.get(itemId);
        ItemStack dummy = new ItemStack(item);
        int extractAmount = Math.min(Math.min(contents.count(), item.getMaxStackSize(dummy)), 64);
        ItemStack extracted = new ItemStack(item, extractAmount);

        folderStack.set(FOLDER_CONTENTS.value(),
                new FolderContents(contents.storedItemId(), Math.max(0, contents.count() - extractAmount)));

        if (!player.getInventory().add(extracted)) {
            player.drop(extracted, false);
        }
        return InteractionResultHolder.success(folderStack);
    }

    private InteractionResultHolder<ItemStack> storeItems(Level level, Player player, ItemStack folderStack, ItemStack itemToStore, FolderContents contents) {
        if (itemToStore.isEmpty() || itemToStore.getItem() instanceof FilingFolderItem) {
            return InteractionResultHolder.pass(folderStack);
        }
        if (hasSignificantNBT(itemToStore)) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.standard_folder_no_nbt"), true);
            return InteractionResultHolder.fail(folderStack);
        }

        ResourceLocation newItemId = BuiltInRegistries.ITEM.getKey(itemToStore.getItem());
        ResourceLocation effectiveItemId;

        if (contents.storedItemId().isEmpty()) {
            effectiveItemId = newItemId;
        } else {
            effectiveItemId = contents.storedItemId().get();
            if (!effectiveItemId.equals(newItemId)) {
                Item storedItem = BuiltInRegistries.ITEM.get(effectiveItemId);
                player.displayClientMessage(Component.translatable("message.realfilingreborn.wrong_item_type",
                        storedItem.getDescription().copy().withStyle(ChatFormatting.YELLOW)), true);
                return InteractionResultHolder.fail(folderStack);
            }
        }

        int toAdd = Math.min(itemToStore.getCount(), Config.getMaxFolderStorage() - contents.count());
        if (toAdd <= 0) {
            player.displayClientMessage(Component.translatable("message.realfilingreborn.folder_full"), true);
            return InteractionResultHolder.fail(folderStack);
        }

        folderStack.set(FOLDER_CONTENTS.value(),
                new FolderContents(Optional.of(effectiveItemId), contents.count() + toAdd));
        itemToStore.shrink(toAdd);
        return InteractionResultHolder.success(folderStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FolderContents contents = stack.get(FOLDER_CONTENTS.value());
        if (contents != null && contents.storedItemId().isPresent()) {
            Item item = BuiltInRegistries.ITEM.get(contents.storedItemId().get());
            tooltip.add(Component.translatable("tooltip.realfilingreborn.stored_item",
                            Component.literal(item.getDescription().getString()).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY));
            if (contents.count() > 0) {
                tooltip.add(Component.translatable("tooltip.realfilingreborn.item_count",
                                Component.literal(String.format("%,d", contents.count())).withStyle(ChatFormatting.GREEN))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.realfilingreborn.empty_folder")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.realfilingreborn.unregistered_folder")
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable("tooltip.realfilingreborn.folder_info")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.realfilingreborn.standard_folder_info")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.realfilingreborn.folder_gui_hint")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public record FolderContents(Optional<ResourceLocation> storedItemId, int count) {}
}
