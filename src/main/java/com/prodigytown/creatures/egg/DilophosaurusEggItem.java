package com.prodigytown.creatures.egg;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Huevo de carnotauro de un variante. Se incuba en el nido (clic derecho sobre el nido) y nace un chibi tuyo. */
public final class DilophosaurusEggItem extends Item {
    private final int variant;

    public DilophosaurusEggItem(int variant, Properties props) {
        super(props);
        this.variant = variant;
    }

    /** Variante del dilofosaurio que nacera (DilophosaurusEntity.NATURAL..SPECTRAL). */
    public int variant() { return variant; }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Ponlo dentro de un nido para incubarlo").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Tarda de 5 a 10 minutos en nacer").withStyle(ChatFormatting.DARK_GRAY));
    }
}
