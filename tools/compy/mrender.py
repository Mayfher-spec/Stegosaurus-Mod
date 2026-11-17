"""Visor ortogonal del Compy: dibuja la geometria REAL (los mismos cubos y UV que van al juego) con la textura del atlas.
Vistas: izquierda, derecha, frontal, trasera, superior, inferior y 3/4 isometrica. Uso: python3 mrender.py <png_salida> [variante] [vista]"""
import os, sys, math
import numpy as np
from PIL import Image, ImageDraw
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import mrig, mpaint
from mrig import crig
cp = mpaint.cp

def _pt(c, face, fu, fv):
    cx, cy, cz, w, h, d = c['x0'], c['y0'], c['z0'], c['w'], c['h'], c['d']
    if face == 'xmin': return (cx, cy + h - fv * h, cz + fu * d)
    if face == 'xmax': return (cx + w, cy + h - fv * h, cz + d - fu * d)
    if face == 'north': return (cx + fu * w, cy + h - fv * h, cz + d)
    if face == 'south': return (cx + w - fu * w, cy + h - fv * h, cz)
    if face == 'up': return (cx + w - fu * w, cy + h, cz + d - fv * d)
    return (cx + w - fu * w, cy, cz + fv * d)
NORMAL = {'xmin': (-1, 0, 0), 'xmax': (1, 0, 0), 'north': (0, 0, 1), 'south': (0, 0, -1), 'up': (0, 1, 0), 'down': (0, -1, 0)}

def basis(name):
    """(derecha, arriba, hacia el observador) en el marco de diseno."""
    n = lambda v: np.array(v, float) / np.linalg.norm(v)
    if name == 'left':   t = n((1, 0, 0));  u = n((0, 1, 0))
    elif name == 'right': t = n((-1, 0, 0)); u = n((0, 1, 0))
    elif name == 'front': t = n((0, 0, 1));  u = n((0, 1, 0))
    elif name == 'back':  t = n((0, 0, -1)); u = n((0, 1, 0))
    elif name == 'top':   t = n((0, 1, 0));  u = n((0, 0, 1))
    elif name == 'bottom': t = n((0, -1, 0)); u = n((0, 0, 1))
    else:                 t = n((0.75, 0.55, 0.85)); u = n((0, 1, 0)); u = n(u - t * (u @ t))
    r = np.cross(u, t)
    return r, u, t
VIEWS = ('left', 'right', 'front', 'back', 'top', 'bottom', 'iso')

def posed(pose=None):
    """cuadros de cada hueso; pose: {hueso: (rx, ry, rz)} se SUMA a la rotacion de reposo (para comprobar animaciones)."""
    (spec, cubes), H = mrig.get_all()
    sp = []
    for p in spec:
        q = dict(p)
        if pose and p['name'] in pose: q['rot'] = tuple(a + b for a, b in zip(p['rot'], pose[p['name']]))
        sp.append(q)
    fr = cp.frames(sp)
    return cubes, fr, H

def render(atlas, view, S=14, pad=6, bg=(236, 232, 222), pose=None, bone_off=None, hy=None, with_ground=True, size=None):
    cubes, fr, H = posed(pose)
    r, u, t = basis(view)
    L = np.array([0.35, 0.8, 0.5]); L /= np.linalg.norm(L)
    quads = []
    lo = 0.0
    for c in cubes:
        R, T = fr[c['part']]
        if bone_off and c['part'] in bone_off: T = T + np.array(bone_off[c['part']])
        for face, reg in crig.regions(c).items():
            n = R @ np.array(NORMAL[face], float)
            if n @ t <= 1e-6: continue
            x0, y0, x1, y1 = crig.prect(reg)
            shade = 0.62 + 0.38 * max(0.0, n @ L) + 0.10 * (n @ u > 0.5)
            for j in range(y0, y1):
                for i in range(x0, x1):
                    col = atlas[j, i]
                    if col[3] == 0: continue
                    fu0 = np.clip((i - reg[0]) / max(1e-6, reg[2] - reg[0]), 0, 1); fu1 = np.clip((i + 1 - reg[0]) / max(1e-6, reg[2] - reg[0]), 0, 1)
                    fv0 = np.clip((j - reg[1]) / max(1e-6, reg[3] - reg[1]), 0, 1); fv1 = np.clip((j + 1 - reg[1]) / max(1e-6, reg[3] - reg[1]), 0, 1)
                    pts = [T + R @ np.array(_pt(c, face, a, b)) for a, b in ((fu0, fv0), (fu1, fv0), (fu1, fv1), (fu0, fv1))]
                    sx = [p @ r for p in pts]; sy = [p @ u for p in pts]; dz = sum(p @ t for p in pts) / 4
                    quads.append((dz, sx, sy, tuple(int(min(255, col[k] * shade)) for k in range(3))))
    quads.sort(key=lambda q: q[0])
    allx = [x for q in quads for x in q[1]]; ally = [y for q in quads for y in q[2]]
    if size is None:
        x_lo, x_hi, y_lo, y_hi = min(allx), max(allx), min(ally), max(ally)
    else:
        x_lo, x_hi, y_lo, y_hi = size
    Wp = int((x_hi - x_lo) * S) + 2 * pad; Hp = int((y_hi - y_lo) * S) + 2 * pad
    img = Image.new('RGB', (Wp, Hp), bg); dr = ImageDraw.Draw(img)
    for dz, sx, sy, col in quads:
        poly = [((x - x_lo) * S + pad, (y_hi - y) * S + pad) for x, y in zip(sx, sy)]
        dr.polygon(poly, fill=col)
    return img, (x_lo, x_hi, y_lo, y_hi)

if __name__ == '__main__':
    out = sys.argv[1]; var = sys.argv[2] if len(sys.argv) > 2 else 'verde'; views = sys.argv[3].split(',') if len(sys.argv) > 3 else VIEWS
    atlas, _ = mpaint.make_atlas(var)
    atlas = cp.dilate(atlas)
    ims = [render(atlas, v)[0] for v in views]
    Wt = sum(i.width for i in ims) + 10 * (len(ims) + 1); Ht = max(i.height for i in ims) + 20
    sheet = Image.new('RGB', (Wt, Ht), (236, 232, 222)); x = 10
    for im in ims: sheet.paste(im, (x, 10)); x += im.width + 10
    sheet.save(out)
