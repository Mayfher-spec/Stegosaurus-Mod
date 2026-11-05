package com.prodigytown.creatures.egg;

import com.prodigytown.chibi.ChibiAlpha;
import com.prodigytown.chibi.ChibiStego;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Estado de incubacion: avanza solo mientras la fogata de debajo esta apagada. Al terminar nace un chibi del dueno. */
public class StegosaurusEggBlockEntity extends BlockEntity {
    /** Los ultimos ticks son la animacion de eclosion (sacudidas fuertes, grietas, sonidos). */
    public static final int HATCH_ANIM = 200;

    public int progress = 0, total = 0;
    public boolean warm = false;
    @Nullable public UUID owner;
    /** Solo cliente: progreso interpolado entre sincronizaciones. */
    public float shown = 0.0F;

    public StegosaurusEggBlockEntity(BlockPos pos, BlockState state) {
        super(ProdigyCreatures.EGG_BE.get(), pos, state);
    }

    public void setOwner(UUID id) { this.owner = id; this.setChanged(); }

    public boolean isWarm(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof CampfireBlock && below.hasProperty(CampfireBlock.LIT) && !below.getValue(CampfireBlock.LIT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StegosaurusEggBlockEntity be) {
        if (level.isClientSide) {
            if (be.warm) be.shown = Math.min(be.total, be.shown + 1.0F);
            return;
        }
        if (be.total == 0) { // 5 a 10 minutos
            be.total = 6000 + level.random.nextInt(6001);
            be.sync();
        }
        boolean warm = be.isWarm(level, pos);
        if (warm != be.warm) { be.warm = warm; be.sync(); }
        if (!warm) return;
        be.progress++;
        int left = be.total - be.progress;
        if (be.progress % 20 == 0) be.sync();
        if (left <= HATCH_ANIM && left > 0) {
            if (left % 20 == 0) {
                level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.9F, 0.8F + level.random.nextFloat() * 0.4F);
                ((ServerLevel) level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNIFFER_EGG.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() - 0.25, pos.getZ() + 0.5, 5, 0.18, 0.2, 0.18, 0.03);
            }
        }
        if (left <= 0) be.hatch((ServerLevel) level, pos, state);
    }

    private void hatch(ServerLevel level, BlockPos pos, BlockState state) {
        double x = pos.getX() + 0.5, y = pos.getY() - 0.1, z = pos.getZ() + 0.5;
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.4F, 0.9F);
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 1.1F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNIFFER_EGG.defaultBlockState()), x, y + 0.3, z, 40, 0.25, 0.3, 0.25, 0.12);
        level.sendParticles(ParticleTypes.POOF, x, y + 0.3, z, 14, 0.25, 0.25, 0.25, 0.04);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.4, z, 12, 0.3, 0.35, 0.3, 0.05);
        ChibiStego chibi = ChibiAlpha.STEGO.get().create(level);
        if (chibi == null) return;
        chibi.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        chibi.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        StegosaurusEggs.applyToChibi(chibi, state.getValue(StegosaurusEggBlock.VARIANT));
        if (owner != null) { chibi.setOwnerUUID(owner); chibi.setTame(true); }
        level.addFreshEntity(chibi);
        if (owner != null && level.getPlayerByUUID(owner) != null) {
            level.getPlayerByUUID(owner).displayClientMessage(net.minecraft.network.chat.Component.literal("Tu huevo de estegosaurio ha eclosionado!"), true);
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
        tag.putBoolean("Warm", warm);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        total = tag.getInt("Total");
        warm = tag.getBoolean("Warm");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        shown = progress;
    }

    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).expandTowards(0.0, -1.0, 0.0).inflate(0.3); }
}
