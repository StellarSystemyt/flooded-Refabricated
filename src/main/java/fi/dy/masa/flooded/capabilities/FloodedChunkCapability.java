package fi.dy.masa.flooded.capabilities;

import net.minecraft.world.level.chunk.LevelChunk;

public class FloodedChunkCapability implements IFloodedChunkCapability {
    private int waterLevel;

    @Override
    public int getWaterLevel() {
        return this.waterLevel;
    }

    @Override
    public void setWaterLevel(LevelChunk chunk, int waterLevel) {
        this.waterLevel = waterLevel;
        chunk.setUnsaved(true);
    }

    @Override
    public void setWaterLevelFromNBT(int waterLevel) {
        this.waterLevel = waterLevel;
    }
}
