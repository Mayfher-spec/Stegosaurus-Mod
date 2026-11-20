package com.prodigytown.creatures.client;

import com.prodigytown.creatures.CompyEntity;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.DiloConfig;
import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyPet;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de companero del estegosaurio y del dilofosaurio (tecla K) con el arte de los mockups del usuario: tres paginas (Comandos, Ataques,
 * Talentos) sobre fondos limpios (textures/gui/stego2 y dilo2, 1086 px de ancho) y todo lo variable dibujado en vivo. Las tablas de coordenadas
 * estan en pixeles de la imagen (1086 de ancho); se dibujan escaladas a 724 de ancho virtual y luego con sc para que quepan siempre en pantalla.
 * Solo cliente; el servidor valida cada accion.
 */
public final class PetSkinScreen extends Screen {
    private static final float K = 724.0F / 1086.0F;
    private static final int W = 724;
    private static final String MOD = ProdigyCreatures.MODID;
    private static final ResourceLocation ATLAS = TexButton.TEX;

    private static final int GOLD = 0xFFFFD35A, CREAM = 0xFFF6E3A1, TEXT = 0xFFEDEDED, DIM = 0xFF9AA0A6;
    private static final int[] BRANCH_COLOR = {0xFFE8602C, 0xFF3FB4FF, 0xFF7CE03C, 0xFFFFE066};

    // ---------------------------------------------------------------- datos por especie (coordenadas de la imagen)
    private static final class Skin {
        final ResourceLocation[] bg = new ResourceLocation[4];
        int[] texH;
        int[][] close, namebox, portrait, hp, exp, statY;
        int[][][] tabs;
        int[] statRight, lvl;
        int[] variantC, pointsC, resetBox, medal, bar, panelIcon, panelPill, btn, overlay1, overlay2, atkDetailIcon;
        int panelTitleX, panelTitleY, panelTitleW, panelEffX, panelEffY, panelEffW, reqHeadY;
        int[] cardX, cardY, cardW, cardH;
        int[][] cmdTitle; // {x, y} del titulo y {x, y, ancho} del cuerpo
        int[][] atkCard; // {x0,y0,x1,y1} de las 4 tarjetas y la alta
        int[][] atkCircle, atkNameXY, atkPillXY;
        int[] tall, tallName;
        int[][] detPill;
        int detTitleX, detTitleY, detDescX, detDescY, detDescW, detPillOff;
        int[] colX, rowY;
        int[][] plateC; // {x, nombre y, sub y} por columna
        int[] ultNodesY;
        int[] progText;
        String[] colName, colSub;
        int[] colOfBranch;
        // ---- solo Compy: el fondo ya trae nombres, pastillas, nodos y lineas dibujados
        boolean baked;
        int circ = 96, nodeHalf;
        int[] detOff, detKey, passive, overlay0, p1, lock, nodeX, nodeY;
        String[] nodeId;
        // ---- Compy: pestana Manada (grupo de caza)
        int[] overlay3, pkTitle, pkDesc;
        int[][] pkCards, pkButtons, pkCheck, pkCheckRow, pkSelRow, pkSelL, pkSelR, pkSelVal, pkChip, pkArt;
        int pkCheckHalf, pkNameY, pkLvlY;
    }

