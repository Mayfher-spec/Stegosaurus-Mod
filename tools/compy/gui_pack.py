"""Pagina "Manada" (grupo de caza) de la pantalla del Compy y barra de 4 pestanas comun a las cuatro paginas.
Coordenadas en pixeles de la maqueta original (1448 x 1086). Lo usa gui.py."""
import numpy as np, cv2
from gui_lib import *

# ------------------------------------------------------------------ barra de pestanas
PILLS = [(240, 934, 488, 992), (503, 934, 742, 992), (758, 934, 994, 992), (1010, 932, 1254, 994)]   # Comandos, Ataques, Talentos, Manada (maqueta de Manada)
TAB_SPAN = (240, 1254, 934, 992)
# franja de pestanas de cada pagina en su propia maqueta: (x0, x1, y0, y1) de las pastillas actuales
TABROW = {'cmd': (237, 1215, 887, 950), 'atk': (244, 1237, 887, 954), 'tal': (248, 1208, 961, 1017)}
M = 14   # margen alrededor de cada pastilla (incluye el brillo de la activa)

def _content_mask(crop, m, inset=(15, 10)):
    """Mascara del icono y la etiqueta (claros) dentro de una pastilla recortada con margen m."""
    h, w = crop.shape[:2]
    lum = crop.mean(axis=2)
    mk = np.zeros((h, w), np.uint8)
    ix, iy = inset
    sub = (lum[m + iy:h - m - iy, m + ix:w - m - ix] > 150).astype(np.uint8) * 255
    mk[m + iy:h - m - iy, m + ix:w - m - ix] = sub
    return cv2.dilate(mk, np.ones((3, 3), np.uint8))

def _empty(P, r):
    x0, y0, x1, y1 = r
    crop = np.clip(P[y0 - M:y1 + M, x0 - M:x1 + M], 0, 255).astype(np.uint8)
    mk = _content_mask(crop.astype(np.float32), M)
    return cv2.inpaint(crop, mk, 4, cv2.INPAINT_TELEA).astype(np.float32)

def strip_variants(P):
    """Cuatro franjas (una por pestana activa) con las pastillas de la maqueta de Manada, en pixeles de esa maqueta. Devuelve (lista, (x0, y0))."""
    F_in = _empty(P, PILLS[2])
    F_ac = _empty(P, PILLS[3])
    bx0, by0 = TAB_SPAN[0] - M, TAB_SPAN[2] - M - 2
    bx1, by1 = TAB_SPAN[1] + M, TAB_SPAN[3] + M + 2
    out = []
    for k in range(4):
        base = P[by0:by1, bx0:bx1].copy()
        for j, r in enumerate(PILLS):
            x0, y0, x1, y1 = r
            w, h = x1 - x0 + 2 * M, y1 - y0 + 2 * M
            frame = cv2.resize(F_ac if j == k else F_in, (w, h), interpolation=cv2.INTER_LANCZOS4)
            orig = P[y0 - M:y1 + M, x0 - M:x1 + M].copy()
            mk = _content_mask(orig, M).astype(np.float32) / 255.0
            mk = cv2.GaussianBlur(mk, (0, 0), 0.8)[..., None]
            tile = frame * (1 - mk) + orig * mk
            # costura suave con el fondo: rampa de 7 px en el borde del recorte
            e = np.ones((h, w), np.float32)
            ramp = np.minimum(np.arange(w), np.arange(w)[::-1]); ramp = np.clip(ramp / 7.0, 0, 1)
            rampv = np.minimum(np.arange(h), np.arange(h)[::-1]); rampv = np.clip(rampv / 7.0, 0, 1)
            al = (np.minimum(rampv[:, None], ramp[None, :]))[..., None]
            yy, xx = y0 - M - by0, x0 - M - bx0
            base[yy:yy + h, xx:xx + w] = base[yy:yy + h, xx:xx + w] * (1 - al) + tile * al
        out.append(base)
    return out, (bx0, by0)

