"""Compy (Compsognathus): esqueleto articulado propio, UNA sola geometria (adulto) y 8 texturas.
Reutiliza el empaquetado UV y la exportacion a Java del carnotauro (tools/carnotaurus/rig.py), igual que el Dilophosaurus.
Marco de diseno: x lateral (+x = izquierda del animal), y arriba, z hacia delante. 1 unidad = 1 texel."""
import math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'carnotaurus'))
import rig as crig
crig.ATLAS = 256   # atlas de 256 px de ancho (el Compy es diminuto)

BODY_Y = 24.0
TAIL = ((9, 6, 6), (8, 5, 5), (8, 4, 5), (8, 4, 4), (7, 3, 4), (7, 3, 3))   # (largo, ancho, alto) de cada tramo de la cola

def build_spec():
    P = []
    def part(name, parent, piv, rot=(0, 0, 0), cubes=(), grp='body'):
        P.append(dict(name=name, parent=parent, piv=piv, rot=rot, cubes=list(cubes), grp=grp))
    # ---- cuerpo horizontal y compacto, con una hilera discreta de puas sobre el lomo
    part('body', 'root', (0, BODY_Y, 0), (0.0, 0, 0), [
        (-5, -5.5, -10, 10, 11, 20, 'skin', dict(bf=1)),
        (-3.8, -6.7, -8, 7.6, 1.6, 15, 'belly', dict()),
        (-0.6, 4.8, -7, 1.2, 1.6, 2.4, 'spike', dict()), (-0.6, 4.8, -2, 1.2, 1.8, 2.4, 'spike', dict()), (-0.6, 4.8, 3, 1.2, 1.6, 2.4, 'spike', dict())])
    for i, (L, w, h) in enumerate(TAIL):
        piv = (0, 1.5, -9.5) if i == 0 else (0, 0, -TAIL[i - 1][0] + 1)
        cubes = [(-w / 2.0, -h / 2.0, -L, w, h, L, 'skin', dict(bf=1, ti=i))]
        if i < 4: cubes.append((-0.5, h / 2.0 - 0.2, -L * 0.62, 1.0, 1.4, 2.2, 'spike', dict()))
        part('tail%d' % (i + 1), 'body' if i == 0 else 'tail%d' % i, piv, (0.0, 0, 0), cubes, 'tail')
    part('chest', 'body', (0, 0.8, 8), (0, 0, 0), [
        (-5, -5.3, -2, 10, 10.8, 10, 'skin', dict(bf=1)),
        (-3.6, -6.5, 0, 7.2, 1.4, 7, 'belly', dict()),
        (-0.6, 4.8, 1, 1.2, 1.6, 2.4, 'spike', dict()), (-0.6, 4.8, 6, 1.2, 1.4, 2.2, 'spike', dict())], 'body')
    part('neck1', 'chest', (0, 3.8, 7.5), (-0.90, 0, 0), [
        (-3.0, -2.7, 0, 6.0, 5.6, 9.5, 'skin', dict(bf=1)),
        (-2.3, -3.1, 1, 4.6, 1.6, 7.5, 'belly', dict())], 'neck')
    part('neck2', 'neck1', (0, 0.4, 9), (0.62, 0, 0), [
        (-2.6, -2.5, 0, 5.2, 5.0, 8.5, 'skin', dict(bf=1)),
        (-2.0, -2.9, 0.8, 4.0, 1.6, 7.0, 'belly', dict())], 'neck')
    part('head', 'neck2', (0, 0, 8), (0.42, 0, 0), [
        (-3, -2.4, -2, 6, 5, 8, 'skin', dict(head=1)),                  # craneo
        (-2.1, -1.8, 6, 4.2, 3.6, 5, 'skin', dict(head=1, snout=1)),    # hocico
        (-1.6, -1.4, 11, 3.2, 2.8, 3, 'skin', dict(head=1, snout=1)),   # punta del hocico
        (-3.3, 2.2, 0, 6.6, 1.3, 5.5, 'skin', dict(brow=1)),            # arco de las cejas
        (-1.95, -3.5, 6.5, 3.9, 1.1, 8.0, 'tooth', dict(upper=1)),      # dientes superiores
        (-1.2, 1.3, 12.0, 2.4, 1.0, 2.2, 'skin', dict(nostril=1))], 'head')
    part('jaw', 'head', (0, -2.4, 0.4), (0, 0, 0), [
        (-1.5, -1.9, -2.0, 3.0, 1.9, 15.4, 'belly', dict(jaw=1)),
        (-1.3, 0, 5.0, 2.6, 0.9, 9.0, 'tooth', dict(lower=1)),
        (-0.8, 0, 3.0, 1.6, 0.7, 8.0, 'mouth', dict())], 'head')
    for sgn, n in ((1, 'l'), (-1, 'r')):
        part('arm_' + n, 'chest', (sgn * 4.8, -2.6, 6.5), (0.45, 0, -sgn * 0.08), [(-1.2, -4.6, -1.2, 2.4, 5.0, 2.4, 'skin', dict())], 'arm')
        part('arm2_' + n, 'arm_' + n, (0, -4.0, 0), (0.8, 0, 0), [
            (-1.0, -4.2, -1.0, 2.0, 4.6, 2.0, 'skin', dict()),
            (-1.15, -6.6, -0.6, 0.7, 2.6, 1.2, 'claw', dict()), (-0.35, -7.0, -0.6, 0.7, 3.0, 1.2, 'claw', dict()), (0.45, -6.6, -0.6, 0.7, 2.6, 1.2, 'claw', dict())], 'arm')
        part('thigh_' + n, 'body', (sgn * 4.6, -2.8, -5), (-0.40, 0, 0), [(-2.7, -7.8, -3.5, 5.4, 9.2, 7, 'skin', dict(leg=1))], 'leg')
        part('shin_' + n, 'thigh_' + n, (0, -7.2, 1), (0.95, 0, 0), [(-1.9, -8.2, -1.9, 3.8, 8.8, 3.8, 'skin', dict(leg=1))], 'leg')
        part('meta_' + n, 'shin_' + n, (0, -7.6, -0.8), (-0.65, 0, 0), [(-1.5, -7.0, -1.5, 3.0, 7.8, 3.0, 'skin', dict(leg=1))], 'leg')
        part('foot_' + n, 'meta_' + n, (0, -6.4, 0), (0.12, 0, 0), [(-2.0, -2.0, -3.2, 4.0, 2.0, 6.4, 'skin', dict(leg=1, foot=1))], 'leg')
        for k, (tx, yaw) in enumerate(((0, 0), (1.5, -0.24), (-1.5, 0.24))):
            part('toe%d_%s' % (k, n), 'foot_' + n, (sgn * tx, -1, 3.2), (0, sgn * yaw, 0),
                 [(-0.9, -1.0, 0, 1.8, 2.0, 4.0, 'skin', dict(toe=1)), (-0.6, -0.7, 4.0, 1.2, 1.5, 2.2, 'claw', dict())], 'leg')
    return P

