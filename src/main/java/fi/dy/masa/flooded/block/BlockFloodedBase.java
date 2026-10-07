package fi.dy.masa.flooded.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

public class BlockFloodedBase extends Block {
    protected String blockName;
    protected boolean enabled = true;

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
    @Deprecated
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return super.getDrops(state, builder);
    }

    @Override
    public String getDescriptioId() {
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
