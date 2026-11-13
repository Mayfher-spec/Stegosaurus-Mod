"""Silla ligera y armaduras del Dilophosaurus: texturas transparentes que solo traen las piezas (misma UV que la malla de la piel).
Reutiliza los pintores de tools/carnotaurus/paint.py (saddle_tex / armor_tex / APAL)."""
import numpy as np, drig
from drig import crig
import dpaint
cp = dpaint.cp

def _gear(roles, painter):
    out, H = drig.get_all(); atlas = np.zeros((H, dpaint.W, 4), np.uint8); glow = np.zeros((H, dpaint.W, 4), np.uint8)
    for c in out['adult'][1]:
        if c['role'] not in roles: continue
        for face, r in crig.regions(c).items():
            rect, loc, nl, ii, jj, fu, fv = cp.region_geom(c, face, r)
            res = painter(c, face, loc, ii, jj, fu, fv)
            col, g, m = res
            atlas[jj[m], ii[m], :3] = np.clip(col[m], 0, 255).astype(np.uint8); atlas[jj[m], ii[m], 3] = 255
            lit = m & (g > 0.02)
            if lit.any():
                gv = np.clip(g[lit] * 255, 0, 255).astype(np.uint8)
                glow[jj[lit], ii[lit], 0] = gv; glow[jj[lit], ii[lit], 1] = gv; glow[jj[lit], ii[lit], 2] = gv; glow[jj[lit], ii[lit], 3] = 255
    return atlas, glow

def make_saddle():
    def p(c, face, loc, ii, jj, fu, fv):
        col, m = cp.saddle_tex(c, face, loc, ii, jj, fu, fv)
        return col, np.zeros(len(ii)), m
    return _gear(drig.SADDLE_ROLES, p)[0]

def make_armor(tier):
    return _gear(drig.ARMOR_ROLES, lambda c, f, l, i, j, u, v: cp.armor_tex(tier, c, f, l, i, j, u, v))

def gear_mask():
    """texels de la malla ocupados por silla/armadura (en la piel deben quedar transparentes)."""
    out, H = drig.get_all(); m = np.zeros((H, dpaint.W), bool)
    for c in out['adult'][1]:
        if c['role'] in drig.GEAR_ROLES:
            for face, r in crig.regions(c).items():
                x0, y0, x1, y1 = crig.prect(r); m[y0:y1, x0:x1] = True
    return m
