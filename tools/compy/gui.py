"""Fondos de la pantalla del Compy a partir de las tres maquetas del usuario (Comandos, Ataques, Talentos).
Uso: python3 gui.py <repo> <cmd.png> <atk.png> <tal.png> [<carpeta de vista previa>]
Genera textures/gui/compy2/{cmd,atk,tal}.png (1086 px de ancho, sin texto variable: retrato, nombre, vida, estadisticas, panel de detalle, rangos...)
y client/CompyGui.java con las coordenadas (en pixeles de esos fondos) que usa PetSkinScreen. Determinista."""
import os, sys, json
import numpy as np, cv2
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gui_lib import *

CROP = {'tal': (28, 8, 1432, 1046), 'atk': (40, 20, 1430, 1000), 'cmd': (38, 20, 1410, 999)}
W = 1086

class Map:
    def __init__(self, name):
        self.x0, self.y0, self.x1, self.y1 = CROP[name]
        self.s = W / float(self.x1 - self.x0)
        self.h = int(round((self.y1 - self.y0) * self.s))
    def x(self, v): return int(round((v - self.x0) * self.s))
    def y(self, v): return int(round((v - self.y0) * self.s))
    def r(self, q): return [self.x(q[0]), self.y(q[1]), self.x(q[2]), self.y(q[3])]
    def p(self, q): return [self.x(q[0]), self.y(q[1])]

def save_bg(a, name, repo, preview):
    m = Map(name)
    crop = np.clip(a[m.y0:m.y1, m.x0:m.x1], 0, 255).astype(np.uint8)
    im = Image.fromarray(crop).resize((W, m.h), Image.LANCZOS)
    d = os.path.join(repo, 'src/main/resources/assets/prodigycreatures/textures/gui/compy2'); os.makedirs(d, exist_ok=True)
    im.save(os.path.join(d, name + '.png'), optimize=True)
    if preview:
        os.makedirs(preview, exist_ok=True); im.save(os.path.join(preview, 'compy_gui_%s.png' % name))
    return im

def left_panel(a, g):
    """Retrato, texto del nombre, nivel, vida, estadisticas (y XP / puntos segun la pagina)."""
    blur_region(a, g['portrait_blur'], keep=g['portrait_keep'])
    flat(a, g['name_clear'], band=5)
    flat(a, g['lvl_clear'], band=5)
    vgrad(a, g['hp_fill'], (64, 16, 14), (36, 8, 8))
    for r in g['stat_clear']: dark_fill(a, r, lum=90, noise=1.2)
    if g.get('exp_fill'): vgrad(a, g['exp_fill'], (40, 14, 80), (22, 8, 48))

# centros de los nodos del arbol en la maqueta de talentos (px de la maqueta original)
NODES = {'A1': (518, 339), 'A2': (466.5, 434), 'A3': (569, 434), 'A4': (466.5, 523), 'A5': (569, 523), 'A6': (518, 617.5), 'A7': (518, 707),
         'B1': (737.5, 339), 'B2': (680.5, 434), 'B3': (789.5, 434), 'B4': (680.5, 523), 'B5': (789.5, 523), 'B6': (737.5, 617.5),
         'C1': (955, 339), 'C2': (897.5, 434), 'C3': (1007, 434), 'C4': (897.5, 523), 'C5': (1007, 523), 'C6': (954, 617.5), 'C7': (954, 707)}

