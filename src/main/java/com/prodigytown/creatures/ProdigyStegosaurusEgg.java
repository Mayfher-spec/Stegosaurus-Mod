package com.prodigytown.creatures;

import com.prodigytown.chibi.ChibiAlpha;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeSpawnEggItem;

/**
 * One visible The Stegosaurus Mod egg:
 * right-click block -> chibi, shift + right-click block -> adult.
 * Existing adult egg registry id remains unchanged.
 */
public class ProdigyStegosaurusEgg extends ForgeSpawnEggItem {
    public ProdigyStegosaurusEgg(Properties props) {
        super(ChibiAlpha.STEGO, 0x607D32, 0xE7C79D, props);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        }
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel server)) return InteractionResult.PASS;

        BlockPos clicked = context.getClickedPos();
        boolean solid = !server.getBlockState(clicked).getCollisionShape(server, clicked).isEmpty();
        BlockPos spawnAt = solid ? clicked.relative(context.getClickedFace()) : clicked;
        Entity result = ProdigyCreatures.STEGOSAURUS.get().spawn(
            server, context.getItemInHand(), context.getPlayer(),
            spawnAt, MobSpawnType.SPAWN_EGG, true, solid && spawnAt != clicked);
        if (result == null) return InteractionResult.FAIL;

        if (!context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
