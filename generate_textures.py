#!/usr/bin/env python3
"""Generate original 512x512 PNG atlases using Python standard library only.
Used by GitHub Actions during builds. No extracted ROR assets or user artwork.
"""
import random, math, zlib, struct
from pathlib import Path
SIZE=512
OUT=Path(__file__).parent/'src/main/resources/assets/prodigycreatures/textures/entity'
OUT.mkdir(parents=True, exist_ok=True)
COLORS={
'cybernetic':{'skin':'132437','ridge':'263C52','belly':'A6B4BD','limb':'253547','muzzle':'1C2C3D','plate':'0199AF','plate_lite':'6CFDF5','rim':'071324','spike':'C6E3EE','claw':'CDD6DB','accent':'00E8F8','eye':'1AF8FD','scar':'0F4C5D'},
'golden':{'skin':'E6D8C2','ridge':'F6EDDB','belly':'FFF6E8','limb':'D3C6B4','muzzle':'E9DDD1','plate':'DA9B24','plate_lite':'FFF0AB','rim':'9C6512','spike':'F9F6E8','claw':'E2A42A','accent':'F7C54D','eye':'EFC65A','scar':'A8874B'},
'magma':{'skin':'281D26','ridge':'483038','belly':'84635C','limb':'231D23','muzzle':'241A23','plate':'971D1B','plate_lite':'FF7418','rim':'110F18','spike':'CFC4B2','claw':'B8ABA3','accent':'FF4A09','eye':'FFB33A','scar':'D74C3D'},
'glacial':{'skin':'18243A','ridge':'304766','belly':'DDE8F1','limb':'344861','muzzle':'24364D','plate':'2CA9DE','plate_lite':'D4FFFF','rim':'0F1A2F','spike':'EAF6FD','claw':'E2F4FC','accent':'64F5FF','eye':'C9FFFF','scar':'9EDEEE'},
}
def rgb(text):return tuple(int(text[i:i+2],16) for i in (0,2,4))
def shade(c,a):return tuple(max(0,min(255,round(v*a))) for v in c)
class Canvas:
 def __init__(self,base):
  self.data=bytearray(bytes(base)*SIZE*SIZE)
 def rect(self,coords,color):
  x0,y0,x1,y1=map(int,coords);x0=max(0,x0);y0=max(0,y0);x1=min(SIZE-1,x1);y1=min(SIZE-1,y1)
  if x0>x1 or y0>y1:return
  strip=bytes(color)*(x1-x0+1)
  for y in range(y0,y1+1):
   i=(y*SIZE+x0)*3;self.data[i:i+len(strip)]=strip
 def line(self,coords,color,width=1):
  points=list(zip(coords[0::2],coords[1::2]))
  for (x0,y0),(x1,y1) in zip(points,points[1:]):
   x0=int(x0);x1=int(x1);y0=int(y0);y1=int(y1)
   dx=abs(x1-x0);sx=1 if x0<x1 else -1;dy=-abs(y1-y0);sy=1 if y0<y1 else -1;err=dx+dy
   while True:
    rr=(width-1)//2
    self.rect((x0-rr,y0-rr,x0+width-rr-1,y0+width-rr-1),color)
    if x0==x1 and y0==y1:break
    e2=2*err
    if e2>=dy:err+=dy;x0+=sx
    if e2<=dx:err+=dx;y0+=sy
 def save(self,path):
  def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
  raw=bytearray()
  for y in range(SIZE):raw.append(0);raw.extend(self.data[y*SIZE*3:(y+1)*SIZE*3])
  png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',SIZE,SIZE,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
  path.write_bytes(png)

