"""Texturas de la interfaz del Cronista a partir del poster de conceptos del usuario (Prodigy Quests).
Uso: python3 qgui.py <repo> <poster.png>
Salida: src/main/resources/assets/prodigycreatures/textures/gui/quests/*.png
 - scene.png   : marco de piedra con hiedra + biblioteca (estandarte, antorcha) SIN el NPC (se dibuja en vivo) y sin contenido
 - plate.png   : placa del titulo (cuerpo sin texto)
 - parch.png   : pergamino 9-slice
 - btn_green.png / btn_grey.png : botones 9-slice (cuerpo sin texto/icono)
 - row.png / row_sel.png : tarjetas de la lista 9-slice
Escala: tex = 2 x unidades virtuales de la GUI (pantalla virtual 420x277). Determinista."""
import os, sys
import numpy as np, cv2
from PIL import Image

OUT = 'src/main/resources/assets/prodigycreatures/textures/gui/quests'
MCMETA = '{"texture":{"blur":true}}\n'
X0, Y0, X1, Y1 = 10, 606, 714, 1070      # panel 3 del poster
S = 840.0 / (X1 - X0)                    # origen -> textura

def save(repo, name, arr):
    d = os.path.join(repo, OUT); os.makedirs(d, exist_ok=True)
    Image.fromarray(arr).save(os.path.join(d, name), optimize=True)
    open(os.path.join(d, name + '.mcmeta'), 'w').write(MCMETA)

def rs(a, f=S):
    h, w = a.shape[:2]
    return cv2.resize(a, (max(1, int(round(w * f))), max(1, int(round(h * f)))), interpolation=cv2.INTER_AREA if f < 1 else cv2.INTER_CUBIC)

def greens(a):
    r, g, b = [a[..., i].astype(int) for i in range(3)]
    return (g > r + 22) & (g > b + 22) & (g > 85)

def noise(h, w, seed, sigma=1.6):
    return np.random.RandomState(seed).normal(0, sigma, (h, w, 1))

def build_scene(im, repo):
    P = im[Y0:Y1, X0:X1].copy(); H, W = P.shape[:2]
    L = lambda x, y: (x - X0, y - Y0)
    # 1) esquina superior izquierda: la pestana del poster tapa el marco -> se repite la barra superior limpia
    bar = im[606:652, 330:520]; x = 0; k = 0
    while x < 300 - X0 + 16:
        piece = bar[:, ::-1] if k % 2 else bar
        n = min(piece.shape[1], 300 - X0 + 16 - x); P[0:46, x:x + n] = piece[:, :n]; x += n; k += 1
    orig = P.copy()
    # 2) pared oscura continua: sustituye el contenido del poster a la derecha y al NPC; la hiedra verde del marco se conserva
    def wall(h, w, y_top):
        yy = (np.arange(h)[:, None, None] + y_top).astype(np.float32)
        t = np.clip((yy - (632 - Y0)) / float(1050 - 632), 0, 1)
        floor = np.clip((yy - (985 - Y0)) / 40.0, 0, 1)
        col = np.array([40, 33, 29]) * (1 - t) + np.array([27, 22, 20]) * t
        col = col * (1 - floor) + np.array([47, 34, 25]) * floor
        return col + noise(h, w, 3 + int(y_top), 1.5)
    ry0, ry1 = 632 - Y0, 1048 - Y0
    rx0, rx1 = 212 - X0, 699 - X0
    fill = wall(ry1 - ry0, rx1 - rx0, ry0)
    ramp = np.clip((np.arange(rx1 - rx0) - 0) / 8.0, 0, 1)[None, :, None]    # el borde izquierdo se funde con la escena original
    P[ry0:ry1, rx0:rx1] = np.clip(P[ry0:ry1, rx0:rx1] * (1 - ramp) + fill * ramp, 0, 255).astype(np.uint8)
    for (bx0, by0, bx1, by1) in ((636, 606, 702, 706), (652, 972, 702, 1052)):
        a0, b0, a1, b1 = bx0 - X0, by0 - Y0, bx1 - X0, by1 - Y0
        m = greens(orig[b0:b1, a0:a1]); sub = P[b0:b1, a0:a1]; sub[m] = orig[b0:b1, a0:a1][m]
    # 3) NPC fuera: la silueta (poligono holgado) se rellena con la misma pared; el modelo en vivo se dibuja encima
    mask = np.zeros((H, W), np.uint8)
    poly = np.array([L(98, 698), L(178, 696), L(226, 722), L(228, 860), L(224, 1004), L(30, 1004), L(30, 892), L(84, 880), L(84, 760)], np.int32)
    cv2.fillPoly(mask, [poly], 255)
    cv2.rectangle(mask, L(100, 1000), L(226, 1048), 255, -1)   # garra decorativa del poster
    cv2.rectangle(mask, L(22, 626), L(214, 647), 255, -1)       # banda verde bajo la barra superior
    soft = cv2.GaussianBlur(mask, (0, 0), 5)[..., None] / 255.0
    full = wall(H, W, 0)
    P = np.clip(P * (1 - soft) + full * soft, 0, 255).astype(np.uint8)
    # hiedra inferior izquierda original
    a0, b0, a1, b1 = 22 - X0, 960 - Y0, 140 - X0, 1048 - Y0
    m = greens(orig[b0:b1, a0:a1]); sub = P[b0:b1, a0:a1]; sub[m] = orig[b0:b1, a0:a1][m]
    out = cv2.resize(P, (840, int(round(H * S))), interpolation=cv2.INTER_CUBIC)
    print('scene', out.shape)
    save(repo, 'scene.png', out)

