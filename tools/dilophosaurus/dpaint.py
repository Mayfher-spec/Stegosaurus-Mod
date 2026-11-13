"""Pintor de texturas del Dilophosaurus: 5 variantes sobre la MISMA geometria (natural, electrico, magma, desierto, espectral) + mapa de brillo."""
import math, os, sys
import numpy as np
import drig
from drig import crig
sys.path.insert(0, os.path.join(drig.HERE, '..', 'carnotaurus'))
import paint as cp
from PIL import Image
W = 512
ss, lerp, fbm, voronoi, frames, region_geom = cp.ss, cp.lerp, cp.fbm, cp.voronoi, cp.frames, cp.region_geom
VARIANTS = ('natural', 'electric', 'magma', 'desert', 'spectral')
C = lambda *v: np.array(v, float)
PAL = {
 'natural':  dict(dark=C(66,78,34),  mid=C(104,118,52),  stripe=C(48,58,26),  spot=C(40,52,30),  belly=C(214,198,150), crest_a=C(186,70,38),  crest_b=C(238,150,60),
                  frill_a=C(78,112,50), frill_b=C(190,72,42), frill_c=C(238,150,60), rib=C(52,64,30), eye=C(232,164,40), pupil=C(14,12,8), claw_a=C(70,56,40), claw_b=C(28,24,20), tooth=C(238,230,206), tongue=C(190,84,92), mouth=C(150,60,66), glow=None),
 'electric': dict(dark=C(20,52,138), mid=C(34,92,206),   stripe=C(14,30,92),  spot=C(12,28,86),  belly=C(112,132,164), crest_a=C(40,170,210), crest_b=C(150,240,255),
                  frill_a=C(30,84,196), frill_b=C(50,200,255), frill_c=C(190,250,255), rib=C(14,36,110), eye=C(120,230,255), pupil=C(6,10,24), claw_a=C(44,52,70), claw_b=C(14,18,30), tooth=C(224,236,246), tongue=C(150,80,120), mouth=C(110,56,90), glow=C(70,210,255)),
 'magma':    dict(dark=C(20,14,14),  mid=C(52,24,22),    stripe=C(10,8,8),    spot=C(8,6,6),    belly=C(92,36,28),   crest_a=C(220,86,20),  crest_b=C(255,196,70),
                  frill_a=C(26,16,16), frill_b=C(168,36,16), frill_c=C(255,150,40), rib=C(8,6,6), eye=C(255,150,30), pupil=C(10,6,4), claw_a=C(48,34,30), claw_b=C(14,10,10), tooth=C(226,206,176), tongue=C(220,80,40), mouth=C(150,40,24), glow=C(255,110,24)),
 'desert':   dict(dark=C(136,98,56), mid=C(190,150,90),  stripe=C(112,76,42), spot=C(98,66,38),  belly=C(238,222,182), crest_a=C(206,152,50), crest_b=C(236,196,92),
                  frill_a=C(196,150,88), frill_b=C(176,92,56), frill_c=C(236,196,92), rib=C(110,70,40), eye=C(196,120,30), pupil=C(20,12,6), claw_a=C(120,92,62), claw_b=C(52,38,26), tooth=C(244,236,214), tongue=C(196,96,92), mouth=C(150,68,64), glow=None),
 'spectral': dict(dark=C(8,22,16),   mid=C(18,48,34),    stripe=C(6,14,10),   spot=C(4,12,8),    belly=C(40,74,58),   crest_a=C(70,220,150), crest_b=C(190,255,222),
                  frill_a=C(12,36,26), frill_b=C(40,170,110), frill_c=C(150,255,200), rib=C(6,16,12), eye=C(120,255,170), pupil=C(2,8,6), claw_a=C(36,60,48), claw_b=C(10,20,16), tooth=C(214,240,226), tongue=C(60,120,96), mouth=C(34,84,66), glow=C(70,240,160)),
}

