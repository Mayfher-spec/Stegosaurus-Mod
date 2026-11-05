package com.prodigytown.creatures.egg;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Huevo de una raza concreta. Solo se puede colocar sobre una fogata. */
public final class StegosaurusEggItem extends BlockItem {
    private final int variant;

    public StegosaurusEggItem(Block block, int variant, Properties props) {
        super(block, props);
        this.variant = variant;
    }

    @Nullable @Override protected BlockState getPlacementState(BlockPlaceContext ctx) {
        BlockState s = super.getPlacementState(ctx);
        return s == null ? null : s.setValue(StegosaurusEggBlock.VARIANT, variant);
    }

    @Override public String getDescriptionId() { return this.getOrCreateDescriptionId(); }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Colocalo sobre una fogata apagada").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Tarda de 5 a 10 minutos en nacer").withStyle(ChatFormatting.DARK_GRAY));
    }
}