def strip_fill(a, x_from, x_to, sx0, sx1):
    """Cada fila entre x_from..x_to toma la mediana de su franja limpia sx0..sx1 (quita texto e iconos)."""
    a = a.copy()
    for y in range(a.shape[0]): a[y, x_from:x_to] = np.median(a[y, sx0:sx1], axis=0)
    return a

def build_plate(im, repo):
    c = im[640:703, 262:631].copy()
    c = strip_fill(c, 34, 600 - 262, 346 - 262, 360 - 262)
    print('plate', c.shape); save(repo, 'plate.png', rs(c))

def build_parch(im, repo):
    x0, y0, x1, y1 = 220, 700, 697, 985; cs = 20
    par = im[y0:y1, x0:x1]; h, w = par.shape[:2]
    light = par[(par.sum(axis=2) > 520)]; col = np.median(light, axis=0)
    N = 96; sp = np.zeros((N, N, 3), np.float32)
    sp[:] = col
    yy = np.linspace(0, 1, N)[:, None, None]; xx = np.linspace(0, 1, N)[None, :, None]
    sp *= (1.0 - 0.07 * ((xx - 0.5) ** 2 + (yy - 0.5) ** 2) * 4)
    sp += noise(N, N, 11, 2.0)
    # bordes: perfiles medianos (robustos a iconos) estirados
    top = np.median(par[0:cs, 60:w - 60], axis=1); bot = np.median(par[h - cs:h, 60:w - 60], axis=1)
    lef = np.median(par[40:h - 40, 0:cs], axis=0); rig = np.median(par[40:h - 40, w - cs:w], axis=0)
    sp[0:cs, cs:N - cs] = top[:, None, :]; sp[N - cs:N, cs:N - cs] = bot[:, None, :]
    sp[cs:N - cs, 0:cs] = lef[None, :, :]; sp[cs:N - cs, N - cs:N] = rig[None, :, :]
    sp[0:cs, 0:cs] = par[0:cs, 0:cs]; sp[0:cs, N - cs:N] = par[0:cs, w - cs:w]
    sp[N - cs:N, 0:cs] = par[h - cs:h, 0:cs]; sp[N - cs:N, N - cs:N] = par[h - cs:h, w - cs:w]
    out = rs(np.clip(sp, 0, 255).astype(np.uint8))
    print('parch', out.shape, 'cap', int(round(cs * S))); save(repo, 'parch.png', out)

def build_buttons(im, repo):
    g = im[458:499, 262:471].copy(); g = strip_fill(g, 20, g.shape[1] - 20, g.shape[1] - 32, g.shape[1] - 20)
    save(repo, 'btn_green.png', rs(g))
    b = im[509:550, 487:691].copy(); b = strip_fill(b, 16, b.shape[1] - 16, 645 - 487, 672 - 487)
    save(repo, 'btn_grey.png', rs(b))
    print('btn', rs(g).shape, rs(b).shape)

def build_rows(im, repo):
    n = im[367:419, 965:1389].copy(); n = strip_fill(n, 14, n.shape[1] - 14, 1230 - 965, 1350 - 965)
    s = im[310:361, 965:1389].copy(); s = strip_fill(s, 14, s.shape[1] - 14, 1290 - 965, 1350 - 965)
    save(repo, 'row.png', rs(n)); save(repo, 'row_sel.png', rs(s)); print('rows', rs(n).shape, rs(s).shape)

if __name__ == '__main__':
    repo, poster = sys.argv[1], sys.argv[2]
    im = np.array(Image.open(poster).convert('RGB'))
    build_scene(im, repo); build_plate(im, repo); build_parch(im, repo); build_buttons(im, repo); build_rows(im, repo)
