#!/usr/bin/env python3
"""Four hand-tuned pixel palettes translated to Prodigy Chibi Alpha's shared UV atlas.
References: the user-supplied Phase 1's natural, cybernetic, magma, albino-gold skins.
This intentionally RETAINS the Alpha 0.1 silhouette and does not adopt the other model.
No pillow/numpy dependency on GitHub runners. Natural is also repainted.
"""
import runpy, struct, zlib, math
from pathlib import Path

base = Path(__file__).resolve().parent
ctx = runpy.run_path(str(base / "generate_assets.py"))
W, H = ctx["W"], ctx["H"]
names = ctx["names"]
original = ctx["pixels"]
basecolors = ctx["colors"]
out = base / "src/main/resources/assets/prodigychibi/textures/entity"
out.mkdir(parents=True, exist_ok=True)

styles = {
"natural": {
  "body":(82,113,45),"body_light":(121,141,45),"belly":(216,189,144),
  "head":(87,118,45),"snout":(115,133,48),"mouth":(63,35,38),
  "jaw":(236,214,172),"plate":(143,166,54),"plate_tip":(228,215,126),
  "plate_edge":(80,58,34),"foot":(68,89,35),"claw":(238,218,178),
  "spike":(230,214,169),"eye_left":(90,121,49),"eye_right":(90,121,49),
  "nostrils":(127,152,59)
},
"cibernetico": {
  "body":(43,53,69),"body_light":(23,119,139),"belly":(183,173,178),
  "head":(32,45,63),"snout":(61,70,82),"mouth":(48,29,44),
  "jaw":(214,203,208),"plate":(7,187,223),"plate_tip":(58,238,247),
  "plate_edge":(14,27,39),"foot":(32,47,68),"claw":(206,204,209),
  "spike":(70,218,228),"eye_left":(49,61,76),"eye_right":(49,61,76),
  "nostrils":(46,58,71)
},
"magma": {
  "body":(52,34,35),"body_light":(127,52,33),"belly":(180,143,119),
  "head":(62,35,35),"snout":(83,50,48),"mouth":(37,15,17),
  "jaw":(214,180,156),"plate":(216,69,17),"plate_tip":(255,184,41),
  "plate_edge":(32,18,21),"foot":(56,33,35),"claw":(232,205,168),
  "spike":(236,138,60),"eye_left":(63,34,36),"eye_right":(63,34,36),
  "nostrils":(86,55,51)
},
"dorado": {
  "body":(248,231,211),"body_light":(246,224,201),"belly":(254,242,225),
  "head":(255,239,223),"snout":(254,236,217),"mouth":(111,66,61),
  "jaw":(246,205,185),"plate":(255,226,203),"plate_tip":(255,225,119),
  "plate_edge":(217,145,32),"foot":(247,222,194),"claw":(255,201,64),
  "spike":(255,215,144),"eye_left":(255,240,222),"eye_right":(255,240,222),
  "nostrils":(245,219,201)
}
}
def clamp(v): return max(0, min(255, int(v)))
def png(raw):
    scan = b"".join(b"\0" + raw[y*W*4:(y+1)*W*4] for y in range(H))
    def chunk(key, data):
        return struct.pack(">I",len(data))+key+data+struct.pack(">I",zlib.crc32(key+data)&0xffffffff)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR",struct.pack(">IIBBBBB",W,H,8,6,0,0,0)) + chunk(b"IDAT",zlib.compress(scan,9)) + chunk(b"IEND",b"")

