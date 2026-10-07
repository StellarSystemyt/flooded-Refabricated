package fi.dy.masa.flooded;

import fi.dy.masa.flooded.block.FloodedBlocks;
import fi.dy.masa.flooded.config.Configs;
import fi.dy.masa.flooded.reference.Reference;
import fi.dy.masa.flooded.util.WaterLevelManager;
import fi.dy.masa.flooded.util.WorldUtil;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Random;

// 1.20.1 style: Only the pure mod ID is accepted here
@Mod(Reference.MOD_ID)
public class Flooded {
    public static Flooded instance;

    // Preserved Masa's original globally accessible static random instance
    public static final Random RAND = new Random();
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    public Flooded() {
        instance = this;

        // Fetch the mod event bus to hook up the block registries and configs
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1.20.1 Configuration and Block Registry injection strings
        Configs.register();
        FloodedBlocks.register(modEventBus);

        // Map the baseline common setup lifecycle listener
        modEventBus.addListener(this::commonSetup);

        // Bind global server start/stop triggers onto the main Forge event bus
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        logger.info("Initializing Flooded common setup pipeline...");
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        // Modern 1.20.1 way to resolve the active save folder safely from the active server instance
        File saveDir = event.getServer().getWorldPath(LevelResource.ROOT).toFile();

        WaterLevelManager.INSTANCE.readFromDisk(saveDir);
        WorldUtil.setScheduleCount(WaterLevelManager.INSTANCE.getScheduleCount());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        WaterLevelManager.INSTANCE.setScheduleCount(WorldUtil.getScheduleCount());
        WaterLevelManager.INSTANCE.writeToDisk();
    }

    // Modernized utility logger mapping to read your NightConfig properties dynamically
    public static void logInfo(String message, Object... params) {
        if (Configs.enableLoggingInfo()) {
            logger.info(message, params);
        } else {
            logger.trace(message, params);
        }
    }
}
