package com.prodigytown.creatures.egg;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Huevo de estegosaurio. Solo se sostiene sobre una fogata; se incuba (5-10 min) mientras la fogata esta APAGADA.
 * Se dibuja con un BlockEntityRenderer (se mueve, se agrieta y eclosiona), asi que no tiene modelo de bloque.
 */
public final class StegosaurusEggBlock extends BaseEntityBlock {
    /** Variante adulta del huevo (0 cibernetico, 1 dorado, 2 magma, 3 glacial, 4 natural). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 4);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 9.0, 13.0);

    public StegosaurusEggBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 4));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(VARIANT); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.DESTROY; }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof CampfireBlock;
    }

    @Override public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.DOWN && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(ProdigyCreatures.EGG_ITEMS.get(state.getValue(VARIANT)).get()));
    }

    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ProdigyCreatures.EGG_ITEMS.get(state.getValue(VARIANT)).get());
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null && level.getBlockEntity(pos) instanceof StegosaurusEggBlockEntity be) be.setOwner(placer.getUUID());
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND && level.getBlockEntity(pos) instanceof StegosaurusEggBlockEntity be) {
            String msg = be.isWarm(level, pos)
                ? "Algo se mueve dentro del huevo..."
                : "El huevo esta quieto: necesita una fogata apagada debajo";
            player.displayClientMessage(Component.literal(msg), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StegosaurusEggBlockEntity(pos, state); }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ProdigyCreatures.EGG_BE.get(), StegosaurusEggBlockEntity::tick);
    }
}
