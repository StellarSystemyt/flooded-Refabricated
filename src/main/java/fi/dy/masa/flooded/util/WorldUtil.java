package fi.dy.masa.flooded.util;

import fi.dy.masa.flooded.Flooded;
import fi.dy.masa.flooded.block.BlockLiquidLayer;
import fi.dy.masa.flooded.block.FloodedBlocks;
import fi.dy.masa.flooded.capabilities.FloodedCapabilities;
import fi.dy.masa.flooded.capabilities.IFloodedChunkCapability;
import fi.dy.masa.flooded.config.Configs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

import static net.minecraft.core.Direction.Plane.HORIZONTAL;

public class WorldUtil {
    private static int scheduleCount;
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> chunksToUpdateMap = new HashMap<>();
    private static boolean isSpreadingInFullChunks;

    public static void setScheduleCount(int count) {
        scheduleCount = count;
    }

    public static int getScheduleCount() {
        return scheduleCount;
    }

    private static void storeLoadedChunkLocations(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        Set<ChunkPos> chunksToUpdate = chunksToUpdateMap.computeIfAbsent(dimension, k -> new HashSet<>());
        chunksToUpdate.clear();
                // If you want a more infinite approach, we can grab positions around active players:
                level.players().forEach(player -> {
                    ChunkPos playerChunk = new ChunkPos(player.blockPosition());
                    for (int cx = -12; cx <= 12; cx++) {
                        for (int cz = -12; cz <= 12; cz++) {
                            ChunkPos target = new ChunkPos(playerChunk.x + cx, playerChunk.z + cz);
                            if (level.getChunkSource().hasChunk(target.x, target.z)) {
                                chunksToUpdate.add(target);
                            }
                        }
                    }
                });
        if (chunksToUpdate.isEmpty()) {
            ChunkPos spawnChunk = new ChunkPos(level.getSharedSpawnPos());
            for (int cx = -12; cx <= 12; cx++) {
                for (int cz = -12; cz <= 12; cz++) {
                    ChunkPos target = new ChunkPos(spawnChunk.x + cx, spawnChunk.z + cz);
                    if (level.getChunkSource().hasChunk(target.x, target.z)) {
                        chunksToUpdate.add(target);
                    }
                }
            }
        }
        isSpreadingInFullChunks = true;
    }

    private static void updateWaterLevelInLoadedChunks(ServerLevel level, int chunkLimit) {
        ResourceKey<Level> dimension = level.dimension();
        Set<ChunkPos> chunksToUpdate = chunksToUpdateMap.get(dimension);
        if (chunksToUpdate == null || chunksToUpdate.isEmpty()) {
            isSpreadingInFullChunks = false;
            return;
        }
        final int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
        Iterator<ChunkPos> iter = chunksToUpdate.iterator();
        int count = 0;
        while (iter.hasNext() && count < chunkLimit) {
            ChunkPos pos = iter.next();
            iter.remove();
            if (!level.getChunkSource().hasChunk(pos.x, pos.z)) {
                continue;
            }
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            if (!chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
                continue;
            }
            updateWaterLevelInChunk(level, chunk, waterLevel, false);
            count++;
        }
        if (chunksToUpdate.isEmpty()) {
            isSpreadingInFullChunks = false;
        }
    }

