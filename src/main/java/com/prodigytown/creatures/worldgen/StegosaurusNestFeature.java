package com.prodigytown.creatures.worldgen;

import com.mojang.serialization.Codec;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.egg.StegosaurusNestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Nido silvestre con un huevo dormido y adultos que lo guardan.
 * Comun (llanuras/bosque): 2 padres naturales, huevo natural. Desierto (raro): padres mezclados, ~15 % huevo dorado.
 * Profundo (bajo y=0): manada de 3-4 padres esmeralda y huevo esmeralda.
 */
public final class StegosaurusNestFeature extends Feature<NoneFeatureConfiguration> {
    public StegosaurusNestFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos pos = ctx.origin();
        RandomSource rnd = ctx.random();
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) return false;
        if (!level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.above(2)).isAir()) return false;

        boolean deep = pos.getY() < 0;
        var biome = level.getBiome(pos);
        // raza propia del bioma y probabilidad de que el huevo sea de esa raza (el resto es natural)
        int biomeRace = -1, biomeChance = 0;
        if (!deep) {
            if (biome.is(Biomes.DESERT)) { biomeRace = StegosaurusEntity.GOLDEN; biomeChance = 20; }
            else if (biome.is(net.minecraft.tags.BiomeTags.IS_SAVANNA) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS)) { biomeRace = StegosaurusEntity.MAGMA; biomeChance = 20; }
            else if (biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.SNOWY_TAIGA) || biome.is(Biomes.ICE_SPIKES) || biome.is(Biomes.GROVE) || biome.is(Biomes.SNOWY_SLOPES)) { biomeRace = StegosaurusEntity.CYBERNETIC; biomeChance = 20; }
        }
        int eggVariant;
        int[] parents;
        if (deep) {
            eggVariant = rnd.nextInt(100) < 20 ? StegosaurusEntity.GLACIAL : StegosaurusEntity.NATURAL; // 20 % esmeralda
            parents = new int[3 + rnd.nextInt(2)];
            java.util.Arrays.fill(parents, StegosaurusEntity.GLACIAL);
        } else if (biomeRace >= 0) {
            eggVariant = rnd.nextInt(100) < biomeChance ? biomeRace : StegosaurusEntity.NATURAL;
            int[] pool = {biomeRace, StegosaurusEntity.NATURAL};
            parents = new int[]{pool[rnd.nextInt(2)], pool[rnd.nextInt(2)]};
        } else {
            eggVariant = com.prodigytown.creatures.egg.StegosaurusEggs.nestEgg(rnd, StegosaurusEntity.NATURAL);
            parents = new int[]{StegosaurusEntity.NATURAL, StegosaurusEntity.NATURAL};
        }

        BlockState nest = ProdigyCreatures.NEST_BLOCK.get().defaultBlockState()
            .setValue(StegosaurusNestBlock.EGG, eggVariant + 1).setValue(StegosaurusNestBlock.WILD, true);
        level.setBlock(pos, nest, 2);

        ServerLevel server = level.getLevel();
        for (int i = 0; i < parents.length; i++) {
            StegosaurusEntity e = ProdigyCreatures.STEGOSAURUS.get().create(server);
            if (e == null) continue;
            BlockPos at = findSpot(level, pos, rnd);
            e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360.0F, 0.0F);
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
            e.setVariant(parents[i]);
            e.setGuard(pos);
            level.addFreshEntity(e);
        }
        return true;
    }

    private static BlockPos findSpot(WorldGenLevel level, BlockPos nest, RandomSource rnd) {
        for (int t = 0; t < 12; t++) {
            int dx = rnd.nextInt(11) - 5, dz = rnd.nextInt(11) - 5;
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) continue;
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos p = nest.offset(dx, dy, dz);
                if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                    && level.getBlockState(p).isAir() && level.getBlockState(p.above(2)).isAir() && level.getBlockState(p.above()).isAir()
                    && level.getBlockState(p.east()).isAir() && level.getBlockState(p.west()).isAir()
                    && StegosaurusEntity.roomFor(level, p)) return p;
            }
        }
        return nest.above();
    }
}
