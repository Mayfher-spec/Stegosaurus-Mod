package com.prodigytown.creatures.quests;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.Nullable;

/** Comprueba si un rincon cumple como hogar del Cronista: cama, atril, luz y techo sobre la cama. */
public final class HomeCheck {
    public static final int BED = 1, LECTERN = 2, LIGHT = 4, ROOF = 8, ALL = 15;
    private static final int RH = 8, RV = 4;   // radio horizontal y vertical de busqueda

    private HomeCheck() {}

    /** Resultado: banderas cumplidas y posicion de la cama (la mitad de la cabecera) mas cercana, si hay. */
    public record Result(int flags, @Nullable BlockPos bed) {}

    public static boolean loaded(ServerLevel level, BlockPos c) {
        return level.hasChunksAt(c.offset(-RH, -RV, -RH), c.offset(RH, RV, RH));
    }

    public static boolean isBedHead(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        return st.getBlock() instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    /** Busca la cama mas cercana a "center" y evalua su rincon. Los chunks deben estar cargados (se comprueba antes). */
    public static Result scan(ServerLevel level, BlockPos center) {
        if (!loaded(level, center)) return new Result(0, null);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-RH, -RV, -RH), center.offset(RH, RV, RH))) {
            if (!isBedHead(level, p)) continue;
            double d = p.distSqr(center);
            if (d < bestD) { bestD = d; best = p.immutable(); }
        }
        return best == null ? new Result(0, null) : new Result(evaluate(level, best), best);
    }

    /** Banderas del hogar alrededor de una cama concreta (0 si ya no hay cama). */
    public static int evaluate(ServerLevel level, BlockPos bed) {
        if (!loaded(level, bed) || !isBedHead(level, bed)) return 0;
        int flags = BED;
        for (BlockPos p : BlockPos.betweenClosed(bed.offset(-RH, -RV, -RH), bed.offset(RH, RV, RH))) {
            if (level.getBlockState(p).is(Blocks.LECTERN)) { flags |= LECTERN; break; }
        }
        if (level.getBrightness(LightLayer.BLOCK, bed.above()) >= 8) flags |= LIGHT;
        if (!level.canSeeSky(bed.above())) flags |= ROOF;
        return flags;
    }
}
