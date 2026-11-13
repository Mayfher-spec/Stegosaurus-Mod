"""Recursos graficos del Dilophosaurus a partir de las maquetas del usuario (HUD: barra de vida y botones de habilidad).
Uso: python3 gui.py <repo> <hpbar.png> <abil.png> [<armaduras.png>]
 - hud_frame_{0..4}.png : marco con retrato por variante (sin texto, sin relleno de vida y sin segmentos de XP: eso lo dibuja el codigo)
 - ab_{bite,claw,pounce,frenzy,spit}.png : discos de habilidad (anillo incluido) con mascara circular
Determinista: no usa aleatoriedad."""
import os, sys
import numpy as np, cv2
from PIL import Image

OUT = 'src/main/resources/assets/prodigycreatures/textures/gui/dilo'
MCMETA = '{"texture":{"blur":true}}\n'

def save(repo, name, arr):
    d = os.path.join(repo, OUT); os.makedirs(d, exist_ok=True)
    Image.fromarray(arr).save(os.path.join(d, name), optimize=True)
    open(os.path.join(d, name + '.mcmeta'), 'w').write(MCMETA)

def alpha_from_border(rgb, tol=26):
    """Flood fill desde el borde sobre el fondo casi negro/azulado de la maqueta -> alfa 0 fuera del marco."""
    h, w, _ = rgb.shape
    bg = np.median(np.concatenate([rgb[:6].reshape(-1, 3), rgb[-6:].reshape(-1, 3), rgb[:, :6].reshape(-1, 3), rgb[:, -6:].reshape(-1, 3)]), axis=0)
    close = (np.abs(rgb.astype(int) - bg).sum(axis=2) < tol * 3 // 2 + 10).astype(np.uint8)
    n, lab = cv2.connectedComponents(close, connectivity=4)
    edge = set(lab[0].tolist()) | set(lab[-1].tolist()) | set(lab[:, 0].tolist()) | set(lab[:, -1].tolist()); edge.discard(0)
    out = np.isin(lab, list(edge))
    a = (~out).astype(np.uint8) * 255
    a = cv2.morphologyEx(a, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    # el interior oscuro (placa, pista) se parece al fondo y se cuela por huecos del marco: se cierra para que quede opaco
    a = cv2.morphologyEx(a, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (71, 71)))
    cnt, _ = cv2.findContours(a, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    a = np.zeros_like(a); cv2.drawContours(a, cnt, -1, 255, -1)
    return cv2.GaussianBlur(a, (0, 0), 1.2)

def clean_hp(im):
    """Quita nombre, relleno rojo, texto de vida y segmentos de XP rellenando con el color de la pista."""
    im = im.copy()
    # nombre/nivel: la placa es plana; cada fila toma el color de su extremo izquierdo limpio
    for y in range(194, 252):
        im[y, 742:1772] = np.median(im[y, 668:732], axis=0)
    # barra de vida: cada fila toma el color de la pista vacia a la derecha del relleno
    for y in range(290, 376):
        im[y, 651:1902] = np.median(im[y, 1668:1700], axis=0)
    # segmentos de XP (y su brillo cian superior): idem con la guia oscura a su derecha
    for y in range(401, 457):
        im[y, 656:1893] = np.median(im[y, 1775:1800], axis=0)
    return im

def retint_portrait(im, variant):
    """Cambia el color de piel (verdes) y cresta (rojos) del retrato segun la variante."""
    if variant == 0: return im
    out = im.copy()
    hsv = cv2.cvtColor(im, cv2.COLOR_RGB2HSV).astype(np.float32)
    h, s, v = hsv[..., 0], hsv[..., 1], hsv[..., 2]
    yy, xx = np.mgrid[:im.shape[0], :im.shape[1]]
    region = (((xx - 380) / 190.0) ** 2 + ((yy - 330) / 188.0) ** 2 < 1.0) & (yy < 492) & (xx > 200)
    green = region & (h >= 30) & (h <= 85) & (s > 55)
    red = region & ((h <= 9) | (h >= 168)) & (s > 120)
    hs, ss_, vs = h.copy(), s.copy(), v.copy()
    if variant == 1:    # electrico: piel azul, cresta cian
        hs[green] = 108; ss_[green] = np.minimum(255, s[green] * 1.15)
        hs[red] = 92
    elif variant == 2:  # magma: piel casi negra con tinte rojo, cresta anaranjada
        hs[green] = 4; ss_[green] = s[green] * 0.35; vs[green] = v[green] * 0.42
        hs[red] = 14
    elif variant == 3:  # desierto: piel arena
        hs[green] = 17; ss_[green] = s[green] * 0.62; vs[green] = np.minimum(255, v[green] * 1.12)
        hs[red] = 20; ss_[red] = s[red] * 0.8
    elif variant == 4:  # espectral: verde-azulado oscuro, cresta menta
        hs[green] = 80; vs[green] = v[green] * 0.8
        hs[red] = 75; ss_[red] = s[red] * 0.7
    hsv2 = np.stack([hs, np.clip(ss_, 0, 255), np.clip(vs, 0, 255)], axis=-1).astype(np.uint8)
    res = cv2.cvtColor(hsv2, cv2.COLOR_HSV2RGB)
    m = cv2.GaussianBlur((green | red).astype(np.float32), (0, 0), 0.8)[..., None]
    return np.clip(im * (1 - m) + res * m, 0, 255).astype(np.uint8)

def build_hud(repo, hp_path):
    im = np.array(Image.open(hp_path).convert('RGB'))
    a = alpha_from_border(im)
    base = clean_hp(im)
    # recorte con margen
    ys, xs = np.where(a > 40); x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    print('hud bbox', x0, x1, y0, y1)
    W, H = 768, int(round(768 * (y1 - y0) / (x1 - x0)))
    s = W / float(x1 - x0)
    for v in range(5):
        rgb = retint_portrait(base, v)
        rgba = np.dstack([rgb, a])[y0:y1, x0:x1]
        prem = rgba.copy().astype(np.float32); prem[..., :3] *= prem[..., 3:] / 255.0
        small = cv2.resize(prem, (W, H), interpolation=cv2.INTER_AREA)
        al = small[..., 3:]; col = np.where(al > 0, small[..., :3] / np.maximum(al, 1e-3) * 255.0, 0)
        out = np.dstack([np.clip(col, 0, 255), np.clip(al, 0, 255)]).astype(np.uint8)
        save(repo, 'hud_frame_%d.png' % v, out)
    f = lambda x, y: (round((x - x0) * s, 1), round((y - y0) * s, 1))
    print('tex size', W, H)
    print('HP  hole', f(657, 294), f(1900, 370))
    print('XP  hole', f(665, 410), f(1890, 448))
    print('name center', f(1255, 222), 'portrait center', f(380, 330), 'banner', f(370, 540))
    return W, H

def circle(im, cx, cy, r, out=128, feather=2.0, glow=False):
    pad = int(r + 6)
    crop = im[cy - pad:cy + pad, cx - pad:cx + pad].copy()
    yy, xx = np.mgrid[:crop.shape[0], :crop.shape[1]]
    d = np.hypot(xx - pad, yy - pad)
    a = np.clip((r - d) / feather, 0, 1) * 255
    prem = np.dstack([crop.astype(np.float32) * (a[..., None] / 255.0), a]).astype(np.float32)
    small = cv2.resize(prem, (out, out), interpolation=cv2.INTER_AREA)
    al = small[..., 3:]; col = np.where(al > 0, small[..., :3] / np.maximum(al, 1e-3) * 255.0, 0)
    return np.dstack([np.clip(col, 0, 255), np.clip(al, 0, 255)]).astype(np.uint8)

def build_abilities(repo, ab_path):
    im = np.array(Image.open(ab_path).convert('RGB'))
    spec = {'bite': (190, 652, 150), 'claw': (522, 652, 150), 'pounce': (857, 652, 150), 'frenzy': (1216, 652, 150)}
    icons = {}
    for k, (cx, cy, r) in spec.items():
        icons[k] = circle(im, cx, cy, r)
        # el circulo de recarga numerado del zarpazo (burbuja "7") se tapa con el color del disco
    # la burbuja "7" de la maqueta del zarpazo: se rellena con inpaint
    cl = np.array(Image.open(ab_path).convert('RGB')); m = np.zeros(cl.shape[:2], np.uint8)
    cv2.circle(m, (608, 556), 48, 255, -1)
    cl2 = cv2.inpaint(cl, m, 14, cv2.INPAINT_TELEA)
    icons['claw'] = circle(cl2, 522, 652, 150)
    # escupitajo: el icono del frenesi virado a verde toxico + gotas
    rgb = icons['frenzy'][..., :3]; hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV).astype(np.float32)
    h, s, v = hsv[..., 0], hsv[..., 1], hsv[..., 2]
    gold = (h > 12) & (h < 32) & (s > 140)          # anillo dorado -> verde lima
    purple = (h > 120) & (h < 160)
    h2 = h.copy(); h2[purple] = 62; h2[gold] = 45
    h2[(~purple) & (~gold) & (h < 12)] = 50
    spit = cv2.cvtColor(np.dstack([h2, s, v]).astype(np.uint8), cv2.COLOR_HSV2RGB)
    spit = np.dstack([spit, icons['frenzy'][..., 3]])
    yy, xx = np.mgrid[:128, :128]
    for (dx, dy, rr) in ((100, 56, 6), (108, 70, 4), (96, 80, 5), (110, 52, 3)):
        m = ((xx - dx) ** 2 + (yy - dy) ** 2) < rr * rr
        spit[m, :3] = (176, 255, 90)
    icons['spit'] = spit
    for k, arr in icons.items(): save(repo, 'ab_%s.png' % k, arr)


def build_screen_bg(repo, armor_path):
    """Fondo de la pantalla del Dilo: el marco dorado con follaje de la maqueta de armaduras, con el contenido interior vaciado
    (titulo, retrato, tarjetas y pestanas se dibujan en vivo). Salida 840x?: se estira al panel (400x252 + 10 de margen por lado)."""
    im = np.array(Image.open(armor_path).convert('RGB'))
    def tile(dst_x0, dst_x1, src_x0, src_x1, y0, y1):
        strip = im[y0:y1, src_x0:src_x1].copy(); w = src_x1 - src_x0; x = dst_x0; k = 0
        while x < dst_x1:
            piece = strip[:, ::-1] if k % 2 else strip
            n = min(w, dst_x1 - x); im[y0:y1, x:x + n] = piece[:, :n]; x += n; k += 1
    tile(335, 1115, 1150, 1322, 100, 200)      # rotulo del titulo -> borde superior limpio
    tile(66, 196, 214, 274, 100, 275)          # bandera del retrato
    tile(1330, 1420, 1190, 1322, 100, 200)     # boton X
    # interior vaciado con degradado oscuro y un poco de ruido determinista
    x0, x1, y0, y1 = 36, 1418, 152, 985
    h, w = y1 - y0, x1 - x0
    yy = np.linspace(0, 1, h)[:, None, None]; xx = np.linspace(-1, 1, w)[None, :, None]
    base = (np.array([27, 33, 36]) * (1 - yy) + np.array([14, 18, 20]) * yy) * (1.0 - 0.18 * xx ** 2)
    rng = np.random.RandomState(7); base = base + rng.normal(0, 1.6, (h, w, 1))
    im[y0:y1, x0:x1] = np.clip(base, 0, 255).astype(np.uint8)
    out = im[112:1050]
    sm = cv2.resize(out, (840, int(round(840 * out.shape[0] / out.shape[1]))), interpolation=cv2.INTER_AREA)
    print('screen bg', sm.shape)
    save(repo, 'screen_bg.png', sm)

if __name__ == '__main__':
    repo, hp, ab = sys.argv[1], sys.argv[2], sys.argv[3]
    build_hud(repo, hp); build_abilities(repo, ab)
    if len(sys.argv) > 4: build_screen_bg(repo, sys.argv[4])
