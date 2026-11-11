package com.prodigytown.creatures.egg;

import com.prodigytown.chibi.ChibiAlpha;
import com.prodigytown.chibi.ChibiStego;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Estado de incubacion del nido. Al terminar nace un chibi del dueno y el nido queda vacio. */
public class StegosaurusNestBlockEntity extends BlockEntity {
    /** Los ultimos ticks son la animacion de eclosion (balanceo creciente, grietas, sonidos). */
    public static final int HATCH_ANIM = 240;

    public int progress = 0, total = 0;
    @Nullable public UUID owner;
    /** Solo cliente: progreso interpolado entre sincronizaciones. */
    public float shown = 0.0F;

    public StegosaurusNestBlockEntity(BlockPos pos, BlockState state) {
        super(ProdigyCreatures.NEST_BE.get(), pos, state);
    }

    public boolean hasEgg() { return getBlockState().getValue(StegosaurusNestBlock.EGG) > 0; }

    public void startIncubation(UUID who) {
        this.owner = who;
        this.progress = 0;
        this.total = 6000 + (level != null ? level.random.nextInt(6001) : 0); // 5 a 10 minutos
        this.shown = 0.0F;
        sync();
    }

    public void clearIncubation() {
        this.progress = 0; this.total = 0; this.owner = null; this.shown = 0.0F;
        sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StegosaurusNestBlockEntity be) {
        if (state.getValue(StegosaurusNestBlock.EGG) <= 0 || state.getValue(StegosaurusNestBlock.WILD)) return;
        if (level.isClientSide) {
            be.shown = Math.min(be.total, be.shown + 1.0F);
            return;
        }
        if (be.total == 0) { be.total = 6000 + level.random.nextInt(6001); be.sync(); }
        be.progress++;
        int left = be.total - be.progress;
        if (be.progress % 20 == 0) be.sync();
        if (left <= HATCH_ANIM && left > 0 && left % 24 == 0) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.9F, 0.8F + level.random.nextFloat() * 0.4F);
            ((ServerLevel) level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNIFFER_EGG.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.18, 0.2, 0.18, 0.03);
        }
        if (left <= 0) be.hatch((ServerLevel) level, pos, state);
    }

    private void hatch(ServerLevel level, BlockPos pos, BlockState state) {
        int variant = state.getValue(StegosaurusNestBlock.EGG) - 1;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.375, z = pos.getZ() + 0.5;
        level.setBlock(pos, state.setValue(StegosaurusNestBlock.EGG, 0), 3);
        UUID who = owner;
        clearIncubation();
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.4F, 0.9F);
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 1.1F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNIFFER_EGG.defaultBlockState()), x, y + 0.3, z, 40, 0.25, 0.3, 0.25, 0.12);
        level.sendParticles(ParticleTypes.POOF, x, y + 0.3, z, 14, 0.25, 0.25, 0.25, 0.04);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.4, z, 12, 0.3, 0.35, 0.3, 0.05);
        if (variant >= CarnotaurusEggs.NEST_BASE - 1) { hatchCarnotaurus(level, pos, x, y, z, variant - (CarnotaurusEggs.NEST_BASE - 1), who); return; }
        ChibiStego chibi = ChibiAlpha.STEGO.get().create(level);
        if (chibi == null) return;
        chibi.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        chibi.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        StegosaurusEggs.applyToChibi(chibi, variant);
        if (who != null) { chibi.setOwnerUUID(who); chibi.setTame(true); }
        level.addFreshEntity(chibi);
        if (who != null && level.getPlayerByUUID(who) != null) {
            level.getPlayerByUUID(who).displayClientMessage(net.minecraft.network.chat.Component.literal("Tu huevo de estegosaurio ha eclosionado!"), true);
        }
    }

    /** Nace un carnotauro chibi del color del huevo; si hay dueno, ya viene domado. */
    private void hatchCarnotaurus(ServerLevel level, BlockPos pos, double x, double y, double z, int color, @Nullable UUID who) {
        CarnotaurusEntity baby = ProdigyCreatures.CARNOTAURUS.get().create(level);
        if (baby == null) return;
        baby.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        baby.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        baby.initFromEgg(color, who);
        level.addFreshEntity(baby);
        if (who != null && level.getPlayerByUUID(who) != null) {
            level.getPlayerByUUID(who).displayClientMessage(net.minecraft.network.chat.Component.literal("Tu huevo de carnotauro ha eclosionado!"), true);
        }
    }

    private void sync() {
        this.setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putInt("Total", total);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        total = tag.getInt("Total");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        // en el cliente, no saltar hacia atras por la latencia: solo corregir si se desvia mucho
        if (shown <= 0.0F || Math.abs(shown - progress) > 30.0F) shown = progress;
    }

    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(0.5).expandTowards(0.0, 1.0, 0.0); }
}