def apply_tabs(a, name, k, variants, origin):
    """Sustituye la franja de pestanas de la pagina `name` por la variante con la pestana k activa (escalada a la franja de esa pagina)."""
    xa, xb, ya, yb = TABROW[name]
    sx = (xb - xa) / float(TAB_SPAN[1] - TAB_SPAN[0])
    sy = (yb - ya) / float(TAB_SPAN[3] - TAB_SPAN[2])
    v = variants[k]
    bx0, by0 = origin
    w = int(round(v.shape[1] * sx)); h = int(round(v.shape[0] * sy))
    t = cv2.resize(v, (w, h), interpolation=cv2.INTER_AREA if sx < 1 else cv2.INTER_LANCZOS4)
    x0 = int(round(xa - (TAB_SPAN[0] - bx0) * sx)); y0 = int(round(ya - (TAB_SPAN[2] - by0) * sy))
    ramp = np.clip(np.minimum(np.arange(w), np.arange(w)[::-1]) / 6.0, 0, 1)
    rampv = np.clip(np.minimum(np.arange(h), np.arange(h)[::-1]) / 6.0, 0, 1)
    al = np.minimum(rampv[:, None], ramp[None, :])[..., None]
    a[y0:y0 + h, x0:x0 + w] = a[y0:y0 + h, x0:x0 + w] * (1 - al) + t * al

def tab_rects(name, k=None):
    """Rectangulos de clic de las cuatro pestanas en la maqueta `name` (la de Manada usa los suyos)."""
    if name == 'pack': return [tuple(p) for p in PILLS]
    xa, xb, ya, yb = TABROW[name]
    sx = (xb - xa) / float(TAB_SPAN[1] - TAB_SPAN[0]); sy = (yb - ya) / float(TAB_SPAN[3] - TAB_SPAN[2])
    return [(int(round(xa + (p[0] - TAB_SPAN[0]) * sx)), int(round(ya + (p[1] - TAB_SPAN[2]) * sy)),
             int(round(xa + (p[2] - TAB_SPAN[0]) * sx)), int(round(ya + (p[3] - TAB_SPAN[2]) * sy))) for p in PILLS]

# ------------------------------------------------------------------ pagina Manada
# tarjetas de miembros (maqueta): lider, 3 miembros y el hueco "Invitar compy"
CARDS = [(453, 244, 637, 430), (648, 247, 818, 425), (828, 247, 1006, 425), (1017, 247, 1191, 425), (1205, 244, 1342, 428)]
BUTTONS = [(453, 443, 630, 487), (640, 443, 832, 487), (845, 443, 985, 487), (997, 443, 1172, 487), (1185, 443, 1345, 487)]  # crear, invitar, expulsar, nombrar lider, disolver
CHECK_X = 489
CHECK_Y = [580, 611, 644, 677, 709, 741]
SEL_Y = [555, 600, 649, 695, 740]
SEL_L = (1068, 1108)   # x de la flecha izquierda
SEL_R = (1296, 1336)   # x de la flecha derecha
SEL_VAL = (1110, 1294) # x del campo del valor

def strip_fill(a, r, lstrip, rstrip, sigma=2.0):
    """Rellena el rectangulo r fila a fila interpolando entre dos tiras limpias (x0, x1) de la izquierda y la derecha, con las filas suavizadas."""
    x0, y0, x1, y1 = r
    L = a[y0:y1, lstrip[0]:lstrip[1]].mean(axis=1); R = a[y0:y1, rstrip[0]:rstrip[1]].mean(axis=1)
    L = cv2.GaussianBlur(L.astype(np.float32), (1, 0), sigmaX=0.1, sigmaY=sigma).reshape(L.shape) if False else cv2.GaussianBlur(L.astype(np.float32).reshape(-1, 1, 3), (1, 0), 0.1, sigmaY=sigma).reshape(-1, 3)
    R = cv2.GaussianBlur(R.astype(np.float32).reshape(-1, 1, 3), (1, 0), 0.1, sigmaY=sigma).reshape(-1, 3)
    t = np.linspace(0, 1, x1 - x0)[None, :, None]
    out = L[:, None, :] * (1 - t) + R[:, None, :] * t
    a[y0:y1, x0:x1] = np.clip(out + rng.normal(0, 1.0, out.shape).astype(np.float32), 0, 255)