for variant, palette in styles.items():
    image = bytearray(W*H*4)
    emissive = bytearray(W*H*4)
    def dot(x,y,color):
        if 0<=x<W and 0<=y<H:
            ofs=4*(y*W+x)
            image[ofs:ofs+4]=bytes((*color,255))
    def light(x,y,color):
        if 0<=x<W and 0<=y<H:
            ofs=4*(y*W+x)
            emissive[ofs:ofs+4]=bytes((*color,255))
    for tile, name in enumerate(names):
        col = palette.get(name,basecolors[name])
        old = basecolors[name]
        x0=(tile%16)*16; y0=(tile//16)*16
        for y in range(16):
            for x in range(16):
                xx=x0+x; yy=y0+y
                offset=4*(yy*W+xx)
                if original[offset+3]==0:continue
                before=original[offset:offset+3]
                shade=sum(int(before[i])-int(old[i]) for i in range(3))/3
                if variant=="dorado":shade*=0.62
                shade=max(-37,min(37,shade))
                v=[clamp(c+shade) for c in col]
                # Colour story of the Phase-1 skins, projected onto the original Alpha's UV.
                if name in ("body","body_light","head","foot","eye_left","eye_right"):
                    if variant=="natural" and (x*7+y*13+tile)%23 in (0,1,2,3):
                        v=[clamp(v[0]*.67),clamp(v[1]*.76),clamp(v[2]*.64)]
                    if variant=="cibernetico" and ((x==y//2+3) or (y==6 and x%4!=3)):
                        v=[7,173,210]
                    if variant=="magma" and ((x==(y//3+2)%14) or (y==10 and x>4 and x<12)):
                        v=[min(255,202+x*3),55+(y%3)*22,11]
                    if variant=="dorado" and (x*5+y*3+tile)%31 in (0,1):
                        v=[229,166,62]
                if name in ("plate","plate_tip"):
                    edge=x in (0,15) or y in (0,15)
                    if edge and variant=="natural":v=[70,58,35]
                    if edge and variant=="cibernetico":v=[19,31,47]
                    if edge and variant=="magma":v=[50,22,27]
                    if edge and variant=="dorado":v=[220,155,38]
                    if 3<y<13 and 3<x<13 and variant in ("cibernetico","magma"):
                        gradient=(15-y)*3
                        v=[clamp(c+gradient) for c in v]
                dot(xx,yy,tuple(v))
                if variant=="cibernetico" and name in ("plate","plate_tip","spike") and not(x in (0,15) or y in(0,15)):
                    light(xx,yy,tuple(v))
                if variant=="magma" and name in ("plate","plate_tip") and not(x in (0,15) or y in(0,15)):
                    light(xx,yy,tuple(v))
                if variant=="magma" and name in ("body","body_light") and v[0]>125 and v[0]>v[1]*1.8:
                    light(xx,yy,tuple(v))
                if variant=="dorado" and name in ("plate_tip",):
                    light(xx,yy,tuple(v))
        if name.startswith("eye_"):
            # Paint directly onto the head-side texture: NO extra eye cubes.
            for y in range(3,13):
                for x in range(5,13):
                    xx=x0+x;yy=y0+y
                    if variant=="dorado":
                        # gold chibi's characteristic sleepy closed eye
                        if y==9+(1 if x in (5,12) else 0):dot(xx,yy,(89,52,38))
                        continue
                    ellipse=((x-8.5)/4.1)**2+((y-7.7)/5.1)**2
                    if ellipse<1:
                        if ellipse>.8: color=(33,30,29)
                        elif x in (6,7) and y in(4,5,6):color=(252,250,242)
                        elif x>=7 and y>=5:
                            color={"natural":(106,58,32),"cibernetico":(25,155,199),"magma":(188,46,25)}[variant]
                        else:color=(255,250,237)
                        if x in (7,8) and y in (4,5):color=(255,255,255)
                        dot(xx,yy,color)
            if variant=="magma":
                for x in range(4,14):
                    y=3+(x-4)//4
                    dot(x0+x,y0+y,(35,18,19))
    fn=out/("stegosaurus_chibi_"+variant+".png")
    fn.write_bytes(png(image))
    (out/("stegosaurus_chibi_"+variant+"_glow.png")).write_bytes(png(emissive))
    print("Created",fn.name,len(fn.read_bytes()),"bytes")
# The baby cannot wear or use a saddle; avoid shipping a dormant saddle mesh.
saddle_geo=base/"src/main/resources/assets/prodigychibi/geo/stegosaurus_chibi_saddled.geo.json"
if saddle_geo.exists(): saddle_geo.unlink()
