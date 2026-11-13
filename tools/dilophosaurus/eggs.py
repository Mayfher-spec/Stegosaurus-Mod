"""Huevos de dilofosaurio: 5 variantes x 4 etapas (64x64, mismo modelo que el huevo de estegosaurio) + icono 16x16.
Reutiliza las utilidades de tools/carnotaurus/eggs.py. Uso: python3 eggs.py [carpeta textures]."""
import os, random, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'carnotaurus'))
import numpy as np
from PIL import Image
import eggs as CE

# base clara, base oscura, mota A, mota B, acento (abanico)
PAL = {
 'natural':  [(112,148,74),(84,116,56),(226,218,168),(70,52,34),(214,90,50)],
 'electric': [(80,150,214),(56,114,176),(200,236,255),(30,56,96),(90,230,255)],
 'magma':    [(150,46,34),(104,30,26),(240,140,60),(52,18,18),(255,190,70)],
 'desert':   [(206,180,128),(176,150,102),(238,222,176),(138,100,60),(214,120,50)],
 'spectral': [(96,196,150),(62,150,112),(216,255,236),(30,84,70),(120,255,200)],
}
NAMES = list(PAL)
def base_img(name):
    rnd = random.Random(sum(map(ord, name)) * 17 + 3); p = PAL[name]
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    for (w, h, d), (u, v) in CE.CUBES:
        for fname, (x, y, fw, fh) in CE.faces(u, v, w, h, d).items():
            for j in range(fh):
                for i in range(fw):
                    r = rnd.random(); gx, gy = x + i, y + j
                    c = p[0] if r < .6 else p[1]
                    blob = ((gx // 2) * 5 + (gy // 2) * 11 + len(name) * 3) % 9
                    if blob in (0, 1) and r < .75: c = p[2]            # motas claras
                    elif blob in (4, 5) and r < .6: c = p[3]           # motas oscuras
                    img.putpixel((gx, gy), c + (255,))
    # abanico (membrana): cinco rayitas que salen de un punto, en la cara frontal del cubo grande
    (w, h, d), (u, v) = CE.CUBES[2]; fx, fy, fw, fh = CE.faces(u, v, w, h, d)['front']
    for k, ox in enumerate((1, 3, 5, 7, 9)):
        for j in range(4):
            x = fx + 5 + (ox - 5) * (j + 1) // 4; y = fy + 4 - j
            if fx <= x < fx + fw and fy <= y < fy + fh: img.putpixel((x, y), p[4] + (255,))
    return img

def icon(name):
    p = PAL[name]; im = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            if not CE.mask[y, x]: continue
            t = (x - 8) * 0.5 + (y - 8) * 0.5
            c = p[1] if t > 2.2 else p[0]
            if (x, y) in [(6, 3), (5, 4), (6, 4), (5, 5)]: c = tuple(min(255, int(v * 1.25 + 25)) for v in p[0])
            if (x, y) in CE.SPOTS: c = p[2]
            if (x, y) in [(6, 7), (8, 6), (10, 7), (7, 8), (9, 8)]: c = p[4]
            if CE.edge[y, x]: c = tuple(int(v * 0.62) for v in p[1])
            im[y, x] = (*c, 255)
    return Image.fromarray(im)

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else CE.ROOT
    for name in NAMES:
        b = base_img(name); b.save(out + 'entity/dilophosaurus_egg_%s_0.png' % name)
        for st in (1, 2, 3):
            o = b.copy(); o.alpha_composite(CE.cracks(st, 'x' if name in ('magma',) else 'natural')); o.save(out + 'entity/dilophosaurus_egg_%s_%d.png' % (name, st))
        icon(name).save(out + 'item/dilophosaurus_egg_%s.png' % name)
        print(name)
