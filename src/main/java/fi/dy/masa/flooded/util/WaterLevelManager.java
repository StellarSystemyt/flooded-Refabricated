package fi.dy.masa.flooded.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import fi.dy.masa.flooded.Flooded;
import fi.dy.masa.flooded.block.BlockLiquidLayer;
import fi.dy.masa.flooded.reference.Reference;

public class WaterLevelManager {
    public static final WaterLevelManager INSTANCE = new WaterLevelManager();
    private final Map<ResourceKey<Level>, Integer> waterLevels = new HashMap<>();
    private int scheduleCount;
    private boolean dirty;

    public int getWaterLevelInDimension(ResourceKey<Level> dimension) {
        Integer storedLevel = this.waterLevels.get(dimension);
        if (storedLevel != null) {
            return storedLevel;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel serverLevel = server != null ? server.getLevel(dimension) : null;
        int level = serverLevel != null ? serverLevel.getSeaLevel() * BlockLiquidLayer.DIVISOR : 63 * BlockLiquidLayer.DIVISOR;
        Flooded.logInfo("Initialized the water level in dimension {} to {}", dimension.location(), WorldUtil.getWaterLevelString(level));
        this.setWaterLevelInDimension(dimension, level);
        return level;
    }

    public int getScheduleCount() {
        return this.scheduleCount;
    }

    public void setScheduleCount(int count) {
        this.dirty |= (count != this.scheduleCount);
        this.scheduleCount = count;
    }

    public void setWaterLevelInDimension(ResourceKey<Level> dimension, int level) {
        this.waterLevels.put(dimension, level);
        this.dirty = true;
    }

    public void readFromDisk(@Nullable File saveDir) {
        this.waterLevels.clear();
        if (saveDir != null) {
            File floodedDataDir = getModDataDirectoryInWorld(saveDir);
            File file = new File(floodedDataDir, "data_tracker.dat");
            if (file.exists() && file.isFile() && file.canRead()) {
                try (FileInputStream is = new FileInputStream(file)) {
                    this.readFromNBT(NbtIo.readCompressed(is));
                } catch (Exception e) {
                    Flooded.logger.warn("Failed to read WaterLevelManager data from file '{}'", file.getAbsolutePath());
                }
            }
        }
    }

    public void writeToDisk() {
        if (this.dirty) {
            try {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                File saveDir = server.getWorldPath(LevelResource.ROOT).toFile();
                File floodedDataDir = getModDataDirectoryInWorld(saveDir);
                if (!floodedDataDir.exists() && !floodedDataDir.mkdirs()) {
                    Flooded.logger.warn("Failed to create the save directory '{}'", floodedDataDir.getAbsolutePath());
                    return;
                }
                File fileTmp = new File(floodedDataDir, "data_tracker.dat.tmp");
                File fileReal = new File(floodedDataDir, "data_tracker.dat");
                try (FileOutputStream os = new FileOutputStream(fileTmp)) {
                    NbtIo.writeCompressed(this.writeToNBT(new CompoundTag()), os);
                }
                if (fileReal.exists()) {
                    fileReal.delete();
                }
                fileTmp.renameTo(fileReal);
                this.dirty = false;
            } catch (Exception e) {
                Flooded.logger.warn("Failed to write WaterLevelManager data to file", e);
            }
        }
    }

    private void readFromNBT(CompoundTag nbt) {
        if (nbt != null) {
            if (nbt.contains("WaterLevels", Tag.TAG_LIST)) {
                ListTag tagList = nbt.getList("WaterLevels", Tag.TAG_COMPOUND);
                final int count = tagList.size();
                for (int i = 0; i < count; ++i) {
                    CompoundTag tag = tagList.getCompound(i);
                    if (tag.contains("Dimension", Tag.TAG_STRING) && tag.contains("WaterLevel", Tag.TAG_INT)) {
                        ResourceKey<Level> dimKey = ResourceKey.create(
                                Registries.DIMENSION,
                                new ResourceLocation(tag.getString("Dimension"))
                        );
                        this.waterLevels.put(dimKey, tag.getInt("WaterLevel"));
                    }
                }
            }
            this.scheduleCount = nbt.getInt("ScheduleCount");
        }
    }

    private CompoundTag writeToNBT(CompoundTag nbt) {
        ListTag tagList = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Integer> entry : this.waterLevels.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", entry.getKey().location().toString());
            tag.putInt("WaterLevel", entry.getValue());
            tagList.add(tag);
        }
        nbt.put("WaterLevels", tagList);
        nbt.putInt("ScheduleCount", this.scheduleCount);
        return nbt;
    }

    public static File getModDataDirectoryInWorld(File worldDir) {
        return new File(new File(worldDir, "data"), Reference.MOD_ID);
    }
}
