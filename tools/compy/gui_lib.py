"""Utilidades de limpieza de las maquetas de la interfaz del Compy (coordenadas en pixeles de la maqueta original, 1448 x 1086)."""
import numpy as np, cv2
from PIL import Image

rng = np.random.default_rng(11)

def load(path):
    return np.array(Image.open(path).convert('RGB')).astype(np.float32)

def _noise(h, w, amp):
    n = rng.normal(0, 1, (h // 2 + 1, w // 2 + 1, 1)).astype(np.float32)
    return cv2.resize(n, (w, h), interpolation=cv2.INTER_NEAREST)[..., None] * amp

def flat(a, r, band=4, noise=2.0):
    """Rellena el rectangulo fila a fila interpolando entre las bandas de fondo de su izquierda y su derecha."""
    x0, y0, x1, y1 = r
    L = a[y0:y1, x0 - band:x0].mean(axis=1); R = a[y0:y1, x1:x1 + band].mean(axis=1)
    t = np.linspace(0, 1, x1 - x0)[None, :, None]
    out = L[:, None, :] * (1 - t) + R[:, None, :] * t
    a[y0:y1, x0:x1] = np.clip(out + _noise(y1 - y0, x1 - x0, noise), 0, 255)

def solid(a, r, color, noise=1.5):
    x0, y0, x1, y1 = r
    out = np.empty((y1 - y0, x1 - x0, 3), np.float32); out[:] = color
    a[y0:y1, x0:x1] = np.clip(out + _noise(y1 - y0, x1 - x0, noise), 0, 255)

def vgrad(a, r, top, bot, noise=1.5):
    x0, y0, x1, y1 = r
    t = np.linspace(0, 1, y1 - y0)[:, None, None]
    out = np.array(top, np.float32) * (1 - t) + np.array(bot, np.float32) * t
    out = np.repeat(out, x1 - x0, axis=1)
    a[y0:y1, x0:x1] = np.clip(out + _noise(y1 - y0, x1 - x0, noise), 0, 255)

def dark_fill(a, r, lum=80, noise=2.0):
    """Rellena con la mediana de los pixeles oscuros del propio rectangulo (paneles de fondo casi plano con texto claro encima)."""
    x0, y0, x1, y1 = r
    sub = a[y0:y1, x0:x1]
    m = sub.mean(axis=2) < lum
    c = np.median(sub[m], axis=0) if m.sum() > 20 else np.array([30, 32, 34], np.float32)
    solid(a, r, c, noise)

def text_inpaint(a, r, hi=170, lo=-1, grow=5, radius=5, minchan=0):
    """Borra el texto claro (y oscuro si lo>=0) del rectangulo con inpainting."""
    x0, y0, x1, y1 = r
    sub = a[y0:y1, x0:x1]
    lum = sub.mean(axis=2)
    m = (lum > hi)
    if minchan: m &= sub.min(axis=2) > minchan
    if lo >= 0: m |= lum < lo
    m = cv2.dilate(m.astype(np.uint8) * 255, np.ones((grow, grow), np.uint8))
    full = np.zeros(a.shape[:2], np.uint8); full[y0:y1, x0:x1] = m
    a[:] = cv2.inpaint(np.clip(a, 0, 255).astype(np.uint8), full, radius, cv2.INPAINT_TELEA).astype(np.float32)

def blur_region(a, r, sigma=14, dark=0.8, keep=()):
    """Desenfoca y oscurece una region (retrato); 'keep' = rectangulos que se restauran tal cual (iconos de las esquinas)."""
    x0, y0, x1, y1 = r
    orig = a.copy()
    reg = a[y0:y1, x0:x1]
    # primero se quita el dino (pixeles saturados rojos / crema / amarillos) para que no deje una mancha
    rr, gg, bb = reg[..., 0], reg[..., 1], reg[..., 2]
    m = (((rr - gg) > 40) & ((rr - bb) > 40) & (rr > 90)) | ((rr > 170) & (gg > 150) & (bb > 105) & (bb < rr - 8)) | ((rr > 200) & (gg > 190) & (bb < 100))
    m = cv2.dilate(m.astype(np.uint8) * 255, np.ones((9, 9), np.uint8), iterations=2)
    full = np.zeros(a.shape[:2], np.uint8); full[y0:y1, x0:x1] = m
    inp = cv2.inpaint(np.clip(a, 0, 255).astype(np.uint8), full, 12, cv2.INPAINT_TELEA).astype(np.float32)
    reg = inp[y0:y1, x0:x1]
    bl = cv2.GaussianBlur(reg, (0, 0), sigma)
    mk = cv2.GaussianBlur(m.astype(np.float32) / 255, (0, 0), sigma * 0.7)[..., None]
    out = (reg * (1 - mk) + bl * mk) * dark
    a[y0:y1, x0:x1] = out
    for (kx0, ky0, kx1, ky1) in keep: a[ky0:ky1, kx0:kx1] = orig[ky0:ky1, kx0:kx1]

def copy_rect(a, src, dst_xy):
    x0, y0, x1, y1 = src; dx, dy = dst_xy
    a[dy:dy + (y1 - y0), dx:dx + (x1 - x0)] = a[y0:y1, x0:x1].copy()

def recolor_text(a, r):
    """Convierte el texto amarillo (resaltado) en blanco: rellena el canal azul con el promedio de rojo y verde."""
    x0, y0, x1, y1 = r
    sub = a[y0:y1, x0:x1].copy()
    m = (sub[..., 0] > 150) & (sub[..., 1] > 130) & (sub[..., 2] < sub[..., 1] * 0.75)
    sub[..., 2] = np.where(m, np.minimum(255, (sub[..., 0] + sub[..., 1]) / 2 * 0.97), sub[..., 2])
    sub[..., 0] = np.where(m, np.minimum(255, sub[..., 0] * 1.04), sub[..., 0])
    a[y0:y1, x0:x1] = sub

def unglow(a, r, target, sigma=18, lo=95, hi=160):
    """Quita el brillo del resaltado: estima el sobretono del fondo oscuro como un campo suave (convolucion normalizada) y lo resta."""
    x0, y0, x1, y1 = r
    sub = a[y0:y1, x0:x1]
    lum = sub.mean(axis=2)
    bg = (lum < lo).astype(np.float32)
    diff = (sub - np.array(target, np.float32)) * bg[..., None]
    num = cv2.GaussianBlur(diff, (0, 0), sigma)
    den = cv2.GaussianBlur(bg, (0, 0), sigma)[..., None]
    field = num / np.maximum(den, 1e-3)
    w = np.clip((hi - lum) / float(hi - lo), 0, 1)[..., None]
    a[y0:y1, x0:x1] = np.clip(sub - field * w, 0, 255)