    private static final Skin STEGO = new Skin(), DILO = new Skin(), COMPY = new Skin();
    static {
        for (int i = 0; i < 3; i++) {
            String n = new String[]{"cmd", "atk", "tal"}[i];
            STEGO.bg[i] = new ResourceLocation(MOD, "textures/gui/stego2/" + n + ".png");
            DILO.bg[i] = new ResourceLocation(MOD, "textures/gui/dilo2/" + n + ".png");
        }
        // ---- estegosaurio
        Skin s = STEGO;
        s.texH = new int[]{723, 746, 629};
        s.close = new int[][]{{1005, 50, 1052, 97}, {995, 52, 1050, 100}, {992, 55, 1038, 95}};
        s.tabs = new int[][][]{{{160, 640, 408, 690}, {428, 640, 665, 690}, {685, 640, 925, 690}},
            {{172, 655, 410, 702}, {425, 653, 665, 705}, {680, 655, 912, 702}},
            {{325, 58, 535, 98}, {550, 58, 755, 98}, {768, 58, 977, 98}}};
        s.namebox = new int[][]{{200, 78, 430, 116}, {200, 80, 440, 126}, {155, 60, 295, 95}};
        s.portrait = new int[][]{{66, 152, 425, 380}, {62, 150, 405, 395}, {52, 120, 252, 276}};
        s.lvl = new int[]{251, 409, 235, 425, 150, 335};
        s.hp = new int[][]{{100, 433, 415, 462}, {98, 452, 402, 483}, null};
        s.exp = new int[][]{{68, 597, 417, 615}, null, {47, 358, 253, 377}};
        s.statRight = new int[]{410, 398, 250};
        s.statY = new int[][]{{489, 516, 543, 570}, {511, 541, 570, 599}, {403, 424, 445, 466}};
        s.variantC = new int[]{150, 291};
        s.pointsC = new int[]{168, 500, 523};
        s.resetBox = new int[]{68, 548, 250, 582};
        s.medal = new int[]{490, 430, 640, 520};
        s.bar = new int[]{441, 577, 705, 592};
        s.progText = new int[]{716, 579, 0};
        s.panelIcon = new int[]{812, 134, 868, 190};
        s.panelPill = new int[]{880, 166, 1000, 188};
        s.btn = new int[]{806, 478, 1030, 518};
        s.overlay1 = new int[]{415, 140, 1045, 615};
        s.overlay2 = new int[]{280, 110, 1045, 540};
        s.panelTitleX = 880; s.panelTitleY = 138; s.panelTitleW = 150;
        s.panelEffX = 812; s.panelEffY = 206; s.panelEffW = 215; s.reqHeadY = 350;
        s.cardX = new int[]{458, 658, 858}; s.cardY = new int[]{138, 303}; s.cardW = new int[]{190, 190, 187}; s.cardH = new int[]{154, 152};
        s.cmdTitle = new int[][]{{527, 492}, {490, 525, 540}};
        s.atkCard = new int[][]{{420, 148, 650, 273}, {657, 148, 887, 273}, {420, 283, 650, 408}, {657, 283, 887, 408}, {893, 148, 1040, 408}};
        s.atkCircle = new int[][]{{432, 162}, {672, 162}, {432, 297}, {672, 297}};
        s.atkNameXY = new int[][]{{535, 165}, {775, 165}, {535, 300}, {775, 300}};
        s.atkPillXY = new int[][]{{538, 226, 638, 254}, {775, 226, 878, 254}, {535, 361, 638, 388}, {775, 361, 878, 388}, {905, 352, 1030, 383}};
        s.tallName = new int[]{966, 290};
        s.detPill = new int[][]{{455, 558, 615, 592}, {635, 558, 822, 592}, {842, 558, 1008, 592}};
        s.atkDetailIcon = new int[]{460, 430, 526, 496};
        s.detTitleX = 545; s.detTitleY = 442; s.detDescX = 535; s.detDescY = 490; s.detDescW = 480; s.detPillOff = 42;
        s.colX = new int[]{367, 543, 697}; s.rowY = new int[]{245, 288, 331, 374, 417};
        s.plateC = new int[][]{{407, 186, 205}, {562, 186, 205}, {721, 186, 205}};
        s.ultNodesY = new int[]{395};
        s.colName = new String[]{"Combate", "Defensa", "Soporte"};
        s.colSub = new String[]{"Cola y embestida", "Guardia y vida", "Apoyo y manada"};
        s.colOfBranch = new int[]{0, 1, 2, 3};
        // ---- dilofosaurio
        s = DILO;
        s.texH = new int[]{723, 697, 629};
        s.close = new int[][]{{1005, 48, 1052, 95}, {1010, 50, 1060, 96}, {1018, 45, 1057, 82}};
        s.tabs = new int[][][]{{{165, 630, 408, 680}, {425, 632, 665, 680}, {682, 632, 928, 680}},
            {{152, 610, 398, 658}, {418, 607, 670, 660}, {688, 610, 935, 658}},
            {{198, 573, 400, 610}, {438, 573, 653, 610}, {690, 573, 925, 610}}};
        s.namebox = new int[][]{{190, 68, 445, 120}, {180, 72, 470, 120}, {145, 58, 335, 96}};
        s.portrait = new int[][]{{64, 150, 384, 380}, {54, 148, 378, 370}, {52, 112, 285, 285}};
        s.lvl = new int[]{224, 406, 218, 405, 154, 335};
        s.hp = new int[][]{{99, 432, 388, 458}, {99, 431, 387, 458}, {77, 357, 289, 375}};
        s.exp = new int[][]{{80, 583, 371, 600}, null, null};
        s.statRight = new int[]{368, 372, 280};
        s.statY = new int[][]{{484, 510, 536, 562}, {482, 508, 534, 560}, {396, 416, 436, 456}};
        s.variantC = new int[]{170, 311};
        s.pointsC = new int[]{190, 486, 505};
        s.resetBox = new int[]{93, 525, 268, 555};
        s.medal = new int[]{500, 405, 620, 490};
        s.bar = new int[]{458, 541, 692, 555};
        s.progText = new int[]{832, 542, 1};
        s.panelIcon = new int[]{828, 126, 884, 182};
        s.panelPill = new int[]{896, 158, 1010, 180};
        s.btn = new int[]{827, 452, 1033, 492};
        s.overlay1 = new int[]{400, 128, 1050, 585};
        s.overlay2 = new int[]{305, 100, 1055, 500};
        s.panelTitleX = 896; s.panelTitleY = 130; s.panelTitleW = 140;
        s.panelEffX = 828; s.panelEffY = 196; s.panelEffW = 205; s.reqHeadY = 338;
        s.cardX = new int[]{417, 628, 840}; s.cardY = new int[]{135, 288}; s.cardW = new int[]{202, 202, 202}; s.cardH = new int[]{143, 140};
        s.cmdTitle = new int[][]{{462, 467}, {450, 517, 570}};
        s.atkCard = new int[][]{{408, 135, 642, 260}, {652, 135, 884, 260}, {408, 267, 642, 388}, {652, 267, 884, 388}, {892, 135, 1046, 388}};
        s.atkCircle = new int[][]{{418, 148}, {660, 148}, {418, 277}, {660, 277}};
        s.atkNameXY = new int[][]{{522, 156}, {764, 156}, {522, 285}, {764, 285}};
        s.atkPillXY = new int[][]{{520, 208, 630, 240}, {763, 208, 870, 240}, {520, 337, 630, 369}, {763, 337, 870, 369}, {905, 330, 1035, 365}};
        s.tallName = new int[]{969, 262};
        s.detPill = new int[][]{{432, 516, 620, 552}, {640, 516, 830, 552}, {850, 516, 1012, 552}};
        s.atkDetailIcon = new int[]{421, 406, 505, 490};
        s.detTitleX = 545; s.detTitleY = 423; s.detDescX = 515; s.detDescY = 462; s.detDescW = 500; s.detPillOff = 40;
        s.colX = new int[]{393, 560, 722}; s.rowY = new int[]{212, 256, 300, 344, 388};
        s.plateC = new int[][]{{420, 150, 170}, {589, 150, 170}, {748, 150, 170}};
        s.ultNodesY = new int[]{0};
        s.colName = new String[]{"Cazador", "Supervivencia", "Movilidad"};
        s.colSub = new String[]{"Daño y cacería", "Aguante y veneno", "Ritmo y saltos"};
        s.colOfBranch = new int[]{0, 2, 1, 1}; // rama de la tabla -> columna (roja, verde, azul)
        // ---- compy (coordenadas generadas por tools/compy/gui.py a partir de las maquetas del usuario)
        s = COMPY;
        for (int i = 0; i < 4; i++) s.bg[i] = new ResourceLocation(MOD, "textures/gui/compy2/" + new String[]{"cmd", "atk", "tal", "pack"}[i] + ".png");
        s.baked = true;
        s.texH = CompyGui.TEX_H;
        s.close = CompyGui.CLOSE;
        s.tabs = CompyGui.TABS;
        s.namebox = CompyGui.NAMEBOX; s.portrait = CompyGui.PORTRAIT;
        s.lvl = CompyGui.LVL; s.hp = CompyGui.HP;
        s.exp = new int[][]{CompyGui.EXP[0], CompyGui.EXP[1], null, CompyGui.EXP[3]};
        s.statRight = CompyGui.STAT_RIGHT; s.statY = CompyGui.STAT_Y;
        s.variantC = new int[]{0, 0}; s.pointsC = CompyGui.POINTS_C; s.resetBox = CompyGui.RESET_BOX;
        s.medal = CompyGui.MEDAL; s.bar = CompyGui.BAR;
        s.progText = CompyGui.PROG_TEXT;
        s.panelIcon = CompyGui.PANEL_ICON; s.panelPill = CompyGui.PANEL_PILL; s.btn = CompyGui.BTN;
        s.overlay0 = CompyGui.OVERLAY0; s.overlay1 = CompyGui.OVERLAY1; s.overlay2 = CompyGui.OVERLAY2; s.overlay3 = CompyGui.OVERLAY3;
        s.panelTitleX = CompyGui.PANEL_TITLE[0]; s.panelTitleY = CompyGui.PANEL_TITLE[1]; s.panelTitleW = CompyGui.PANEL_TITLE[2];
        s.panelEffX = CompyGui.PANEL_EFF[0]; s.panelEffY = CompyGui.PANEL_EFF[1]; s.panelEffW = CompyGui.PANEL_EFF[2];
        s.reqHeadY = CompyGui.REQ_HEAD_Y;
        s.cardX = CompyGui.CARD_X; s.cardY = CompyGui.CARD_Y; s.cardW = CompyGui.CARD_W; s.cardH = CompyGui.CARD_H;
        s.cmdTitle = CompyGui.CMD_TITLE;
        s.atkCard = CompyGui.ATK_CARD; s.atkCircle = CompyGui.ATK_CIRCLE; s.circ = CompyGui.ATK_CIRC; s.passive = CompyGui.ATK_PASSIVE;
        s.atkDetailIcon = CompyGui.ATK_DETAIL_ICON;
        s.detTitleX = CompyGui.DET_TITLE[0]; s.detTitleY = CompyGui.DET_TITLE[1];
        s.detDescX = CompyGui.DET_DESC[0]; s.detDescY = CompyGui.DET_DESC[1]; s.detDescW = CompyGui.DET_DESC[2];
        s.detPill = CompyGui.DET_PILL; s.detOff = CompyGui.DET_OFF; s.detKey = CompyGui.DET_KEY; s.detPillOff = CompyGui.DET_OFF[0];
        s.nodeId = CompyGui.NODE_ID; s.nodeX = CompyGui.NODE_X; s.nodeY = CompyGui.NODE_Y; s.nodeHalf = CompyGui.NODE_HALF;
        s.p1 = CompyGui.P1; s.lock = CompyGui.LOCK;
        s.pkTitle = CompyGui.PK_TITLE; s.pkDesc = CompyGui.PK_DESC; s.pkCards = CompyGui.PK_CARDS; s.pkButtons = CompyGui.PK_BUTTONS;
        s.pkCheck = CompyGui.PK_CHECK; s.pkCheckRow = CompyGui.PK_CHECK_ROW; s.pkCheckHalf = CompyGui.PK_CHECK_HALF;
        s.pkSelRow = CompyGui.PK_SEL_ROW; s.pkSelL = CompyGui.PK_SEL_L; s.pkSelR = CompyGui.PK_SEL_R; s.pkSelVal = CompyGui.PK_SEL_VAL;
        s.pkChip = CompyGui.PK_CHIP; s.pkArt = CompyGui.PK_ART; s.pkNameY = CompyGui.PK_NAME_Y; s.pkLvlY = CompyGui.PK_LVL_Y;
        s.colOfBranch = new int[]{0, 2, 1, 1};
        s.colX = new int[]{0, 0, 0}; s.rowY = new int[]{0, 0, 0, 0, 0}; s.plateC = new int[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}}; s.ultNodesY = new int[]{0};
        s.colName = new String[]{"", "", ""}; s.colSub = new String[]{"", "", ""};
    }

    // ---------------------------------------------------------------- habilidades
    private record Ab(String[] name, String pill, String title, String desc, String[] stats, Item item) {}

    private static final String[] STEGO_ULT_NAME = {"Tormenta cibernetica", "Resplandor dorado", "Erupcion de magma", "Llamarada espectral", "Estampida natural"};
    private static final String[] STEGO_ULT_DESC = {
        "Rayos sobre hasta 8 enemigos cercanos (3x dano de ataque).",
        "Cura 40 % a ti y a tus aliados cercanos, y ciega y danya a los enemigos.",
        "Onda de fuego: 2,5x dano, enciende 8 s y empuja a los enemigos.",
        "Fuego espectral verde (el doble de dano que el normal) 8 s, 2x dano y debilidad.",
        "Pisoton: 1,5x dano, lentitud fuerte y empuje; te regenera."};
    private static final String[] STEGO_VARIANT = {"Cibernetico", "Dorado", "Magma", "Esmeralda", "Natural"};
    private static final int[] CARD_ORDER_STEGO = {PetOrders.FOLLOW, PetOrders.STAY, PetOrders.WANDER, PetOrders.PATROL, PetOrders.SIT, PetOrders.SLEEP};
    private static final int[] CARD_ORDER_DILO = {PetOrders.STAY, PetOrders.WANDER, PetOrders.FOLLOW, PetOrders.PATROL, PetOrders.SIT, PetOrders.SLEEP};

    // ---------------------------------------------------------------- estado
    private final TamableAnimal pet;
    private final StegosaurusEntity stego;
    private final DilophosaurusEntity dilo;
    private final CompyEntity compy;
    private final Skin skin;
    private int tab = 0, selAtk = 0, selected = 0;
    private boolean ultSel = false, renaming = false;
    private float sc = 1.0F;
    private int left, top, hv;
    private EditBox nameBox;
    private long resetConfirmUntil = 0L, chibiConfirmUntil = 0L;
    // distribucion calculada del arbol: indice de talento -> centro (px de la imagen)
    private int[][] nodePos;

    public PetSkinScreen(TamableAnimal pet) {
        super(Component.literal(pet instanceof DilophosaurusEntity ? "Dilofosaurio" : pet instanceof CompyEntity ? "Compy" : "Estegosaurio"));
        this.pet = pet;
        this.stego = pet instanceof StegosaurusEntity s ? s : null;
        this.dilo = pet instanceof DilophosaurusEntity d ? d : null;
        this.compy = pet instanceof CompyEntity c ? c : null;
        this.skin = compy != null ? COMPY : dilo != null ? DILO : STEGO;
    }

    // ---------------------------------------------------------------- acceso comun a las dos especies
    private ProdigyProgress prog() { return ((ProdigyPet) pet).getProgress(); }
    private int idx(String id) { Talent[] t = prog().table(); for (int i = 0; i < t.length; i++) if (t[i].id.equals(id)) return i; return -1; }
    private boolean adult() { return compy != null || dilo == null || dilo.isAdultStage(); }
    /** Compy soldado: su interfaz esta bloqueada (modo seguidor del lider). */
    private int tabCount() { return compy != null ? 4 : 3; }
    private boolean locked() { return compy != null && !compy.isLeader(); }
    private boolean saddled() { return compy == null && (dilo != null ? dilo.isSaddled() : stego.isSaddled()); }
    private int armorTier() { return compy != null ? 0 : dilo != null ? dilo.getArmorTier() : stego.getArmorTier(); }
    private int variant() { return compy != null ? compy.getVariant() : Math.max(0, Math.min(4, dilo != null ? dilo.getVariant() : stego.getVariant())); }
    private boolean fallen() { return compy == null && (dilo != null ? dilo.isFallen() : stego.isFallen()); }
    private int order() { return compy != null ? compy.getOrder() : dilo != null ? dilo.getOrder() : stego.getOrder(); }
    private String variantName() { return compy != null ? CompyEntity.VARIANT_NAMES[variant()] : dilo != null ? DilophosaurusEntity.VARIANT_NAMES[variant()] : STEGO_VARIANT[variant()]; }
    private String petName() { return compy != null ? "Compy" : dilo != null ? "Dilofosaurio" : "Estegosaurio"; }

    // ---------------------------------------------------------------- conversion de coordenadas (imagen -> virtual)
    private float X(float px) { return left + px * K; }
    private float Y(float px) { return top + px * K; }
    private int[] VR(int[] r) { return new int[]{(int) X(r[0]), (int) Y(r[1]), (int) X(r[2]), (int) Y(r[3])}; }

    /** Boton invisible sobre una zona del fondo: solo se ilumina al pasar el raton. */
    private static final class Hit extends Button {
        Hit(int x, int y, int w, int h, OnPress press) { super(x, y, w, h, Component.empty(), press, DEFAULT_NARRATION); }
        static Hit box(float x0, float y0, float x1, float y1, OnPress press) { return new Hit((int) x0, (int) y0, Math.max(1, (int) (x1 - x0)), Math.max(1, (int) (y1 - y0)), press); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            if (this.active && this.isHoveredOrFocused()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x22FFFFFF);
        }
    }

    private Hit hit(int[] r, Button.OnPress press) { return Hit.box(X(r[0]), Y(r[1]), X(r[2]), Y(r[3]), press); }

    /** Boton con el icono de un objeto (quitar silla / armadura). */
    private final class GearButton extends Button {
        final boolean armor;
        GearButton(int x, int y, boolean armor, OnPress press) {
            super(x, y, 22, 22, Component.empty(), press, DEFAULT_NARRATION);
            this.armor = armor;
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            g.pose().pushPose(); g.pose().translate(0.0F, 0.0F, 200.0F); // por encima del modelo del retrato (que se dibuja a z=50)
            draw(g);
            g.pose().popPose();
        }
        private void draw(GuiGraphics g) {
            int x = getX(), y = getY();
            boolean on = this.active;
            g.fill(x, y, x + width, y + height, on ? 0xCC10161C : 0x8810161C);
            g.renderOutline(x, y, width, height, on ? (isHoveredOrFocused() ? GOLD : 0xFFB8862A) : 0xFF4A4F54);
            Item it = armor ? new Item[]{Items.LEATHER_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE}[Math.max(0, Math.min(4, armorTier()))] : Items.SADDLE;
            g.pose().pushPose();
            g.pose().translate(x + 3.0F, y + 3.0F, 0.0F);
            if (!on) g.setColor(1.0F, 1.0F, 1.0F, 0.35F);
            g.renderItem(new ItemStack(it), 0, 0);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().popPose();
            if (on) g.fill(x + width - 7, y + 1, x + width - 1, y + 7, 0xFFB02A22);
        }
    }

    /** Boton "volver a pequeno": icono de huevo; pide confirmar con un segundo clic (3 s). Solo activo en adulto. */
    private final class ChibiButton extends Button {
        ChibiButton(int x, int y, OnPress press) { super(x, y, 22, 22, Component.empty(), press, DEFAULT_NARRATION); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            g.pose().pushPose(); g.pose().translate(0.0F, 0.0F, 200.0F);
            draw(g);
            g.pose().popPose();
        }
        private void draw(GuiGraphics g) {
            int x = getX(), y = getY();
            boolean on = this.active, armed = System.currentTimeMillis() < chibiConfirmUntil, up = !adult();
            g.fill(x, y, x + width, y + height, on ? 0xCC10161C : 0x8810161C);
            g.renderOutline(x, y, width, height, !on ? 0xFF4A4F54 : armed ? 0xFFE0453A : (isHoveredOrFocused() ? GOLD : 0xFFB8862A));
            g.pose().pushPose();
            g.pose().translate(x + 3.0F, y + 3.0F, 0.0F);
            if (!on) g.setColor(1.0F, 1.0F, 1.0F, 0.35F);
            g.renderItem(new ItemStack(up ? Items.BEEF : Items.EGG), 0, 0);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().popPose();
            if (on && up) {
                int cx = x + width - 4;
                g.fill(cx - 1, y + 3, cx + 1, y + 8, 0xFF6FE06A);
                g.fill(cx - 3, y + 4, cx + 3, y + 5, 0xFF6FE06A);
                g.fill(cx - 2, y + 3, cx + 2, y + 4, 0xFF6FE06A);
                g.fill(cx - 1, y + 2, cx + 1, y + 3, 0xFF6FE06A);
            } else if (on) {
                int cx = x + width - 4;
                g.fill(cx - 1, y + 2, cx + 1, y + 7, 0xFF58C7F2);
                g.fill(cx - 3, y + 6, cx + 3, y + 7, 0xFF58C7F2);
                g.fill(cx - 2, y + 7, cx + 2, y + 8, 0xFF58C7F2);
                g.fill(cx - 1, y + 8, cx + 1, y + 9, 0xFF58C7F2);
            }
        }
    }

    // ---------------------------------------------------------------- inicio
    @Override protected void init() {
        hv = Math.round(skin.texH[tab] * K);
        sc = Math.max(0.4F, Math.min(1.35F, Math.min((this.width - 8.0F) / W, (this.height - 8.0F) / hv)));
        left = (int) ((this.width / sc - W) / 2.0F);
        top = (int) ((this.height / sc - hv) / 2.0F);
        buildTree();
        addRenderableWidget(hit(skin.close[tab], b -> this.onClose()));
        for (int i = 0; i < tabCount(); i++) {
            if (i == tab) continue;
            final int t = i;
            addRenderableWidget(hit(skin.tabs[tab][i], b -> { tab = t; renaming = false; rebuildWidgets(); }));
        }
        int[] nb = VR(skin.namebox[tab]);
        nameBox = new EditBox(this.font, nb[0] + 2, nb[1] + (nb[3] - nb[1] - 18) / 2, nb[2] - nb[0] - 4, 18, Component.literal("Nombre"));
        nameBox.setMaxLength(32);
        nameBox.setValue(pet.hasCustomName() ? pet.getCustomName().getString() : "");
        nameBox.setHint(Component.literal("Nombre"));
        nameBox.visible = renaming; nameBox.active = renaming;
        addRenderableWidget(hit(skin.namebox[tab], b -> { renaming = true; nameBox.visible = true; nameBox.active = true; setFocused(nameBox); nameBox.setFocused(true); }));
        addRenderableWidget(nameBox);
        int[] pr = VR(skin.portrait[tab]);
        if (compy == null) {
            addRenderableWidget(new GearButton(pr[0] + 4, pr[3] - 28, false, b -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 3, 0))));
            addRenderableWidget(new GearButton(pr[2] - 26, pr[3] - 28, true, b -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 4, 0))));
            ChibiButton toChibi = new ChibiButton((pr[0] + pr[2]) / 2 - 11, pr[3] - 28, b -> {
                long now = System.currentTimeMillis();
                if (!adult()) {
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 6, 0));
                    this.onClose();
                } else if (now < chibiConfirmUntil) {
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 5, 0));
                    this.onClose();
                } else chibiConfirmUntil = now + 3000L;
            });
            toChibi.active = true;
            addRenderableWidget(toChibi);
        }
        if (locked()) return;   // soldado: solo se puede mirar (pestanas, cerrar); nada de ordenes, habilidades ni talentos
        switch (tab) {
            case 0 -> {
                int[] ord = dilo != null || compy != null ? CARD_ORDER_DILO : CARD_ORDER_STEGO;
                for (int i = 0; i < 6; i++) {
                    final int k = i;
                    int cx = skin.cardX[i % 3], cy = skin.cardY[i / 3];
                    addRenderableWidget(Hit.box(X(cx), Y(cy), X(cx + skin.cardW[i % 3]), Y(cy + skin.cardH[i / 3]),
                        b -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), ord[k]))));
                }
                if (dilo != null) {
                    addRenderableWidget(Hit.box(X(700), Y(562), X(860), Y(590), b -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), PetOrders.DEFEND))));
                    addRenderableWidget(Hit.box(X(872), Y(562), X(1030), Y(590), b -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), PetOrders.ATTACK))));
                }
            }
            case 3 -> initPack();
            case 1 -> {
                for (int i = 0; i < 5; i++) {
                    final int k = i;
                    addRenderableWidget(hit(skin.atkCard[i], b -> selAtk = k));
                }
            }
            default -> {
                addRenderableWidget(hit(skin.btn, b -> {
                    if (!ultSel && adult()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 0, selected));
                }));
                addRenderableWidget(hit(skin.resetBox, b -> {
                    long now = System.currentTimeMillis();
                    if (now < resetConfirmUntil) { ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 1, 0)); resetConfirmUntil = 0L; }
                    else resetConfirmUntil = now + 3000L;
                }));
                addRenderableWidget(hit(skin.medal, b -> {
                    if (dilo != null || compy != null) selected = idx("P1"); else ultSel = true;
                }));
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() {
        super.tick();
        if (!pet.isAlive() || pet.isRemoved()) this.onClose();
        if (tab == 3 && compy != null && compy.isLeader() && ++pkRefresh >= 20) { pkRefresh = 0; pkSend(0, 0, 0, null); }
    }

    private void sendName() {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.RenamePet(pet.getId(), nameBox.getValue()));
        renaming = false; nameBox.setFocused(false); nameBox.visible = false; nameBox.active = false;
    }

    /** Al renombrar, las letras van siempre a la caja de texto: el clic en la zona del nombre deja el foco en el boton invisible y la caja no recibia nada. */
    @Override public boolean charTyped(char c, int mods) {
        if (renaming && nameBox != null) { nameBox.setFocused(true); return nameBox.charTyped(c, mods); }
        return super.charTyped(c, mods);
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (renaming) {
            if (key == 257 || key == 335) { sendName(); return true; }
            if (key == 256) { renaming = false; nameBox.setFocused(false); nameBox.visible = false; nameBox.active = false; return true; }
            return nameBox.keyPressed(key, scan, mods) || true;
        }
        if (ClientKeys.OPEN.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        mx /= sc; my /= sc;
        if (tab == 2 && button == 0 && adult() && !locked()) {
            Talent[] all = prog().table();
            float ns = nodeSize();
            for (int i = 0; i < all.length; i++) {
                if (nodePos[i] == null) continue;
                float x = X(nodePos[i][0]) - ns / 2.0F, y = Y(nodePos[i][1]) - ns / 2.0F;
                if (mx >= x && mx < x + ns && my >= y && my < y + ns) { selected = i; ultSel = false; return true; }
            }
        }
        boolean res = super.mouseClicked(mx, my, button);
        if (renaming && nameBox != null) { setFocused(nameBox); nameBox.setFocused(true); }   // el boton invisible acaba de robar el foco
        return res;
    }

    // ---------------------------------------------------------------- utilidades de dibujo
    private void text(GuiGraphics g, String s, float x, float y, float scale, int color, boolean shadow) {
        g.pose().pushPose(); g.pose().translate(x, y, 0.0F); g.pose().scale(scale, scale, 1.0F);
        g.drawString(this.font, s, 0, 0, color, shadow); g.pose().popPose();
    }
    private void textC(GuiGraphics g, String s, float cx, float y, float scale, int color, boolean shadow) {
        text(g, s, cx - this.font.width(s) * scale / 2.0F, y, scale, color, shadow);
    }
    private void textR(GuiGraphics g, String s, float xr, float y, float scale, int color, boolean shadow) {
        text(g, s, xr - this.font.width(s) * scale, y, scale, color, shadow);
    }
    /** Parrafo con ajuste de linea; devuelve la y siguiente. */
    private float para(GuiGraphics g, String s, float x, float y, float width, float scale, float pitch, int color, int maxLines) {
        List<FormattedCharSequence> lines = this.font.split(Component.literal(s), (int) (width / scale));
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.pose().pushPose(); g.pose().translate(x, y, 0.0F); g.pose().scale(scale, scale, 1.0F);
            g.drawString(this.font, lines.get(i), 0, 0, color, false); g.pose().popPose();
            y += pitch;
        }
        return y;
    }
    private String shorten(String s, float px, float scale) {
        while (this.font.width(s) * scale > px && s.length() > 3) s = s.substring(0, s.length() - 1);
        return s;
    }
    private void frame(GuiGraphics g, float x0, float y0, float x1, float y1, int color, int glow) {
        int a = (int) x0, b = (int) y0, c = (int) x1, d = (int) y1;
        g.fill(a, b, c, d, glow);
        g.renderOutline(a, b, c - a, d - b, color);
        g.renderOutline(a + 1, b + 1, c - a - 2, d - b - 2, (color & 0x00FFFFFF) | 0x66000000);
    }
    private static String sec(double s) {
        return (s == Math.rint(s) ? String.valueOf((int) s) : String.format("%.1f", s).replace('.', ',')) + " s";
    }
    private static String[] split2(String s) {
        int mid = s.length() / 2, best = -1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == ' ' && (best < 0 || Math.abs(i - mid) < Math.abs(best - mid))) best = i;
        return best < 0 ? new String[]{s} : new String[]{s.substring(0, best), s.substring(best + 1)};
    }

    // ---------------------------------------------------------------- dibujo
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int rmx = mx, rmy = my;
        mx = (int) (mx / sc); my = (int) (my / sc);
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1.0F);
        g.blit(skin.bg[tab], left, top, W, hv, 0.0F, 0.0F, 1086, skin.texH[tab], 1086, skin.texH[tab]);
        renderLeft(g, mx, my);
        switch (tab) {
            case 0 -> renderCommands(g);
            case 1 -> renderAttacks(g);
            case 3 -> renderPack(g, mx, my);
            default -> renderTalents(g);
        }
        if (locked()) {
            int[] o = VR(tab == 0 ? skin.overlay0 : tab == 1 ? skin.overlay1 : tab == 3 ? skin.overlay3 : skin.overlay2);
            g.fill(o[0], o[1], o[2], o[3], 0xE6101418);
            g.renderOutline(o[0], o[1], o[2] - o[0], o[3] - o[1], 0xFFB8862A);
            float cx = (o[0] + o[2]) / 2.0F, cy = (o[1] + o[3]) / 2.0F;
            textC(g, "Soldado de la manada", cx, cy - 26, 1.6F, GOLD, true);
            textC(g, "Su interfaz está bloqueada en modo seguidor:", cx, cy - 4, 1.1F, TEXT, false);
            textC(g, "acompaña al líder y copia sus órdenes.", cx, cy + 8, 1.1F, TEXT, false);
            textC(g, "Comandos, ataques, talentos y el grupo de caza son del líder", cx, cy + 28, 1.0F, DIM, false);
            textC(g, "(el Compy con las marcas rojas en la cola).", cx, cy + 40, 1.0F, DIM, false);
        }
        if (!adult() && tab > 0) {
            int[] o = VR(tab == 1 ? skin.overlay1 : skin.overlay2);
            g.fill(o[0], o[1], o[2], o[3], 0xF0101418);
            g.renderOutline(o[0], o[1], o[2] - o[0], o[3] - o[1], 0xFFB8862A);
            float cx = (o[0] + o[2]) / 2.0F, cy = (o[1] + o[3]) / 2.0F;
            textC(g, tab == 1 ? "Sus ataques se despiertan al crecer" : "Los talentos se desbloquean al crecer", cx, cy - 10, 1.5F, GOLD, true);
            textC(g, "Alimentalo con carne cruda o usa el boton de carne del retrato.", cx, cy + 10, 1.1F, TEXT, false);
        }
        super.render(g, mx, my, pt);
        int[] pr = VR(skin.portrait[tab]);
        if (compy == null && my >= pr[3] - 28 && my < pr[3] - 6) {
            if (mx >= pr[0] + 4 && mx < pr[0] + 26) g.renderTooltip(this.font, Component.literal(saddled() ? "Quitar silla" : "Sin silla (clic con una silla en la mano)"), rmx, rmy);
            else if (mx >= pr[2] - 26 && mx < pr[2] - 4) g.renderTooltip(this.font, Component.literal(armorTier() > 0 ? "Quitar armadura" : "Sin armadura (clic con una en la mano)"), rmx, rmy);
            else if (mx >= (pr[0] + pr[2]) / 2 - 11 && mx < (pr[0] + pr[2]) / 2 + 11) g.renderTooltip(this.font, Component.literal(!adult() ? "Hacerlo crecer (adulto)"
                : System.currentTimeMillis() < chibiConfirmUntil ? "Pulsa otra vez para confirmar" : "Volver a pequeno (conserva silla y armadura)"), rmx, rmy);
        }
        int[] nb = VR(skin.namebox[tab]);
        if (!renaming && mx >= nb[0] && mx < nb[2] && my >= nb[1] && my < nb[3]) g.renderTooltip(this.font, Component.literal("Clic para ponerle nombre"), rmx, rmy);
        g.pose().popPose();
        if (tab == 2 && adult() && !locked()) renderTalentTooltip(g, mx, my, rmx, rmy);
    }

    /** Panel izquierdo: nombre, retrato con la criatura real, nivel, vida, estadisticas (y XP / puntos si la pagina los tiene). */
    private void renderLeft(GuiGraphics g, int mx, int my) {
        ProdigyProgress p = prog();
        int[] nb = skin.namebox[tab];
        if (!renaming) {
            String nm = pet.hasCustomName() ? pet.getCustomName().getString() : locked() ? "Compy soldado" : petName();
            float scale = tab == 2 ? 1.2F : 2.0F;
            nm = shorten(nm, (nb[2] - nb[0]) * K - 6, scale);
            float cx = X((nb[0] + nb[2]) / 2.0F), cy = Y((nb[1] + nb[3]) / 2.0F) - 4.0F * scale;
            textC(g, nm, cx + 1.0F, cy + 1.0F, scale, 0xFF2A1A08, false);
            textC(g, nm, cx, cy, scale, CREAM, false);
        }
        int[] pr = VR(skin.portrait[tab]);
        int x0 = pr[0], y0 = pr[1], x1 = pr[2], y1 = pr[3];
        g.enableScissor((int) (x0 * sc), (int) (y0 * sc), (int) (x1 * sc), (int) (y1 * sc));
        boolean ad = adult();
        float k = (x1 - x0) / 238.0F;
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, (x0 + x1) / 2, y1 - (int) ((compy != null ? 22 : 14) * k) - 4, (int) ((compy != null ? 95.0F : ad ? (dilo != null ? 30.0F : 24.0F) : 56.0F) * k),
            (float) ((x0 + x1) / 2 - mx), (float) (y0 + (y1 - y0) / 3 - my), pet);
        g.disableScissor();
        textC(g, locked() ? "Soldado" : "Nv. " + p.level(), X(skin.lvl[tab * 2]), Y(skin.lvl[tab * 2 + 1]) - 6.0F, 1.5F, CREAM, true);
        float hp = Math.max(0.0F, pet.getHealth()), max = Math.max(1.0F, pet.getMaxHealth());
        int[] h = skin.hp[tab];
        if (h != null) {
            float bx = X(h[0]), by = Y(h[1]), bw = (h[2] - h[0]) * K, bh = (h[3] - h[1]) * K;
            int w = Math.round(bw * Math.min(1.0F, hp / max));
            boolean weak = hp / max < PetOrders.WEAK_FRACTION;
            if (w > 0) {
                g.fillGradient((int) bx, (int) by, (int) bx + w, (int) (by + bh), weak ? 0xFFE8483C : 0xFF4CD83A, weak ? 0xFF9A1E16 : 0xFF1E8C14);
                g.fill((int) bx, (int) by, (int) bx + w, (int) by + 2, 0x55FFFFFF);
            }
            textC(g, Math.round(hp) + " / " + Math.round(max), bx + bw / 2.0F, by + bh / 2.0F - 4.5F * 1.2F, 1.2F, 0xFFFFFFFF, true);
        }
        int[] e = locked() ? null : skin.exp[tab];
        if (e != null) {
            float bx = X(e[0]), by = Y(e[1]), bw = (e[2] - e[0]) * K, bh = (e[3] - e[1]) * K;
            int need = p.xpToNext();
            int fill = need <= 0 ? (int) bw : Math.min((int) bw, (int) (bw * (p.xp() / (float) Math.max(1, need))));
            if (fill > 0) { g.fillGradient((int) bx, (int) by, (int) bx + fill, (int) (by + bh), 0xFFB060FF, 0xFF6A2CC0); g.fill((int) bx, (int) by, (int) bx + fill, (int) by + 1, 0x66FFFFFF); }
            textC(g, need <= 0 ? "Nivel maximo" : "EXP " + p.xp() + " / " + need, bx + bw / 2.0F, by + bh / 2.0F - 4.0F * 0.95F, 0.95F, 0xFFFFFFFF, true);
        }
        String[] v = {String.valueOf(Math.round(max)), String.valueOf((int) pet.getAttributeValue(Attributes.ATTACK_DAMAGE)),
            String.valueOf((int) pet.getAttributeValue(Attributes.ARMOR)), String.format("%.2f", pet.getAttributeValue(Attributes.MOVEMENT_SPEED))};
        for (int i = 0; i < 4; i++) textR(g, v[i], X(skin.statRight[tab]), Y(skin.statY[tab][i]) - 5.0F, tab == 2 ? 1.0F : 1.2F, 0xFFFFFFFF, true);
        if (tab == 2) {
            if (compy == null) textC(g, shorten("Variante: " + variantName(), 88, 0.8F), X(skin.variantC[0]), Y(skin.variantC[1]) - 3.5F, 0.8F, 0xFF9BE08A, false);
            textC(g, "Puntos de talento", X(skin.pointsC[0]), Y(skin.pointsC[1]) - 4.0F, 0.8F, 0xFFE9D9A6, true);
            textC(g, String.valueOf(p.pointsAvailable()), X(skin.pointsC[0]), Y(skin.pointsC[2]) - 8.0F, 2.0F, 0xFFFFFFFF, true);
            if (System.currentTimeMillis() < resetConfirmUntil) {
                int[] r = VR(skin.resetBox);
                g.fill(r[0], r[1], r[2], r[3], 0xCC3A1010);
                g.renderOutline(r[0], r[1], r[2] - r[0], r[3] - r[1], 0xFFE0453A);
                textC(g, "Seguro? Clic otra vez", (r[0] + r[2]) / 2.0F, (r[1] + r[3]) / 2.0F - 4.0F, 1.0F, 0xFFFFE0D0, true);
            }
        }
    }

    // ---------------------------------------------------------------- manada (grupo de caza del Compy)
    private static final class PkMember { java.util.UUID id; String name = ""; int variant, hp, mhp, lv = -1, role = -1; }
    private final List<PkMember> pkMembers = new ArrayList<>();
    private int[] pkSet = {63, 0, 0, 3, 5, 0};
    private java.util.UUID pkSel;
    private int pkPage = 0;
    private long dissolveConfirmUntil = 0L, releaseConfirmUntil = 0L;
    private static final int[] PK_PREV = {896, 146, 944, 174}, PK_NEXT = {986, 146, 1034, 174}, PK_REL = {330, 146, 470, 174};   // pager y "Liberar" (coords. de la maqueta)
    private int pkRefresh = 0;
    private static final int[] ROLE_COL = {0xFF3FB4FF, 0xFFE6B422, 0xFF7CE03C};
    private static final String[] CHECK_BITMAP = {"......X", ".....XX", "X...XX.", "XX.XX..", ".XXX...", "..X...."};
    private static final String[] BTN_HELP = {
        "Crear grupo: reúne en este grupo a todos tus compys cercanos (y a sus manadas) hasta el máximo.",
        "Invitar cercano: suma al compy tuyo más cercano que no esté en el grupo (a toda su manada si es líder).",
        "Expulsar: el compy elegido deja el grupo y forma el suyo. Elígelo con un clic en su tarjeta.",
        "Nombrar líder: el compy elegido pasa a mandar el grupo y hereda el nivel y los talentos del líder actual.",
        "Disolver grupo: todos los soldados dejan el grupo y van cada uno por su cuenta (pulsa dos veces)."};
    private static final String[] CHECK_HELP = {
        "Seguir al líder: los soldados acompañan al líder y ocupan su sitio en la formación. Apagado, siguen a su dueño.",
        "Atacar mismo objetivo: los soldados atacan lo que ataca el líder (no en modo Defensivo ni Pasivo).",
        "Cazar en grupo: los soldados salen a cazar solos cerca del líder según el modo, el radio y la prioridad.",
        "Volver al silbido: la tecla de silbido hace que la manada deje todo y vuelva con el líder.",
        "Proteger al líder: los soldados atacan a quien hiera al líder.",
        "Reagruparse si hay peligro: con el líder por debajo de la mitad de vida, los soldados se juntan a su alrededor."};
    private static final String[] SEL_HELP = {
        "Modo de caza: Coordinado (cazan cerca del líder), Agresivo (cazan más lejos), Defensivo (solo protegen) o Pasivo (nunca atacan).",
        "Formación: el sitio de los soldados alrededor del líder al seguirlo (Abanico, Línea, Círculo o Libre).",
        "Radio: distancia al líder dentro de la cual los soldados buscan presas, protegen y se reagrupan.",
        "Tamaño máx.: miembros que admite el grupo, líder incluido (de 2 a 10). Si está lleno, nadie más cabe.",
        "Prioridad: qué eligen cazar primero: presas pequeñas, hostiles, lo más cercano o lo más débil."};
    private static final String[] ROLE_HELP = {
        "Explorador: más veloz; marca con brillo a los hostiles cercanos. Clic para cambiar de rol.",
        "Rastreador: pega un 20 % más a presas marcadas o heridas. Clic para cambiar de rol.",
        "Apoyo: cura al compy más herido que tenga cerca, pero pega algo menos. Clic para cambiar de rol."};
    private static final String[] PRIO_TXT = {"las presas pequeñas", "los hostiles", "lo más cercano", "lo más débil"};

    private void pkSend(int action, int a, int b, java.util.UUID id) {
        if (compy == null) return;
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.GroupAction(pet.getId(), action, a, b, id == null ? 0L : id.getMostSignificantBits(), id == null ? 0L : id.getLeastSignificantBits()));
    }

    /** Lee el estado del grupo que el servidor publica en el lider (ajustes y miembros). */
    private void pkParse() {
        pkMembers.clear();
        net.minecraft.nbt.CompoundTag t = compy.getGroupTag();
        int[] st = t.getIntArray("s");
        if (st.length >= 6) pkSet = st;
        net.minecraft.nbt.ListTag l = t.getList("m", 10);
        for (int i = 0; i < l.size(); i++) {
            net.minecraft.nbt.CompoundTag c = l.getCompound(i);
            PkMember m = new PkMember();
            m.id = c.getUUID("id"); m.name = c.getString("n"); m.variant = c.getInt("v"); m.hp = c.getInt("hp"); m.mhp = c.getInt("mh"); m.lv = c.getInt("lv"); m.role = c.getInt("r");
            pkMembers.add(m);
        }
        if (pkMembers.isEmpty()) {   // aun sin datos del servidor: solo el lider
            PkMember m = new PkMember();
            m.id = compy.getUUID(); m.name = compy.hasCustomName() ? compy.getCustomName().getString() : "Compy Alfa";
            m.hp = Math.round(compy.getHealth()); m.mhp = Math.round(compy.getMaxHealth()); m.lv = prog().level();
            pkMembers.add(m);
        }
        if (pkSel != null && pkMembers.stream().noneMatch(m -> m.id.equals(pkSel))) pkSel = null;
    }

    private void initPack() {
        pkSend(0, 0, 0, null);
        for (int s = 0; s < 5; s++) {   // las pastillas de rol van primero: ganan el clic sobre la tarjeta
            final int slot = s;
            addRenderableWidget(hit(skin.pkChip[s], b -> {
                int idx = pkPage * 5 + slot;
                if (idx == 0) { pkSel = null; return; }
                if (idx > 0 && idx < pkMembers.size()) pkSend(8, 0, 0, pkMembers.get(idx).id);
                else if (idx >= pkMembers.size() && idx < pkSet[4]) pkSend(2, 0, 0, null);
            }));
        }
        for (int s = 0; s < 5; s++) {
            final int slot = s;
            addRenderableWidget(hit(skin.pkCards[s], b -> {
                int idx = pkPage * 5 + slot;
                if (idx == 0) { pkSel = null; return; }
                if (idx < pkMembers.size()) { java.util.UUID id = pkMembers.get(idx).id; pkSel = id.equals(pkSel) ? null : id; }
                else if (idx < pkSet[4]) pkSend(2, 0, 0, null);
            }));
        }
        addRenderableWidget(hit(PK_PREV, b -> { if (pkPages() > 1) pkPage = Math.floorMod(pkPage - 1, pkPages()); }));
        addRenderableWidget(hit(PK_NEXT, b -> { if (pkPages() > 1) pkPage = Math.floorMod(pkPage + 1, pkPages()); }));
        addRenderableWidget(hit(PK_REL, b -> pkRelease()));
        for (int i = 0; i < 5; i++) { final int k = i; addRenderableWidget(hit(skin.pkButtons[i], b -> pkButton(k))); }
        for (int i = 0; i < 6; i++) {
            final int bit = i;
            addRenderableWidget(hit(skin.pkCheckRow[i], b -> pkSend(6, bit, ((pkSet[0] >> bit) & 1) != 0 ? 0 : 1, null)));
        }
        for (int i = 0; i < 5; i++) {
            final int w = i;
            addRenderableWidget(hit(skin.pkSelL[i], b -> pkSend(7, w, -1, null)));
            addRenderableWidget(hit(skin.pkSelR[i], b -> pkSend(7, w, 1, null)));
        }
    }

    private int pkPages() { return Math.max(1, (Math.max(pkMembers.size(), pkSet[4]) + 4) / 5); }

    /** Liberar: el compy elegido (o el unico del grupo) vuelve a ser salvaje. Pide confirmar con una segunda pulsacion. */
    private void pkRelease() {
        java.util.UUID id = pkSel != null ? pkSel : pkMembers.size() == 1 ? compy.getUUID() : null;
        if (id == null) return;
        long now = System.currentTimeMillis();
        if (now < releaseConfirmUntil) {
            pkSend(9, 0, 0, id);
            releaseConfirmUntil = 0L; pkSel = null;
            if (id.equals(compy.getUUID())) this.onClose();
        } else releaseConfirmUntil = now + 3000L;
    }

    private void pkButton(int k) {
        switch (k) {
            case 0 -> pkSend(1, 0, 0, null);
            case 1 -> pkSend(2, 0, 0, null);
            case 2 -> { if (pkSel != null) { pkSend(3, 0, 0, pkSel); pkSel = null; } }
            case 3 -> { if (pkSel != null) { pkSend(4, 0, 0, pkSel); this.onClose(); } }
            default -> {
                long now = System.currentTimeMillis();
                if (now < dissolveConfirmUntil) { pkSend(5, 0, 0, null); dissolveConfirmUntil = 0L; pkSel = null; }
                else dissolveConfirmUntil = now + 3000L;
            }
        }
    }

    private boolean over(int[] imgRect, int mx, int my) {
        int[] r = VR(imgRect);
        return mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
    }

    private void blitTex(GuiGraphics g, int[] src, int[] dst) {
        int[] d = VR(dst);
        g.blit(skin.bg[tab], d[0], d[1], d[2] - d[0], d[3] - d[1], (float) src[0], (float) src[1], src[2] - src[0], src[3] - src[1], 1086, skin.texH[tab]);
    }

    @org.jetbrains.annotations.Nullable private CompyEntity pkEntity(java.util.UUID id) {
        if (id.equals(compy.getUUID())) return compy;
        if (this.minecraft == null || this.minecraft.level == null) return null;
        for (CompyEntity c : this.minecraft.level.getEntitiesOfClass(CompyEntity.class, compy.getBoundingBox().inflate(80.0), e -> e.getUUID().equals(id))) return c;
        return null;
    }

    private void pkChip(GuiGraphics g, int[] chipImg, int role) {
        int r = Math.max(0, Math.min(2, role)), col = ROLE_COL[r];
        int[] c = VR(chipImg);
        g.fill(c[0], c[1], c[2], c[3], 0xDD0C1410);
        g.renderOutline(c[0], c[1], c[2] - c[0], c[3] - c[1], col);
        int ix = c[0] + 6, iy = (c[1] + c[3]) / 2;
        switch (r) {
            case 0 -> { g.fill(ix - 1, iy - 2, ix + 2, iy + 3, col); g.fill(ix + 3, iy - 2, ix + 6, iy + 3, col); g.fill(ix + 2, iy - 1, ix + 3, iy, col); }   // prismaticos
            case 1 -> { g.fill(ix, iy, ix + 5, iy + 3, col); g.fill(ix - 1, iy - 3, ix + 1, iy - 1, col); g.fill(ix + 1, iy - 4, ix + 3, iy - 2, col); g.fill(ix + 3, iy - 3, ix + 5, iy - 1, col); }   // pata
            default -> { g.fill(ix - 1, iy - 1, ix + 6, iy + 1, col); g.fill(ix + 1, iy - 4, ix + 3, iy + 3, col); }   // cruz de apoyo
        }
        textC(g, CompyEntity.ROLE_NAMES[r], (c[0] + c[2]) / 2.0F + 5.0F, (c[1] + c[3]) / 2.0F - 3.5F, 0.8F, 0xFFFFFFFF, true);
    }

    private void pkCard(GuiGraphics g, int slot, int idx, PkMember m, int mx, int my) {
        int[] cr = VR(skin.pkCards[slot]);
        float cx = (cr[0] + cr[2]) / 2.0F;
        int[] ar = VR(skin.pkArt[slot]);
        CompyEntity e = pkEntity(m.id);
        g.enableScissor((int) ((cr[0] + 3) * sc), (int) ((cr[1] + 3) * sc), (int) ((cr[2] - 3) * sc), (int) (ar[3] * sc));
        if (e != null) InventoryScreen.renderEntityInInventoryFollowsMouse(g, (int) cx, ar[3] - 1, 58, cx - mx, (float) (ar[1] - my), e);
        else textC(g, "?", cx, (ar[1] + ar[3]) / 2.0F - 8.0F, 2.0F, DIM, true);
        g.disableScissor();
        textC(g, shorten(m.name, cr[2] - cr[0] - 8, 1.0F), cx, Y(skin.pkNameY) - 4.0F, 1.0F, 0xFFFFFFFF, true);
        textC(g, m.lv >= 0 ? "Nv. " + m.lv : "PV " + m.hp + "/" + m.mhp, cx, Y(skin.pkLvlY) - 4.0F, 0.95F, CREAM, true);
        if (idx > 0) pkChip(g, skin.pkChip[slot], m.role);
        if (m.id.equals(pkSel)) {
            g.renderOutline(cr[0] - 1, cr[1] - 1, cr[2] - cr[0] + 2, cr[3] - cr[1] + 2, 0xFFFFFFFF);
            g.renderOutline(cr[0] + 1, cr[1] + 1, cr[2] - cr[0] - 2, cr[3] - cr[1] - 2, 0x88FFFFFF);
        }
    }

    private void drawCheck(GuiGraphics g, float cx, float cy) {
        int x0 = (int) (cx - 7), y0 = (int) (cy - 6);
        for (int pass = 0; pass < 2; pass++)
            for (int r = 0; r < CHECK_BITMAP.length; r++)
                for (int c = 0; c < CHECK_BITMAP[r].length(); c++)
                    if (CHECK_BITMAP[r].charAt(c) == 'X') {
                        int x = x0 + c * 2 + (pass == 0 ? 1 : 0), y = y0 + r * 2 + (pass == 0 ? 1 : 0);
                        g.fill(x, y, x + 2, y + 2, pass == 0 ? 0xFF1C3A0C : 0xFFA6F034);
                    }
    }

    private boolean pkFlag(int bit) { return ((pkSet[0] >> bit) & 1) != 0; }

    private String pkSummary() {
        return "Tu grupo de caza " + (pkFlag(0) ? "seguirá al líder" : "seguirá a su dueño") + (pkFlag(1) ? " y atacará el mismo objetivo" : "") + ". "
            + (pkFlag(3) ? "Los compys se reagruparán al silbido" : "Los compys no responden al silbido") + " y priorizarán " + PRIO_TXT[Math.floorMod(pkSet[5], 4)]
            + ". Modo " + CompyEntity.G_MODES[Math.floorMod(pkSet[1], 4)].toLowerCase(java.util.Locale.ROOT) + ", formación " + CompyEntity.G_FORMS[Math.floorMod(pkSet[2], 4)].toLowerCase(java.util.Locale.ROOT) + ".";
    }

    @org.jetbrains.annotations.Nullable private String pkHover(int mx, int my) {
        int n = pkMembers.size();
        if (over(PK_REL, mx, my)) return "Liberar: el compy elegido deja de ser tuyo y vuelve a ser salvaje (sin grupo, sin nombre y sin nivel). Elígelo con un clic en su tarjeta; pulsa dos veces para confirmar.";
        if (pkPages() > 1 && (over(PK_PREV, mx, my) || over(PK_NEXT, mx, my))) return "Cambia de página: el grupo admite hasta 10 compys (5 por página).";
        for (int s = 0; s < 5; s++) { int idx = pkPage * 5 + s; if (idx > 0 && idx < n && over(skin.pkChip[s], mx, my)) return ROLE_HELP[Math.max(0, Math.min(2, pkMembers.get(idx).role))]; }
        for (int i = 0; i < 5; i++) if (over(skin.pkButtons[i], mx, my)) return BTN_HELP[i];
        for (int i = 0; i < 6; i++) if (over(skin.pkCheckRow[i], mx, my)) return CHECK_HELP[i];
        for (int i = 0; i < 5; i++) if (over(skin.pkSelRow[i], mx, my)) return SEL_HELP[i];
        for (int s = 0; s < 5; s++) if (over(skin.pkCards[s], mx, my)) {
            int idx = pkPage * 5 + s;
            if (idx == 0) return "El líder del grupo (el Compy con las marcas rojas en la cola). Sus talentos y habilidades mejoran a todo el grupo.";
            if (idx < n) return "Clic para elegir a este compy (luego Expulsar, Liberar o Nombrar líder); clic en su pastilla para cambiar su rol.";
            return idx < pkSet[4] ? "Hueco libre: clic para invitar al compy tuyo más cercano." : "Hueco cerrado: sube el Tamaño máx. del grupo (hasta 10) para usarlo.";
        }
        return null;
    }

    /** Pager de paginas (grupos de mas de 5) y boton Liberar, dibujados sobre la maqueta. */
    private void pkControls(GuiGraphics g, int mx, int my, int n) {
        int pages = pkPages();
        if (pages > 1) {
            for (int[] r : new int[][]{PK_PREV, PK_NEXT}) {
                int[] b = VR(r);
                boolean hv = over(r, mx, my);
                g.fill(b[0], b[1], b[2], b[3], hv ? 0xFF3A2E10 : 0xFF1E1A0C);
                g.renderOutline(b[0], b[1], b[2] - b[0], b[3] - b[1], hv ? 0xFFFFD35A : 0xFFB8892A);
                textC(g, r == PK_PREV ? "‹" : "›", (b[0] + b[2]) / 2.0F, (b[1] + b[3]) / 2.0F - 5.0F, 1.6F, 0xFFFFE9A8, true);
            }
            int[] p0 = VR(PK_PREV), p1 = VR(PK_NEXT);
            textC(g, (pkPage + 1) + " / " + pages, (p0[2] + p1[0]) / 2.0F, (p0[1] + p0[3]) / 2.0F - 4.0F, 1.0F, CREAM, true);
        }
        boolean can = pkSel != null || n == 1;
        int[] b = VR(PK_REL);
        boolean confirm = System.currentTimeMillis() < releaseConfirmUntil && can;
        g.fill(b[0], b[1], b[2], b[3], confirm ? 0xFF4A1612 : 0xFF2A1210);
        g.renderOutline(b[0], b[1], b[2] - b[0], b[3] - b[1], can ? (over(PK_REL, mx, my) || confirm ? 0xFFFF6A5A : 0xFFC9453A) : 0xFF5A3A36);
        textC(g, confirm ? "¿Seguro? Otra vez" : "Liberar", (b[0] + b[2]) / 2.0F, (b[1] + b[3]) / 2.0F - 4.0F, 0.95F, can ? 0xFFFFE0D0 : 0xFF8A7470, true);
    }

    private void renderPack(GuiGraphics g, int mx, int my) {
        pkParse();
        int n = pkMembers.size(), max = pkSet[4];
        textC(g, "Miembros del grupo (" + n + " / " + max + ")", X(skin.pkTitle[0]), Y(skin.pkTitle[1]) - 5.5F, 1.4F, CREAM, true);
        pkPage = Math.max(0, Math.min(pkPage, pkPages() - 1));
        if (pkPage > 0) blitTex(g, new int[]{330, 172, 350, 181}, new int[]{352, 171, 454, 181});   // tapa la etiqueta "Líder" si no estamos en la primera pagina
        for (int s = 0; s < 5; s++) {   // fondo de cada hueco: lider / miembro / libre ("Invitar compy") / cerrado (sobre el tamaño maximo)
            int idx = pkPage * 5 + s;
            int kind = idx == 0 ? 0 : idx < n ? 1 : idx < max ? 2 : 3;
            int baked = s == 0 ? 0 : s < 4 ? 1 : 2;
            int eff = kind == 3 ? 2 : kind;
            if (eff != baked) blitTex(g, eff == 1 ? skin.pkCards[s == 4 ? 3 : 1] : skin.pkCards[4], skin.pkCards[s]);
            if (kind == 3) { int[] c = VR(skin.pkCards[s]); g.fill(c[0] + 2, c[1] + 2, c[2] - 2, c[3] - 2, 0xB0101418); }
        }
        for (int s = 0; s < 5; s++) { int idx = pkPage * 5 + s; if (idx < n) pkCard(g, s, idx, pkMembers.get(idx), mx, my); }
        pkControls(g, mx, my, n);
        boolean full = n >= max;
        boolean[] off = {full, full, pkSel == null, pkSel == null, n <= 1};
        for (int i = 0; i < 5; i++) if (off[i]) { int[] b = VR(skin.pkButtons[i]); g.fill(b[0] + 2, b[1] + 2, b[2] - 2, b[3] - 2, 0xA0101418); }
        if (System.currentTimeMillis() < dissolveConfirmUntil) {
            int[] b = VR(skin.pkButtons[4]);
            g.fill(b[0] + 2, b[1] + 2, b[2] - 2, b[3] - 2, 0xDD3A1010);
            g.renderOutline(b[0], b[1], b[2] - b[0], b[3] - b[1], 0xFFE0453A);
            textC(g, "¿Seguro? Otra vez", (b[0] + b[2]) / 2.0F, (b[1] + b[3]) / 2.0F - 4.0F, 0.9F, 0xFFFFE0D0, true);
        }
        for (int i = 0; i < 6; i++) if (pkFlag(i)) drawCheck(g, X(skin.pkCheck[i][0]), Y(skin.pkCheck[i][1]));
        String[] vals = {CompyEntity.G_MODES[Math.floorMod(pkSet[1], 4)], CompyEntity.G_FORMS[Math.floorMod(pkSet[2], 4)],
            CompyEntity.G_RADII[Math.floorMod(pkSet[3], CompyEntity.G_RADII.length)] + " bloques", String.valueOf(pkSet[4]), CompyEntity.G_PRIOS[Math.floorMod(pkSet[5], 4)]};
        for (int i = 0; i < 5; i++) textC(g, vals[i], X(skin.pkSelVal[i][0]), Y(skin.pkSelVal[i][1]) - 5.0F, 1.3F, 0xFFFFFFFF, true);
        String desc = pkHover(mx, my);
        para(g, desc != null ? desc : pkSummary(), X(skin.pkDesc[0]), Y(skin.pkDesc[1]), skin.pkDesc[2] * K, 1.0F, 11.0F, TEXT, 3);
    }

    // ---------------------------------------------------------------- comandos
    private void renderCommands(GuiGraphics g) {
        boolean fall = fallen();
        int cur = order();
        int[] ord = dilo != null || compy != null ? CARD_ORDER_DILO : CARD_ORDER_STEGO;
        for (int i = 0; i < 6; i++) {
            int cx = skin.cardX[i % 3], cy = skin.cardY[i / 3];
            float x0 = X(cx), y0 = Y(cy), x1 = X(cx + skin.cardW[i % 3]), y1 = Y(cy + skin.cardH[i / 3]);
            boolean sel = !fall && ord[i] == cur;
            if (fall) g.fill((int) x0 + 3, (int) y0 + 3, (int) x1 - 3, (int) y1 - 3, 0x99000000);
            if (sel) frame(g, x0 - 1, y0 - 1, x1 + 1, y1 + 1, GOLD, 0x24FFD35A);
        }
        String[] names = dilo != null ? PetOrders.DILO_NAMES : PetOrders.NAMES;
        String[] help = dilo != null ? PetOrders.DILO_HELP : PetOrders.HELP;
        String title = fall ? "Fuera de combate" : names[Math.max(0, Math.min(names.length - 1, cur))];
        text(g, title, X(skin.cmdTitle[0][0]), Y(skin.cmdTitle[0][1]), 1.5F, fall ? 0xFFE0453A : GOLD, true);
        String body = fall ? "Dormido: no recibe ordenes. Reanimalo con la ensalada de domesticacion (clic derecho): se recupera con 20 de vida."
            : help[Math.max(0, Math.min(help.length - 1, cur))];
        para(g, body, X(skin.cmdTitle[1][0]), Y(skin.cmdTitle[1][1]), skin.cmdTitle[1][2] * K, 1.2F, 12.0F, TEXT, dilo != null ? 3 : 4);
        if (dilo != null) { // dos ordenes extra del dilofosaurio
            String[] lab = {"Defender", "Atacar objetivo"};
            int[][] bx = {{700, 562, 860, 590}, {872, 562, 1030, 590}};
            int[] ordX = {PetOrders.DEFEND, PetOrders.ATTACK};
            for (int i = 0; i < 2; i++) {
                float a = X(bx[i][0]), b = Y(bx[i][1]), c = X(bx[i][2]), d = Y(bx[i][3]);
                boolean on = !fall && cur == ordX[i];
                g.fill((int) a, (int) b, (int) c, (int) d, on ? 0xCC4A3A10 : 0xCC101418);
                g.renderOutline((int) a, (int) b, (int) (c - a), (int) (d - b), on ? GOLD : 0xFFB8862A);
                textC(g, lab[i], (a + c) / 2.0F, (b + d) / 2.0F - 4.0F, 1.0F, on ? GOLD : 0xFFFFFFFF, true);
            }
        }
    }

    // ---------------------------------------------------------------- ataques
    private Ab[] abilities() {
        ProdigyProgress p = prog();
        float dmg = (float) pet.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (compy != null) {
            String[] key = {ClientKeys.COMPY_BITE.getTranslatedKeyMessage().getString(), ClientKeys.COMPY_LEAP.getTranslatedKeyMessage().getString(),
                ClientKeys.COMPY_SCREECH.getTranslatedKeyMessage().getString(), ClientKeys.COMPY_PACK.getTranslatedKeyMessage().getString()};
            return new Ab[]{
                new Ab(new String[]{"Mordida", "fugaz"}, null, "Mordida fugaz", "Se lanza sobre la presa o el enemigo más cercano y lo muerde con un ataque veloz y preciso." + (p.rank("A7") > 0 ? " Con Depredador alfa muerde dos veces." : ""),
                    new String[]{"Daño: " + Math.round(dmg * 1.4F * (1.0F + 0.08F * p.rank("A1"))), "Recarga: " + sec(CompyEntity.cooldownTicks(p, 0) / 20.0), "Tecla: " + key[0]}, null),
                new Ab(new String[]{"Salto", "evasivo"}, null, "Salto evasivo", "Da un salto ágil lejos del peligro: durante un instante nadie lo alcanza." + (p.rank("C7") > 0 ? " Con Maestro del sigilo se vuelve invisible y su siguiente mordida pega más." : ""),
                    new String[]{"Distancia: " + Math.round(CompyEntity.leapDistance(p)) + " bloques", "Recarga: " + sec(CompyEntity.cooldownTicks(p, 1) / 20.0), "Tecla: " + key[1]}, null),
                new Ab(new String[]{"Chillido", "de alarma"}, null, "Chillido de alarma", "Un chillido agudo que ralentiza, debilita y marca a los monstruos cercanos, y llama a la manada con un empujón de velocidad.",
                    new String[]{"Radio: " + Math.round(CompyEntity.screechRadius(p)) + " bloques", "Recarga: " + sec(CompyEntity.cooldownTicks(p, 2) / 20.0), "Tecla: " + key[2]}, null),
                new Ab(new String[]{"Ataque", "en bandada"}, null, "Ataque en bandada", "El Compy coordina golpes rápidos junto a sus congéneres, aumentando la presión sobre enemigos cercanos: todos los soldados cargan contra el objetivo.",
                    new String[]{"Daño: " + Math.round(dmg * CompyEntity.packDamageMul(p)), "Recarga: " + sec(CompyEntity.cooldownTicks(p, 3) / 20.0), "Tecla: " + key[3]}, null),
                new Ab(new String[]{"Cazador", "diminuto"}, null, "Cazador diminuto", "Pasiva: sus mordidas hacen +25 % de daño contra criaturas pequeñas (gallinas, conejos, arañas...).",
                    new String[]{"+25 % de daño", "Recarga: ninguna", "Tecla: pasiva"}, null)};
        }
        if (dilo != null) {
            boolean spit = p.rank("S7") > 0;
            double cdf = 1.0 - 0.10 * Math.max(0, p.rank("M3"));
            return new Ab[]{
                new Ab(new String[]{"Mordida", "rápida"}, null, "Mordida rápida", "Un mordisco rápido y preciso. El daño llega justo en el momento del impacto.",
                    new String[]{"Daño: " + Math.round(dmg), "Recarga: " + sec(DiloConfig.get(DiloConfig.BITE_COOLDOWN_TICKS, 14) / 20.0), "Tecla: clic izq. / " + ClientKeys.DILO_BITE.getTranslatedKeyMessage().getString()}, Items.BONE),
                new Ab(new String[]{"Escupitajo", "venenoso"}, null, "Escupitajo venenoso", "Despliega la membrana y escupe veneno a distancia: daño leve y veneno. " + (spit ? "" : "Se desbloquea con el talento Glándula venenosa."),
                    new String[]{spit ? "Veneno activo" : "Bloqueado", "Recarga: " + sec(DiloConfig.get(DiloConfig.SPIT_COOLDOWN_SECONDS, 8)), "Tecla: " + ClientKeys.DILO_SPIT.getTranslatedKeyMessage().getString()}, Items.SPIDER_EYE),
                new Ab(new String[]{"Frenesí", "jurásico"}, null, "Frenesí jurásico", "Durante unos segundos: más daño y velocidad, la membrana del cuello se despliega y un rugido intimida a los enemigos cercanos.",
                    new String[]{"+Daño y velocidad", "Recarga: " + sec(DiloConfig.get(DiloConfig.FRENZY_COOLDOWN_SECONDS, 45)), "Tecla: " + ClientKeys.DILO_FRENZY.getTranslatedKeyMessage().getString()}, Items.BLAZE_POWDER),
                new Ab(new String[]{"Salto", "depredador"}, null, "Salto depredador", "Un gran salto hacia delante: empuja un poco a los que alcanza y deja un impacto al aterrizar. Sirve montado.",
                    new String[]{"Impacto al caer", "Recarga: " + sec(DiloConfig.get(DiloConfig.POUNCE_COOLDOWN_SECONDS, 9)), "Tecla: " + ClientKeys.DILO_POUNCE.getTranslatedKeyMessage().getString()}, Items.RABBIT_FOOT),
                new Ab(new String[]{"Zarpazo", "doble"}, "Daño", "Zarpazo doble", "Dos zarpazos seguidos en un arco corto frente a ti. No atraviesan paredes.",
                    new String[]{"Daño: 2 x " + Math.round(dmg * 0.7F), "Recarga: " + sec(DiloConfig.get(DiloConfig.CLAW_COOLDOWN_SECONDS, 4)), "Tecla: clic der. / " + ClientKeys.DILO_CLAW.getTranslatedKeyMessage().getString()}, Items.FLINT)};
        }
        int vi = variant();
        int ul = stego.ultimateLevel();
        int c5 = p.rank("C5"), c3 = p.rank("C3"), d5 = p.rank("D5");
        double f = 1.0 - 0.10 * p.rank("C7");
        float tailBonus = c5 > 0 ? new float[]{0F, 1.0F, 1.5F, 2.0F}[c5] : 0.5F;
        double tailCd = (c5 > 0 ? new int[]{0, 10, 9, 8}[c5] : 5) * f;
        int ucd = 1200 - 20 * new int[]{0, 8, 14, 20}[p.rank("U3")];
        return new Ab[]{
            new Ab(new String[]{"Coletazo"}, "Daño", "Coletazo", "Un latigazo de la cola alrededor del cuerpo: daña y empuja con fuerza a todos los enemigos cercanos. Con Coletazo devastador pega mucho más fuerte.",
                new String[]{"Daño: " + Math.round(dmg * (1.0F + tailBonus)), "Recarga: " + sec(tailCd), "Tecla: " + ClientKeys.TAIL.getTranslatedKeyMessage().getString()}, Items.BONE),
            new Ab(new String[]{"Embestida"}, "Área", "Embestida", c3 > 0 ? "Carrera veloz de 1,5 s: golpea a todo el que toca y lo aturde 2 s." : "Carrera veloz de 1,5 s que aturde a quien toca. Se desbloquea con el talento Embestida básica.",
                new String[]{c3 > 0 ? "Daño: " + Math.round(dmg * new float[]{0.5F, 0.75F, 1.0F}[c3 - 1]) : "Bloqueada", "Recarga: " + sec(15 * f), "Tecla: " + ClientKeys.CHARGE.getTranslatedKeyMessage().getString()}, Items.IRON_BOOTS),
            new Ab(new String[]{"Guardia del", "Estegosaurio"}, "Defensa", "Guardia del Estegosaurio", "Montado, redirige parte del daño que recibirías al estegosaurio (pasa por su armadura). Se mejora con el talento Guardia del Estegosaurio.",
                new String[]{"Desvío: " + new String[]{"0", "50", "75", "100"}[Math.max(0, Math.min(3, d5))] + " %", "Recarga: ninguna", "Tecla: pasiva"}, Items.SHIELD),
            new Ab(split2(STEGO_ULT_NAME[vi]), "Definitiva", STEGO_ULT_NAME[vi], STEGO_ULT_DESC[vi] + " Solo montado.",
                new String[]{ul > 0 ? "Nivel: " + ul + " / 3" : "Nivel 50", "Recarga: " + sec(ucd / 20.0 * f), "Tecla: " + ClientKeys.ULTIMATE.getTranslatedKeyMessage().getString()}, Items.NETHER_STAR),
            new Ab(new String[]{"Mordida"}, "Básica", "Mordida", "La mordida rápida del clic izquierdo cuando lo montas: golpea al enemigo más cercano delante de la boca.",
                new String[]{"Daño: " + Math.round(dmg * 0.4F), "Recarga: " + sec(0.8), "Tecla: clic izq. (montado)"}, Items.GOLDEN_SWORD)};
    }

    private void renderCompyAttacks(GuiGraphics g, Ab[] ab) {
        for (int i = 0; i < 5; i++) {
            if (selAtk != i) continue;
            int[] c = skin.atkCard[i];
            frame(g, X(c[0]) - 1, Y(c[1]) - 1, X(c[2]) + 1, Y(c[3]) + 1, GOLD, 0x22FFD35A);
        }
        Ab a = ab[selAtk];
        int[] di = skin.atkDetailIcon;
        float dx0 = X(di[0]), dy0 = Y(di[1]), dx1 = X(di[2]);
        int[] src = selAtk < 4 ? new int[]{skin.atkCircle[selAtk][0], skin.atkCircle[selAtk][1], skin.circ} : skin.passive;
        g.pose().pushPose();
        g.pose().translate(dx0, dy0, 0.0F);
        float sx = (dx1 - dx0) / src[2];
        g.pose().scale(sx, sx, 1.0F);
        g.blit(skin.bg[1], 0, 0, src[2], src[2], (float) src[0], (float) src[1], src[2], src[2], 1086, skin.texH[1]);
        g.pose().popPose();
        text(g, a.title, X(skin.detTitleX), Y(skin.detTitleY), 1.4F, GOLD, true);
        para(g, a.desc, X(skin.detDescX), Y(skin.detDescY), skin.detDescW * K, 1.1F, 11.0F, TEXT, 4);
        for (int i = 0; i < 3; i++) {
            int[] r = skin.detPill[i];
            text(g, shorten(a.stats[i], (r[2] - r[0] - skin.detOff[i] - 8) * K, 1.1F), X(r[0] + skin.detOff[i]), Y((r[1] + r[3]) / 2.0F) - 4.5F, 1.1F, 0xFFFFFFFF, true);
        }
        String ks = a.stats[2].startsWith("Tecla: ") ? a.stats[2].substring(7) : "";
        if (!ks.equals("pasiva") && !ks.isEmpty()) textC(g, shorten(ks, 18, 0.9F), X(skin.detKey[0]), Y(skin.detKey[1]) - 4.0F, 0.9F, 0xFFFFFFFF, true);
    }

    private void renderAttacks(GuiGraphics g) {
        Ab[] ab = abilities();
        if (compy != null) { renderCompyAttacks(g, ab); return; }
        int[] pillIcons = null;
        for (int i = 0; i < 4; i++) {
            int[] c = skin.atkCard[i];
            boolean sel = selAtk == i;
            if (sel) frame(g, X(c[0]) - 1, Y(c[1]) - 1, X(c[2]) + 1, Y(c[3]) + 1, GOLD, 0x22FFD35A);
            int[] nxy = skin.atkNameXY[i];
            for (int l = 0; l < ab[i].name.length; l++) text(g, ab[i].name[l], X(nxy[0]), Y(nxy[1]) + l * 14.0F, 1.2F, sel ? GOLD : 0xFFFFFFFF, true);
            if (ab[i].pill != null) pillLabel(g, skin.atkPillXY[i], ab[i].pill);
        }
        // tarjeta alta (quinta habilidad)
        int[] t = skin.atkCard[4];
        boolean selT = selAtk == 4;
        if (selT) frame(g, X(t[0]) - 1, Y(t[1]) - 1, X(t[2]) + 1, Y(t[3]) + 1, GOLD, 0x22FFD35A);
        if (dilo != null) {
            float cx = X((t[0] + t[2]) / 2.0F), cy = Y(205);
            g.fill((int) (cx - 31), (int) (cy - 31), (int) (cx + 31), (int) (cy + 31), 0xFF0E1214);
            g.renderOutline((int) (cx - 31), (int) (cy - 31), 62, 62, GOLD);
            g.renderOutline((int) (cx - 29), (int) (cy - 29), 58, 58, 0x66FFD35A);
            g.pose().pushPose(); g.pose().translate(cx - 16, cy - 16, 0.0F); g.pose().scale(2.0F, 2.0F, 1.0F);
            g.renderItem(new ItemStack(ab[4].item), 0, 0); g.pose().popPose();
            pillBox(g, skin.atkPillXY[4], 0xFF5A1F1A);
        }
        for (int l = 0; l < ab[4].name.length; l++) textC(g, ab[4].name[l], X(skin.tallName[0]), Y(skin.tallName[1]) + l * 14.0F, 1.2F, selT ? GOLD : 0xFFFFFFFF, true);
        pillLabel(g, skin.atkPillXY[4], ab[4].pill);
        // detalle
        Ab a = ab[selAtk];
        int[] di = skin.atkDetailIcon;
        float dx0 = X(di[0]), dy0 = Y(di[1]), dx1 = X(di[2]), dy1 = Y(di[3]);
        if (selAtk < 4) {
            int[] cc = skin.atkCircle[selAtk];
            g.pose().pushPose();
            g.pose().translate(dx0, dy0, 0.0F);
            float sx = (dx1 - dx0) / (96.0F * K), sy = (dy1 - dy0) / (96.0F * K);
            g.pose().scale(sx * K, sy * K, 1.0F);
            g.blit(skin.bg[1], 0, 0, 96, 96, (float) cc[0], (float) cc[1], 96, 96, 1086, skin.texH[1]);
            g.pose().popPose();
        } else {
            float cx = (dx0 + dx1) / 2.0F, cy = (dy0 + dy1) / 2.0F;
            g.fill((int) dx0 + 4, (int) dy0 + 4, (int) dx1 - 4, (int) dy1 - 4, 0xFF0E1214);
            g.renderOutline((int) dx0 + 4, (int) dy0 + 4, (int) (dx1 - dx0) - 8, (int) (dy1 - dy0) - 8, GOLD);
            g.pose().pushPose(); g.pose().translate(cx - 12, cy - 12, 0.0F); g.pose().scale(1.5F, 1.5F, 1.0F);
            g.renderItem(new ItemStack(a.item), 0, 0); g.pose().popPose();
        }
        text(g, a.title, X(skin.detTitleX), Y(skin.detTitleY), 1.4F, GOLD, true);
        para(g, a.desc, X(skin.detDescX), Y(skin.detDescY), skin.detDescW * K, 1.1F, 11.0F, TEXT, 4);
        Item[] icons = {selAtk == 3 && stego != null ? Items.GOLDEN_APPLE : Items.IRON_SWORD, Items.CLOCK, Items.TRIPWIRE_HOOK};
        for (int i = 0; i < 3; i++) {
            int[] r = skin.detPill[i];
            float tx = X(r[0] + skin.detPillOff);
            text(g, shorten(a.stats[i], (r[2] - r[0] - skin.detPillOff - 6) * K, 1.1F), tx, Y((r[1] + r[3]) / 2.0F) - 4.5F, 1.1F, 0xFFFFFFFF, true);
        }
    }

    private void pillBox(GuiGraphics g, int[] r, int color) {
        float a = X(r[0]), b = Y(r[1]), c = X(r[2]), d = Y(r[3]);
        g.fill((int) a, (int) b, (int) c, (int) d, color);
        g.renderOutline((int) a, (int) b, (int) (c - a), (int) (d - b), 0xFFB8862A);
    }

    private void pillLabel(GuiGraphics g, int[] r, String s) {
        float x = X(r[0] + 26), y = Y((r[1] + r[3]) / 2.0F) - 4.0F;
        text(g, shorten(s, (r[2] - r[0] - 30) * K, 1.0F), x, y, 1.0F, 0xFFFFFFFF, true);
    }

    // ---------------------------------------------------------------- talentos
    private static final int NS = 25;
    private static final int SPACING = 34;

    /** Coloca cada talento: columna de su rama, fila por profundidad y posicion horizontal segun sus requisitos. */
    /** Lado de un nodo en unidades virtuales (los del Compy vienen dibujados en el fondo). */
    private float nodeSize() { return compy != null ? 2.0F * skin.nodeHalf * K : NS; }

    private void buildTree() {
        Talent[] all = prog().table();
        nodePos = new int[all.length][];
        if (compy != null) {   // posiciones fijas: coinciden con los nodos de la maqueta
            for (int i = 0; i < all.length; i++)
                for (int k = 0; k < skin.nodeId.length; k++)
                    if (skin.nodeId[k].equals(all[i].id)) nodePos[i] = new int[]{skin.nodeX[k], skin.nodeY[k]};
            return;
        }
        int[] depth = new int[all.length];
        for (int pass = 0; pass < all.length; pass++) {
            for (int i = 0; i < all.length; i++) {
                int d = 0;
                for (String[] rq : all[i].reqs) { int r = idx(rq[0]); if (r >= 0) d = Math.max(d, depth[r] + 1); }
                depth[i] = d;
            }
        }
        float[] dx = new float[all.length];
        boolean[] placed = new boolean[all.length];
        for (int col = 0; col < 3; col++) {
            for (int d = 0; d < 5; d++) {
                List<Integer> row = new ArrayList<>();
                for (int i = 0; i < all.length; i++) {
                    Talent t = all[i];
                    if (t.id.startsWith("U") || t.id.equals("P1")) continue;
                    if (skin.colOfBranch[Math.max(0, Math.min(3, t.branch))] != col || depth[i] != d) continue;
                    row.add(i);
                }
                if (row.isEmpty()) continue;
                float[] want = new float[row.size()];
                for (int k = 0; k < row.size(); k++) {
                    Talent t = all[row.get(k)];
                    float s = 0; int n = 0;
                    for (String[] rq : t.reqs) { int r = idx(rq[0]); if (r >= 0 && placed[r]) { s += dx[r]; n++; } }
                    want[k] = n > 0 ? s / n : (row.size() == 1 ? 0 : (k == 0 ? -SPACING : SPACING));
                }
                // ordena por posicion deseada (estable) y separa los que se pisan
                Integer[] ord = new Integer[row.size()];
                for (int k = 0; k < ord.length; k++) ord[k] = k;
                final float[] w = want;
                java.util.Arrays.sort(ord, (p1, p2) -> Float.compare(w[p1], w[p2]));
                float[] pos = new float[ord.length];
                for (int k = 0; k < ord.length; k++) pos[k] = want[ord[k]];
                for (int k = 1; k < pos.length; k++) if (pos[k] - pos[k - 1] < 2 * SPACING) pos[k] = pos[k - 1] + 2 * SPACING;
                float mean = 0, wm = 0;
                for (int k = 0; k < pos.length; k++) { mean += pos[k]; wm += want[ord[k]]; }
                float shift = (wm - mean) / pos.length;
                for (int k = 0; k < pos.length; k++) {
                    float x = Math.max(-SPACING, Math.min(SPACING, pos[k] + shift));
                    int idx = row.get(ord[k]);
                    dx[idx] = x; placed[idx] = true;
                    nodePos[idx] = new int[]{Math.round(skin.colX[col] + x), skin.rowY[Math.min(4, d)]};
                }
            }
        }
        // mejoras de la definitiva del estegosaurio: fila bajo la rama central
        for (int i = 0; i < all.length; i++) {
            if (all[i].id.startsWith("U") && stego != null) {
                int k = all[i].id.charAt(1) - '1';
                nodePos[i] = new int[]{skin.colX[1] + (k - 1) * 46, skin.ultNodesY[0]};
            }
        }
    }

    private static Item icon(String id) {
        return switch (id) {
            // estegosaurio
            case "D1" -> Items.IRON_CHESTPLATE; case "D2" -> Items.GOLDEN_APPLE; case "D3" -> Items.ANVIL;
            case "D4" -> Items.TOTEM_OF_UNDYING; case "D5" -> Items.SHIELD;
            case "U1" -> Items.NETHER_STAR; case "U2" -> Items.ENDER_EYE; case "U3" -> Items.CLOCK;
            // dilofosaurio (los ids C/M/S se reparten segun la especie)
            case "P1" -> Items.NETHER_STAR;
            default -> Items.PAPER;
        };
    }

    private Item iconFor(Talent t) {
        if (dilo != null) {
            return switch (t.id) {
                case "C1" -> Items.BONE; case "C2" -> Items.FLINT; case "C3" -> Items.REDSTONE; case "C4" -> Items.DIAMOND_SWORD;
                case "C5" -> Items.COMPASS; case "C6" -> Items.IRON_AXE;
                case "M1" -> Items.SUGAR; case "M2" -> Items.FEATHER; case "M3" -> Items.CLOCK; case "M4" -> Items.SLIME_BALL;
                case "M5" -> Items.LEATHER_BOOTS; case "M6" -> Items.FIREWORK_ROCKET;
                case "S1" -> Items.IRON_CHESTPLATE; case "S2" -> Items.GLISTERING_MELON_SLICE; case "S3" -> Items.SPYGLASS;
                case "S4" -> Items.GOAT_HORN; case "S5" -> Items.SHIELD; case "S6" -> Items.TOTEM_OF_UNDYING; case "S7" -> Items.SPIDER_EYE;
                default -> icon(t.id);
            };
        }
        return switch (t.id) {
            case "C1" -> Items.BONE; case "C2" -> Items.DIAMOND_AXE; case "C3" -> Items.FIRE_CHARGE;
            case "C4" -> Items.CACTUS; case "C5" -> Items.TNT; case "C6" -> Items.BLAZE_POWDER; case "C7" -> Items.NETHER_STAR;
            case "S1" -> Items.SUGAR; case "S2" -> Items.GLISTERING_MELON_SLICE; case "S3" -> Items.LEAD;
            case "S4" -> Items.SLIME_BALL; case "S5" -> Items.RED_BED; case "S6" -> Items.GOAT_HORN; case "S7" -> Items.BEACON;
            default -> icon(t.id);
        };
    }

    private void node(GuiGraphics g, int x, int y, int color, boolean owned, boolean maxed, boolean can) {
        int bg = maxed ? 0xFF4A3A10 : owned ? 0xFF1E2A1E : 0xFF14181C;
        int edge = maxed ? GOLD : owned || can ? color : 0xFF4A4F54;
        if (can || owned) g.fill(x - 2, y - 2, x + NS + 2, y + NS + 2, (edge & 0x00FFFFFF) | 0x30000000);
        g.fill(x, y, x + NS, y + NS, bg);
        g.fill(x, y, x + 3, y + 3, 0xFF000000); g.fill(x + NS - 3, y, x + NS, y + 3, 0xFF000000);
        g.fill(x, y + NS - 3, x + 3, y + NS, 0xFF000000); g.fill(x + NS - 3, y + NS - 3, x + NS, y + NS, 0xFF000000);
        g.renderOutline(x, y, NS, NS, edge);
        g.renderOutline(x + 1, y + 1, NS - 2, NS - 2, (edge & 0x00FFFFFF) | 0x55000000);
    }

    private int branchColor(Talent t) { return BRANCH_COLOR[Math.max(0, Math.min(3, t.branch))]; }

    private void renderTalents(GuiGraphics g) {
        // placas de las ramas
        for (int c = 0; c < 3 && !skin.baked; c++) {
            int[] pc = skin.plateC[c];
            textC(g, skin.colName[c], X(pc[0]), Y(pc[1]) - 4.0F, c == 1 && dilo != null ? 0.8F : 1.0F, 0xFFFFFFFF, true);
            textC(g, skin.colSub[c], X(pc[0]), Y(pc[2]) - 3.0F, 0.6F, 0xFFE9D9A6, false);
        }
        // progreso
        ProdigyProgress p = prog();
        Talent[] all = p.table();
        if (!adult()) { medallion(g, p, all); return; }
        // lineas de requisito (debajo de los nodos)
        for (int i = 0; i < all.length && !skin.baked; i++) {
            if (nodePos[i] == null) continue;
            for (String[] req : all[i].reqs) {
                int r = idx(req[0]);
                if (r < 0 || nodePos[r] == null) continue;
                int x1 = (int) X(nodePos[r][0]), y1 = (int) Y(nodePos[r][1]) + NS / 2, x2 = (int) X(nodePos[i][0]), y2 = (int) Y(nodePos[i][1]) - NS / 2;
                boolean met = p.rank(r) >= Integer.parseInt(req[1]);
                int col = met ? branchColor(all[i]) : 0xFF3A4046;
                int ym = (y1 + y2) / 2;
                g.fill(x1 - 1, y1, x1 + 1, ym + 1, col);
                g.fill(Math.min(x1, x2) - 1, ym, Math.max(x1, x2) + 1, ym + 2, col);
                g.fill(x2 - 1, ym, x2 + 1, y2, col);
            }
        }
        for (int i = 0; i < all.length; i++) {
            Talent t = all[i];
            if (nodePos[i] == null) continue;
            if (skin.baked) { bakedNode(g, p, t, i); continue; }
            int x = (int) X(nodePos[i][0]) - NS / 2, y = (int) Y(nodePos[i][1]) - NS / 2;
            boolean owned = p.rank(i) > 0, maxed = p.rank(i) >= t.maxRank, can = p.whyNotUnlock(i) == null;
            node(g, x, y, branchColor(t), owned, maxed, can);
            g.pose().pushPose();
            g.pose().translate(x + (NS - 16) / 2.0F, y + (NS - 16) / 2.0F - 1.0F, 0.0F);
            g.renderItem(new ItemStack(iconFor(t)), 0, 0);
            g.pose().popPose();
            if (!owned && !can) {
                g.fill(x + 2, y + 2, x + NS - 2, y + NS - 2, 0xAA000000);
                g.fill(x + NS / 2 - 3, y + NS / 2 - 1, x + NS / 2 + 3, y + NS / 2 + 4, 0xFFA0A4A8);
                g.fill(x + NS / 2 - 2, y + NS / 2 - 4, x + NS / 2 + 2, y + NS / 2 - 1, 0xFF70747A);
            }
            if (i == selected && !ultSel) g.renderOutline(x - 3, y - 3, NS + 6, NS + 6, 0xFFFFFFFF);
            String rk = p.rank(i) + "/" + t.maxRank;
            text(g, rk, x + NS - this.font.width(rk) * 0.65F, y + NS - 6.0F, 0.65F, owned ? (maxed ? GOLD : TEXT) : DIM, true);
        }
        medallion(g, p, all);
        int total = 0;
        for (Talent t : all) total++;
        int got = p.talentsUnlocked();
        float bx = X(skin.bar[0]), by = Y(skin.bar[1]), bw = (skin.bar[2] - skin.bar[0]) * K, bh = (skin.bar[3] - skin.bar[1]) * K;
        int f = Math.min((int) bw, (int) (bw * got / Math.max(1, total)));
        if (f > 0) { g.fillGradient((int) bx, (int) by, (int) bx + f, (int) (by + bh), 0xFFFFD35A, 0xFFE08A1C); g.fill((int) bx, (int) by, (int) bx + f, (int) by + 1, 0x66FFFFFF); }
        String pt = got + " / " + total + " talentos";
        if (skin.progText[2] == 1) textR(g, pt, X(skin.progText[0]), Y(skin.progText[1]) - 1.0F, 0.95F, 0xFFFFFFFF, true);
        else text(g, pt, X(skin.progText[0]), Y(skin.progText[1]), 0.95F, 0xFFFFFFFF, true);
        renderDetail(g, p);
    }

    /** Nodo del Compy: el icono ya esta en el fondo; solo se dibuja el estado (propio, bloqueado, seleccionado) y el rango. */
    private void bakedNode(GuiGraphics g, ProdigyProgress p, Talent t, int i) {
        float cx = X(nodePos[i][0]), cy = Y(nodePos[i][1]), h = skin.nodeHalf * K;
        int x0 = (int) (cx - h), y0 = (int) (cy - h), x1 = (int) (cx + h), y1 = (int) (cy + h);
        boolean owned = p.rank(i) > 0, maxed = p.rank(i) >= t.maxRank, can = p.whyNotUnlock(i) == null;
        int col = branchColor(t);
        if (!owned && !can) {
            g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, 0xA8000000);
            int lx = (int) cx, ly = (int) cy - 2;
            g.fill(lx - 4, ly, lx + 4, ly + 7, 0xFFA0A4A8);
            g.fill(lx - 3, ly - 4, lx - 1, ly, 0xFF70747A);
            g.fill(lx + 1, ly - 4, lx + 3, ly, 0xFF70747A);
            g.fill(lx - 3, ly - 5, lx + 3, ly - 3, 0xFF70747A);
        } else if (owned) {
            g.renderOutline(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, maxed ? GOLD : col);
            g.renderOutline(x0, y0, x1 - x0, y1 - y0, (maxed ? GOLD : col) & 0x00FFFFFF | 0x88000000);
        }
        if (i == selected && !ultSel) g.renderOutline(x0 - 3, y0 - 3, x1 - x0 + 6, y1 - y0 + 6, 0xFFFFFFFF);
        String rk = p.rank(i) + "/" + t.maxRank;
        textR(g, rk, cx + h - 2.0F, cy + h * 0.5F, 0.75F, owned ? (maxed ? GOLD : 0xFFFFFFFF) : DIM, true);
    }

    /** Medallon "Rey de la bandada": nombre y rueda de fondo ya estan en la maqueta; aqui el candado, el brillo al lograrlo y la seleccion. */
    private void compyMedal(GuiGraphics g, ProdigyProgress p) {
        int pi = idx("P1");
        boolean got = pi >= 0 && p.rank(pi) > 0;
        int[] m = skin.medal;
        if (got) {
            int[] c = VR(m);
            g.renderOutline(c[0] + 2, c[1] + 2, c[2] - c[0] - 4, c[3] - c[1] - 4, 0x66FFD35A);
        } else {
            float lx = X(skin.lock[0]), ly = Y(skin.lock[1]) - 3.0F;
            g.fill((int) lx - 5, (int) ly, (int) lx + 5, (int) ly + 8, 0xFFE6B422);
            g.renderOutline((int) lx - 5, (int) ly, 10, 8, 0xFF7A5A10);
            g.fill((int) lx - 3, (int) ly - 5, (int) lx - 1, (int) ly, 0xFFB0B4B8);
            g.fill((int) lx + 1, (int) ly - 5, (int) lx + 3, (int) ly, 0xFFB0B4B8);
            g.fill((int) lx - 3, (int) ly - 6, (int) lx + 3, (int) ly - 4, 0xFFB0B4B8);
        }
        if (selected == pi && !ultSel) { int[] c = VR(m); g.renderOutline(c[0] + 3, c[1] + 3, c[2] - c[0] - 6, c[3] - c[1] - 6, GOLD); }
    }

    /** Icono de un talento del Compy: se recorta del propio fondo (el arte del nodo o del medallon) y se ajusta al cuadro del panel de detalle. */
    private void compyTalentIcon(GuiGraphics g, Talent t, float x, float y, float size) {
        int cx = skin.p1[0], cy = skin.p1[1], half = 40;
        if (!t.id.equals("P1")) for (int k = 0; k < skin.nodeId.length; k++) if (skin.nodeId[k].equals(t.id)) { cx = skin.nodeX[k]; cy = skin.nodeY[k]; half = skin.nodeHalf; }
        g.blit(skin.bg[2], (int) x, (int) y, (int) size, (int) size, (float) (cx - half), (float) (cy - half), half * 2, half * 2, 1086, skin.texH[2]);
    }

    /** Medallon central: la definitiva del estegosaurio (rugido de su variante) o el Cazador Primordial del dilofosaurio. */
    private void medallion(GuiGraphics g, ProdigyProgress p, Talent[] all) {
        if (compy != null) { compyMedal(g, p); return; }
        int[] m = skin.medal;
        float cx = X((m[0] + m[2]) / 2.0F);
        boolean got;
        String title, note;
        if (dilo != null) {
            int pi = idx("P1");
            got = pi >= 0 && p.rank(pi) > 0;
            title = "Cazador primordial";
            note = null;
            if (selected == pi && !ultSel) g.renderOutline((int) X(m[0]) + 6, (int) Y(m[1]) + 2, (int) ((m[2] - m[0]) * K) - 12, (int) ((m[3] - m[1]) * K) - 2, GOLD);
        } else {
            int ul = stego.ultimateLevel();
            got = ul > 0;
            title = STEGO_ULT_NAME[variant()];
            note = ul > 0 ? "Definitiva nivel " + ul + " / 3 · tecla " + ClientKeys.ULTIMATE.getTranslatedKeyMessage().getString() : "Se obtiene al nivel 50";
            if (ultSel) g.renderOutline((int) X(m[0]) + 6, (int) Y(m[1]) + 2, (int) ((m[2] - m[0]) * K) - 12, (int) ((m[3] - m[1]) * K) - 2, GOLD);
            textC(g, "Mejoras de la definitiva", X(skin.colX[1]), Y(skin.ultNodesY[0] - 36) - 4.0F, 0.7F, 0xFFC8CDD2, true);
        }
        float py = dilo != null ? 497 : 521;
        textC(g, shorten(title, 150 * K, 1.0F), cx, Y(py) - 4.0F, 1.0F, got ? GOLD : 0xFFE9D9A6, true);
        if (note != null) textC(g, shorten(note, 240 * K, 0.8F), cx, Y(543) - 3.0F, 0.8F, got ? 0xFFE6D6FF : 0xFFC8CDD2, false);
        if (!got) { // candado dibujado en vivo
            float lx = X(dilo != null ? 606 : 600), ly = Y(dilo != null ? 470 : 498);
            g.fill((int) lx - 5, (int) ly, (int) lx + 5, (int) ly + 8, 0xFFE6B422);
            g.renderOutline((int) lx - 5, (int) ly, 10, 8, 0xFF7A5A10);
            g.fill((int) lx - 3, (int) ly - 5, (int) lx - 1, (int) ly, 0xFFB0B4B8);
            g.fill((int) lx + 1, (int) ly - 5, (int) lx + 3, (int) ly, 0xFFB0B4B8);
            g.fill((int) lx - 3, (int) ly - 6, (int) lx + 3, (int) ly - 4, 0xFFB0B4B8);
        }
    }

    private int req(GuiGraphics g, float x, float y, boolean ok, String s, float maxW) {
        int ix = (int) x, iy = (int) y;
        if (ok) g.blit(ATLAS, ix, iy, 9, 7, 56.0F, 30.0F, 10, 7, 256, 256); else g.blit(ATLAS, ix + 1, iy, 7, 7, 68.0F, 30.0F, 8, 7, 256, 256);
        text(g, shorten(s, maxW - 13, 1.0F), x + 13, y, 1.0F, ok ? 0xFF7CE08A : 0xFFE07C7C, false);
        return (int) y + 11;
    }

    private void renderDetail(GuiGraphics g, ProdigyProgress p) {
        int[] ic = skin.panelIcon;
        float x = X(skin.panelEffX), y0 = Y(ic[1]);
        float ix = X(ic[0]), iw = (ic[2] - ic[0]) * K;
        float tx = X(skin.panelTitleX), tw = skin.panelTitleW * K;
        int[] pp = skin.panelPill;
        float effW = skin.panelEffW * K;
        float effY = Y(skin.panelEffY);
        float reqY = Y(skin.reqHeadY);
        if (ultSel) {
            frame(g, ix, y0, ix + iw, y0 + iw, 0xFFB060FF, 0x80100A1C);
            g.pose().pushPose(); g.pose().translate(ix + iw / 2 - 16, y0 + iw / 2 - 16, 0.0F); g.pose().scale(2.0F, 2.0F, 1.0F);
            g.renderItem(new ItemStack(Items.NETHER_STAR), 0, 0); g.pose().popPose();
            para(g, STEGO_ULT_NAME[variant()], tx, Y(skin.panelTitleY), tw, 1.1F, 10.5F, GOLD, 2);
            frame(g, X(pp[0]), Y(pp[1]), X(pp[2]), Y(pp[3]), 0xFFB060FF, 0x40100A1C);
            textC(g, "Definitiva", (X(pp[0]) + X(pp[2])) / 2, (Y(pp[1]) + Y(pp[3])) / 2 - 3.5F, 0.95F, 0xFFE6D6FF, false);
            para(g, STEGO_ULT_DESC[variant()] + " Se obtiene por nivel: I a nivel 50, II a 75 y III a 100. Solo montado, tecla R.", x, effY, effW, 1.0F, 10.0F, TEXT, 8);
            float yy = reqY;
            text(g, "Requisitos", x, yy, 1.1F, GOLD, true); yy += 14;
            yy = req(g, x, yy, p.level() >= 50, "Nivel 50: definitiva I", effW);
            yy = req(g, x, yy, p.level() >= 75, "Nivel 75: definitiva II", effW);
            yy = req(g, x, yy, p.level() >= 100, "Nivel 100: definitiva III", effW);
            text(g, "Las mejoras U1-U3 la hacen más fuerte.", x, yy + 4, 0.8F, DIM, false);
            button(g, p.level() >= 50 ? "Desbloqueada" : "Por nivel", p.level() >= 50, false);
            return;
        }
        Talent t = p.table()[selected];
        int col = branchColor(t);
        int cur = p.rank(selected);
        if (compy != null) compyTalentIcon(g, t, ix, y0, iw);
        else {
            frame(g, ix, y0, ix + iw, y0 + iw, cur > 0 ? col : 0xFF5A6066, 0x80101418);
            g.pose().pushPose(); g.pose().translate(ix + iw / 2 - 16, y0 + iw / 2 - 16, 0.0F); g.pose().scale(2.0F, 2.0F, 1.0F);
            g.renderItem(new ItemStack(iconFor(t)), 0, 0); g.pose().popPose();
        }
        para(g, t.name, tx, Y(skin.panelTitleY), tw, 1.1F, 10.5F, 0xFFFFF0C0, 3);
        frame(g, X(pp[0]), Y(pp[1]), X(pp[2]), Y(pp[3]), col, 0x40101418);
        textC(g, "Rango " + cur + " / " + t.maxRank, (X(pp[0]) + X(pp[2])) / 2, (Y(pp[1]) + Y(pp[3])) / 2 - 3.5F, 0.95F, 0xFFFFFFFF, false);
        float yy = para(g, t.effect, x, effY, effW, 1.0F, 10.0F, TEXT, compy != null ? 14 : 9);
        String note = switch (t.id) {
            case "S3" -> stego != null ? "Cruzar portales: pendiente." : null;
            case "D5" -> "Efecto solo montado.";
            default -> null;
        };
        if (note != null) para(g, note, x, yy + 2, effW, 0.9F, 9.5F, 0xFFE0B04C, 2);
        float ry = reqY;
        text(g, "Requisitos", x, ry, 1.1F, GOLD, true); ry += 14;
        int next = Math.min(cur, t.maxRank - 1);
        ry = req(g, x, ry, p.level() >= t.minLevel[next], "Nivel " + t.minLevel[next], effW);
        for (String[] r : t.reqs) {
            int need = Integer.parseInt(r[1]);
            String nm = r[0];
            for (Talent o : p.table()) if (o.id.equals(r[0])) nm = o.name;
            ry = req(g, x, ry, p.rank(r[0]) >= need, nm + " " + need, effW);
        }
        ry = req(g, x, ry, cur >= t.maxRank || p.pointsAvailable() >= t.cost[next], "Coste: " + t.cost[next] + " pts", effW);
        for (String ex : t.exclusive) {
            String nm = ex;
            for (Talent o : p.table()) if (o.id.equals(ex)) nm = o.name;
            ry = req(g, x, ry, p.rank(ex) == 0, "Excluye " + nm, effW);
        }
        boolean can = p.whyNotUnlock(selected) == null;
        button(g, cur >= t.maxRank ? "Máximo" : "Desbloquear", can, cur >= t.maxRank);
    }

    /** Texto sobre el boton verde del fondo; atenuado cuando no se puede pulsar. */
    private void button(GuiGraphics g, String label, boolean enabled, boolean maxed) {
        float x0 = X(skin.btn[0]), y0 = Y(skin.btn[1]), x1 = X(skin.btn[2]), y1 = Y(skin.btn[3]);
        if (!enabled) g.fill((int) x0 + 2, (int) y0 + 2, (int) x1 - 2, (int) y1 - 2, maxed ? 0xBA3A3010 : 0xD0101418);
        textC(g, label, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F - 6.0F, 1.5F, enabled ? 0xFFFFFFFF : maxed ? GOLD : 0xFF8A9096, true);
    }

    private void renderTalentTooltip(GuiGraphics g, int mx, int my, int rmx, int rmy) {
        Talent[] all = prog().table();
        float ns = nodeSize();
        for (int i = 0; i < all.length; i++) {
            if (nodePos[i] == null) continue;
            float x = X(nodePos[i][0]) - ns / 2.0F, y = Y(nodePos[i][1]) - ns / 2.0F;
            if (mx >= x && mx < x + ns && my >= y && my < y + ns) {
                g.renderTooltip(this.font, Component.literal(all[i].name + "  " + prog().rank(i) + "/" + all[i].maxRank), rmx, rmy);
                return;
            }
        }
    }
}
