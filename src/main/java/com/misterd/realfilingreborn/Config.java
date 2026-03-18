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
        COMMON_BUILDER.comment("Filing Index Ranges - Configure range limits for the Filing Index and upgrades")
                .push("filing_index_ranges");

        FILING_INDEX_BASE_RANGE = COMMON_BUILDER
                .comment("Base Filing Index range (blocks)", "Default range with no upgrades installed")
                .defineInRange("base_range", 8, 4, 32);

        IRON_RANGE_UPGRADE = COMMON_BUILDER
                .comment("Range with Iron Range Upgrade installed (blocks)")
                .defineInRange("iron_range", 16, 8, 64);

        DIAMOND_RANGE_UPGRADE = COMMON_BUILDER
                .comment("Range with Diamond Range Upgrade installed (blocks)")
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

    public static int getFilingIndexBaseRange() { return FILING_INDEX_BASE_RANGE.get(); }
    public static int getIronRangeUpgrade()     { return IRON_RANGE_UPGRADE.get(); }
    public static int getDiamondRangeUpgrade()  { return DIAMOND_RANGE_UPGRADE.get(); }
    public static int getNetheriteRangeUpgrade(){ return NETHERITE_RANGE_UPGRADE.get(); }

    private static void validateConfig() {
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
            LOGGER.info("Filing Index Ranges Configuration:");
            LOGGER.info("  Base Range: {} blocks ({}x{} area)", getFilingIndexBaseRange(), getFilingIndexBaseRange() * 2, getFilingIndexBaseRange() * 2);
            LOGGER.info("  Iron Upgrade: {} blocks ({}x{} area)", getIronRangeUpgrade(), getIronRangeUpgrade() * 2, getIronRangeUpgrade() * 2);
            LOGGER.info("  Diamond Upgrade: {} blocks ({}x{} area)", getDiamondRangeUpgrade(), getDiamondRangeUpgrade() * 2, getDiamondRangeUpgrade() * 2);
            LOGGER.info("  Netherite Upgrade: {} blocks ({}x{} area)", getNetheriteRangeUpgrade(), getNetheriteRangeUpgrade() * 2, getNetheriteRangeUpgrade() * 2);

            int netheriteArea = getNetheriteRangeUpgrade() * 2 * getNetheriteRangeUpgrade() * 2;
            if (netheriteArea > 65536) {
                LOGGER.warn("Netherite upgrade covers {} blocks - this is a very large area!", String.format("%,d", netheriteArea));
            }
            validateConfig();
        }
    }
}