#!/usr/bin/env python3
"""Stegosaurus for ROR: original pixel-art atlas and masked emissive pass.
Python stdlib only. NO ROR resources or illustrations are copied.
1024px square: 16 material tiles, 256px each, aligned to StegosaurusModel.
Run from any directory; GitHub Actions generates identical atlases.
"""
from pathlib import Path
import random, math, zlib, struct

S=1024
T=256
OUT=Path(__file__).resolve().parent/'src/main/resources/assets/prodigycreatures/textures/entity'
OUT.mkdir(parents=True,exist_ok=True)
KEYS=['skin','armor','shadow','belly','ridge','rim','core','glow',
      'accent','light','bone','eye','scar','vein','muzzle','joint']
THEMES={
'cybernetic':{
'skin':'15273C','armor':'1C3E5B','shadow':'0A1525','belly':'AEBCC9',
'ridge':'294363','rim':'071325','core':'03A2C3','glow':'6FF9FF',
'accent':'00BFD8','light':'C2FFFF','bone':'C5D3E0','eye':'70FFFF',
'scar':'325477','vein':'00ECFF','muzzle':'34465A','joint':'192433'},
'golden':{
'skin':'F1E4CB','armor':'D9C49B','shadow':'8C7356','belly':'FFF8EA',
'ridge':'FAF1DB','rim':'B27718','core':'E7B63B','glow':'FFF3AE',
'accent':'C78A1D','light':'FFF9E4','bone':'F9F5ED','eye':'FFC84D',
'scar':'B7905C','vein':'EFBF56','muzzle':'EDE0C6','joint':'BC985E'},
'magma':{
'skin':'261E27','armor':'4A272C','shadow':'120F18','belly':'947369',
'ridge':'3A2B34','rim':'15131B','core':'B12B22','glow':'FF8017',
'accent':'B01F28','light':'FFD561','bone':'D6CAC0','eye':'FF9B20',
'scar':'C6A399','vein':'FF4D0B','muzzle':'3D2A2A','joint':'24161E'},
'glacial':{
'skin':'1C293D','armor':'34516C','shadow':'0F192A','belly':'D5E7F2',
'ridge':'365B77','rim':'13283F','core':'29B8F0','glow':'C8FAFF',
'accent':'35DFF7','light':'F0FFFF','bone':'E9F6FF','eye':'DAFFFF',
'scar':'93C6D8','vein':'6CF3FF','muzzle':'334B61','joint':'243D59'},
}

def RGB(s):return tuple(int(s[i:i+2],16) for i in (0,2,4))
def scale(c,f):return tuple(max(0,min(255,int(x*f))) for x in c)
def blend(a,b,p):return tuple(max(0,min(255,int(a[i]*(1-p)+b[i]*p))) for i in range(3))

class Canvas:
 def __init__(self,alpha=False):
  self.alpha=alpha;self.channels=4 if alpha else 3
  self.data=bytearray(S*S*self.channels)
 def rect(self,x,y,w,h,color):
  x=max(0,int(x)); y=max(0,int(y)); endx=min(S,int(x+w)); endy=min(S,int(y+h))
  if endx<=x or endy<=y:return
  pix=bytes(color); row=pix*(endx-x);c=self.channels
  for yy in range(y,endy):
   off=(yy*S+x)*c;self.data[off:off+len(row)]=row
 def line(self,pts,color,w=1):
  for (x0,y0),(x1,y1) in zip(pts[:-1],pts[1:]):
   dx=x1-x0;dy=y1-y0;steps=max(abs(dx),abs(dy),1)
   for k in range(steps+1):
    t=k/steps;x=x0+dx*t;y=y0+dy*t
    self.rect(x-w//2,y-w//2,w,w,color)
 def save(self,p):
  def chunk(name,data):return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data)&0xffffffff)
  raw=bytearray();stride=S*self.channels
  for y in range(S):raw.append(0);raw.extend(self.data[y*stride:(y+1)*stride])
  png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',S,S,8,6 if self.alpha else 2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,8))+chunk(b'IEND',b'')
  p.write_bytes(png)


