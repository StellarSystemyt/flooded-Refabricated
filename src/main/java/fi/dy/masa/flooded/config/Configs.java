package fi.dy.masa.flooded.config;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Configs {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SPEC;
    public static ForgeConfigSpec.BooleanValue ENABLE_LOGGING_INFO;
    public static ForgeConfigSpec.BooleanValue ENABLE_WATER_LAYER_RANDOM_SPREAD;
    public static ForgeConfigSpec.BooleanValue FLOOD_NEW_CHUNKS_UNDERGROUND;
    public static ForgeConfigSpec.BooleanValue DIMENSION_LIST_IS_BLACKLIST;
    public static ForgeConfigSpec.BooleanValue SPREAD_WATER_FULL_CHUNKS_AT_ONCE;
    public static ForgeConfigSpec.IntValue WATER_LAYER_SEEDING_COUNT;
    public static ForgeConfigSpec.IntValue WATER_LAYER_SEEDING_INTERVAL;
    public static ForgeConfigSpec.IntValue WATER_SPREAD_CHUNKS_PER_TICK;
    public static ForgeConfigSpec.IntValue WATER_SPREAD_SCHEDULE_LIMIT;
    public static ForgeConfigSpec.IntValue WATER_RISE_INTERVAL;
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_LIST;
    private static final Set<String> DIMENSIONS = new HashSet<>();

    static {
        BUILDER.push("Generic");

        DIMENSION_LIST = BUILDER
                .comment("The white- or blacklist of dimensions to affect. Use resource locations. Example: [\"minecraft:overworld\", \"minecraft:the_nether\"]")
                .defineListAllowEmpty(List.of("dimensionList"), () -> List.of("minecraft:overworld"), o -> o instanceof String);

        DIMENSION_LIST_IS_BLACKLIST = BUILDER
                .comment("If true, then 'dimensionList' is a blacklist. If false, it's a whitelist.")
                .define("dimensionListIsBlacklist", false);

        ENABLE_LOGGING_INFO = BUILDER
                .comment("Enables a bunch of extra (debug) logging on the INFO level")
                .define("enableLoggingInfo", false);

        ENABLE_WATER_LAYER_RANDOM_SPREAD = BUILDER
                .comment("If enabled, the water layers will try to spread to adjacent lower positions with random ticks")
                .define("enableWaterLayerRandomSpread", true);

        FLOOD_NEW_CHUNKS_UNDERGROUND = BUILDER
                .comment("If enabled, then newly generated chunks will get flooded entirely in every air space that is below the current global water level.")
                .define("floodNewChunksUnderground", false);

        SPREAD_WATER_FULL_CHUNKS_AT_ONCE = BUILDER
                .comment("If enabled, then the water level rise is updated full chunks at a time. This might be less laggy.")
                .define("spreadWaterFullChunksAtOnce", false);

        WATER_SPREAD_CHUNKS_PER_TICK = BUILDER
                .comment("The number of chunks to spread water in per game tick, if spreadWaterFullChunksAtOnce = true")
                .defineInRange("waterSpreadChunksPerTick", 10, 1, Integer.MAX_VALUE);

        WATER_SPREAD_SCHEDULE_LIMIT = BUILDER
                .comment("Maximum number of scheduled updates at once for spreading water layers")
                .defineInRange("waterSpreadScheduleLimit", 800, 1, Integer.MAX_VALUE);

        WATER_LAYER_SEEDING_COUNT = BUILDER
                .comment("How many attempts (max created blocks) are made at every water layer seeding attempt")
                .defineInRange("waterLayerSeedingCount", 20, 1, Integer.MAX_VALUE);

        WATER_LAYER_SEEDING_INTERVAL = BUILDER
                .comment("The interval in game ticks, how often new water layers are attempted to be created randomly")
                .defineInRange("waterLayerSeedingInterval", 20, 1, Integer.MAX_VALUE);

        WATER_RISE_INTERVAL = BUILDER
                .comment("The interval in game ticks, how often the water level should rise by 1/16th of a block")
                .defineInRange("waterRiseInterval", 400, 1, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static void cacheDimensions() {
        DIMENSIONS.clear();
        for (String dim : DIMENSION_LIST.get()) {
            DIMENSIONS.add(dim.trim());
        }
    }

    public static boolean enabledInDimension(ResourceKey<Level> dimensionKey) {
        // Cache data dynamically if the inner array emptied
        if (DIMENSIONS.isEmpty() && !DIMENSION_LIST.get().isEmpty()) {
            cacheDimensions();
        }
        String dimName = dimensionKey.location().toString();
        return DIMENSION_LIST_IS_BLACKLIST.get() != DIMENSIONS.contains(dimName);
    }

    public static boolean enableLoggingInfo() { return ENABLE_LOGGING_INFO.get(); }
    public static boolean enableWaterLayerRandomSpread() { return ENABLE_WATER_LAYER_RANDOM_SPREAD.get(); }
    public static boolean floodNewChunksUnderground() { return FLOOD_NEW_CHUNKS_UNDERGROUND.get(); }
    public static boolean spreadWaterFullChunksAtOnce() { return SPREAD_WATER_FULL_CHUNKS_AT_ONCE.get(); }
    public static int waterSpreadChunksPerTick() { return WATER_SPREAD_CHUNKS_PER_TICK.get(); }
    public static int waterSpreadScheduleLimit() { return WATER_SPREAD_SCHEDULE_LIMIT.get(); }
    public static int waterLayerSeedingCount() { return WATER_LAYER_SEEDING_COUNT.get(); }
    public static int waterLayerSeedingInterval() { return WATER_LAYER_SEEDING_INTERVAL.get(); }
    public static int waterRiseInterval() { return WATER_RISE_INTERVAL.get(); }
}