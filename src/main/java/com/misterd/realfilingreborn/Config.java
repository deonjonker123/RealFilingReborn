package com.misterd.realfilingreborn;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

public class Config {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static ModConfigSpec COMMON_CONFIG;

    // Storage limits
    private static ModConfigSpec.IntValue MAX_FOLDER_STORAGE;
    private static ModConfigSpec.IntValue MAX_CANISTER_STORAGE;

    // Filing Index ranges
    private static ModConfigSpec.IntValue FILING_INDEX_BASE_RANGE;
    private static ModConfigSpec.IntValue IRON_RANGE_UPGRADE;
    private static ModConfigSpec.IntValue DIAMOND_RANGE_UPGRADE;
    private static ModConfigSpec.IntValue NETHERITE_RANGE_UPGRADE;

    static {
        buildCommonConfig();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);
    }

    private static void buildCommonConfig() {
        buildStorageLimitsConfig();
        buildFilingIndexRangesConfig();
    }

    private static void buildStorageLimitsConfig() {
        COMMON_BUILDER.comment("Storage Limits - Configure maximum storage capacities for folders and canisters")
                .push("storage_limits");

        MAX_FOLDER_STORAGE = COMMON_BUILDER
                .comment(
                        "Maximum items storable in a filing folder",
                        "Default: 2147483647 (Integer.MAX_VALUE)",
                        "Minimum: 4096 (64 stacks)"
                )
                .defineInRange("max_folder_storage", Integer.MAX_VALUE, 4096, Integer.MAX_VALUE);

        MAX_CANISTER_STORAGE = COMMON_BUILDER
                .comment(
                        "Maximum fluid storable in canisters (millibuckets)",
                        "Default: 2147483647 (Integer.MAX_VALUE)",
                        "Minimum: 64000 (64 buckets)"
                )
                .defineInRange("max_canister_storage", Integer.MAX_VALUE, 64000, Integer.MAX_VALUE);

        COMMON_BUILDER.pop();
    }

    private static void buildFilingIndexRangesConfig() {
        COMMON_BUILDER.comment("Filing Index Ranges - Configure range limits for the Filing Index and upgrades")
                .push("filing_index_ranges");

        FILING_INDEX_BASE_RANGE = COMMON_BUILDER
                .comment(
                        "Base Filing Index range (blocks)",
                        "Default range with no upgrades installed"
                )
                .defineInRange("base_range", 8, 4, 32);

        IRON_RANGE_UPGRADE = COMMON_BUILDER
                .comment(
                        "Range with Iron Range Upgrade installed (blocks)"
                )
                .defineInRange("iron_range", 16, 8, 64);

        DIAMOND_RANGE_UPGRADE = COMMON_BUILDER
                .comment(
                        "Range with Diamond Range Upgrade installed (blocks)"
                )
                .defineInRange("diamond_range", 32, 16, 128);

        NETHERITE_RANGE_UPGRADE = COMMON_BUILDER
                .comment(
                        "Range with Netherite Range Upgrade installed (blocks)",
                        "WARNING: Large ranges can significantly impact server performance!",
                        "A range of 64 covers a 128x128 block area - use with caution on multiplayer servers"
                )
                .defineInRange("netherite_range", 64, 32, 256);

        COMMON_BUILDER.pop();
    }

    // Getters
    public static int getMaxFolderStorage() {
        return MAX_FOLDER_STORAGE.get();
    }

    public static int getMaxCanisterStorage() {
        return MAX_CANISTER_STORAGE.get();
    }

    public static int getFilingIndexBaseRange() {
        return FILING_INDEX_BASE_RANGE.get();
    }

    public static int getIronRangeUpgrade() {
        return IRON_RANGE_UPGRADE.get();
    }

    public static int getDiamondRangeUpgrade() {
        return DIAMOND_RANGE_UPGRADE.get();
    }

    public static int getNetheriteRangeUpgrade() {
        return NETHERITE_RANGE_UPGRADE.get();
    }

    private static void validateConfig() {
        if (getMaxFolderStorage() < 4096) {
            LOGGER.warn("Folder storage limit ({}) is below recommended minimum of 4096 items", getMaxFolderStorage());
        }

        if (getMaxCanisterStorage() < 64000) {
            LOGGER.warn("Canister storage limit ({}) is below recommended minimum of 64000mb", getMaxCanisterStorage());
        }

        if (getIronRangeUpgrade() <= getFilingIndexBaseRange()) {
            LOGGER.warn("Iron range upgrade ({}) should be greater than base range ({})", getIronRangeUpgrade(), getFilingIndexBaseRange());
        }

        if (getDiamondRangeUpgrade() <= getIronRangeUpgrade()) {
            LOGGER.warn("Diamond range upgrade ({}) should be greater than iron range ({})", getDiamondRangeUpgrade(), getIronRangeUpgrade());
        }

        if (getNetheriteRangeUpgrade() <= getDiamondRangeUpgrade()) {
            LOGGER.warn("Netherite range upgrade ({}) should be greater than diamond range ({})", getNetheriteRangeUpgrade(), getDiamondRangeUpgrade());
        }

        if (getNetheriteRangeUpgrade() > 128) {
            LOGGER.warn("Netherite range ({}) is very large and may impact server performance!", getNetheriteRangeUpgrade());
        }

        if (getNetheriteRangeUpgrade() > 200) {
            LOGGER.error("Netherite range ({}) is extremely large! This WILL cause severe performance issues on multiplayer servers!", getNetheriteRangeUpgrade());
        }
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            LOGGER.info("Real Filing Reborn configuration loaded");
            logConfigValues();
            validateConfig();
        }
    }

    private static void logConfigValues() {
        LOGGER.info("Storage Limits Configuration:");
        LOGGER.info("  Max Folder Storage: {}", getMaxFolderStorage() == Integer.MAX_VALUE ? "unlimited" : String.format("%,d", getMaxFolderStorage()));
        LOGGER.info("  Max Canister Storage: {}mb", getMaxCanisterStorage() == Integer.MAX_VALUE ? "unlimited" : String.format("%,d", getMaxCanisterStorage()));

        LOGGER.info("Filing Index Ranges Configuration:");
        LOGGER.info("  Base Range: {} blocks ({}x{} area)", getFilingIndexBaseRange(), getFilingIndexBaseRange() * 2, getFilingIndexBaseRange() * 2);
        LOGGER.info("  Iron Upgrade: {} blocks ({}x{} area)", getIronRangeUpgrade(), getIronRangeUpgrade() * 2, getIronRangeUpgrade() * 2);
        LOGGER.info("  Diamond Upgrade: {} blocks ({}x{} area)", getDiamondRangeUpgrade(), getDiamondRangeUpgrade() * 2, getDiamondRangeUpgrade() * 2);
        LOGGER.info("  Netherite Upgrade: {} blocks ({}x{} area)", getNetheriteRangeUpgrade(), getNetheriteRangeUpgrade() * 2, getNetheriteRangeUpgrade() * 2);

        int netheriteArea = getNetheriteRangeUpgrade() * 2 * getNetheriteRangeUpgrade() * 2;
        if (netheriteArea > 65536) {
            LOGGER.warn("Netherite upgrade covers {} blocks - this is a very large area!", String.format("%,d", netheriteArea));
        }
    }
}
