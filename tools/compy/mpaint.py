"""Pintor de texturas del Compy: 8 variantes sobre la MISMA geometria y el MISMO patron.
Los patrones (franjas, manchas, vientre, sombras) se calculan UNA vez como mascaras que no dependen de la variante;
cada variante solo cambia la paleta. Asi la distribucion de rayas y zonas claras es equivalente en las 8."""
import os, sys
import numpy as np
import mrig
from mrig import crig
sys.path.insert(0, os.path.join(mrig.HERE, '..', 'carnotaurus'))
import paint as cp
W = crig.ATLAS
ss, lerp, hsh, frames, region_geom = cp.ss, cp.lerp, cp.hsh, cp.frames, cp.region_geom
VARIANTS = ('verde', 'rojo', 'azul', 'gris', 'amarillo', 'rosa', 'albino', 'melanico')
C = lambda *v: np.array(v, float)
def _pal(mid, dark, stripe, spot, belly, belly2, spine, brow, eye, pupil, claw=(52, 42, 34), claw_tip=(196, 182, 156), tooth=(238, 232, 212), mouth=(150, 62, 70), tongue=(196, 92, 100)):
    return dict(mid=C(*mid), dark=C(*dark), stripe=C(*stripe), spot=C(*spot), belly=C(*belly), belly2=C(*belly2), spine=C(*spine), brow=C(*brow),
                eye=C(*eye), pupil=C(*pupil), claw=C(*claw), claw_tip=C(*claw_tip), tooth=C(*tooth), mouth=C(*mouth), tongue=C(*tongue))
PAL = {
 'verde':    _pal((116, 134, 54), (78, 98, 40), (54, 70, 32), (104, 70, 38), (228, 216, 168), (190, 176, 126), (122, 86, 52), (92, 78, 40), (240, 198, 40), (18, 14, 8)),
 'rojo':     _pal((176, 70, 46), (132, 46, 34), (88, 30, 28), (66, 24, 22), (224, 198, 158), (188, 160, 122), (100, 42, 34), (120, 48, 34), (255, 196, 64), (24, 10, 8)),
 'azul':     _pal((38, 112, 196), (26, 80, 156), (16, 46, 112), (22, 58, 124), (194, 202, 212), (154, 164, 178), (30, 62, 114), (24, 70, 140), (255, 224, 90), (8, 14, 28)),
 'gris':     _pal((132, 132, 128), (100, 100, 98), (60, 60, 62), (46, 46, 50), (192, 188, 180), (152, 148, 140), (76, 72, 70), (88, 88, 88), (232, 204, 84), (12, 12, 12)),
 'amarillo': _pal((222, 172, 38), (184, 136, 26), (150, 104, 24), (86, 54, 26), (248, 228, 164), (216, 190, 120), (128, 86, 30), (170, 122, 30), (224, 112, 28), (26, 14, 6)),
 'rosa':     _pal((234, 154, 182), (208, 116, 150), (176, 52, 122), (196, 80, 134), (252, 228, 228), (224, 190, 198), (198, 86, 132), (214, 104, 146), (255, 214, 104), (34, 10, 22)),
 'albino':   _pal((240, 234, 218), (218, 208, 182), (204, 192, 164), (224, 202, 176), (253, 249, 240), (232, 224, 206), (226, 212, 190), (226, 214, 190), (216, 52, 52), (70, 0, 12), claw=(190, 160, 152), claw_tip=(240, 226, 214), mouth=(186, 96, 104), tongue=(226, 130, 136)),
 'melanico': _pal((46, 46, 50), (30, 30, 34), (112, 24, 28), (86, 16, 22), (74, 68, 72), (48, 42, 46), (126, 26, 30), (64, 20, 24), (232, 34, 34), (10, 0, 0), claw=(26, 24, 26), claw_tip=(120, 110, 110), tooth=(214, 206, 196), mouth=(110, 40, 48), tongue=(150, 54, 62)),
}

