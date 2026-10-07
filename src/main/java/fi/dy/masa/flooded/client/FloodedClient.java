package fi.dy.masa.flooded.client;

import fi.dy.masa.flooded.block.FloodedBlocks;
import fi.dy.masa.flooded.reference.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)

public class FloodedClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            blockColors.register((state, level, pos, tintIndex) -> {
                        if (level == null || pos == null) {
                            return 0x3F76E4;
                        }
                        return BiomeColors.getAverageWaterColor(level, pos);
                    },
                    FloodedBlocks.WATER_LAYER.get());

        });
    }

}