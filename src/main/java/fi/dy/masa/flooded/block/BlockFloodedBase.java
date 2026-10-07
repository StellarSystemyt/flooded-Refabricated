package fi.dy.masa.flooded.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockFloodedBase extends Block {
    protected final String blockName;
    protected boolean enabled = true;

    // Properties (hardness, resistance, sounds) are safely built and passed via the registry
    public BlockFloodedBase(String name, Block.Properties properties) {
        super(properties);
        this.blockName = name;
    }

    public String getBlockName() {
        return this.blockName;
    }

    public boolean hasSpecialHitbox() {
        return false;
    }


    @Override
    public String getDescriptionId() {
        return "block.flooded." + this.blockName;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public BlockFloodedBase setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
