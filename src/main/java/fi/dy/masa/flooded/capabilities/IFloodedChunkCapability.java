package fi.dy.masa.flooded.capabilities;

import net.minecraft.world.level.chunk.LevelChunk;

public interface IFloodedChunkCapability
{
    /**
     * Returns the last water level this chunk was updated to
     * @return the current water level
     */
    int getWaterLevel();

    /**
     * Sets the current water level in this chunk
     * @param chunk the chunk being modified
     * @param waterLevel the new water level
     */
    void setWaterLevel(LevelChunk chunk, int waterLevel);

    /**
     * Sets the current water level in this chunk read from the stored capability NBT
     * @param waterLevel the water level value from data storage
     */
    void setWaterLevelFromNBT(int waterLevel);
}
