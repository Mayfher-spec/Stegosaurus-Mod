package com.prodigytown.creatures.client;

import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del carnotauro (tecla K): arbol de talentos (adulto), resumen con nombre y silla, y ordenes.
 * El estado (nivel, XP, rangos) llega por datos sincronizados de la entidad; las acciones viajan por ModNetwork.
 */
public final class CarnoScreen extends Screen {
    private static final String[] TABS = {"Talentos", "Resumen", "Ordenes"};
    private static final String[] BRANCH = {"Combate", "Defensa", "Soporte"};
    private static final String[] VARIANT = {"Rojo", "Verde", "Acero", "Arena"};
    private static final int W = 372, H = 236;

    private final CarnotaurusEntity pet;
    private int tab = 0, selected = -1, sig = -1;
    private EditBox nameBox;
    private int left, top;

    public CarnoScreen(CarnotaurusEntity pet) {
        super(Component.literal("Carnotauro"));
        this.pet = pet;
    }

    private ProdigyProgress prog() { return pet.getProgress(); }
    private int signature() { return java.util.Objects.hash(prog().level(), prog().pointsSpent(), pet.isAdultStage(), pet.getOrder(), pet.isSaddled(), pet.getArmorTier()); }
    private String typedName;

    @Override protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        sig = signature();
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> { tab = t; rebuildWidgets(); })
                .bounds(left + 8 + i * 84, top + 22, 80, 16).build());
        }
        if (tab == 0 && pet.isAdultStage()) buildTalents();
        if (tab == 1) buildSummary();
        if (tab == 2) buildOrders();
    }

    // ---------------------------------------------------------------- talentos
    private void buildTalents() {
        Talent[] all = prog().table();
        int[] row = new int[3];
        for (int i = 0; i < all.length; i++) {
            Talent t = all[i];
            int col = Math.max(0, Math.min(2, t.branch));
            final int idx = i;
            int rank = prog().rank(i);
            boolean can = prog().whyNotUnlock(i) == null;
            ChatFormatting color = rank >= t.maxRank ? ChatFormatting.GREEN : can ? ChatFormatting.GOLD : rank > 0 ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY;
            String label = t.id + " " + t.name;
            if (label.length() > 21) label = label.substring(0, 20) + ".";
            Button b = Button.builder(Component.literal(label + " " + rank + "/" + t.maxRank).withStyle(color), x -> selected = idx)
                .bounds(left + 8 + col * 120, top + 58 + row[col] * 12, 118, 11).build();
            addRenderableWidget(b);
            row[col]++;
        }
        addRenderableWidget(Button.builder(Component.literal("Subir rango"), b -> {
            if (selected >= 0) ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 0, selected));
        }).bounds(left + W - 168, top + H - 22, 78, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Restablecer"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 1, 0))).bounds(left + W - 86, top + H - 22, 78, 16).build());
    }

    // ---------------------------------------------------------------- resumen
    private void buildSummary() {
        nameBox = new EditBox(this.font, left + 12, top + 62, 150, 16, Component.literal("Nombre"));
        nameBox.setMaxLength(32);
        nameBox.setValue(typedName != null ? typedName : pet.hasCustomName() ? pet.getCustomName().getString() : "");
        nameBox.setHint(Component.literal("Nombre del carnotauro"));
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.literal(pet.isOrderedToSit() ? "Levantar" : "Sentar"), b -> {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 2, 0));
        }).bounds(left + 12, top + H - 42, 80, 16).build());
        Button saddle = Button.builder(Component.literal("Quitar silla"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 3, 0))).bounds(left + 98, top + H - 42, 80, 16).build();
        saddle.active = pet.isSaddled();
        addRenderableWidget(saddle);
        Button gear = Button.builder(Component.literal("Quitar armadura"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 4, 0))).bounds(left + 184, top + H - 42, 96, 16).build();
        gear.active = pet.getArmorTier() > 0;
        addRenderableWidget(gear);
    }

    // ---------------------------------------------------------------- ordenes
    private void buildOrders() {
        for (int i = 0; i < PetOrders.COUNT; i++) {
            final int o = i;
            boolean cur = pet.getOrder() == i;
            addRenderableWidget(Button.builder(Component.literal((cur ? "> " : "") + PetOrders.NAMES[i]).withStyle(cur ? ChatFormatting.GOLD : ChatFormatting.WHITE), b -> {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), o));
            }).bounds(left + 12 + (i % 2) * 176, top + 60 + (i / 2) * 22, 168, 18).build());
        }
    }

    @Override public void tick() {
        super.tick();
        if (!pet.isAlive() || pet.isRemoved()) { this.onClose(); return; }
        int s = signature();
        if (s != sig) { if (nameBox != null) typedName = nameBox.getValue(); rebuildWidgets(); }
    }

    @Override public void removed() {
        if (nameBox != null) ModNetwork.CHANNEL.sendToServer(new ModNetwork.RenamePet(pet.getId(), nameBox.getValue()));
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }

    // ---------------------------------------------------------------- dibujo
    private void text(GuiGraphics g, String s, int x, int y, int color) { g.drawString(this.font, s, x, y, color, false); }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF0B0E12);
        g.fill(left, top, left + W, top + H, 0xFF1A2027);
        g.fill(left, top, left + W, top + 18, 0xFF28313B);
        String name = pet.hasCustomName() ? pet.getCustomName().getString() : "Carnotauro " + VARIANT[Math.max(0, Math.min(3, pet.getVariant()))];
        ProdigyProgress p = prog();
        text(g, name + "   Nivel " + p.level() + (pet.isAdultStage() ? "   Puntos: " + p.pointsAvailable() : ""), left + 8, top + 5, 0xFFF2D27A);
        // barra de XP
        int xw = W - 16;
        g.fill(left + 8, top + 41, left + 8 + xw, top + 45, 0xFF0B0E12);
        int fill = p.level() >= 100 ? xw : Math.round(xw * Math.min(1.0F, p.xp() / (float) Math.max(1, p.xpToNext())));
        g.fill(left + 8, top + 41, left + 8 + fill, top + 45, 0xFF5FB35A);
        text(g, p.level() >= 100 ? "Nivel maximo" : "XP " + p.xp() + " / " + p.xpToNext(), left + 8, top + 47 - 1, 0xFF8E98A3);
        super.render(g, mx, my, pt);
        if (tab == 0) renderTalentsInfo(g);
        else if (tab == 1) renderSummary(g);
        else text(g, "Orden actual: " + PetOrders.NAMES[Math.max(0, Math.min(PetOrders.COUNT - 1, pet.getOrder()))], left + 12, top + 46 + 112, 0xFFC8CDD2);
    }

    private void renderTalentsInfo(GuiGraphics g) {
        if (!pet.isAdultStage()) {
            text(g, "Los talentos se desbloquean cuando crece (alimentalo con carne cruda).", left + 12, top + 70, 0xFFC8CDD2);
            return;
        }
        for (int i = 0; i < 3; i++) text(g, BRANCH[i], left + 10 + i * 120, top + 55 - 9, 0xFFF2D27A);
        int by = top + 58 + 9 * 12 + 4;
        g.fill(left + 8, by, left + W - 8, by + 1, 0xFF3A444F);
        if (selected < 0) {
            text(g, "Elige un talento para ver su efecto.", left + 12, by + 6, 0xFF8E98A3);
            return;
        }
        Talent t = prog().table()[selected];
        int rank = prog().rank(selected);
        text(g, t.name + "  (" + rank + "/" + t.maxRank + ")", left + 12, by + 5, 0xFFFFFFFF);
        List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(Component.literal(t.effect), W - 190));
        for (int i = 0; i < Math.min(4, lines.size()); i++) g.drawString(this.font, lines.get(i), left + 12, by + 17 + i * 10, 0xFFC8CDD2, false);
        String why = prog().whyNotUnlock(selected);
        if (rank < t.maxRank) text(g, why == null ? "Coste: " + t.cost[rank] + " pts" : why, left + W - 168, by + 6, why == null ? 0xFF5FB35A : 0xFFD98A4A);
    }

    private void renderSummary(GuiGraphics g) {
        text(g, "Nombre:", left + 12, top + 52, 0xFF8E98A3);
        int y = top + 90, x = left + 12;
        String[] k = {"Vida", "Dano", "Armadura", "Velocidad"};
        String[] v = {String.valueOf((int) pet.getMaxHealth()), String.valueOf((int) pet.getAttributeValue(Attributes.ATTACK_DAMAGE)),
            String.valueOf((int) pet.getAttributeValue(Attributes.ARMOR)), String.format("%.2f", pet.getAttributeValue(Attributes.MOVEMENT_SPEED))};
        for (int i = 0; i < 4; i++) { text(g, k[i] + ": " + v[i], x + (i % 2) * 120, y + (i / 2) * 12, 0xFFC8CDD2); }
        text(g, "Color: " + VARIANT[Math.max(0, Math.min(3, pet.getVariant()))] + (pet.isFemale() ? "  (hembra)" : "  (macho)"), x, y + 30, 0xFFC8CDD2);
        text(g, "Silla: " + (pet.isSaddled() ? "equipada" : "sin silla (clic con una silla en la mano)"), x, y + 42, 0xFFC8CDD2);
        String[] mat = {"", "hierro", "oro", "diamante", "netherita"};
        int at = pet.getArmorTier();
        text(g, "Armadura: " + (at > 0 ? "de " + mat[at] : "sin armadura (clic con una en la mano)"), x, y + 54, 0xFFC8CDD2);
        text(g, "Talentos desbloqueados: " + prog().talentsUnlocked() + " / " + prog().table().length, x, y + 66, 0xFFC8CDD2);
        int ul = pet.ultimateLevel();
        text(g, ul > 0 ? "Rugido ancestral: nivel " + ul + " (tecla R, montado o cerca)" : "Rugido ancestral: se obtiene al nivel 50", x, y + 78, ul > 0 ? 0xFFF2D27A : 0xFF8E98A3);
    }
}
