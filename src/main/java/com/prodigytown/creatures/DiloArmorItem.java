package com.prodigytown.creatures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Armadura ligera de dilofosaurio (hierro / oro / diamante / netherita): protege sin cubrir las crestas ni la membrana. Clic derecho sobre el adulto. */
public final class DiloArmorItem extends Item {
    /** Bonos por nivel (indice 0 = sin armadura): armadura, dureza, vida, dano y velocidad (fraccion). */
    public static final double[] ARMOR = {0, 2, 3, 5, 6};
    public static final double[] TOUGHNESS = {0, 0, 0, 1, 2};
    public static final double[] HEALTH = {0, 2, 3, 4, 5};
    public static final double[] ATTACK = {0, 0.3, 0.5, 0.8, 1.0};
    public static final double[] SPEED = {0, -0.01, -0.01, -0.02, -0.02};
    /** 1 = hierro, 2 = oro, 3 = diamante, 4 = netherita */
    public final int tier;

    public DiloArmorItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.literal("Armadura +" + (int) ARMOR[tier] + (TOUGHNESS[tier] > 0 ? "  Dureza +" + (int) TOUGHNESS[tier] : "")).withStyle(net.minecraft.ChatFormatting.BLUE));
        tooltip.add(Component.literal("Vida +" + (int) HEALTH[tier] + "  Ataque +" + ATTACK[tier] + "  Velocidad " + Math.round(SPEED[tier] * 100) + "%").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("Clic derecho sobre tu dilofosaurio adulto para equiparla").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
