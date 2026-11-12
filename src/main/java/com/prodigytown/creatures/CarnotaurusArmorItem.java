package com.prodigytown.creatures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Armadura de carnotauro Brutus (yelmo con cresta, gola, peto, laminas de flanco, grebas y cola con maza). Se equipa con clic derecho sobre el carnotauro adulto. */
public final class CarnotaurusArmorItem extends Item {
    /** 1 = hierro, 2 = oro, 3 = diamante, 4 = netherita */
    public final int tier;

    public CarnotaurusArmorItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.literal("Armadura +" + StegosaurusEntity.ARMOR_VALUE[tier]
            + (StegosaurusEntity.TOUGHNESS_VALUE[tier] > 0 ? "  Dureza +" + StegosaurusEntity.TOUGHNESS_VALUE[tier] : "")).withStyle(net.minecraft.ChatFormatting.BLUE));
        tooltip.add(Component.literal("Clic derecho sobre tu carnotauro adulto para equiparla").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