MARK = C(214, 30, 38); MARK_DARK = C(120, 14, 22)   # marca roja del lider (igual en las 8 variantes)

def cellhash(P, size, seed, off=(0, 0, 0)):
    q = np.floor((P + np.array(off)) / size)
    return hsh(q[:, 0], q[:, 1], q[:, 2], seed)

def masks(P, N, ly, ctx, face, fu, fv):
    """mascaras de patron, iguales para las 8 variantes: belly, stripe, spot, tone (variacion pixel a pixel), edge (sombreado de borde)."""
    n = len(P); opt = ctx['opt']; role = ctx['role']
    # franjas transversales sobre el lomo y los flancos; en la cola alternan bloques claros y oscuros
    s = P[:, 2] + 0.35 * P[:, 1]
    band = np.floor(s / 4.2)
    bh = hsh(band, np.zeros(n), np.zeros(n), 5)
    parity = (np.mod(band, 2) == 0).astype(float)
    cell = cellhash(P, 1.9, 11)                      # celdas de ~2 texels: aspecto pixelado de ROR
    dw = ss(0.12, 0.62, ly)                          # peso dorsal: las franjas se desvanecen hacia el vientre
    if face == 'up': dw = np.ones(n)
    if face == 'down': dw = np.zeros(n)
    stripe = parity * (0.75 + 0.25 * bh) * dw * (cell > 0.10)
    stripe = np.where(cell > 0.93, 1.0 - stripe, stripe)   # pocas celdas invertidas: rompe la regularidad
    spot = (cellhash(P, 2.2, 23, (1.0, 0.5, 0.0)) > 0.86).astype(float) * dw * (1.0 - stripe)
    b = np.zeros(n)
    if opt.get('bf'): b = 1.0 - ss(0.10, 0.34, ly)
    if opt.get('jaw'): b = 1.0 - ss(0.30, 0.75, ly)
    if role == 'belly': b = np.ones(n)
    if face == 'down' and not opt.get('leg') and ctx['grp'] != 'arm': b = np.ones(n)
    if opt.get('head') and not opt.get('jaw'):
        b = np.maximum(b * 0.0, (1.0 - ss(0.12, 0.30, ly)) * 0.9) if face not in ('up', 'down') else b
    if ctx['part'] in ('neck1', 'neck2') and face in ('xmin', 'xmax'): b = np.maximum(b, 1.0 - ss(0.20, 0.46, ly))   # garganta clara
    tone = 0.94 + 0.12 * cellhash(P, 1.0, 31)
    # sombreado de borde (profundidad pixelada) y oclusion hacia abajo
    ed = np.minimum(np.minimum(fu, 1 - fu), np.minimum(fv, 1 - fv))
    edge = 1.0 - 0.10 * ss(0.14, 0.0, ed)
    ao = 0.86 + 0.14 * ss(0.0, 0.7, ly)
    faceK = {'up': 1.07, 'down': 0.78}.get(face, 1.0)
    return b, stripe, spot, tone, edge * ao * faceK

def skin(var, P, N, ly, ctx, face, fu, fv):
    pal = PAL[var]; opt = ctx['opt']
    b, stripe, spot, tone, shade = masks(P, N, ly, ctx, face, fu, fv)
    n = len(P)
    base = lerp(pal['dark'], pal['mid'], 0.62 + 0.38 * cellhash(P, 3.0, 3))
    col = lerp(base, pal['stripe'], stripe * 0.95)
    col = lerp(col, pal['spot'], spot * 0.85)
    if opt.get('brow'): col = lerp(col, pal['brow'], 0.85)
    if opt.get('head') and face == 'up': col = lerp(col, pal['brow'], 0.45 * ss(0.3, 0.9, (1 - np.abs(fu - 0.5) * 2)))   # franja oscura sobre el craneo
    if opt.get('nostril'): col = lerp(col, pal['stripe'] * 0.8, 0.8)
    belly = pal['belly'] * (0.94 + 0.10 * cellhash(P, 1.0, 41))[:, None]
    col = lerp(col, lerp(pal['belly2'], belly, 1.0 if ctx['role'] == 'belly' or face in ('up', 'down') else ss(0.0, 0.7, ly)), b * 0.96)
    col = col * (tone * shade)[:, None]
    return col

