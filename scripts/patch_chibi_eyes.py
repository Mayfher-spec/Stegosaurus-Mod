#!/usr/bin/env python3
"""Reconstruct the exact chibi 0.5.4 UV-eye texture changes from the pinned 0.5.2 JAR."""
import base64, io, struct, sys, zlib
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from PIL import Image

DELTAS = {"natural":"eNot1D+IHUUcwPFf84z39u17dzO782d33xlRiyBG7SyiQhAEDSiouSIpxMYUopJIQiJYeImnuUuMfziChDtjY9SrhIhYiKSLAUFQo9Z2Yued1z3nt/O95svsm9/MhylOBmJeOrM4kzvEHDjczWSPmIeebmdyp5gHn5rOZC6tDzUzGer3tC7EHDySfh+JObGSWoo5dzl1LObip2l+ImZtI63n0/pqWi+IufRZOt+wzzJX6TnpvJr7nN6X9nvuC2L26/1RzMOH0vdGzDPH0jkDsSdXO/Xao2/0XvvoUqde++TSA+rNHdKCjmgpdnl9ql7bOydiz1+Zqte+f3WqXnths1OvPbu+qN48V9GaOuppEHvwaKde+/wrvdceX5mqt3r3Sv++1Wvnem/1yL696s2do0Na0BEt6Viq3jlJ533Sqbe6sLmo3lzDPksrWlNHPQ00SnVqbVG9ydl769Mf9t66/32P1LPtf9SbO0eHtKAjWtIx8xOp37ncv2+9ttG/b/5u2GdpRWvqqKeBRuYbnANxr6/uVa/D6/A6vG6W/pLXPbEvqDd3REv2jZmfiDu73nvd6sZd6nV4Hd48V9GaOu7x7As0Mt/gHIh/9b271evxerwer7916YB6fX9eId4tjNWbW/L7mPmJ+K2tF9Trv7m+pF6PN++zzFW05lzH7557A43MNzgHEl5++x71BrwBb8Ab8AbeN+ANeAPegDf89dNH6g27f3yl3oA34A14A97A+wa8AW/AG/Bm50DikZP3qjfijXgj3og3t5CIN+KNeCPeuPvnlnrjf79/qd6IN+KNeCPe/N1Rz72BRuYbnANpDh+/T73NqZXH1Nvw/6H5+eb36s0d0oKOaMn+sTQ7t79Qb7Orznm6kKrvbNhnmatoTR31NLA/SvPi6fvVm50DaZdO9O/bXvz4WfW2v/ywqt727+ub6m17xzBV7y/oSNr+/UrWY2l73yR91/3z0m5nb95nqKUVramjnnuDtD9+t6ze9oP159SbnQPp3jr/uHq7G1+/qd7u318/V2+389s19XY7t6+pN68LOqIlHdNJan7fvF6QbltrqKUVramjngYapbv17bJ6e+f/dO2ETA==","cibernetico":"eNot1M9vFVUUwPGzGEv73pv3+u69M/fXvLZCwUhRhGq7MEZD3GniFoOY6FJjXLh0IysXRlwSJTEhIhUiCrRANSbuBEF0YeI/YuKGxfjOzHf1zb3vnJlPhwQpxKxvvNzKE2KOHDvVyoKYvHaylQNixma1lUUxg3FqZWnect4BHXI/ElPqXCmmyhutjMWE1edamYjxK8dbWZ6f1+bnqZi4dqIVw5xlz/GciufW1HMfcEQxaXW+n3Dmufvo3F2Ina1vqd8efPol9dvm0Avqt9PqkPrtkc1T6u87oEM6oqVYl46q33buidh69qz6bff3TLk3YvH3e45WtKaeBrHL7qD6bez9vTPjLsSFJ0+q3zV6XhC3cnhb/c6vbaq/7xId0CEd0ZKOxXXuibiqeUb9rnNPuTfMWepoRWvqaaBR3Gx9W/29M+MupMJfpcOdv+rmD0j13+NW/X2X6IAO6YiWdMz+RKoqH1N/1bmn3BvmLHW0ojX1NNDIfsKZcRdSzzZfVX89O/6K+mv8Nf4af922nb9e3HpD/X1HtGRuzP5Eapc21F93/w5T7g1zlj1HK1rzHs9coJH9hDPjLsTj9yvaBfH4PX6P35/451/1++75Q/EDs6H+viW/j9mfiH/7zXfV7986/Y76Pf5+zrLnaMVza373vDfQyH7CmXEXEvCHtRdfU3/AH/AH/AF/4PsH/AF/wB/wh+8vXlR/eHR3T/0Bf8Af8Af8ge8f8Af8AX/A3zsz7kLi+pn31B+fOvuB+iP+iD/ij/j7DiXij/gj/og/Pti7qf6+U+4Nc5Y9Ryvua+p5b6CR/YQz4y4kbX13V/3p9fNfqT999OUl9Sf+/0k/3/9b/X0HdEhHtGR+LKnzTiQ9vH1L/Ynv398b5ix7jla0pp4G5qOk9y9cUn/a/vSC+nt3IXnzzIfqz6c/+Vz9+fLOVfXnh/reRcnf7t5Tf75/60f151+vXlF/vnfzB/Xn33dvqD//cWdX/blzTzgvMzflbKhlz/F7xXNrzp73BsmP9vfUn7/euab+fPbcefX37kKa5z/+TP3NucvX1N882L+t/ubP/c7fdN9vSZrfblxXf/PLlW/U359H83n9riVzY+4n3C9L89dPd9Tf3xvmLL877iueW3P2zAUccX7W5yRpvti5rv7O/T9eyobk","magma":"eNot0L2P3EQYgPG3MXe7a++HZzwztldKkHJHspfb3OU2kY6C3Vs4CRQkUBokOkSRAlEAgrRpUiKRgESXgi4SSFBAx4U0VGmgIpD8Awho6BLA+B0/bh7ZM+/MT5ZE8hd25408I/lWXTeyIfnB9nONbEq+3r/YSE/yN5ZHjfQlv+PyRgY0pRkd6v6DRkaSr/baubHkz59vz53wPWefoZYW1LX3rNaNeO4Nki+2zzZSSn7K+0YqdW03kohZ715Qr7mszg0xi85r4jk92qcDmtJMzPLCvnrNUec1R53XROdETPTn7DPMWVpQRz0NYg7PzdRr9s+cUa9ZzefqtcfzXfXa5WymXhv3b9Ke2K2RV2/XAU1pRodio3Mk9sWLC/Xa6JzQnH2GWlpQRz33Blq2rp2ZejtnIsV656x6i+NZ/L9FnNuUovn7L/V27dMBTWlGh8yNpHjp4JJ6i7W6J3zP2WeopQV11NPAXImrwpmIe3n7tHrd6+dOq9fhdXgdXte0T+t1b42tertmrA+ZG4k7XlxWr8Pr8Dq83ZylBec61j0NzJW4KpyJ+Bs7Qb0+rm+073u1ej1ej9f/sUzV63f6Y/V2zVgfMj8S/8mH76jXf3z9XfV6vB5vN2dpwbmOdU8DjpL5Cmci4buVU2/AG+4eBvUGvAFvwBvwBryh+fa8egPe8PjeXfWGf3/7Qb0Bb7fPMGdpwbmOezwNOErmK5yJlA9WE/WWJ8tCvWXcv0l7tC8l3jLem9KMDqXEW/736L56y39+vafeEm+Jt6ulBec67vE00BJXhTOR6ubilHqrO1f21Ft9fe2Keqt4T4/26YCmNKNDqZ4+PFFvFZ3jtt3/reJ/ztlnqKUFddTTINWXb7+i3urW4bPq7ZyJ1G/OZ+qtP311V731T7feV2/94OYH6q2f/vK9euvoGNCUZnRIx7r/RJ3t+4k6u++GWlpQJ/UTPd9zX5D659sfqbP+7LUDdXa+RKZfXN1S5/T+tUvqnP7+1W11TuN8T6bx3j7vA5rSjA7pSKZ4p08eRm/7PXq7dUMtLaijngaZ/vnN5+qd/vjesXqj83/MYVbT"}
def apply(jar_in, jar_out):
    with ZipFile(jar_in) as src:
        originals = [(info,src.read(info.filename)) for info in src.infolist() if not info.is_dir()]
    replacement = {}
    patched = {}
    for skin, compressed in DELTAS.items():
        name = f"assets/prodigychibi/textures/entity/stegosaurus_chibi_{skin}.png"
        old = next(raw for info, raw in originals if info.filename == name)
        im = Image.open(io.BytesIO(old)).convert("RGBA")
        payload = zlib.decompress(base64.b64decode(compressed))
        assert len(payload) % 8 == 0
        for offset in range(0, len(payload), 8):
            x, y, r, g, b, a = struct.unpack(">HH4B", payload[offset:offset+8])
            im.putpixel((x,y),(r,g,b,a))
        new_png = io.BytesIO()
        im.save(new_png, format="PNG")
        replacement[name] = new_png.getvalue()
        patched[skin] = im
        print(f"Updated {skin}: {len(payload)//8} corrected eye pixels")
    # El chibi dorado no tenia ojos propios (cara con "bigote"): se le pone el mismo ojo ambar del natural.
    name = "assets/prodigychibi/textures/entity/stegosaurus_chibi_dorado.png"
    gold = Image.open(io.BytesIO(next(raw for info, raw in originals if info.filename == name))).convert("RGBA")
    nat = patched["natural"]
    dark = lambda px, lim: sum(px[:3]) < lim
    cream = gold.getpixel((12, 24))
    for y in range(16, 25):                      # borra el dibujo de ojos viejo
        for x in range(3, 30):
            if dark(gold.getpixel((x, y)), 200): gold.putpixel((x, y), cream)
    for x0, x1 in ((6, 14), (17, 25)):           # copia cada ojo del natural (contorno, esclera, iris, pupila)
        for y in range(20, 27):
            xs = [x for x in range(x0, x1 + 1) if dark(nat.getpixel((x, y)), 150)]
            if not xs: continue
            for x in range(min(xs), max(xs) + 1):
                if y in (20, 26) and not dark(nat.getpixel((x, y)), 150): continue
                gold.putpixel((x, y), nat.getpixel((x, y)))
    buf = io.BytesIO(); gold.save(buf, format="PNG")
    replacement[name] = buf.getvalue()
    print("Updated dorado: ojo del natural")
    with ZipFile(jar_out, "w", compression=ZIP_DEFLATED, compresslevel=9) as out:
        for info, raw in originals:
            out.writestr(info.filename, replacement.get(info.filename, raw))
    with ZipFile(jar_out) as z:
        assert z.testzip() is None
if __name__ == "__main__":
    apply(*sys.argv[1:])
