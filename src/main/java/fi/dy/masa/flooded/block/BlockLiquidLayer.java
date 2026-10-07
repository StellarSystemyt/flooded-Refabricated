package fi.dy.masa.flooded.block;

import fi.dy.masa.flooded.config.Configs;
import fi.dy.masa.flooded.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;

public class BlockLiquidLayer extends BlockFloodedBase {
    public static final int DIVISOR = 8;
    public static final int LEVEL_BITMASK = 0x7;
    public static final int BITMASK_SIZE = 3;

    public static final VoxelShape SHAPE_LAYER_01 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_02 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_03 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_04 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_05 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 5.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_06 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_07 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_08 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_09 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_10 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_11 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 11.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_12 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_13 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_14 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);
    public static final VoxelShape SHAPE_LAYER_15 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    private final List<VoxelShape> shapesList = new ArrayList<>();

    public BlockLiquidLayer(String name, Block.Properties properties) {
        super(name, properties);

        this.shapesList.add(SHAPE_LAYER_01); // level 0 = height 8/8 (not used)

        this.shapesList.add(SHAPE_LAYER_14); // level 1 = height 7/8
        this.shapesList.add(SHAPE_LAYER_12); // level 2 = height 6/8
        this.shapesList.add(SHAPE_LAYER_10); // level 3 = height 5/8
        this.shapesList.add(SHAPE_LAYER_08); // level 4 = height 4/8
        this.shapesList.add(SHAPE_LAYER_06); // level 5 = height 3/8
        this.shapesList.add(SHAPE_LAYER_04); // level 6 = height 2/8
        this.shapesList.add(SHAPE_LAYER_02); // level 7 = height 1/8

        this.shapesList.add(SHAPE_LAYER_08); // The rest are not used
        this.shapesList.add(SHAPE_LAYER_09);
        this.shapesList.add(SHAPE_LAYER_10);
        this.shapesList.add(SHAPE_LAYER_11);
        this.shapesList.add(SHAPE_LAYER_12);
        this.shapesList.add(SHAPE_LAYER_13);
        this.shapesList.add(SHAPE_LAYER_14);
        this.shapesList.add(SHAPE_LAYER_15);

        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 7));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapesList.get(state.getValue(LEVEL));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
    }

    @Override
    public void tick (BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WorldUtil.trySpreadWaterLayer(level, pos, state, true);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WorldUtil.trySpreadWaterLayer(level, pos, state, false);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            WorldUtil.trySpreadWaterLayer(level, pos, state, false);
        }
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        if (adjacentState.is(this) &&  adjacentState.getValue(LEVEL) >= state.getValue(LEVEL)) {
            return true;
        }
        return side == Direction.UP ? false : super.skipRendering(state, adjacentState, side);
    }
}