def skin(var, P, N, ly, ctx, face):
    pal = PAL[var]; n = len(P); q = P / ctx['qs']; opt = ctx['opt']; grp = ctx['grp']
    n1 = fbm(q * 0.07, 3); d1, d2, rid = voronoi(P / 2.6, 5)
    cell = (0.94 + 0.12 * rid) * (1 - 0.16 * ss(0.10, 0.0, d2 - d1))
    dw = ss(0.15, 0.65, ly)
    if face == 'up': dw = np.ones(n)
    if face == 'down': dw = np.zeros(n)
    if opt.get('head'): dw = dw * 0.7 + 0.15
    b = np.zeros(n)
    if opt.get('bf') or ctx['role'] == 'belly': b = 1 - ss(0.10, 0.38, ly)
    if opt.get('jaw'): b = 1 - ss(0.30, 0.70, ly)
    if face == 'down' and grp not in ('arm',) and not opt.get('leg'): b = np.ones(n)
    if ctx['role'] == 'belly': b = np.ones(n)
    col = lerp(pal['dark'], pal['mid'], n1) * cell[:, None]
    gl = np.zeros((n, 3))
    wave = np.sin(P[:, 2] * 0.42 + 2.4 * fbm(q * 0.05 + 7, 11, 2) + 0.6 * np.sin(P[:, 1] * 0.10))
    bands = ss(0.30, 0.62, wave) * dw
    spots = ss(0.60, 0.74, fbm(q * 0.16 + 9, 23, 2)) * dw
    if var == 'natural':
        col = lerp(col, pal['stripe'] * cell[:, None], bands * 0.55)
        col = lerp(col, pal['spot'], spots * 0.75)
    elif var == 'desert':
        col = lerp(col, pal['stripe'] * cell[:, None], bands * 0.70)
        col = lerp(col, pal['mid'] * 1.12, spots * 0.35)
    elif var == 'electric':
        col = lerp(col, pal['stripe'] * cell[:, None], bands * 0.6)
        zig = np.abs(np.sin(P[:, 2] * 0.55 + np.sin(P[:, 1] * 0.5) * 1.6))
        line = ss(0.10, 0.0, zig - 0.06) * dw * ss(0.45, 0.6, fbm(q * 0.1 + 3, 41, 2))
        col = lerp(col, pal['glow'], line * 0.85); gl += pal['glow'] * (line * 0.9)[:, None]
    elif var == 'magma':
        col = lerp(col, pal['stripe'], bands * 0.4)
        crack = ss(0.16, 0.0, d2 - d1) * ss(0.40, 0.58, fbm(q * 0.09 + 5, 17, 2)) * (0.45 + 0.55 * dw)
        col = lerp(col, lerp(C(200, 50, 18), pal['glow'], fbm(q * 0.4, 8, 2)), crack)
        gl += pal['glow'] * (crack * 0.95)[:, None]
    elif var == 'spectral':
        col = lerp(col, pal['stripe'], bands * 0.5)
        vein = ss(0.12, 0.0, d2 - d1) * ss(0.38, 0.55, fbm(q * 0.08 + 2, 29, 2)) * (0.5 + 0.5 * dw)
        col = lerp(col, lerp(C(40, 190, 120), C(170, 255, 214), fbm(q * 0.3, 4, 2)), vein)
        gl += pal['glow'] * (vein * 0.9)[:, None]
    col = lerp(col, pal['belly'] * (0.92 + 0.12 * cell)[:, None], b * (0.92 if var in ('natural', 'desert') else 0.78))
    return col, gl

def crest_tex(var, ly, ctx):
    pal = PAL[var]; k = ctx['opt'].get('k', 0); g = np.clip((k + ly) / 3.0, 0, 1)
    ridge = np.where((np.floor(ly * 12) % 3) == 0, 0.9, 1.0)
    col = lerp(pal['crest_a'], pal['crest_b'], g ** 1.3) * ridge[:, None]; gl = np.zeros((len(ly), 3))
    if pal['glow'] is not None: t = ss(0.35, 1.0, g) * 0.8; gl = pal['glow'] * t[:, None]; col = lerp(col, pal['crest_b'], t * 0.5)
    return col, gl

def frill_tex(var, fu, fv, ly, ctx):
    pal = PAL[var]; k = ctx['opt'].get('k', 0); g = np.clip((k + ly) / 3.0, 0, 1)
    base = lerp(pal['frill_a'], pal['frill_b'], g ** 1.2); base = lerp(base, pal['frill_c'], ss(0.72, 1.0, g) * 0.8)
    # costillas: lineas radiales en el centro y en los bordes de cada tramo
    rib = np.maximum(ss(0.12, 0.0, np.abs(fu - 0.5)), np.maximum(ss(0.10, 0.0, fu), ss(0.90, 1.0, fu)) * 0.6)
    col = lerp(base, pal['rib'], rib * 0.72)
    # marcas propias de cada variante
    gl = np.zeros_like(col)
    pat = (np.floor(ly * 5 + k * 5) % 4) == 1
    if var == 'natural': col = lerp(col, pal['frill_c'], (pat & (rib < 0.2)) * 0.45)
    if var == 'desert': col = lerp(col, pal['rib'], (pat & (rib < 0.2)) * 0.5)
    if pal['glow'] is not None:
        line = rib * ss(0.15, 0.55, g)
        if var == 'magma': line = np.maximum(line * 0.7, ss(0.55, 1.0, g) * ss(0.12, 0.0, np.abs(fu - 0.5)) * 0.9)
        col = lerp(col, pal['glow'], line * 0.7); gl = pal['glow'] * (line * 0.85)[:, None]
    return col, gl

