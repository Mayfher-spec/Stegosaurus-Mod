package com.prodigytown.creatures.quests;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Visitas del Cronista, al estilo del mercader errante: a partir del dia 5, si el jugador tiene su base cerca (su cama de reaparicion
 * o una aldea de MCA), cada dia hay una probabilidad de que llegue con un caballo de maximas estadisticas. Se queda unos 3 dias y se marcha si nadie le da
 * un hogar (mision "Un hogar para la Cronista"). Cuando se instala, las visitas terminan para ese jugador.
 */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID)
public final class ChroniclerVisits {
    public static final String KEY = "ProdigyChronicler";
    /** Primer dia en que puede aparecer, probabilidad diaria (%), estancia (ticks) y distancia maxima a la base (bloques). */
    private static final int FIRST_DAY = 5, CHANCE = 35, STAY_TICKS = 72000, BASE_RANGE = 128;

    private ChroniclerVisits() {}

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag persistent = p.getPersistentData();
        if (!persistent.contains(KEY, 10)) persistent.put(KEY, new CompoundTag());
        return persistent.getCompound(KEY);
    }

    public static boolean isSettled(ServerPlayer p) { return data(p).getBoolean("settled"); }
    public static void markSettled(ServerPlayer p) { data(p).putBoolean("settled", true); }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 200 == 91) maybeArrive(sp);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        var old = event.getOriginal().getPersistentData();
        if (old.contains(KEY, 10)) event.getEntity().getPersistentData().put(KEY, old.getCompound(KEY).copy());
    }

    private static void maybeArrive(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        if (level.dimension() != Level.OVERWORLD || sp.isSpectator()) return;
        long time = level.getDayTime(), day = time / 24000L, tod = time % 24000L;
        if (day < FIRST_DAY || tod < 1000L || tod > 10000L) return;
        CompoundTag d = data(sp);
        if (d.getBoolean("settled") || day < d.getLong("nextDay") || (d.contains("lastRoll", 4) && d.getLong("lastRoll") == day)) return;
        if (!nearBase(sp)) return;   // no se gasta la tirada si el jugador esta lejos de su base
        if (!level.getEntitiesOfClass(CronistaEntity.class, sp.getBoundingBox().inflate(160.0)).isEmpty()) return;
        d.putLong("lastRoll", day);
        if (sp.getRandom().nextInt(100) >= CHANCE) return;
        tryArrive(sp);
    }

    private static boolean nearBase(ServerPlayer sp) {
        BlockPos r = sp.getRespawnPosition();
        if (r != null && sp.getRespawnDimension() == sp.serverLevel().dimension()) {
            double dx = r.getX() - sp.getX(), dz = r.getZ() - sp.getZ();
            if (dx * dx + dz * dz <= (double) BASE_RANGE * BASE_RANGE) return true;
        }
        return McaBridge.near(sp) != null;
    }

    /** Hace llegar al Cronista (con un caballo) cerca del jugador. Devuelve false si no hay un sitio adecuado. Tambien lo usa el comando "arrive". */
    public static boolean tryArrive(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        if (level.dimension() != Level.OVERWORLD) return false;
        BlockPos spot = findSpot(level, sp);
        if (spot == null) return false;
        CronistaEntity npc = ProdigyCreatures.CRONISTA.get().create(level);
        if (npc == null) return false;
        float yaw = (float) (Mth.atan2(sp.getZ() - spot.getZ(), sp.getX() - spot.getX()) * (180.0 / Math.PI)) - 90.0F;
        npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
        npc.setYHeadRot(yaw);
        npc.makeVisitor(STAY_TICKS);
        level.addFreshEntity(npc);
        // el mejor caballo posible de Minecraft base: 30 de vida (15 corazones), velocidad maxima 0.3375 (~14,5 bloques/s) y salto maximo 1.0 (~5,3 bloques)
        Horse horse = EntityType.HORSE.create(level);
        if (horse != null) {
            horse.moveTo(spot.getX() + 1.6, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
            if (!level.noCollision(horse)) horse.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);   // color y manchas al azar; las estadisticas se fijan despues
            horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0);
            horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3375);
            horse.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(1.0);
            horse.setHealth(30.0F);
            horse.setPersistenceRequired();
            level.addFreshEntity(horse);
            horse.setLeashedTo(npc, true);
            npc.addCompanion(horse.getUUID());
        }
        long day = level.getDayTime() / 24000L;
        CompoundTag d = data(sp);
        d.putLong("nextDay", day + 7 + sp.getRandom().nextInt(4));   // si no se instala, vuelve a pasar en una semana o algo mas

        double dx = spot.getX() - sp.getX(), dz = spot.getZ() - sp.getZ();
        String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "este" : "oeste") : (dz > 0 ? "sur" : "norte");
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        sp.sendSystemMessage(Component.literal("El Cronista Mutante ha llegado de viaje, a unos " + dist + " bloques al " + dir
            + ". Prepárale un hogar (cama, atril, luz y techo) para que se quede.").withStyle(ChatFormatting.GOLD));
        sp.playNotifySound(SoundEvents.WANDERING_TRADER_REAPPEARED, SoundSource.NEUTRAL, 1.0F, 0.8F);
        return true;
    }

    @Nullable
    private static BlockPos findSpot(ServerLevel level, ServerPlayer sp) {
        var type = ProdigyCreatures.CRONISTA.get();
        for (int i = 0; i < 16; i++) {
            double ang = sp.getRandom().nextDouble() * Math.PI * 2.0, dist = 24.0 + sp.getRandom().nextDouble() * 16.0;
            int x = Mth.floor(sp.getX() + Math.cos(ang) * dist), z = Mth.floor(sp.getZ() + Math.sin(ang) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z), below = pos.below();
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(below).isEmpty()) continue;
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            if (!level.noCollision(type.getAABB(x + 0.5, y, z + 0.5))) continue;
            return pos;
        }
        return null;
    }
}