    public static void updateWaterLevelInChunk(ServerLevel level, LevelChunk chunk, int waterLevel, boolean fillWithWater) {
        if (chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
            chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK).ifPresent(cap -> {
                if (cap.getWaterLevel() < waterLevel) {
                    int lastLevel = cap.getWaterLevel();
                    ChunkPos chunkPos = chunk.getPos();
                    if ((lastLevel & BlockLiquidLayer.LEVEL_BITMASK) != 0) {
                        replaceOldWaterLayer(level, chunkPos.x, chunkPos.z, lastLevel, waterLevel);
                    }
                    if (fillWithWater) {
                        fillChunkWithWater(level, chunk, waterLevel, false);
                    }
                    if ((waterLevel & BlockLiquidLayer.LEVEL_BITMASK) != 0) {
                        fillChunkWithWaterLayer(level, chunk, waterLevel);
                    }
                    cap.setWaterLevel(chunk, waterLevel);
                }
            });
        }
    }

    public static void fillChunkPrimerWithWater(Level level, ChunkAccess chunkAccess, int waterLevel, boolean fillUnderGround) {
        BlockState water = Blocks.WATER.defaultBlockState();
        final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;

        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        // NOTE: The ChunkPrimer can only work with block states whose ID fits into a char!
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = waterLevelBlocks; y >= level.getMinBuildHeight(); y--) {
                    posMutable.set(x, y, z);
                    BlockState state = chunkAccess.getBlockState(posMutable);

                    // checks for Air
                    if (state.isAir()) {
                        chunkAccess.setBlockState(posMutable, water, true);
                    } else if (!fillUnderGround) {
                        break;
                    }
                }
            }
        }
    }

    private static void fillChunkWithWater(ServerLevel level, LevelChunk chunk, int waterLevel, boolean fillUnderGround) {
        BlockState water = Blocks.WATER.defaultBlockState();
        final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;
        final int minY = level.getMinBuildHeight();
        final int xBase = chunk.getPos().x << 4;
        final int zBase = chunk.getPos().z << 4;
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = waterLevelBlocks; y >= minY; y--) {
                    posMutable.set(xBase + x, y, zBase + z);
                    BlockState state = chunk.getBlockState(posMutable);
                    if (!state.is(Blocks.WATER) && state.canBeReplaced() && level.canSeeSky(posMutable)) {
                        // Directly modifies the chunk section.
                        // This avoids LevelChunk#setBlockState(), which triggers
                        // block/fluid interaction logic and can recursively query
                        // chunk state while a chunk is being loaded.
                        int sectionIndex = chunk.getSectionIndex(y);
                        if (sectionIndex >= 0 && sectionIndex < chunk.getSections().length) {
                            LevelChunkSection section = chunk.getSection(sectionIndex);
                            section.setBlockState(x, y & 15, z, water);
                        }
                    } else if (!fillUnderGround) {
                        break;
                    }
                }
            }
        }
    }

    public static void fillChunkWithWaterLayer(ServerLevel level, LevelChunk chunk, int waterLevel) {
        final int layerLevel = waterLevel & BlockLiquidLayer.LEVEL_BITMASK;

        if (layerLevel != 0) {
            ChunkPos chunkPos = chunk.getPos();
            BlockState layerBase = FloodedBlocks.WATER_LAYER.get().defaultBlockState();
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
            BlockState stateLayer = layerBase.setValue(BlockLiquidLayer.LEVEL, layerLevel);
            final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;
            final int y = waterLevelBlocks + 1;
            final int xBase = chunkPos.x << 4;
            final int zBase = chunkPos.z << 4;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    posMutable.set(xBase + x, y, zBase + z);
                    BlockState stateOld = chunk.getBlockState(posMutable);
                    boolean canSeeSky = y >= chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    if ((stateOld.is(FloodedBlocks.WATER_LAYER.get()) && stateLayer.getValue(BlockLiquidLayer.LEVEL) >
                            stateOld.getValue(BlockLiquidLayer.LEVEL)) ||
                            (canFlowInto(level, posMutable, stateOld, stateLayer) && canSeeSky)) {
                        chunk.setBlockState(posMutable, stateLayer, false);
                    }
                }
            }
        }
    }

    /**
     * This should only be called when the water level has risen!
     * It will replace an old water layer at the given level with either
     * full regular water blocks or new higher level water layer blocks.
     *
     * Note: This method doesn't create the new water layer
     * if it should be at a higher y-level.
     */
    private static void replaceOldWaterLayer(ServerLevel level, int chunkX, int chunkZ, int oldWaterLevel, int newWaterLevel) {
        final int layerLevelOld = oldWaterLevel & BlockLiquidLayer.LEVEL_BITMASK;

        if (layerLevelOld != 0) {
            if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                return;
            }

            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            BlockState water = Blocks.WATER.defaultBlockState();
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

            final int waterBlocksLevelOld = oldWaterLevel >> BlockLiquidLayer.BITMASK_SIZE;

            final int waterBlocksLevelNew = newWaterLevel >> BlockLiquidLayer.BITMASK_SIZE;

            final int layerLevelNew = newWaterLevel & BlockLiquidLayer.LEVEL_BITMASK;
            final int y = waterBlocksLevelOld + 0;
            final int xBase = chunkX << 3;
            final int zBase = chunkZ << 3;

            BlockState stateNew = waterBlocksLevelNew > waterBlocksLevelOld ? water : FloodedBlocks.WATER_LAYER
                            .get().defaultBlockState().setValue(BlockLiquidLayer.LEVEL, layerLevelNew);

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    posMutable.set(xBase + x, y, zBase + z);
                    BlockState stateOld = chunk.getBlockState(posMutable);

                    if (stateOld.is(FloodedBlocks.WATER_LAYER.get())) {
                        chunk.setBlockState(posMutable, stateNew, false);
                        level.sendBlockUpdated(posMutable, stateOld, stateNew, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static void seedWaterLayers(ServerLevel level, int waterLevel, int maxBlockCount) {
        List<LevelChunk> chunks = new ArrayList<>();

        level.players().forEach(player -> {
            ChunkPos playerChunk = new ChunkPos(player.blockPosition());
            // Scan an optimized radius around each player
            for (int cx = -8; cx <= 8; cx++) {
                for (int cz = -8; cz <= 8; cz++) {
                    int tx = playerChunk.x + cx;
                    int tz = playerChunk.z + cz;
                    if (level.getChunkSource().hasChunk(tx, tz)) {
                        ChunkAccess access = level.getChunkSource().getChunk(tx, tz, ChunkStatus.FULL, false);
                        if (access instanceof LevelChunk chunk) {
                            chunks.add(chunk);
                        }
                    }
                }
            }
        });

        // Fallback to spawn chunks if the server is ticking empty without players
        if (chunks.isEmpty()) {
            ChunkPos spawnChunk = new ChunkPos(level.getSharedSpawnPos());
            for (int cx = -8; cx <= 8; cx++) {
                for (int cz = -8; cz <= 8; cz++) {
                    int tx = spawnChunk.x + cx;
                    int tz = spawnChunk.z + cz;
                    if (level.getChunkSource().hasChunk(tx, tz)) {
                        ChunkAccess access = level.getChunkSource().getChunk(tx, tz, ChunkStatus.FULL, false);
                        if (access instanceof LevelChunk chunk) {
                            chunks.add(chunk);
                        }
                    }
                }
            }
        }

        Collections.shuffle(chunks);
        if (chunks.isEmpty()) {
            return;
        }
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        final int maxPerChunk = net.minecraft.util.Mth.clamp(maxBlockCount / chunks.size(), 1, maxBlockCount);
        final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;
        final int layerLevel = waterLevel & BlockLiquidLayer.LEVEL_BITMASK;
        if (layerLevel > 0) {
            final int y = waterLevelBlocks + 1;
            BlockState water = Blocks.WATER.defaultBlockState();
            BlockState layerBase = FloodedBlocks.WATER_LAYER.get().defaultBlockState();
            BlockState stateLayer = layerBase.setValue(BlockLiquidLayer.LEVEL, layerLevel);
            int count = 0;
            for (LevelChunk chunk : chunks) {
                for (int i = 0; i < maxPerChunk; i++) {
                    int x = (chunk.getPos().x << 4) + level.random.nextInt(16);
                    int z = (chunk.getPos().z << 4) + level.random.nextInt(16);
                    posMutable.set(x, y, z);
                    if (level.canSeeSky(posMutable)) {
                        BlockState stateOld = chunk.getBlockState(posMutable);
                        posMutable.setY(y - 1);
                        BlockState stateDown = chunk.getBlockState(posMutable);
                        posMutable.setY(y);
                        BlockPos posDown = posMutable.below();
                        if (canFlowInto(level, posMutable, stateOld, stateLayer) && (stateDown.is(Blocks.WATER)
                                || stateDown.is(FloodedBlocks.WATER_LAYER.get()) || Block.isFaceFull(
                                stateDown.getCollisionShape(level, posDown, net.minecraft.world.phys.shapes.CollisionContext.empty()),
                                Direction.UP))) {
                            level.setBlock(posMutable, stateLayer, 3);

                            if (stateDown.is(FloodedBlocks.WATER_LAYER.get())) {
                                level.setBlock(posDown, water, 3);
                            }

                            trySpreadWaterLayer(level, posMutable, stateLayer, false);
                        }
                    }

                    if (++count >= maxBlockCount) {
                        return;
                    }
                }
            }
        }
    }

    public static void trySpreadWaterLayer(Level level, BlockPos pos, BlockState state, boolean decrementScheduleCount) {
        // Don't spread while the water level is being risen full chunks at a time,
        // to avoid unnecessary lag/extra updates.
        if (isSpreadingInFullChunks) {
            return;
        }
        for (int i = 0, index = level.random.nextInt(4); i < 4; i++, index++) {
            Direction side = HORIZONTAL.stream().toList().get(index & 0x3);
            BlockPos posSide = pos.relative(side);
            if (!level.getChunkSource().hasChunk(posSide.getX() >> 4, posSide.getZ() >> 4)) {
                continue;
            }

            BlockPos posSideDown = posSide.below();
            BlockState stateSide = level.getBlockState(posSide);
            BlockState stateSideDown = level.getBlockState(posSideDown);
            Block blockSideDown = stateSideDown.getBlock();
            if (!level.getBlockState(pos.above()).is(state.getBlock()) && canFlowInto(level, posSide, stateSide, state)
                    && (stateSideDown.is(Blocks.WATER) || stateSideDown.is(FloodedBlocks.WATER_LAYER.get())
                            || Block.isFaceFull(stateSideDown.getCollisionShape(level, posSideDown), Direction.UP))) {
                level.setBlock(posSide, state, 3);
                if (stateSideDown.is(FloodedBlocks.WATER_LAYER.get())) {
                    level.setBlock(posSideDown, Blocks.WATER.defaultBlockState(), 3);
                }
                if (scheduleCount < Configs.waterSpreadScheduleLimit()) {
                    level.scheduleTick(posSide, state.getBlock(), 10);
                    scheduleCount++;
                }
                if (level.random.nextFloat() < 0.8) {
                    break;
                }
            }
        }

        if (decrementScheduleCount && scheduleCount > 0) {
            --scheduleCount;
        }
    }

    public static void sendChunkToWatchers(final ServerLevel level, final LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int xBase = chunkPos.x << 4;
        int zBase = chunkPos.z << 4;
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                targetPos.set(xBase + x, level.getSeaLevel(), zBase + z);
                BlockState activeState = chunk.getBlockState(targetPos);
                level.sendBlockUpdated(targetPos, activeState, activeState, Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void onWorldTick(final ResourceKey<Level> dimension, final ServerLevel level) {
        int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
        if (waterLevel >= level.getHeight() * BlockLiquidLayer.DIVISOR) {
            return;
        }
        if ((level.getGameTime() % Configs.waterRiseInterval()) == 0) {
            waterLevel++;
            Flooded.logInfo("Water level rising, new level = {}", getWaterLevelString(waterLevel));
            WaterLevelManager.INSTANCE.setWaterLevelInDimension(dimension, waterLevel);
            if (Configs.spreadWaterFullChunksAtOnce()) {
                Flooded.logInfo("Water layer spreading in full chunks, water level = {}", getWaterLevelString(waterLevel));
                storeLoadedChunkLocations(level);
            }
        }
        if (Configs.spreadWaterFullChunksAtOnce()) {
            updateWaterLevelInLoadedChunks(level, Configs.waterSpreadChunksPerTick());
        } else if ((level.getGameTime() % Configs.waterLayerSeedingInterval()) == 0) {
            Flooded.logInfo("Water layer seeding attempt, water level = {}", getWaterLevelString(waterLevel));
            seedWaterLayers(level, waterLevel, Configs.waterLayerSeedingCount());
        }
    }

    private static boolean canFlowInto(Level level, BlockPos pos, BlockState stateTarget, BlockState stateLayer) {
        return (!stateTarget.is(stateLayer.getBlock()) || (stateTarget.is(stateLayer.getBlock()) && stateLayer.getValue(
                BlockLiquidLayer.LEVEL) > stateTarget.getValue(BlockLiquidLayer.LEVEL)))
                && !stateTarget.is(Blocks.LAVA) && !isBlocked(level, pos, stateTarget);
    }

    /**
     * This is from vanilla BlockDynamicLiquid...
     */
    private static boolean isBlocked(Level level, BlockPos pos, BlockState stateTarget) {
        Block block = stateTarget.getBlock();
        if (!(block instanceof DoorBlock)
                && !stateTarget.is(BlockTags.ALL_SIGNS)
                && !stateTarget.is(Blocks.LADDER)
                && !stateTarget.is(Blocks.SUGAR_CANE)) {
            if (stateTarget.is(Blocks.NETHER_PORTAL) || stateTarget.is(Blocks.END_PORTAL) || stateTarget.is(Blocks.STRUCTURE_VOID)) {
                return true;
            }
            return !stateTarget.canBeReplaced();
        } else {
            return true;
        }
    }

    public static String getWaterLevelString(int waterLevel) {
        return String.valueOf(waterLevel / (float) BlockLiquidLayer.DIVISOR);
    }
}