def eye_tex(var, loc, ctx, col, gl):
    pal = PAL[var]; r = {'chibi': 3.6, 'adult': 3.1}[ctx['stage']]; sx, sy, sz = ctx['sv']
    cz, cy = 7.5 * sz, 0.8 * sy
    dz = loc[:, 2] - cz; dy = loc[:, 1] - cy; e2 = (dz / r) ** 2 + (dy / r) ** 2
    m = e2 <= 1.0; ring = m & (e2 > 0.58); iris = m & ~ring
    out = col.copy(); g = gl.copy()
    out[m] = pal['pupil']; out[iris] = pal['eye']
    pup = iris & (np.abs(dz) < 0.6) & (np.abs(dy) < r * 0.78); out[pup] = pal['pupil']
    glint = iris & ((dz - 1.0) ** 2 + (dy - 1.1) ** 2 < 0.8); out[glint] = out[glint] * 0.35 + np.array([255, 255, 240]) * 0.65
    if var in ('electric', 'magma', 'spectral') or ctx['stage'] == 'adult': g[iris & ~pup] = pal['eye'] * (1.0 if var != 'natural' and var != 'desert' else 0.55); g[glint] = (255, 255, 240)
    return out, g

def make_atlas(var):
    out, H = drig.get_all()
    fr = {st: frames(out[st][0]) for st in out}
    atlas = np.zeros((H, W, 4), np.uint8); glow = np.zeros((H, W, 4), np.uint8); pal = PAL[var]
    for st, (spec, cubes) in out.items():
        qs = {'adult': 1.0, 'chibi': 0.6}[st]
        for c in cubes:
            R, T = fr[st][c['part']]
            for face, r in crig.regions(c).items():
                rect, loc, nl, ii, jj, fu, fv = region_geom(c, face, r)
                P = loc @ R.T + T; Nw = nl @ R.T
                ly = np.clip((loc[:, 1] - c['y0']) / max(1e-6, c['h']), 0.0, 1.0)
                ctx = dict(qs=qs, opt=c['opt'], grp=c['grp'], part=c['part'], role=c['role'], sv=c['sv'], stage=st)
                role = c['role']; op = c['opt']
                if role in ('skin', 'belly'): col, gl = skin(var, P, Nw, ly, ctx, face)
                elif role == 'crest': col, gl = crest_tex(var, ly, ctx)
                elif role == 'frill':
                    flen = np.clip((loc[:, 1] - c['y0']) / max(1e-6, c['h']), 0, 1)
                    col, gl = frill_tex(var, np.clip((loc[:, 0] - c['x0']) / max(1e-6, c['w']), 0, 1), fv, flen, ctx)
                elif role == 'spike': col = lerp(pal['crest_a'], pal['crest_b'], ly ** 1.2); gl = np.zeros_like(col)
                elif role == 'claw':
                    t = np.clip((loc[:, 2] - c['z0']) / max(1e-6, c['d']), 0, 1) if not c['part'].startswith('arm2') else 1 - (loc[:, 1] - c['y0']) / max(1e-6, c['h'])
                    col = lerp(pal['claw_a'], pal['claw_b'], np.clip(t, 0, 1) ** 1.5); gl = np.zeros_like(col)
                elif role == 'tooth':
                    t = np.clip((loc[:, 1] - c['y0']) / max(1e-6, c['h']), 0, 1)
                    tip = (1 - t) if op.get('upper') else t
                    col = lerp(C(*pal['tooth'] * 0.8), pal['tooth'], tip[:, None] if tip.ndim == 1 else tip); gl = np.zeros_like(col)
                    if op.get('upper') and face == 'down': col = np.tile(pal['mouth'] * 0.9, (len(ii), 1))
                    if op.get('lower') and face == 'up': col = np.tile(pal['mouth'] * 0.8, (len(ii), 1))
                else:   # lengua
                    col = np.tile(pal['tongue'], (len(ii), 1)) * (0.92 + 0.08 * np.cos(loc[:, 0] * 1.1))[:, None]; gl = np.zeros_like(col)
                if role in ('skin', 'belly') and ((op.get('head') and face == 'down') or (op.get('jaw') and face == 'up')): col = np.tile(pal['mouth'], (len(ii), 1)) * (0.9 if op.get('jaw') else 1.0)
                if c['part'] == 'head' and c['i'] == 0 and face in ('xmin', 'xmax'): col, gl = eye_tex(var, loc, ctx, col, gl)
                atlas[jj, ii, :3] = np.clip(col, 0, 255).astype(np.uint8); atlas[jj, ii, 3] = 255
                if gl.any():
                    lit = gl.sum(axis=1) > 6
                    glow[jj[lit], ii[lit], :3] = np.clip(gl[lit], 0, 255).astype(np.uint8); glow[jj[lit], ii[lit], 3] = 255
    return atlas, glow