def build_pack(src, left_panel):
    a = load(src)
    g = dict(portrait_blur=(112, 224, 404, 514), portrait_keep=[(116, 470, 170, 519), (352, 470, 402, 519), (110, 220, 152, 246), (362, 220, 406, 246), (238, 204, 292, 246)],
             name_clear=(297, 134, 545, 178), lvl_clear=(208, 554, 318, 584), hp_fill=(150, 609, 408, 636), exp_fill=(122, 809, 400, 834),
             stat_clear=[(322, 662, 404, 694), (322, 696, 404, 726), (322, 729, 404, 759), (322, 761, 404, 791)])
    left_panel(a, g)
    # tarjetas de miembros: se quitan dibujo, nombre, nivel y la pastilla de rol (se dibujan en vivo con el Compy real)
    for i, (x0, y0, x1, y1) in enumerate(CARDS[:4]):
        ins = 18 if i == 0 else 13
        top = 268 if i == 0 else 252
        ls = a[:, x0 + ins + 2:x0 + ins + 8]
        def col(ya, yb): return ls[ya:yb].reshape(-1, 3).mean(axis=0)
        c_top, c_mid = col(top + 4, top + 10), col(292, 300)
        c_low = c_mid * 0.88
        for (ya, yb) in [(top, 362), (366, 421 if i > 0 else 392)]:
            ys = np.arange(ya, yb, dtype=np.float32)
            t1 = np.clip((ys - top) / float(300 - top), 0, 1)[:, None]
            t2 = np.clip((ys - 300) / 37.0, 0, 1)[:, None]
            colr = (c_top * (1 - t1) + c_mid * t1) * (1 - t2) + c_low * t2
            out = np.repeat(colr[:, None, :], (x1 - ins) - (x0 + ins), axis=1)
            a[ya:yb, x0 + ins:x1 - ins] = np.clip(out + rng.normal(0, 1.0, out.shape).astype(np.float32), 0, 255)
    # titulo "Miembros del grupo (n / m)": se redibuja entero
    flat(a, (706, 203, 1090, 234), band=8, noise=1.2)
    # casillas: se dejan vacias (borde verde y fondo oscuro); la marca se dibuja en vivo
    for cy in CHECK_Y:
        x0, y0, x1, y1 = CHECK_X - 16, cy - 15, CHECK_X + 16, cy + 15
        vgrad(a, (x0 + 5, y0 + 5, x1 - 5, y1 - 5), (36, 40, 30), (22, 26, 20), noise=1.0)
    # valores de los selectores
    for cy in SEL_Y:
        flat(a, (SEL_VAL[0], cy - 16, SEL_VAL[1], cy + 16), band=6, noise=1.2)
    # descripcion inferior
    dark_fill(a, (466, 826, 1302, 890), lum=90)
    return a, g

def layout_pack(Mp):
    """Coordenadas de la pagina Manada (en pixeles del fondo de 1086 de ancho)."""
    R = Mp.r; P = Mp.p
    L = {}
    L['PK_CARDS'] = [R(c) for c in CARDS]
    L['PK_BUTTONS'] = [R(b) for b in BUTTONS]
    L['PK_CHECK'] = [P((CHECK_X, y)) for y in CHECK_Y]
    L['PK_CHECK_HALF'] = int(round(13 * Mp.s))
    L['PK_CHECK_ROW'] = [R((470, y - 16, 806, y + 16)) for y in CHECK_Y]
    L['PK_SEL_ROW'] = [R((SEL_L[0], y - 17, SEL_R[1], y + 17)) for y in SEL_Y]
    L['PK_SEL_L'] = [R((SEL_L[0], y - 17, SEL_L[1], y + 17)) for y in SEL_Y]
    L['PK_SEL_R'] = [R((SEL_R[0], y - 17, SEL_R[1], y + 17)) for y in SEL_Y]
    L['PK_SEL_VAL'] = [P(((SEL_VAL[0] + SEL_VAL[1]) // 2, y)) for y in SEL_Y]
    L['PK_TITLE'] = P((895, 219))
    L['PK_DESC'] = P((470, 836)) + [int(round(830 * Mp.s))]
    L['PK_CHIP'] = [R((c[0] + 12, 394, c[2] - 12, 420)) for c in CARDS]
    L['PK_ART'] = [R((c[0] + 12, 262, c[2] - 12, 334)) for c in CARDS]
    L['PK_NAME_Y'] = Mp.y(350)
    L['PK_LVL_Y'] = Mp.y(379)
    return L
