"""Exporta el Dilophosaurus: DilophosaurusLayers.java + texturas (5 variantes + brillo). Uso: python3 dexport.py <repo>"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import dlayers, dpaint
from PIL import Image
repo = sys.argv[1] if len(sys.argv) > 1 else '.'
tex = os.path.join(repo, 'src/main/resources/assets/prodigycreatures/textures/entity')
os.makedirs(tex, exist_ok=True)
src, hy, H = dlayers.layers_java()
open(os.path.join(repo, 'src/main/java/com/prodigytown/creatures/client/DilophosaurusLayers.java'), 'w').write(src)
for var in dpaint.VARIANTS:
    a, g = dpaint.make_atlas(var)
    a = dpaint.cp.dilate(a)
    Image.fromarray(a).save(os.path.join(tex, 'dilophosaurus_%s.png' % var), optimize=True)
    Image.fromarray(g).save(os.path.join(tex, 'dilophosaurus_%s_glow.png' % var), optimize=True)
print('H', H, 'hy', hy)
