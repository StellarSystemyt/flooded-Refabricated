package fi.dy.masa.flooded.block;

import fi.dy.masa.flooded.reference.Reference;
import fi.dy.masa.flooded.reference.ReferenceNames;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class FloodedBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Reference.MOD_ID);

    public static final RegistryObject<BlockLiquidLayer> WATER_LAYER = BLOCKS.register(
            ReferenceNames.NAME_BLOCK_WATER_LAYER,
            () -> new BlockLiquidLayer(ReferenceNames.NAME_BLOCK_WATER_LAYER, Block.Properties.of().noCollission()
                            .strength(4.0f, 10.0f).sound(SoundType.WET_GRASS).liquid())
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
