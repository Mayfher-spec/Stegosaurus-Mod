"""Poses de comprobacion (las mismas cifras que CompyModel.java): sentado y dormido. Calcula cuanto baja el cuerpo para apoyar en el suelo."""
import numpy as np
import mrig, mpaint, mrender
cp = mpaint.cp
def pose(kind):
    P = {}
    if kind == 'sit':
        th, sh, me, fo = -1.17, 0.62, -0.90, 1.45
        P['body'] = (0.12, 0, 0); P['neck1'] = (-0.2, 0, 0); P['neck2'] = (0.3, 0, 0); P['head'] = (0.1, 0, 0)
        arm, arm2, ny, ty, tx = -0.5, 0.0, 0.0, 0.0, 0.0
    else:
        th, sh, me, fo = -1.35, 0.95, -1.25, 1.70
        P['body'] = (0.0, 0, 0); P['neck1'] = (0.85, -0.55, 0); P['neck2'] = (-0.35, 0, 0); P['head'] = (0.35, 0, 0)
        arm, arm2, ny, ty, tx = -0.4, 0.6, -0.55, 0.35, 0.05
    for s in 'lr':
        P['thigh_' + s] = (th, 0, 0); P['shin_' + s] = (sh, 0, 0); P['meta_' + s] = (me, 0, 0); P['foot_' + s] = (fo, 0, 0)
        P['arm_' + s] = (arm, 0, 0); P['arm2_' + s] = (arm2, 0, 0)
    for i in range(6): P['tail%d' % (i + 1)] = (tx, ty, 0)
    return P
def lowest(P):
    cubes, fr, H = mrender.posed(P); lo = 1e9
    for c in cubes:
        R, T = fr[c['part']]
        for x in (c['x0'], c['x0'] + c['w']):
            for y in (c['y0'], c['y0'] + c['h']):
                for z in (c['z0'], c['z0'] + c['d']): lo = min(lo, (T + R @ np.array([x, y, z]))[1])
    return lo
if __name__ == '__main__':
    import mlayers
    from PIL import Image
    lo0 = lowest(None)
    atlas, _ = mpaint.make_atlas('verde'); atlas = cp.dilate(atlas)
    ims = []
    for k in ('sit', 'sleep'):
        P = pose(k); lo = lowest(P)
        print(k, 'drop', round(lo - lo0, 2))
        for v in ('left', 'top'):
            ims.append(mrender.render(atlas, v, pose=P, bone_off={'body': (0, -(lo - lo0), 0)} if False else None)[0])
    W = sum(i.width for i in ims) + 10 * (len(ims) + 1); Hh = max(i.height for i in ims) + 20
    sh = Image.new('RGB', (W, Hh), (236, 232, 222)); x = 10
    for i in ims: sh.paste(i, (x, 10)); x += i.width + 10
    import sys; sh.save(sys.argv[1] if len(sys.argv) > 1 else 'poses.png')
