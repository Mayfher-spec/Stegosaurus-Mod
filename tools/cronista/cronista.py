"""Textura del Cronista (64x128): la skin del usuario ocupa la mitad superior (esquema de jugador, brazos finos) y la mitad inferior lleva la cola de dinosaurio
(tres tramos + puas dorsales) pintada con la paleta de la propia skin. Determinista.
Uso: python3 tools/cronista/cronista.py [skin.png] [salida.png]"""
import os, sys, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
DARK = (39, 43, 38); DARKER = (28, 31, 28)
GREENS = [(65, 100, 73), (75, 115, 84), (80, 123, 89), (96, 115, 95)]
PALE = [(212, 227, 199), (185, 203, 175)]
CREAM = (241, 239, 228)

def box(img, rng, u, v, w, h, d, spike=False):
    """Pinta las 6 caras de un cubo de Minecraft con origen de textura (u, v)."""
    px = img.load()
    faces = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'left': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
             'right': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
    for name, (x0, y0, fw, fh) in faces.items():
        for j in range(int(fh + 0.99)):
            for i in range(int(fw + 0.99)):
                if name == 'bottom' or (name in ('left', 'right', 'front', 'back') and j >= fh * 0.62):
                    col = CREAM if (j % 2 == 0 or name == 'bottom') and (i + j) % 3 else PALE[(i + j) % 2]   # vientre claro con costillas
                elif spike:
                    col = PALE[0] if j < fh - 1 else DARK
                else:
                    cell = ((i + (j // 2) % 2) // 2, j // 2)
                    if (i + (j // 2) % 2) % 2 == 1 and j % 2 == 1:
                        col = DARK                                           # borde entre escamas
                    else:
                        col = GREENS[hash((cell[0] * 7 + cell[1] * 13 + rng.randint(0, 3))) % len(GREENS)]
                px[x0 + i, y0 + j] = col + (255,)

def main(skin, out):
    base = Image.open(skin).convert('RGBA')
    img = Image.new('RGBA', (64, 128), (0, 0, 0, 0))
    img.paste(base, (0, 0))
    rng = random.Random(1337)
    box(img, rng, 0, 64, 4, 4, 6)          # cola, tramo 1
    box(img, rng, 0, 76, 3, 3, 6)          # tramo 2
    box(img, rng, 0, 88, 2, 2, 5)          # tramo 3
    box(img, rng, 24, 64, 1, 1.5, 2, spike=True)
    box(img, rng, 24, 72, 1, 1.3, 2, spike=True)
    box(img, rng, 24, 80, 1, 1.1, 2, spike=True)
    img.save(out, optimize=True)

if __name__ == '__main__':
    skin = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'skin.png')
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'src/main/resources/assets/prodigycreatures/textures/entity/cronista.png')
    main(skin, out)
