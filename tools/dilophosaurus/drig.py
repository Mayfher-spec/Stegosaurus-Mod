"""Dilophosaurus: esqueleto articulado propio (cria + adulto), doble cresta y membrana de 7 petalos articulados.
Reutiliza el empaquetado UV y la exportacion a Java del carnotauro (tools/carnotaurus/rig.py). Marco de diseno: x lateral, y arriba, z hacia delante."""
import math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'carnotaurus'))
import rig as crig

FRILL_ANGLES = (-135, -90, -45, 0, 45, 90, 135)   # grados desde "arriba" hacia +x; el hueco inferior queda libre (pecho)
FRILL_R0 = 5.6                                      # radio del cuello donde nacen los petalos

def build_spec(stage='adult'):
    P = []
    def part(name, parent, piv, rot=(0, 0, 0), cubes=(), grp='body'):
        P.append(dict(name=name, parent=parent, piv=piv, rot=rot, cubes=list(cubes), grp=grp))
    part('body', 'root', (0, 66, 0), (0.05, 0, 0), [
        (-10, -12, -22, 20, 24, 42, 'skin', dict(bf=1)),
        (-8, -14, -20, 16, 3, 34, 'belly', dict()),
        (-1.2, 11.6, -10, 2.4, 2.2, 3, 'spike', dict()), (-1.2, 11.6, -2, 2.4, 2.2, 3, 'spike', dict()), (-1.2, 11.6, 6, 2.4, 2.2, 3, 'spike', dict())])
    for i, (L, w, z0) in enumerate(((24, 12, -22), (22, 9.6, -22), (21, 7.2, -21), (19, 5.2, -19), (17, 3.6, -17))):
        piv = (0, 3, -22) if i == 0 else (0, -0.5 if i == 1 else 0, -[0, 22, 20, 19, 17][i])
        cubes = [(-w / 2, -w / 2, -L, w, w, L, 'skin', dict(bf=1))]
        if i < 3: cubes.append((-0.9, w / 2 - 0.2, -L * 0.6, 1.8, 2.0, 2.4, 'spike', dict()))
        part('tail%d' % (i + 1), 'body' if i == 0 else 'tail%d' % i, piv, (-0.04, 0, 0), cubes, 'tail')
    part('chest', 'body', (0, 2, 18), (0, 0, 0), [
        (-10.5, -11, -6, 21, 23, 22, 'skin', dict(bf=1)),
        (-8, -13, -4, 16, 3, 16, 'belly', dict())], 'body')
    part('neck1', 'chest', (0, 8, 15), (-1.15, 0, 0), [(-6, -5.5, 0, 12, 11.5, 22, 'skin', dict(bf=1))], 'neck')
    part('neck2', 'neck1', (0, 1, 20), (0.85, 0, 0), [(-5, -5, 0, 10, 10, 18, 'skin', dict(bf=1)), (-4, -5.4, 1, 8, 2, 14, 'belly', dict())], 'neck')
    part('head', 'neck2', (0, 0, 17), (0.20, 0, 0), [
        (-6, -5, -3, 12, 11, 15, 'skin', dict(head=1)),                 # craneo
        (-4.5, -3.5, 12, 9, 8.5, 11, 'skin', dict(head=1, snout=1)),    # hocico
        (-3.8, -3, 23, 7.6, 7, 6, 'skin', dict(head=1, snout=1)),       # punta del hocico
        (-6.4, 4.2, 3, 12.8, 2.2, 8, 'skin', dict(brow=1)),             # arco de las cejas
        (-4.3, -7.6, 12, 8.6, 2.6, 17, 'tooth', dict(upper=1)),         # dientes superiores
        (-3, 3.8, 26, 6, 2, 4, 'skin', dict(nostril=1))], 'head')
    part('jaw', 'head', (0, -5, 1), (0, 0, 0), [
        (-4, -4, -4, 8, 4, 27, 'belly', dict(jaw=1)),
        (-3.7, 0, 5, 7.4, 1.6, 17, 'tooth', dict(lower=1)),
        (-2.5, 0, 3, 5, 1.2, 15, 'mouth', dict())], 'head')
    for sgn, n in ((1, 'l'), (-1, 'r')):
        # DOBLE CRESTA: dos laminas finas y paralelas sobre el craneo (siempre visibles)
        part('crest_' + n, 'head', (sgn * 3.4, 6, 0), (0, 0, 0), [
            (-0.8, 0, -1, 1.6, 4.5, 8, 'crest', dict(k=0)),
            (-0.8, 0.6, 7, 1.6, 6.5, 10, 'crest', dict(k=1)),
            (-0.8, 0, 17, 1.6, 3.5, 7, 'crest', dict(k=2))], 'crest')
        part('arm_' + n, 'chest', (sgn * 10.5, -3, 12), (0.45, 0, -sgn * 0.08), [(-2.5, -10, -2.5, 5, 11, 5, 'skin', dict())], 'arm')
        part('arm2_' + n, 'arm_' + n, (0, -9, 0), (0.8, 0, 0), [
            (-2, -9, -2, 4, 10, 4, 'skin', dict()),
            (-2.3, -13, -1.3, 1.5, 4, 2.2, 'claw', dict()), (-0.8, -13.6, -1.3, 1.6, 4.6, 2.2, 'claw', dict()), (0.8, -13, -1.3, 1.5, 4, 2.2, 'claw', dict())], 'arm')
        part('thigh_' + n, 'body', (sgn * 8.5, -3, -10), (-0.40, 0, 0), [(-5.5, -22, -7, 11, 25, 14, 'skin', dict(leg=1))], 'leg')
        part('shin_' + n, 'thigh_' + n, (0, -21, 1), (0.95, 0, 0), [(-3.8, -22, -3.8, 7.6, 24, 7.6, 'skin', dict(leg=1))], 'leg')
        part('meta_' + n, 'shin_' + n, (0, -22, -1), (-0.65, 0, 0), [(-3, -19, -3, 6, 21, 6, 'skin', dict(leg=1))], 'leg')
        part('foot_' + n, 'meta_' + n, (0, -18, 0), (0.12, 0, 0), [(-4.5, -5, -7, 9, 5, 14, 'skin', dict(leg=1, foot=1))], 'leg')
        for k, (tx, yaw) in enumerate(((0, 0), (3.6, -0.26), (-3.6, 0.26))):
            part('toe%d_%s' % (k, n), 'foot_' + n, (sgn * tx, -2, 7), (0, sgn * yaw, 0),
                 [(-1.9, -2, 0, 3.8, 4, 9, 'skin', dict(toe=1)), (-1.3, -1.5, 9, 2.6, 3, 5.5, 'claw', dict())], 'leg')
    # MEMBRANA: 7 petalos; cada uno son dos huesos (fp: sitio en el cuello y direccion radial; fq: abanico/plegado) y 3 tramos que se ensanchan
    for k, a in enumerate(FRILL_ANGLES):
        th = math.radians(a)
        part('fp%d' % k, 'neck2', (FRILL_R0 * math.sin(th), FRILL_R0 * math.cos(th), 15), (0, 0, th), [], 'frill')
        part('fq%d' % k, 'fp%d' % k, (0, 0, 0), (-1.45, 0, 0), [
            (-2.3, 0, -0.5, 4.6, 5, 1.0, 'frill', dict(k=0, rib=k)),
            (-4.0, 5, -0.5, 8.0, 6, 1.0, 'frill', dict(k=1, rib=k)),
            (-6.5, 11, -0.5, 13.0, 7, 1.0, 'frill', dict(k=2, rib=k))], 'frill')
    return P