STAGE = dict(body=1.0, tail=1.0, neck=1.0, head=1.0, arm=1.0, leg=1.0)
def _v3(s): return tuple(s) if isinstance(s, (tuple, list)) else (s, s, s)
def scaled():
    spec = build_spec(); byn = {p['name']: p for p in spec}
    for p in spec: p['sv'] = _v3(STAGE[p['grp']]); p['s'] = p['sv'][1]
    for p in spec:
        par = byn.get(p['parent'])
        if par: p['piv'] = tuple(q * k for q, k in zip(p['piv'], par['sv']))
        sx, sy, sz = p['sv']
        p['cubes'] = [(c[0] * sx, c[1] * sy, c[2] * sz, c[3] * sx, c[4] * sy, c[5] * sz, c[6], c[7]) for c in p['cubes']]
    return spec

_ALL = None
def get_all():
    global _ALL
    if _ALL is not None: return _ALL
    spec = scaled(); cubes = []
    for p in spec:
        for i, c in enumerate(p['cubes']):
            cubes.append(dict(part=p['name'], grp=p['grp'], s=p['s'], sv=p['sv'], i=i, x0=c[0], y0=c[1], z0=c[2], w=c[3], h=c[4], d=c[5], role=c[6], opt=c[7], stage='adult'))
    used = crig.pack(cubes)
    H = int(math.ceil(used / 64.0) * 64)
    _ALL = ((spec, cubes), H); return _ALL
