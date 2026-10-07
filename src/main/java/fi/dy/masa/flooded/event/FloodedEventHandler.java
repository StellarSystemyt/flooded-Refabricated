package fi.dy.masa.flooded.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import fi.dy.masa.flooded.capabilities.FloodedCapabilities;
import fi.dy.masa.flooded.capabilities.IFloodedChunkCapability;
import fi.dy.masa.flooded.config.Configs;
import fi.dy.masa.flooded.reference.Reference;
import fi.dy.masa.flooded.util.WaterLevelManager;
import fi.dy.masa.flooded.util.WorldUtil;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class FloodedEventHandler {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            if (Configs.enabledInDimension(dimension)) {
                WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
            }
        }
    }

    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            if (Configs.enabledInDimension(dimension)) {
                WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            if (Configs.enabledInDimension(dimension)) {
                final int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
                if (event.isNewChunk()) {
                    WorldUtil.fillChunkWithWaterLayer(serverLevel, chunk.getPos().x, chunk.getPos().z, waterLevel);
                    chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK).ifPresent(cap -> {
                        cap.setWaterLevel(chunk, waterLevel);
                    });
                }
                WorldUtil.updateWaterLevelInChunk(serverLevel, chunk, waterLevel, true);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            if (Configs.enabledInDimension(dimension)) {
                int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
                chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK).ifPresent(cap -> {
                    cap.setWaterLevel(chunk, waterLevel);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerLevel serverLevel && event.phase == TickEvent.Phase.END) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            if (Configs.enabledInDimension(dimension) && serverLevel.isRaining()) {
                WorldUtil.onWorldTick(dimension, serverLevel);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Overworld matching is checked via modern static keys instead of integer 0
            if (serverLevel.dimension() == Level.OVERWORLD) {
                WaterLevelManager.INSTANCE.setScheduleCount(WorldUtil.getScheduleCount());
            }
            WaterLevelManager.INSTANCE.writeToDisk();
        }
    }
}