def build(kind,colors):
  c={key:rgb(val) for key,val in colors.items()}
  seed={'cybernetic':154,'golden':277,'magma':896,'glacial':230}[kind]
  can=Canvas(c['skin']); rng=random.Random(seed)
  zones=[((0,0,190,71),'skin',.15,'ridge'),((0,72,190,157),'ridge',.12,'skin'),((192,0,287,159),'muzzle',.12,'ridge'),
    ((288,0,383,159),'plate',.16,'plate_lite'),((384,0,479,159),'plate_lite',.12,'plate'),
    ((0,160,191,255),'belly',.14,'skin'),((192,160,287,255),'limb',.16,'ridge'),
    ((288,160,383,271),'spike',.13,'scar'),((384,160,479,254),'ridge',.16,'accent'),
    ((0,274,191,351),'muzzle',.16,'skin'),((192,274,287,352),'ridge',.16,'skin'),
    ((288,272,383,367),'claw',.13,'spike'),((384,270,479,367),'rim',.12,None),
    ((0,370,511,511),'skin',.15,'ridge')]
  for idx,(rect,key,var,extra) in enumerate(zones):
   rand=random.Random(seed+idx*137);x0,y0,x1,y1=rect
   can.rect(rect,c[key]);pixel=3 if idx%3==0 else 4
   for y in range(y0,y1,pixel):
    for x in range(x0,x1,pixel):
     a=1+rand.uniform(-var,var)
     if rand.random()<.09:a+=rand.choice([-.12,.12])
     can.rect((x,y,min(x+pixel-1,x1),min(y+pixel-1,y1)),shade(c[key],a))
   if extra:
    for row in range(y0+7,y1-3,10):
     for x in range(x0+(row//10%2)*5+3,x1-8,14):
      if rand.random()>.25:
       can.line((x,row,x+3,row+2,x+7,row),shade(c[extra],rand.uniform(.5,.9)),1)
  # Dense crystalline/lava plate ribs and sharpened outlines.
  for x0,y0,x1,y1 in ((290,6,378,149),(388,4,473,145)):
   for j in range(11):
    x=x0+j*8;v=j/10
    can.line((x,y0+int(14*v),x+int(4*math.sin(j)),y1-10),shade(c['plate_lite'],.43+.45*(1-v)),2)
    can.line((x+4,y0,x+10,y1-2),shade(c['rim'],.65+v*.2),2)
   for k in range(12):
    y=y0+8+k*10;can.line((x0+4,y,x1-5,y-2),shade(c['plate_lite'],.6+.035*k),1)
  # UV islands for luminous eyes and nose.
  can.rect((425,285,441,305),c['rim']);can.rect((428,288,438,296),c['eye']);can.rect((431,289,435,291),(248,250,246))
  can.rect((384,270,407,288),c['rim']);can.line((386,273,401,273),c['scar'],2)
  if kind in ('cybernetic','glacial'):
   for i in range(20):
    x=rng.randrange(0,172);y=rng.randrange(0,60)
    can.line((x,y,x+rng.randrange(8,20),y),c['accent'],2)
    if i%3==0:can.line((x+10,y,x+10,y+rng.randrange(5,13)),c['accent'],2)
   if kind=='glacial':
    for i in range(13):
     x=290+i*7;can.line((x,4,x-6,20,x-5,42),c['plate_lite'],2);can.line((x,42,x+5,67),c['plate_lite'],1)
  elif kind=='magma':
   for i in range(20):
    x=rng.randint(6,158);y=rng.randint(7,49)
    coords=(x,y,x+rng.randint(5,11),y+rng.randint(5,10),x+rng.randint(10,24),y+rng.randint(9,19))
    can.line(coords,c['rim'],6);can.line(coords,c['accent'],3)
    if i%4==0:can.line((x-3,y+6,x+8,y-5),c['scar'],4)
   for i in range(11):
    x=rng.randint(290,360);y=rng.randint(5,119)
    can.line((x,y,x+10,y+10,x+14,y+19),c['accent'],3);can.line((x+1,y,x+7,y+7),c['plate_lite'],2)
  else:
   for i in range(15):
    x=rng.randint(6,153);y=rng.randint(7,55)
    can.line((x,y,x+7,y-5,x+14,y+1,x+9,y+11,x,y+11),c['accent'],2)
   for i in range(14):
    x=291+i*6;can.line((x,4,x+3,30),c['plate_lite'],2)
  path=OUT/f'stegosaurus_{kind}.png';can.save(path)
  return path
if __name__=='__main__':
 for name,pal in COLORS.items():print(build(name,pal))
