package com.prodigytown.creatures.client;

import com.prodigytown.creatures.DiloConfig;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import com.prodigytown.creatures.DiloArmorItem;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Pantalla del dilofosaurio (tecla K) con seis pestanas: Comandos, Estadisticas, Habilidades, Talentos, Equipamiento y Variantes.
 * Todo se dibuja en vivo con datos reales de la entidad (sin texturas); el servidor valida cada accion.
 */
public final class DiloScreen extends Screen {
    private static final int W = 400, H = 252;
    private static final String[] TABS = {"Comandos", "Estadísticas", "Habilidades", "Talentos", "Equipamiento", "Variantes"};
    private static final int GOLD = 0xFFFFD35A, TEXT = 0xFFEDEDED, DIM = 0xFF9AA0A6, PANEL = 0xF0101614;
    private static final int[] BRANCH_COLOR = {0xFFE8602C, 0xFF3FB4FF, 0xFF7CE03C, 0xFFFFE066};
    private static final String[] BRANCH_NAME = {"Combate", "Movilidad", "Supervivencia", "Cazador Primordial"};
    private static final String[] ABILITY_DESC = {
        "Un mordisco rápido y preciso. El daño llega justo en el momento del impacto.",
        "Dos zarpazos seguidos en un arco corto frente a ti. No atraviesan paredes.",
        "Un gran salto hacia delante: empuja un poco a los que alcanza y deja un impacto al aterrizar. Sirve montado.",
        "Durante unos segundos: más daño y velocidad, la membrana del cuello se despliega y un rugido intimida a los enemigos cercanos.",
        "Despliega la membrana y escupe veneno a distancia: daño leve y veneno. Se desbloquea con el talento Glándula venenosa."};
    private static final String[] VARIANT_DESC = {
        "Verde oliva con vientre claro. La más común.", "Azul eléctrico con vetas luminosas. Frecuente en bosques.",
        "Rojo magma con brillo de brasa. Rara.", "Arena del desierto. Habitual en sabanas y tierras áridas.",
        "Esmeralda espectral, con brillo fantasmal. La más rara."};

    private static final ResourceLocation BG = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/dilo/screen_bg.png");

    /** Constructor de botones con el estilo del mod (en lugar de los grises de vanilla). */
    private static final class TB {
        private final Component msg; private final Button.OnPress press; private int x, y, w, h;
        private TB(Component msg, Button.OnPress press) { this.msg = msg; this.press = press; }
        static TB builder(Component msg, Button.OnPress press) { return new TB(msg, press); }
        TB bounds(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; return this; }
        Button build() { return new Themed(x, y, w, h, msg, press); }
    }

