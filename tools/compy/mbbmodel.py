"""Exporta el Compy como modelo editable de Blockbench (compy.bbmodel, formato "Modded Entity" con UV por caja):
la MISMA jerarquia de huesos, cubos y UV que va al juego, las 8 texturas incrustadas y animaciones de muestra (idle, walk, bite, sit, sleep).
Las animaciones del juego son procedimentales (CompyModel.setupAnim); las de este archivo son una version por fotogramas para editar y previsualizar.
Convencion: BB = (x, y, -z) del marco de diseno (igual que el Java generado); rotaciones de hueso BB = (-rx, +ry, -rz) en grados, orden ZYX.
Uso: python3 mbbmodel.py <salida.bbmodel>"""
import base64, io, json, math, os, sys, uuid
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import mrig, mpaint, mlayers
from mrig import crig
from PIL import Image

NS = uuid.UUID('6f1c1c0e-0a0e-4a8e-9a7e-0c0a0c0a0c0a')
def uid(*k): return str(uuid.uuid5(NS, '/'.join(map(str, k))))
def r3(v): return [round(float(x), 4) for x in v]

def build():
    (spec, cubes), H = mrig.get_all(); hy = mlayers.fit_hy()
    byn = {p['name']: p for p in spec}
    org = {}
    for p in spec:                       # origen absoluto sin girar (BB): suma de desplazamientos de pivote
        px, py, pz = p['piv']
        off = np.array([px, py, -pz])
        if p['parent'] == 'root': org[p['name']] = np.array([0.0, hy, 0.0]) if p['name'] == 'body' else off
        else: org[p['name']] = org[p['parent']] + off
    uvs = {(c['part'], c['i']): c['uv'] for c in cubes}
    elements = []; groups = {}
    for p in spec:
        deg = [-math.degrees(p['rot'][0]), math.degrees(p['rot'][1]), -math.degrees(p['rot'][2])]
        groups[p['name']] = dict(name=p['name'], origin=r3(org[p['name']]), rotation=r3(deg), color=0, uuid=uid('g', p['name']), export=True, isOpen=True, visibility=True, autouv=0, children=[])
        for i, c in enumerate(p['cubes']):
            x0, y0, z0, w, h, d = c[:6]
            fr = org[p['name']] + np.array([x0, y0, -(z0 + d)]); to = fr + np.array([w, h, d])
            u, v = uvs[(p['name'], i)]
            f = lambda a, b, c2, d2: dict(uv=[round(a, 4), round(b, 4), round(c2, 4), round(d2, 4)], texture=0)
            faces = dict(north=f(u + d, v + d, u + d + w, v + d + h), east=f(u, v + d, u + d, v + d + h), south=f(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
                         west=f(u + d + w, v + d, u + 2 * d + w, v + d + h), up=f(u + d + w, v + d, u + d, v), down=f(u + d + 2 * w, v, u + d + w, v + d))
            el = dict(name='%s_%d_%s' % (p['name'], i, c[6]), box_uv=True, rescale=False, locked=False, light_emission=0, render_order='default', allow_mirror_modeling=True,
                      from_=r3(fr), to=r3(to), autouv=0, color=0, origin=r3(org[p['name']]), uv_offset=[u, v], faces=faces, type='cube', uuid=uid('e', p['name'], i))
            el['from'] = el.pop('from_')
            elements.append(el); groups[p['name']]['children'].append(el['uuid'])
    for p in spec:
        if p['parent'] != 'root': groups[p['parent']]['children'].append(groups[p['name']])
    outliner = [groups['body']]
    return spec, elements, outliner, groups, H

def kf(channel, t, x, y, z, key):
    return dict(channel=channel, data_points=[dict(x=round(x, 3), y=round(y, 3), z=round(z, 3))], uuid=uid('k', key, channel, t), time=round(t, 4), color=-1, interpolation='linear')

def animations(groups):
    """animaciones de muestra: giros ADITIVOS por hueso, dados como (rx, ry, rz) del marco de diseno (se convierten a la convencion BB)."""
    S = math.sin; D = math.degrees
    def bbrot(rx, ry, rz): return (-D(rx), D(ry), -D(rz))
    anims = []
    def make(name, length, loop, fn, steps, hold=False):
        an = {}
        for k in range(steps + (0 if loop == 'loop' else 1)):
            t = length * k / steps
            for bone, rot in fn(2 * math.pi * k / steps, k / max(1, steps)).items():
                x, y, z = bbrot(*rot)
                an.setdefault(bone, []).append(kf('rotation', t, x, y, z, name + bone))
        anims.append(dict(uuid=uid('a', name), name=name, loop=loop, override=False, length=length, snapping=24, selected=False, anim_time_update='', blend_weight='', start_delay='', loop_delay='',
                          animators={groups[b]['uuid']: dict(name=b, type='bone', keyframes=kfs) for b, kfs in an.items()}))
    def idle(a, k):
        P = {'chest': (0.025 * S(a), 0, 0), 'head': (0, 0.35 * S(a), 0.15 * max(0, S(2 * a))), 'neck2': (0, 0.15 * S(a), 0)}
        for i in range(6): P['tail%d' % (i + 1)] = (0, 0.05 * (1 + 0.25 * i) * S(a - 0.5 * i), 0)
        return P
    def walk(a, k):
        P = {'body': (0.05, 0.05 * S(a), 0.03 * S(a)), 'neck2': (-0.07 * S(2 * a), 0, 0), 'head': (0.05 * S(2 * a + 0.6), 0, 0)}
        for s, ph in (('l', 0), ('r', math.pi)):
            p = a + ph; lift = max(0, -math.cos(p))
            th = 0.5 * S(p); sh = 0.7 * lift; me = -0.4 * lift
            P['thigh_' + s] = (th, 0, 0); P['shin_' + s] = (sh, 0, 0); P['meta_' + s] = (me, 0, 0); P['foot_' + s] = (-(th + sh + me), 0, 0)
            P['arm_' + s] = (0.2 * S(p + math.pi), 0, 0)
        for i in range(6): P['tail%d' % (i + 1)] = (0, 0.12 * (1 + 0.25 * i) * S(a - 0.6 * i), 0)
        return P
    def bite(a, k):
        wind = max(0, S(min(k, 0.4) / 0.4 * math.pi)) if k < 0.4 else 0
        strike = max(0, S((k - 0.18) / 0.82 * math.pi)) if k > 0.18 else 0
        return {'neck1': (-0.25 * wind + 0.45 * strike, 0.12 * wind, 0), 'neck2': (0.1 * wind - 0.14 * strike, 0, 0), 'head': (-0.28 * wind + 0.2 * strike, 0, 0),
                'jaw': (0.95 * max(0, S(min(k, 0.7) / 0.7 * math.pi)) , 0, 0), 'body': (0.12 * strike, 0, 0)}
    def hold(pose): return lambda a, k: pose
    sit = {'body': (0.12, 0, 0), 'neck1': (-0.2, 0, 0), 'neck2': (0.3, 0, 0), 'head': (0.1, 0, 0), 'arm_l': (-0.5, 0, 0), 'arm_r': (-0.5, 0, 0)}
    sleep = {'neck1': (0.85, -0.55, 0), 'neck2': (-0.35, 0, 0), 'head': (0.35, 0, 0), 'arm_l': (-0.4, 0, 0), 'arm_r': (-0.4, 0, 0), 'arm2_l': (0.6, 0, 0), 'arm2_r': (0.6, 0, 0)}
    for s in 'lr':
        sit.update({'thigh_' + s: (-1.17, 0, 0), 'shin_' + s: (0.62, 0, 0), 'meta_' + s: (-0.90, 0, 0), 'foot_' + s: (1.45, 0, 0)})
        sleep.update({'thigh_' + s: (-1.35, 0, 0), 'shin_' + s: (0.95, 0, 0), 'meta_' + s: (-1.25, 0, 0), 'foot_' + s: (1.70, 0, 0)})
    for i in range(6): sleep['tail%d' % (i + 1)] = (0.05, 0.35, 0)
    make('idle', 3.0, 'loop', idle, 12); make('walk', 0.8, 'loop', walk, 8); make('bite', 0.5, 'once', bite, 10)
    make('sit', 0.05, 'hold', hold(sit), 1); make('sleep', 0.05, 'hold', hold(sleep), 1)
    return anims

def main(out):
    spec, elements, outliner, groups, H = build()
    textures = []
    for i, var in enumerate(mpaint.VARIANTS):
        a, _ = mpaint.make_atlas(var); a = mpaint.cp.dilate(a)
        buf = io.BytesIO(); Image.fromarray(a).save(buf, 'PNG')
        textures.append(dict(path='', name='compy_%s.png' % var, folder='', namespace='', id=str(i), particle=False, render_mode='normal', render_sides='auto', frame_time=1, frame_order_type='loop', frame_order='', frame_interpolate=False,
                             visible=True, internal=True, saved=False, uuid=uid('t', var), relative_path='', source='data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()))
    doc = dict(meta=dict(format_version='4.5', creation_time=0, model_format='modded_entity', box_uv=True), name='compy', model_identifier='compy', visible_box=[1, 1, 0], variable_placeholders='', variable_placeholder_buttons=[],
               timeline_setups=[], unhandled_root_fields={}, resolution=dict(width=crig.ATLAS, height=H), elements=elements, outliner=outliner, textures=textures, animations=animations(groups))
    json.dump(doc, open(out, 'w'))
    print('bbmodel', out, 'elementos', len(elements), 'huesos', len(groups), 'texturas', len(textures), 'animaciones', len(doc['animations']))

if __name__ == '__main__': main(sys.argv[1] if len(sys.argv) > 1 else 'compy.bbmodel')