STAGES = {
    'chibi': dict(body=0.58, tail=0.52, neck=0.60, head=1.05, crest=0.75, arm=0.70, leg=0.58, frill=0.95),
    'adult': dict(body=1.0, tail=1.0, neck=1.0, head=1.14, crest=1.15, arm=1.0, leg=1.0, frill=1.75),
}
ROT_OVERRIDE = {}

def _v3(s): return tuple(s) if isinstance(s, (tuple, list)) else (s, s, s)
def scaled(stage):
    spec = build_spec(stage); sc = STAGES[stage]; byn = {p['name']: p for p in spec}
    for p in spec: p['sv'] = _v3(sc[p['grp']]); p['s'] = p['sv'][1]
    for nm, rot in ROT_OVERRIDE.get(stage, {}).items(): byn[nm]['rot'] = rot
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
    import math as m
    out = {}; allc = []
    for st in ('chibi', 'adult'):
        spec = scaled(st); cubes = []
        for p in spec:
            for i, c in enumerate(p['cubes']):
                cubes.append(dict(part=p['name'], grp=p['grp'], s=p['s'], sv=p['sv'], i=i, x0=c[0], y0=c[1], z0=c[2], w=c[3], h=c[4], d=c[5], role=c[6], opt=c[7], stage=st))
        out[st] = (spec, cubes); allc += cubes
    used = crig.pack(allc)
    H = int(m.ceil(used / 64.0) * 64)
    _ALL = (out, H); return _ALL