def draw_theme(name,colors):
 palette={k:RGB(v) for k,v in colors.items()}; rng=random.Random({'cybernetic':1041,'golden':2508,'magma':4701,'glacial':9092}[name]);
 atlas=Canvas();mask=Canvas(alpha=True)
 for material,key in enumerate(KEYS):
  tx=(material%4)*T; ty=(material//4)*T
  c=palette[key]
  # Special materials get several tones; fine-grain 3–5 px scales are deliberately
  # large enough for Minecraft nearest-neighbor rendering at normal game distance.
  for py in range(0,T,4):
   for px in range(0,T,4):
    fract=rng.random();k=.88+rng.random()*.22
    if fract<.04:k=.72
    elif fract>.965:k=1.22
    if key in ['core','glow']:
     # Glowing plate shards: bright central veins, dark frame, crystalline facets.
     t=(px%76)/76
     k*=.92+(.26 if abs(t-.5)<.14 else -.10 if abs(t-.5)>.38 else .07)
    elif key in ['skin','armor','ridge','muzzle']:
     k*=1.0+(.07 if ((px//20+py//16)%5==0) else 0)
    elif key=='belly':k*=1-.05*(py//17%2)
    atlas.rect(tx+px,ty+py,4,4,scale(c,k))
  # Scales: interlocking pixel-angle marks rather than random TV static.
  if key in ['skin','armor','ridge','muzzle','joint']:
   for y in range(8,T-8,17):
    off=(y//17%2)*10
    for x in range(8+off,T-12,20):
     atlas.line([(tx+x,ty+y),(tx+x+6,ty+y+3),(tx+x+13,ty+y)],scale(c,.72),2)
     atlas.line([(tx+x+2,ty+y+4),(tx+x+7,ty+y+6)],scale(c,1.20),1)
  if key in ['rim','shadow']:
   for i in range(25):
    x=rng.randrange(0,245);y=rng.randrange(0,245)
    atlas.rect(tx+x,ty+y,rng.randrange(2,12),rng.randrange(2,6),scale(c,.5+rng.random()*.6))
  if key in ['core','glow']:
   for x in range(14,245,21):
    atlas.line([(tx+x,ty+10),(tx+x+8,ty+39),(tx+x-1,ty+83),(tx+x+10,ty+125)],scale(palette['light'],.68),3)
    atlas.line([(tx+x+5,ty+10),(tx+x-1,ty+45)],scale(palette['rim'],.70),2)
   for i in range(15):
    x=rng.randrange(15,241);y=rng.randrange(10,230)
    atlas.line([(tx+x,ty+y),(tx+x+7,ty+y+12),(tx+x+1,ty+y+20)],scale(palette['light'],.83),2)
  if key in ['accent','vein']:
   for i in range(23):
    x=rng.randrange(12,200);y=rng.randrange(9,230)
    atlas.line([(tx+x,ty+y),(tx+x+8,ty+y+5),(tx+x+12,ty+y+5),(tx+x+22,ty+y+18)],scale(palette['light'],.55),2)
  if key in ['scar']:
   for i in range(22):
    x=rng.randrange(5,210);y=rng.randrange(5,210)
    atlas.line([(tx+x,ty+y),(tx+x+8,ty+y+3),(tx+x+16,ty+y+15)],scale(c,1.32),3)
    atlas.line([(tx+x+2,ty+y+2),(tx+x+10,ty+y+5)],scale(palette['shadow'],.92),2)
  if key=='eye':
   for y in range(12,244,32):
    for x in range(15,240,33):
     atlas.rect(tx+x,ty+y,11,6,scale(c,1.2));atlas.rect(tx+x+3,ty+y+1,2,2,(255,255,255))
  if key=='bone':
   for y in range(3,245,20):
    atlas.line([(tx+12,ty+y),(tx+230,ty+y+2)],scale(c,.88),1)
  # Fullbright overlay: selected materials only, mask translucent brightness
  # instead of uniformly glowing all skin and armor. Zero-alpha elsewhere.
  if key in ('core','glow','light','eye','vein'):
   for py in range(0,T,4):
    for px in range(0,T,4):
     # Core remains mostly normal lit colour; narrow glowing crystal lines
     # have more intensity. Emissive does NOT cast dynamic light into the world.
     a=(65 if key=='core' else 145 if key=='glow' else 210)
     color=palette[key] if key!='vein' else palette['light']
     mask.rect(tx+px,ty+py,4,4,(*color,a))
 # More deliberate visual marks varying by skin type on skin tile, with
 # corresponding light-mask thin inlays for volcanic cracks and blue circuitry.
 sx=0;sy=0
 if name in ('cybernetic','glacial'):
  for i in range(24):
   x=18+(i*37)%207;y=12+(i*31)%180
   col=palette['accent']
   pts=[(x,y),(x+12,y),(x+12,y+9),(x+20,y+9)]
   atlas.line(pts,col,3);mask.line(pts,(*col,110),2)
 elif name=='magma':
  for i in range(22):
   x=17+(i*47)%196;y=16+(i*29)%190
   pts=[(x,y),(x+13,y+9),(x+10,y+20),(x+22,y+31)]
   atlas.line(pts,palette['shadow'],7)
   atlas.line(pts,palette['vein'],3)
   mask.line(pts,(*palette['glow'],200),2)
  for i in range(10):
   x=12+(i*31)%205;y=13+(i*37)%177
   atlas.line([(x,y),(x+10,y+3),(x+5,y+12)],palette['scar'],3)
 else:
  for i in range(19):
   x=16+(i*41)%204;y=17+(i*35)%201
   atlas.line([(x,y),(x+9,y-7),(x+18,y+5),(x+8,y+16)],palette['accent'],2)
   atlas.line([(x+3,y+1),(x+11,y+2)],palette['light'],1)
 atlas.save(OUT/f'stegosaurus_{name}.png')
 mask.save(OUT/f'stegosaurus_{name}_emissive.png')
 print(name,'-> base + emissive')

if __name__=='__main__':
 for k,v in THEMES.items():draw_theme(k,v)