def build_tal(src):
    a = load(src)
    g = dict(portrait_blur=(97, 192, 380, 408), portrait_keep=[(95, 387, 134, 426), (337, 387, 376, 426), (97, 192, 128, 214), (349, 192, 380, 214)],
             name_clear=(250, 109, 440, 148), lvl_clear=(192, 464, 284, 488), hp_fill=(124, 507, 386, 535),
             stat_clear=[(322, 558, 384, 582), (322, 587, 384, 613), (322, 616, 384, 641), (322, 644, 384, 670)])
    left_panel(a, g)
    dark_fill(a, (172, 705, 386, 778))                       # puntos de talento
    dark_fill(a, (1090, 190, 1368, 716), lum=70)           # panel de detalle
    flat(a, (1120, 733, 1334, 778), band=8, noise=1.0)      # etiqueta del boton Desbloquear (degradado vertical del propio boton)
    for cx, cy in NODES.values():                           # rangos "0/3" de los nodos (se dibujan en vivo)
        text_inpaint(a, (int(cx - 3), int(cy + 14), int(cx + 40), int(cy + 36)), hi=190, minchan=175, grow=5, radius=3)
    flat(a, (962, 903, 1142, 935), band=5)                  # "0 / 22 talentos"
    # candado del medallon: se sustituye por la zona simetrica (espejo) del arco, que esta limpia; el candado se dibuja en vivo
    src = a[777:809, 628:680].copy()[:, ::-1]
    a[777:809, 790:842] = src
    return a, g

def build_atk(src):
    a = load(src)
    g = dict(portrait_blur=(119, 231, 522, 532), portrait_keep=[(123, 479, 175, 529), (462, 479, 513, 529), (119, 231, 160, 262), (480, 231, 522, 262)],
             name_clear=(296, 134, 560, 178), lvl_clear=(266, 566, 384, 592), hp_fill=(166, 612, 526, 646), exp_fill=(136, 812, 510, 838),
             stat_clear=[(435, 665, 522, 693), (435, 701, 522, 729), (435, 736, 522, 764), (435, 771, 522, 799)])
    left_panel(a, g)
    # la tarjeta "Ataque en bandada" viene resaltada en la maqueta: se le devuelve el aspecto normal (marco y huecos de la tarjeta 2)
    dy = 379 - 215
    for (x0, y0, x1, y1) in [(864, 215, 1173, 233), (864, 347, 1173, 364), (864, 215, 882, 364), (1155, 215, 1173, 364)]:
        copy_rect(a, (x0, y0, x1, y1), (x0, y0 + dy))
    copy_rect(a, (852, 215, 864, 364), (852, 379))
    copy_rect(a, (1173, 215, 1182, 364), (1173, 379))
    copy_rect(a, (560, 365, 869, 379), (864, 365))
    copy_rect(a, (560, 528, 869, 552), (864, 528))
    unglow(a, (880, 391, 1157, 516), (24, 28, 28))
    # panel de detalle de la habilidad
    flat(a, (716, 593, 1022, 632), band=6, noise=1.5)           # titulo
    sub = a[646:742, 704:1300]; m = sub.mean(axis=2) < 90               # descripcion (sin tocar el circulo del icono): un solo color para las dos mitades
    c = np.median(sub[m], axis=0)
    solid(a, (704, 646, 1300, 685), c, 2.0); solid(a, (600, 686, 1300, 742), c, 2.0)
    flat(a, (672, 768, 812, 798), band=5, noise=1.0)             # Dano
    flat(a, (905, 768, 1098, 798), band=5, noise=1.0)            # Recarga
    flat(a, (1200, 768, 1328, 798), band=5, noise=1.0)           # Tecla
    flat(a, (1160, 777, 1181, 797), band=4, noise=1.0)           # letra de la tecla
    return a, g

def build_cmd(src):
    a = load(src)
    g = dict(portrait_blur=(117, 223, 518, 528), portrait_keep=[(123, 479, 173, 527), (454, 479, 507, 527), (117, 223, 158, 254), (478, 223, 518, 254)],
             name_clear=(292, 134, 540, 178), lvl_clear=(260, 566, 376, 592), hp_fill=(152, 611, 524, 645), exp_fill=(132, 814, 500, 838),
             stat_clear=[(425, 665, 515, 693), (425, 701, 515, 729), (425, 736, 515, 764), (425, 771, 515, 799)])
    left_panel(a, g)
    # la tarjeta "Seguir" viene resaltada: marco copiado de "Deambular" y fondo sin tinte
    for (x0, y0, x1, y1) in [(826, 208, 1084, 224), (826, 385, 1084, 401), (826, 208, 842, 401), (1068, 208, 1084, 401)]:
        copy_rect(a, (x0, y0, x1, y1), (x0 + 270, y0))
    copy_rect(a, (814, 208, 826, 401), (814, 208)) if False else None
    copy_rect(a, (1084, 208, 1096, 401), (1084, 208))  # hueco entre tarjetas (sin brillo)
    unglow(a, (1108, 222, 1342, 386), (10, 36, 22))
    recolor_text(a, (1130, 352, 1320, 398))
    flat(a, (650, 650, 852, 684), band=10, noise=1.5)            # titulo de la orden
    dark_fill(a, (592, 696, 1300, 776), lum=90)                  # texto de ayuda
    return a, g

