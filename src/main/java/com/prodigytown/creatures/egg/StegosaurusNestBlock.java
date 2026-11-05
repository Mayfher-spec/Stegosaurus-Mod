package com.prodigytown.creatures.egg;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Nido de estegosaurio (crafteable). Clic derecho con un huevo lo coloca dentro y empieza la incubacion (5-10 min);
 * el huevo se dibuja con un BlockEntityRenderer, con temporizador encima. Agachado + mano vacia lo saca.
 * Estado EGG: 0 = vacio, 1..5 = huevo de la variante adulta (EGG-1).
 */
public final class StegosaurusNestBlock extends BaseEntityBlock {
    public static final IntegerProperty EGG = IntegerProperty.create("egg", 0, 5);
    /** Huevo silvestre dormido (generado en un nido natural): no incuba hasta que se reclame y se ponga en un nido propio. */
    public static final BooleanProperty WILD = BooleanProperty.create("wild");
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

    public StegosaurusNestBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(EGG, 0).setValue(WILD, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(EGG, WILD); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; } // el nido es modelo normal; solo el huevo va por BER
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(ProdigyCreatures.NEST_ITEM.get()));
        int e = state.getValue(EGG);
        if (e > 0 && !state.getValue(WILD)) out.add(new ItemStack(ProdigyCreatures.EGG_ITEMS.get(e - 1).get()));
        return out;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        int egg = state.getValue(EGG);
        if (!(level.getBlockEntity(pos) instanceof StegosaurusNestBlockEntity be)) return InteractionResult.PASS;
        if (state.getValue(WILD)) { // huevo silvestre: solo se reclama si ningun adulto lo protege
            if (!level.isClientSide) {
                boolean guarded = !level.getEntitiesOfClass(StegosaurusEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(24.0),
                    o -> o.isAlive() && !o.isTame()).isEmpty();
                if (guarded && !player.getAbilities().instabuild) {
                    player.displayClientMessage(Component.literal("Los padres protegen este huevo..."), true);
                    return InteractionResult.sidedSuccess(false);
                }
                level.setBlock(pos, state.setValue(EGG, 0).setValue(WILD, false), 3);
                be.clearIncubation();
                ItemStack got = new ItemStack(ProdigyCreatures.EGG_ITEMS.get(egg - 1).get());
                if (!player.getInventory().add(got)) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, got));
                level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 1.0F, 0.7F);
                player.displayClientMessage(Component.literal("Has reclamado el huevo. Ponlo en tu propio nido para que eclosione."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (egg == 0 && held.getItem() instanceof StegosaurusEggItem ei) { // meter el huevo en el nido
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(EGG, ei.variant() + 1), 3);
                be.startIncubation(player.getUUID());
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 1.0F, 0.9F);
                player.displayClientMessage(Component.literal("Huevo en el nido: incubando..."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (egg > 0 && held.isEmpty() && player.isShiftKeyDown()) { // sacar el huevo (se pierde el avance)
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(EGG, 0), 3);
                be.clearIncubation();
                ItemStack back = new ItemStack(ProdigyCreatures.EGG_ITEMS.get(egg - 1).get());
                if (!player.getInventory().add(back)) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, back));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (egg > 0 && held.isEmpty()) {
            if (!level.isClientSide) {
                int left = Math.max(0, be.total - be.progress);
                player.displayClientMessage(Component.literal(String.format("Algo se mueve dentro... faltan %d:%02d", left / 1200, (left / 20) % 60)), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StegosaurusNestBlockEntity(pos, state); }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ProdigyCreatures.NEST_BE.get(), StegosaurusNestBlockEntity::tick);
    }
}
