"""Vistas previas del Compy: 7 vistas ortogonales, las 8 variantes, y la escala junto a un jugador (1,8 bloques) y una gallina.
Uso: python3 mpreview.py <carpeta de salida>"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import mrig, mpaint, mrender
cp = mpaint.cp
out = sys.argv[1] if len(sys.argv) > 1 else 'preview'; os.makedirs(out, exist_ok=True)
BG = (236, 232, 222)
def font(n):
    try: return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', n)
    except Exception: return ImageFont.load_default()
def atlas(var): a, _ = mpaint.make_atlas(var); return cp.dilate(a)
def sheet(ims, labels, cols, pad=14, lab=22):
    cw = max(i.width for i in ims); ch = max(i.height for i in ims)
    rows = (len(ims) + cols - 1) // cols
    sh = Image.new('RGB', (cols * (cw + pad) + pad, rows * (ch + lab + pad) + pad), BG); d = ImageDraw.Draw(sh)
    for k, (im, lb) in enumerate(zip(ims, labels)):
        x = pad + (k % cols) * (cw + pad); y = pad + (k // cols) * (ch + lab + pad)
        sh.paste(im, (x + (cw - im.width) // 2, y + lab)); d.text((x + 4, y), lb, fill=(40, 40, 40), font=font(15))
    return sh
# 1) siete vistas del Compy verde (misma escala para todas)
a = atlas('verde'); names = {'left': 'Lateral izquierda', 'right': 'Lateral derecha', 'front': 'Frontal', 'back': 'Trasera', 'top': 'Superior', 'bottom': 'Inferior', 'iso': 'Isometrica 3/4'}
ims = [mrender.render(a, v, S=8)[0] for v in mrender.VIEWS]
sheet(ims, [names[v] for v in mrender.VIEWS], 2).save(os.path.join(out, 'compy_vistas.png'))
# 2) ocho variantes (lateral + 3/4)
ims = []; lbs = []
for v in mpaint.VARIANTS:
    aa = atlas(v); ims.append(mrender.render(aa, 'iso', S=9)[0]); lbs.append(v.capitalize())
sheet(ims, lbs, 4).save(os.path.join(out, 'compy_variantes.png'))
ims = [mrender.render(atlas(v), 'left', S=7)[0] for v in mpaint.VARIANTS]
sheet(ims, [v.capitalize() for v in mpaint.VARIANTS], 2).save(os.path.join(out, 'compy_variantes_lateral.png'))
# 3) escala: jugador (1,8), gallina (0,7 x 0,4) y Compy a la escala del renderer (0,185)
SC = 0.185; PXB = 220            # pixeles por bloque
img, (xl, xh, yl, yh) = mrender.render(a, 'left', S=1)
S = SC / 16.0 * PXB              # pixeles por unidad de modelo
comp, _ = mrender.render(a, 'left', S=S, pad=0)
W, H = 1500, int(2.0 * PXB) + 60
im = Image.new('RGB', (W, H), BG); d = ImageDraw.Draw(im); gy = H - 30
d.line([(0, gy), (W, gy)], fill=(90, 90, 90), width=3)
# jugador esquematico
px = 140; hb = lambda b: int(b * PXB)
d.rectangle([px, gy - hb(0.75), px + hb(0.25), gy], fill=(60, 70, 170)); d.rectangle([px + hb(0.25), gy - hb(0.75), px + hb(0.5), gy], fill=(60, 70, 170))
d.rectangle([px - hb(0.0), gy - hb(1.5), px + hb(0.5), gy - hb(0.75)], fill=(40, 170, 170)); d.rectangle([px + hb(0.0), gy - hb(1.8), px + hb(0.5), gy - hb(1.5)], fill=(190, 140, 110))
d.text((px - 30, gy - hb(1.8) - 26), 'Jugador 1,8 bloques', fill=(30, 30, 30), font=font(16))
# gallina (caja)
gx = 480; d.rectangle([gx, gy - hb(0.7), gx + hb(0.4), gy], fill=(235, 235, 235), outline=(120, 120, 120), width=2); d.text((gx - 10, gy - hb(0.7) - 24), 'Gallina 0,4 x 0,7', fill=(30, 30, 30), font=font(16))
# compy
cx = 760; im.paste(comp, (cx, gy - comp.height)); d.text((cx, gy - comp.height - 26), 'Compy ~1,1 largo x %.2f alto' % (comp.height / PXB), fill=(30, 30, 30), font=font(16))
# hitbox 0,4 x 0,5
hx = cx + int(comp.width * 0.55); d.rectangle([hx - hb(0.2), gy - hb(0.5), hx + hb(0.2), gy], outline=(200, 40, 40), width=2); d.text((hx + hb(0.25), gy - hb(0.5)), 'hitbox 0,4 x 0,5', fill=(200, 40, 40), font=font(14))
im.save(os.path.join(out, 'compy_escala.png'))
print('largo bloques', round(comp.width / PXB, 3), 'alto bloques', round(comp.height / PXB, 3))