def java_arrays(d, indent='    '):
    def conv(v):
        if isinstance(v, (list, tuple)): return '{' + ', '.join(conv(i) for i in v) + '}'
        if isinstance(v, bool): return 'true' if v else 'false'
        return str(int(v))
    def typ(v):
        n = 0
        while isinstance(v, (list, tuple)): v = v[0] if len(v) else 0; n += 1
        return 'int' + '[]' * n
    return '\n'.join('%spublic static final %s %s = %s;' % (indent, typ(v), k, conv(v)) for k, v in d.items())

def layout():
    """Coordenadas de todo lo que PetSkinScreen dibuja o pulsa, en pixeles de los fondos de 1086 de ancho."""
    M = {n: Map(n) for n in CROP}
    def R(n, q): return M[n].r(q)
    def P(n, q): return M[n].p(q)
    def S(n, v): return int(round(v * M[n].s))
    cmd, atk, tal = 'cmd', 'atk', 'tal'
    L = {}
    L['TEX_H'] = [M[cmd].h, M[atk].h, M[tal].h]
    L['CLOSE'] = [R(cmd, (1315, 81, 1372, 138)), R(atk, (1335, 94, 1394, 150)), R(tal, (1344, 77, 1397, 131))]
    L['TABS'] = [[R(cmd, q) for q in [(237, 887, 556, 950), (572, 887, 891, 950), (910, 887, 1215, 950)]],
                 [R(atk, q) for q in [(244, 887, 569, 954), (581, 887, 902, 954), (925, 887, 1237, 954)]],
                 [R(tal, q) for q in [(248, 961, 552, 1017), (575, 961, 875, 1017), (898, 961, 1208, 1017)]]]
    L['NAMEBOX'] = [R(cmd, (292, 126, 556, 186)), R(atk, (296, 126, 570, 186)), R(tal, (255, 102, 455, 153))]
    L['PORTRAIT'] = [R(cmd, (111, 217, 523, 533)), R(atk, (113, 225, 527, 537)), R(tal, (92, 186, 384, 420))]
    L['LVL'] = P(cmd, (317, 579)) + P(atk, (322, 579)) + P(tal, (236, 474))
    L['HP'] = [R(cmd, (152, 613, 524, 642)), R(atk, (166, 614, 526, 643)), R(tal, (124, 508, 386, 534))]
    L['EXP'] = [R(cmd, (132, 814, 500, 838)), R(atk, (136, 812, 510, 838)), [0, 0, 0, 0]]
    L['STAT_RIGHT'] = [M[cmd].x(500), M[atk].x(511), M[tal].x(376)]
    L['STAT_Y'] = [[M[cmd].y(v) for v in (679, 715, 750, 785)], [M[atk].y(v) for v in (679, 715, 750, 785)], [M[tal].y(v) for v in (570, 600, 628, 657)]]
    L['POINTS_C'] = P(tal, (273, 720)) + [M[tal].y(752)]
    L['RESET_BOX'] = R(tal, (103, 797, 372, 842))
    L['MEDAL'] = R(tal, (598, 686, 861, 848))
    L['BAR'] = R(tal, (545, 908, 930, 928))
    L['PROG_TEXT'] = P(tal, (1140, 913)) + [1]
    L['PANEL_ICON'] = R(tal, (1092, 198, 1177, 282))
    L['PANEL_PILL'] = R(tal, (1192, 238, 1358, 273))
    L['BTN'] = R(tal, (1092, 725, 1362, 785))
    L['OVERLAY0'] = R(cmd, (551, 205, 1356, 840))
    L['OVERLAY1'] = R(atk, (553, 210, 1376, 840))
    L['OVERLAY2'] = R(tal, (395, 145, 1385, 880))
    L['PANEL_TITLE'] = P(tal, (1185, 200)) + [S(tal, 185)]
    L['PANEL_EFF'] = P(tal, (1088, 292)) + [S(tal, 282)]
    L['REQ_HEAD_Y'] = M[tal].y(590)
    cm = M[cmd]
    L['CARD_X'] = [cm.x(556), cm.x(826), cm.x(1096)]; L['CARD_Y'] = [cm.y(208), cm.y(411)]
    L['CARD_W'] = [S(cmd, 258)] * 3; L['CARD_H'] = [S(cmd, 193), S(cmd, 190)]
    L['CMD_TITLE'] = [P(cmd, (664, 654)), P(cmd, (596, 705)) + [S(cmd, 700)]]
    L['ATK_CARD'] = [R(atk, q) for q in [(557, 215, 852, 365), (864, 215, 1173, 365), (557, 379, 852, 528), (864, 379, 1173, 528), (1182, 215, 1373, 528)]]
    circ = S(atk, 94)
    L['ATK_CIRCLE'] = [P(atk, (cx - 47, cy - 47)) for cx, cy in [(615, 288), (923, 285), (618, 449), (923, 449)]]
    L['ATK_CIRC'] = circ
    L['ATK_PASSIVE'] = P(atk, (1220, 240)) + [S(atk, 123)]
    L['ATK_DETAIL_ICON'] = R(atk, (597, 581, 700, 685))
    L['DET_TITLE'] = P(atk, (720, 598))
    L['DET_DESC'] = P(atk, (605, 650)) + [S(atk, 690)]
    L['DET_PILL'] = [R(atk, q) for q in [(597, 758, 820, 808), (838, 758, 1106, 808), (1125, 758, 1336, 808)]]
    L['DET_OFF'] = [S(atk, 75), S(atk, 74), S(atk, 84)]
    L['DET_KEY'] = P(atk, (1169, 787))
    tm = M[tal]
    ids = list(NODES.keys())
    L['NODE_ID'] = None
    L['NODE_X'] = [tm.x(NODES[i][0]) for i in ids]
    L['NODE_Y'] = [tm.y(NODES[i][1]) for i in ids]
    L['NODE_HALF'] = S(tal, 40)
    L['P1'] = P(tal, (734, 745))
    L['LOCK'] = P(tal, (815, 798))
    del L['NODE_ID']
    return L, ids

def write_java(repo, L, ids):
    path = os.path.join(repo, 'src/main/java/com/prodigytown/creatures/client/CompyGui.java')
    body = java_arrays(L)
    idl = ', '.join('"%s"' % i for i in ids)
    open(path, 'w').write('package com.prodigytown.creatures.client;\n\n/** GENERADO por tools/compy/gui.py: coordenadas (pixeles de textures/gui/compy2/*.png, 1086 de ancho) de los tres fondos de la pantalla del Compy. No editar a mano. */\n'
        'final class CompyGui {\n    private CompyGui() {}\n    /** Orden de NODE_X / NODE_Y. */\n    static final String[] NODE_ID = {%s};\n%s\n}\n' % (idl, body))

if __name__ == '__main__':
    repo, cmd, atk, tal = sys.argv[1:5]
    prev = sys.argv[5] if len(sys.argv) > 5 else None
    for name, fn, src in (('cmd', build_cmd, cmd), ('atk', build_atk, atk), ('tal', build_tal, tal)):
        a, g = fn(src)
        save_bg(a, name, repo, prev)
    L, ids = layout()
    write_java(repo, L, ids)
    print('ok', L['TEX_H'])