    private static final class Themed extends Button {
        boolean selected;
        Themed(int x, int y, int w, int h, Component msg, OnPress press) { super(x, y, w, h, msg, press, DEFAULT_NARRATION); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hover = this.isHoveredOrFocused() && this.active;
            int top = selected ? 0xFF3A2C0E : hover ? 0xFF26302C : 0xFF171D1B, bot = selected ? 0xFF1E1606 : hover ? 0xFF161C1A : 0xFF0C100F;
            g.fillGradient(getX(), getY(), getX() + width, getY() + height, top, bot);
            int edge = selected ? 0xFFFFD35A : !active ? 0xFF2C3330 : hover ? 0xFFE8C46A : 0xFF8A7A44;
            g.renderOutline(getX(), getY(), width, height, edge);
            int col = !active ? 0xFF6A706C : selected ? 0xFFFFE9A8 : 0xFFEDE6D0;
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, col);
        }
    }

    private final DilophosaurusEntity pet;
    private int tab = 0, selTalent = -1;
    private int left, top;
    private EditBox nameBox;
    private long resetUntil = 0L, chibiUntil = 0L;

    public DiloScreen(DilophosaurusEntity pet) {
        super(Component.literal("Dilofosaurio"));
        this.pet = pet;
    }

    private ProdigyProgress prog() { return pet.getProgress(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() { super.tick(); if (!pet.isAlive() || pet.isRemoved()) this.onClose(); }

    @Override protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(2, (this.height - H) / 2);
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        int tw = (W - 8) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            Button b = TB.builder(Component.literal(TABS[i]), x -> { tab = t; rebuild(); })
                .bounds(left + 4 + i * tw, top + 4, tw - 2, 16).build();
            ((Themed) b).selected = tab == i;
            this.addRenderableWidget(b);
        }
        this.addRenderableWidget(TB.builder(Component.literal("x"), x -> onClose()).bounds(left + W - 18, top - 14, 16, 12).build());
        switch (tab) {
            case 0 -> buildCommands();
            case 3 -> buildTalents();
            case 4 -> buildGear();
            default -> { }
        }
    }

    private void buildCommands() {
        boolean adult = pet.isAdultStage();
        for (int i = 0; i < PetOrders.DILO_COUNT; i++) {
            final int ord = i;
            int col = i % 4, row = i / 4;
            Button b = TB.builder(Component.literal(PetOrders.DILO_NAMES[i]), x -> {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), ord));
                if (ord == PetOrders.ATTACK) onClose();
            }).bounds(left + 150 + col * 61, top + 36 + row * 22, 59, 18).build();
            b.active = pet.getOrder() != i && !pet.isFallen() && (i != PetOrders.ATTACK || adult);
            this.addRenderableWidget(b);
        }
        nameBox = new EditBox(this.font, left + 150, top + 148, 150, 16, Component.literal("Nombre"));
        nameBox.setMaxLength(32);
        nameBox.setValue(pet.hasCustomName() ? pet.getCustomName().getString() : "");
        this.addRenderableWidget(nameBox);
        this.addRenderableWidget(TB.builder(Component.literal("Renombrar"), x ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.RenamePet(pet.getId(), nameBox.getValue()))).bounds(left + 304, top + 147, 70, 18).build());
        Button chibi = TB.builder(Component.literal(System.currentTimeMillis() < chibiUntil ? "¿Seguro? Pulsa otra vez" : "Volver a cría"), x -> {
            if (System.currentTimeMillis() < chibiUntil) { ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 5, 0)); chibiUntil = 0L; }
            else chibiUntil = System.currentTimeMillis() + 3000L;
            rebuild();
        }).bounds(left + 150, top + 172, 130, 18).build();
        chibi.active = adult && !pet.isFallen();
        this.addRenderableWidget(chibi);
    }

    private void buildTalents() {
        if (!pet.isAdultStage()) return;
        Talent[] all = prog().table();
        int[] colCount = new int[4];
        for (int i = 0; i < all.length; i++) {
            final int idx = i;
            Talent t = all[i];
            int c = branchOf(t);
            int row = colCount[c]++;
            int x = c == 3 ? left + 12 + 2 * 124 : left + 12 + c * 124, y = c == 3 ? top + 168 : top + 46 + row * 17;
            int r = prog().rank(i);
            String label = t.name + " " + r + "/" + t.maxRank;
            Button b = TB.builder(Component.literal(label), bt -> { selTalent = idx; rebuild(); }).bounds(x, y, 120, 15).build();
            b.active = selTalent != i;
            this.addRenderableWidget(b);
        }
        if (selTalent >= 0) {
            Button up = TB.builder(Component.literal("Subir rango"), x -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 0, selTalent)))
                .bounds(left + 12, top + 228, 80, 16).build();
            up.active = prog().whyNotUnlock(selTalent) == null;
            this.addRenderableWidget(up);
        }
        this.addRenderableWidget(TB.builder(Component.literal(System.currentTimeMillis() < resetUntil ? "¿Seguro?" : "Restablecer"), x -> {
            if (System.currentTimeMillis() < resetUntil) { ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 1, 0)); resetUntil = 0L; }
            else resetUntil = System.currentTimeMillis() + 3000L;
            rebuild();
        }).bounds(left + W - 92, top + 228, 80, 16).build());
    }

    private static int branchOf(Talent t) {
        char c = t.id.charAt(0);
        return c == 'C' ? 0 : c == 'M' ? 1 : c == 'S' ? 2 : 3;
    }

    private void buildGear() {
        Button sad = TB.builder(Component.literal("Quitar silla"), x -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 3, 0)))
            .bounds(left + 290, top + 164, 100, 18).build();
        sad.active = pet.isSaddled();
        this.addRenderableWidget(sad);
        Button arm = TB.builder(Component.literal("Quitar armadura"), x -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 4, 0)))
            .bounds(left + 290, top + 186, 100, 18).build();
        arm.active = pet.getArmorTier() > 0;
        this.addRenderableWidget(arm);
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (nameBox != null && nameBox.isFocused()) {
            if (key == 257 || key == 335) { ModNetwork.CHANNEL.sendToServer(new ModNetwork.RenamePet(pet.getId(), nameBox.getValue())); nameBox.setFocused(false); return true; }
            return nameBox.keyPressed(key, scan, mods) || nameBox.canConsumeInput() || super.keyPressed(key, scan, mods);
        }
        if (ClientKeys.OPEN.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int accent = DiloMountHud.ACCENT[Math.max(0, Math.min(4, pet.getVariant()))];
        g.pose().pushPose();
        g.pose().translate(left - 10, top - 10, 0.0F);
        g.pose().scale((W + 20) / 840.0F, (H + 20) / 544.0F, 1.0F);
        g.blit(BG, 0, 0, 840, 544, 0.0F, 0.0F, 840, 544, 840, 544);
        g.pose().popPose();
        g.fill(left + 4, top + 22, left + W - 4, top + 23, accent);
        switch (tab) {
            case 0 -> drawCommands(g, accent);
            case 1 -> drawStats(g, accent);
            case 2 -> drawAbilities(g, accent);
            case 3 -> drawTalents(g);
            case 4 -> drawGear(g, accent);
            default -> drawVariants(g, accent);
        }
        super.render(g, mx, my, pt);
    }

    private void drawPortrait(GuiGraphics g, int x0, int y0, int x1, int y1, int accent) {
        g.fill(x0, y0, x1, y1, 0xFF0B100E);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, accent);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, (x0 + x1) / 2, y1 - 8, pet.isAdultStage() ? 30 : 50, (x0 + x1) / 2 - 20f, y0 + 30f, pet);
    }

    private void drawHeader(GuiGraphics g, int x, int y) {
        String name = pet.hasCustomName() ? pet.getCustomName().getString() : "Dilofosaurio";
        g.drawString(this.font, name, x, y, GOLD, true);
        String sub = (pet.isAdultStage() ? "Adulto" : "Cría") + " · " + DilophosaurusEntity.VARIANT_NAMES[Math.max(0, Math.min(4, pet.getVariant()))]
            + " · Nv " + prog().level() + (pet.isFemale() ? " · Hembra" : " · Macho");
        g.drawString(this.font, sub, x, y + 11, DIM, false);
    }

    private void drawCommands(GuiGraphics g, int accent) {
        drawPortrait(g, left + 10, top + 30, left + 140, top + 190, accent);
        g.drawString(this.font, "Órdenes", left + 150, top + 28, GOLD, false);
        int ord = pet.getOrder();
        int hi = Math.max(0, Math.min(PetOrders.DILO_COUNT - 1, ord));
        g.drawString(this.font, "Actual: " + PetOrders.DILO_NAMES[hi], left + 150, top + 84, TEXT, false);
        String help = PetOrders.DILO_HELP[hi];
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(help), 230);
        for (int i = 0; i < lines.size() && i < 4; i++) g.drawString(this.font, lines.get(i), left + 150, top + 96 + i * 10, DIM, false);
        g.drawString(this.font, "Nombre", left + 150, top + 138, GOLD, false);
        drawHeader(g, left + 12, top + 194);
        float hp = pet.getHealth(), max = pet.getMaxHealth();
        g.fill(left + 12, top + 222, left + 140, top + 230, 0xFF1A0E0E);
        g.fill(left + 12, top + 222, left + 12 + Math.round(128 * Math.min(1.0F, hp / Math.max(1.0F, max))), top + 230, 0xFF5FB04A);
        g.drawString(this.font, Math.round(hp) + " / " + Math.round(max), left + 12, top + 232, TEXT, false);
        if (pet.isFallen()) g.drawString(this.font, "Fuera de combate: reanímalo con carne de cordero dorada", left + 150, top + 196, 0xFFE0453A, false);
    }

    private void drawStats(GuiGraphics g, int accent) {
        drawPortrait(g, left + 10, top + 30, left + 140, top + 190, accent);
        drawHeader(g, left + 12, top + 194);
        int x = left + 156, y = top + 32;
        ProdigyProgress p = prog();
        int need = p.xpToNext();
        String[][] rows = {
            {"Nivel", Integer.toString(p.level())},
            {"Experiencia", p.level() >= 100 ? "máximo" : p.xp() + " / " + need},
            {"Vida", Math.round(pet.getHealth()) + " / " + Math.round(pet.getMaxHealth())},
            {"Daño de ataque", String.format("%.1f", pet.getAttributeValue(Attributes.ATTACK_DAMAGE))},
            {"Armadura", String.format("%.1f", pet.getAttributeValue(Attributes.ARMOR))},
            {"Velocidad", String.format("%.1f bloques/s", pet.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17)},
            {"Resist. empuje", String.format("%.0f%%", pet.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 100.0)},
            {"Puntos de talento", p.pointsAvailable() + " libres (" + p.pointsEarned() + " ganados)"},
            {"Talentos", p.talentsUnlocked() + " desbloqueados"},
            {"Confianza (salvaje)", pet.isTame() ? "domado" : pet.getTrust() + " / " + pet.trustNeeded()}};
        for (String[] r : rows) {
            g.drawString(this.font, r[0], x, y, DIM, false);
            g.drawString(this.font, r[1], x + 110, y, TEXT, false);
            y += 14;
        }
        if (p.level() < 100) {
            int bw = 230;
            g.fill(x, y + 4, x + bw, y + 10, 0xFF1A1A1A);
            g.fill(x, y + 4, x + Math.round(bw * Math.min(1.0F, p.xp() / (float) Math.max(1, need))), y + 10, accent);
        }
        g.drawString(this.font, pet.isAdultStage() ? "Los números son los reales de la criatura (talentos y equipo incluidos)." : "Crece con carne cruda hasta ser adulto (nivel 30).", x, top + 214, DIM, false);
    }

    private static final ResourceLocation[] AB_ICON = new ResourceLocation[5];
    static {
        String[] n = {"bite", "claw", "pounce", "frenzy", "spit"};
        for (int i = 0; i < 5; i++) AB_ICON[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/dilo/ab_" + n[i] + ".png");
    }

    private void drawAbilities(GuiGraphics g, int accent) {
        g.drawString(this.font, pet.isAdultStage() ? "Habilidades" : "Habilidades (el adulto las usa todas; la cría solo muerde)", left + 12, top + 27, GOLD, false);
        String[] keys = {"Clic izq. / " + ClientKeys.DILO_BITE.getTranslatedKeyMessage().getString(), "Clic der. / " + ClientKeys.DILO_CLAW.getTranslatedKeyMessage().getString(),
            ClientKeys.DILO_POUNCE.getTranslatedKeyMessage().getString(), ClientKeys.DILO_FRENZY.getTranslatedKeyMessage().getString(), ClientKeys.DILO_SPIT.getTranslatedKeyMessage().getString()};
        double[] cd = {DiloConfig.get(DiloConfig.BITE_COOLDOWN_TICKS, 14) / 20.0, DiloConfig.get(DiloConfig.CLAW_COOLDOWN_SECONDS, 4), DiloConfig.get(DiloConfig.POUNCE_COOLDOWN_SECONDS, 9),
            DiloConfig.get(DiloConfig.FRENZY_COOLDOWN_SECONDS, 45), DiloConfig.get(DiloConfig.SPIT_COOLDOWN_SECONDS, 8)};
        boolean spitOn = prog().rank("S7") > 0;
        for (int i = 0; i < 5; i++) {
            int y = top + 38 + i * 42;
            g.fill(left + 10, y, left + W - 10, y + 39, 0xB0080C0B);
            g.renderOutline(left + 10, y, W - 20, 39, i == 3 ? GOLD : accent);
            g.pose().pushPose();
            g.pose().translate(left + 14, y + 4, 0.0F);
            g.pose().scale(31.0F / 128.0F, 31.0F / 128.0F, 1.0F);
            g.blit(AB_ICON[i], 0, 0, 128, 128, 0.0F, 0.0F, 128, 128, 128, 128);
            g.pose().popPose();
            int tx = left + 52;
            g.drawString(this.font, DilophosaurusEntity.ABILITY_NAMES[i], tx, y + 3, GOLD, false);
            g.drawString(this.font, "Tecla: " + keys[i] + "  ·  Recarga base: " + String.format("%.1f", cd[i]) + " s", tx, y + 13, TEXT, false);
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(ABILITY_DESC[i]), W - 150);
            for (int k = 0; k < lines.size() && k < 2; k++) g.drawString(this.font, lines.get(k), tx, y + 23 + k * 9, DIM, false);
            float l = pet.cooldownLeft(i);
            boolean locked = i == 4 ? !spitOn : !pet.isAdultStage() && i != 0;
            String st = locked ? "Bloqueada" : l > 0 ? pet.cooldownSeconds(i) + " s" : "Lista";
            g.drawString(this.font, st, left + W - 16 - this.font.width(st), y + 3, l > 0 || locked ? 0xFFE0A040 : 0xFF6FD05A, false);
        }
    }

    private void drawTalents(GuiGraphics g) {
        if (!pet.isAdultStage()) {
            g.drawCenteredString(this.font, "El árbol de talentos se abre cuando evoluciona a adulto (nivel 30).", left + W / 2, top + 100, DIM);
            return;
        }
        ProdigyProgress p = prog();
        g.drawString(this.font, "Puntos libres: " + p.pointsAvailable(), left + 12, top + 28, GOLD, false);
        for (int c = 0; c < 3; c++) g.drawString(this.font, BRANCH_NAME[c], left + 12 + c * 124, top + 36, BRANCH_COLOR[c], false);
        g.drawString(this.font, BRANCH_NAME[3], left + 12 + 2 * 124, top + 158, BRANCH_COLOR[3], false);
        if (selTalent >= 0) {
            Talent t = p.table()[selTalent];
            g.drawString(this.font, t.name + " (rango " + p.rank(selTalent) + "/" + t.maxRank + ")", left + 12, top + 160, BRANCH_COLOR[branchOf(t)], false);
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(t.effect), 240);
            for (int i = 0; i < lines.size() && i < 4; i++) g.drawString(this.font, lines.get(i), left + 12, top + 172 + i * 10, TEXT, false);
            String why = p.whyNotUnlock(selTalent);
            g.drawString(this.font, why == null ? "Disponible: coste " + t.cost[Math.min(p.rank(selTalent), t.maxRank - 1)] : why, left + 100, top + 232, why == null ? 0xFF6FD05A : 0xFFE0A040, false);
        } else g.drawString(this.font, "Elige un talento para ver su efecto.", left + 12, top + 160, DIM, false);
    }

    private static final String[] TIER_NAME = {"Hierro", "Oro", "Diamante", "Netherita"};
    private static final int[] TIER_COLOR = {0xFFB8BCC8, 0xFFE8B83A, 0xFF4EE0E8, 0xFFB060F0};

    private void drawGear(GuiGraphics g, int accent) {
        drawPortrait(g, left + 10, top + 30, left + 140, top + 190, accent);
        drawHeader(g, left + 12, top + 194);
        g.drawString(this.font, "Armaduras ligeras (no cubren crestas ni membrana)", left + 150, top + 27, GOLD, false);
        int cur = pet.getArmorTier();
        for (int t = 1; t <= 4; t++) {
            int x = left + 150 + (t - 1) * 62, y = top + 38, w = 59, h = 112;
            boolean on = cur == t;
            g.fill(x, y, x + w, y + h, 0xC00A0F0E);
            g.renderOutline(x, y, w, h, on ? GOLD : TIER_COLOR[t - 1]);
            if (on) g.renderOutline(x + 1, y + 1, w - 2, h - 2, GOLD);
            g.drawCenteredString(this.font, TIER_NAME[t - 1], x + w / 2, y + 4, TIER_COLOR[t - 1]);
            g.pose().pushPose();
            g.pose().translate(x + w / 2.0F - 16.0F, y + 16.0F, 0.0F);
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.renderItem(new ItemStack(ProdigyCreatures.DILO_ARMOR_ITEMS.get(t - 1).get()), 0, 0);
            g.pose().popPose();
            String[] lines = {"Armadura +" + (int) DiloArmorItem.ARMOR[t], "Dureza +" + (int) DiloArmorItem.TOUGHNESS[t], "Vida +" + (int) DiloArmorItem.HEALTH[t],
                "Ataque +" + DiloArmorItem.ATTACK[t], "Veloc. " + Math.round(DiloArmorItem.SPEED[t] * 100) + "%"};
            for (int i = 0; i < lines.length; i++) {
                g.pose().pushPose();
                g.pose().translate(x + 5, y + 52 + i * 11, 0.0F);
                g.pose().scale(0.8F, 0.8F, 1.0F);
                g.drawString(this.font, lines[i], 0, 0, i == 1 && DiloArmorItem.TOUGHNESS[t] == 0 ? 0xFF5A605C : TEXT, false);
                g.pose().popPose();
            }
        }
        g.drawString(this.font, "Silla", left + 150, top + 158, GOLD, false);
        g.renderItem(new ItemStack(Items.SADDLE), left + 150, top + 168);
        g.drawString(this.font, pet.isSaddled() ? "Equipada (montable)" : pet.isAdultStage() ? "Sin silla (usa una)" : "Solo el adulto", left + 170, top + 172, TEXT, false);
        g.drawString(this.font, "Armadura total: " + String.format("%.1f", pet.getAttributeValue(Attributes.ARMOR)), left + 150, top + 188, DIM, false);
        g.drawString(this.font, "Equipa con clic derecho sosteniendo la pieza.", left + 150, top + 204, DIM, false);
    }

    private void drawVariants(GuiGraphics g, int accent) {
        g.drawString(this.font, "Variantes: solo cambian el color; misma geometría, animaciones y estadísticas.", left + 12, top + 28, GOLD, false);
        for (int i = 0; i < DilophosaurusEntity.VARIANTS; i++) {
            int y = top + 42 + i * 38;
            boolean cur = pet.getVariant() == i;
            g.fill(left + 10, y, left + W - 10, y + 34, 0xFF0B100E);
            g.renderOutline(left + 10, y, W - 20, 34, cur ? GOLD : 0xFF3A4440);
            g.fill(left + 16, y + 5, left + 40, y + 29, DiloMountHud.ACCENT[i]);
            g.drawString(this.font, DilophosaurusEntity.VARIANT_NAMES[i] + (cur ? "  (esta criatura)" : ""), left + 48, y + 6, cur ? GOLD : TEXT, false);
            g.drawString(this.font, VARIANT_DESC[i], left + 48, y + 18, DIM, false);
        }
    }
}
