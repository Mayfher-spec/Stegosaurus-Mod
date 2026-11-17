"""Exporta el Compy: CompyLayers.java + 16 texturas (8 variantes, abiertas y con los ojos cerrados) + hoja de vistas previas.
Uso: python3 mexport.py <repo>"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import mlayers, mpaint
from PIL import Image
repo = sys.argv[1] if len(sys.argv) > 1 else '.'
tex = os.path.join(repo, 'src/main/resources/assets/prodigycreatures/textures/entity')
os.makedirs(tex, exist_ok=True)
src, hy, H = mlayers.layers_java()
open(os.path.join(repo, 'src/main/java/com/prodigytown/creatures/client/CompyLayers.java'), 'w').write(src)
for var in mpaint.VARIANTS:
    for closed in (False, True):
        a, _ = mpaint.make_atlas(var, closed=closed)
        a = mpaint.cp.dilate(a)
        Image.fromarray(a).save(os.path.join(tex, 'compy_%s%s.png' % (var, '_closed' if closed else '')), optimize=True)
print('H', H, 'hy', hy)