def eye_tex(var, loc, ctx, col, closed=False):
    pal = PAL[var]; r = 1.75
    cz, cy = 3.3, 0.2
    dz = loc[:, 2] - cz; dy = loc[:, 1] - cy; e2 = (dz / r) ** 2 + (dy / r) ** 2
    m = e2 <= 1.0; ring = m & (e2 > 0.62); iris = m & ~ring
    out = col.copy()
    if closed:
        line = m & (np.abs(dy) < 0.5)
        out[m] = out[m] * 0.85; out[line] = pal['pupil']
        return out
    out[m] = pal['pupil']; out[iris] = pal['eye']
    pup = iris & (np.abs(dz) < 0.5) & (np.abs(dy) < 1.15); out[pup] = pal['pupil']
    glint = iris & ((dz - 0.75) ** 2 + (dy - 0.8) ** 2 < 0.36); out[glint] = np.array([255, 255, 240])
    return out

def make_atlas(var, closed=False):
    (spec, cubes), H = mrig.get_all()
    fr = frames(spec)
    atlas = np.zeros((H, W, 4), np.uint8); pal = PAL[var]
    for c in cubes:
        R, T = fr[c['part']]
        for face, r in crig.regions(c).items():
            rect, loc, nl, ii, jj, fu, fv = region_geom(c, face, r)
            P = loc @ R.T + T; Nw = nl @ R.T
            ly = np.clip((loc[:, 1] - c['y0']) / max(1e-6, c['h']), 0.0, 1.0)
            ctx = dict(opt=c['opt'], grp=c['grp'], part=c['part'], role=c['role'])
            role = c['role']; op = c['opt']
            if role in ('skin', 'belly'): col = skin(var, P, Nw, ly, ctx, face, fu, fv)
            elif role == 'mark':
                e2 = np.minimum(np.minimum(fu, 1 - fu), np.minimum(fv, 1 - fv))
                col = lerp(MARK, MARK_DARK, 0.55 * ss(0.20, 0.0, e2) + 0.25 * (1 - ss(0.0, 0.7, ly)))
                if face in ('up', 'down'): col = col * (1.08 if face == 'up' else 0.8)
            elif role == 'spike':
                t = ly; col = lerp(pal['spine'] * 0.78, pal['spine'] * 1.12, t[:, None] if t.ndim == 1 else t)
            elif role == 'claw':
                t = np.clip((loc[:, 2] - c['z0']) / max(1e-6, c['d']), 0, 1) if c['part'].startswith('toe') else 1 - ly
                col = lerp(pal['claw'], pal['claw_tip'], np.clip(t, 0, 1) ** 1.4)
            elif role == 'tooth':
                tip = (1 - ly) if op.get('upper') else ly
                col = lerp(pal['tooth'] * 0.82, pal['tooth'], tip)
                if op.get('upper') and face == 'down': col = np.tile(pal['mouth'] * 0.9, (len(ii), 1))
                if op.get('lower') and face == 'up': col = np.tile(pal['mouth'] * 0.8, (len(ii), 1))
            else:   # lengua
                col = np.tile(pal['tongue'], (len(ii), 1)) * (0.92 + 0.08 * np.cos(loc[:, 0] * 1.4))[:, None]
            if role in ('skin', 'belly') and ((op.get('head') and face == 'down') or (op.get('jaw') and face == 'up')):
                col = np.tile(pal['mouth'], (len(ii), 1)) * (0.9 if op.get('jaw') else 1.0)
            if c['part'] == 'head' and c['i'] == 0 and face in ('xmin', 'xmax'): col = eye_tex(var, loc, ctx, col, closed)
            atlas[jj, ii, :3] = np.clip(col, 0, 255).astype(np.uint8); atlas[jj, ii, 3] = 255
    return atlas, None
