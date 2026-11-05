package com.prodigytown.creatures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Armadura de estegosaurio (placas, casco, protectores de patas y cola). Se equipa con clic derecho sobre el estegosaurio. */
public final class StegosaurusArmorItem extends Item {
    /** 1 = hierro, 2 = oro, 3 = diamante, 4 = netherite */
    public final int tier;

    public StegosaurusArmorItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.literal("Armadura +" + StegosaurusEntity.ARMOR_VALUE[tier]
            + (StegosaurusEntity.TOUGHNESS_VALUE[tier] > 0 ? "  Dureza +" + StegosaurusEntity.TOUGHNESS_VALUE[tier] : "")).withStyle(net.minecraft.ChatFormatting.BLUE));
        tooltip.add(Component.literal("Clic derecho sobre tu estegosaurio para equiparla").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
