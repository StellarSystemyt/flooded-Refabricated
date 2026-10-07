package fi.dy.masa.flooded.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import fi.dy.masa.flooded.reference.Reference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class FloodedCapabilities {
    public static final Capability<IFloodedChunkCapability> CAPABILITY_FLOODED_CHUNK =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(IFloodedChunkCapability.class);
    }

    @SubscribeEvent
    public static void attachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        event.addCapability(
        new ResourceLocation(Reference.MOD_ID, "flooded_chunk"),
                new FloodedChunkCapabilityProvider()
        );
    }

    public static class FloodedChunkCapabilityProvider implements ICapabilityProvider, INBTSerializable<Tag> {
        private final IFloodedChunkCapability cap = new FloodedChunkCapability();
        private final LazyOptional<IFloodedChunkCapability> optional = LazyOptional.of(() -> this.cap);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
            return capability == CAPABILITY_FLOODED_CHUNK ? CAPABILITY_FLOODED_CHUNK.orEmpty(capability, optional) : LazyOptional.empty();
        }

        @Override
        public Tag serializeNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("WaterLevel", this.cap.getWaterLevel());
            return nbt;
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            if (nbt instanceof CompoundTag tags) {
                this.cap.setWaterLevelFromNBT(tags.getInt("WaterLevel"));
            }
        }
    }
}