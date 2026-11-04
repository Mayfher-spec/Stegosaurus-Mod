#!/usr/bin/env python3
"""Generate self-contained GeckoLib voxel geometry and 256px UV texture for alpha."""
import json, math, random, struct, zlib
from pathlib import Path
random.seed(71)
OUT = Path(__file__).parent / "src/main/resources/assets/prodigychibi"
(OUT/"geo").mkdir(parents=True,exist_ok=True)
(OUT/"textures/entity").mkdir(parents=True,exist_ok=True)
W=H=256
pixels=bytearray(W*H*4)
colors={
"body":(83,115,41),"body_light":(123,151,52),"belly":(212,190,145),
"head":(103,136,48),"snout":(129,161,60),"mouth":(91,43,43),
"jaw":(209,178,141),"plate":(139,170,49),"plate_tip":(211,199,127),
"plate_edge":(70,58,37),"foot":(70,91,42),"claw":(237,219,175),
"spike":(220,206,166),"saddle":(128,69,38),"saddle_trim":(203,156,75),
"strap":(91,51,33),"eye_left":(107,138,46),"eye_right":(107,138,46),"nostrils":(129,161,60)
}
names=list(colors)
assert len(names)<=16*16
def pixel(x,y,rgb):
 if 0<=x<W and 0<=y<H:
  i=(y*W+x)*4
  pixels[i:i+4]=bytes((*rgb,255))
def clamp(v):return max(0,min(255,int(v)))
for k,name in enumerate(names):
 tx=(k%16)*16; ty=(k//16)*16
 c=colors[name]
 for y in range(16):
  for x in range(16):
   shade=math.sin(x*1.8+y*1.3+k)*7 + random.randrange(-12,13)
   if y<2:shade+=16
   if y>13:shade-=15
   if x<2:shade+=6
   if name.startswith("plate") and x in (0,1,14,15):shade-=24
   if name=="spike": shade+=15-x*1.0
   pix=tuple(clamp(v+shade) for v in c)
   pixel(tx+x,ty+y,pix)
 if name.startswith("eye_"):
  # Large chibi eye: painted on the cheek's side face, no eye geometry.
  for yy in range(3,13):
   for xx in range(6,13):
    rr=((xx-9.2)/4.2)**2+((yy-8)/5.2)**2
    if rr<1:
     v=(22,20,19) if rr>.73 else (54,31,23)
     if xx>=8 and yy>=6:v=(127,65,29)
     pixel(tx+xx,ty+yy,v)
  for xx in (7,8,9): pixel(tx+xx,ty+4,(255,252,239))
  for xx in (8,9): pixel(tx+xx,ty+5,(255,252,239))
 if name=="nostrils":
  for xx in (4,5,10,11):
   for yy in (6,7): pixel(tx+xx,ty+yy,(39,53,26))
def png_rgba():
 raw=b"".join(b"\0"+pixels[y*W*4:(y+1)*W*4] for y in range(H))
 def chunk(tag,data):return struct.pack(">I",len(data))+tag+data+struct.pack(">I",zlib.crc32(tag+data)&0xffffffff)
 return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",W,H,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b"")
(OUT/"textures/entity/stegosaurus_chibi_natural.png").write_bytes(png_rgba())
def uv_for(kind,face):
 name=kind
 if kind=="head":
  name = "eye_right" if face=="east" else "eye_left" if face=="west" else "head"
 if kind=="snout" and face=="north":name="nostrils"
 ix=names.index(name)
 return {"uv":[(ix%16)*16,(ix//16)*16],"uv_size":[16,16]}
def cube(origin,size,kind):
 return {"origin":[round(v,3) for v in origin],"size":[round(v,3) for v in size],"uv":{face:uv_for(kind,face) for face in ("north","east","south","west","up","down")}}
bones=[]
def bone(name,pivot,parent=None):
 b={"name":name,"pivot":pivot,"cubes":[]}
 if parent:b["parent"]=parent
 bones.append(b);return b
root=bone("root",[0,0,0])
body=bone("body",[0,14,0],"root")
for origin,size,kind in [
 ([-8.5,9.8,-10.8],[17,9.8,21.3],"body"),
 ([-7,9.2,-9],[14,3,18],"belly"),
 ([-7,10,-12],[14,8.5,5],"body_light"),
 ([7.8,11.5,-5.8],[1,7,13],"body_light"),
 ([-8.8,11.5,-5.8],[1,7,13],"body_light"),
 ([-7.2,18.6,-8],[14.4,1.8,18],"body")]:body["cubes"].append(cube(origin,size,kind))
neck=bone("neck",[0,15,-10],"body");neck["cubes"].append(cube([-5.8,11.2,-16],[11.6,8.1,6.3],"body_light"))
head=bone("head",[0,15.9,-14],"neck")
head["cubes"].extend([
 cube([-6.8,11.2,-25],[13.6,10.3,12],"head"),
 cube([-5.9,12,-29],[11.8,5,6.5],"snout"),
 cube([-5.3,15.9,-28.8],[10.6,1.5,5.3],"snout"),
 cube([-6,11,-24],[12,1.6,5],"belly")])
jaw=bone("jaw",[0,11.2,-23],"head")
jaw["cubes"].extend([cube([-5,9.3,-28.3],[10,2.1,6.7],"jaw"),cube([-5.7,8.7,-28],[11.4,1.5,6.7],"mouth")])
for front in (True,False):
 for sign in (-1,1):
  suffix=("front" if front else "back")+("_left" if sign==1 else "_right")
  z=-7.2 if front else 7.1
  x=(6.4 if front else 6.5)*sign
  b=bone("leg_"+suffix,[x,10,z],"body")
  bx=x-3.1
  b["cubes"].append(cube([bx,2.7,z-2.9],[6.2,10.1,5.8 if front else 6.2],"body"))
  b["cubes"].append(cube([bx-.6,1.3,z-3.8],[7.4,2.9,6.9],"foot"))
  for k in range(3):b["cubes"].append(cube([bx-.65+k*2.5,1.2,z-4.35],[1.8,1.9,1],"claw"))
plates=bone("plates",[0,19,0],"body")
for i in range(5):
 z=-12+i*4.15
 height=[6.2,9.1,11.2,9.1,6.2][i]
 width=[5.5,6.0,6.8,6.0,5.5][i]
 for sign in (-1,1):
  x=sign*3.3-1.1
  zo=z+(0.8 if sign<0 else 0)
  for lvl,(dh,thick,zr,kind) in enumerate([(0,2.25,width,"plate_edge"),(2.1,1.9,width*.75,"plate"),(height-1.9,1.45,width*.4,"plate_tip")]):
   oy=19.8+dh
   sy=(height-2.1 if lvl==1 else 2.4 if lvl==0 else 1.9)
   plates["cubes"].append(cube([x,oy,zo-zr/2],[thick,sy,zr],kind))
tail=bone("tail_base",[0,15,10.5],"body")
tail["cubes"].extend([cube([-4.5,11.9,9.8],[9,5.4,8.2],"body"),cube([-3.1,12.4,17.7],[6.2,4,5.8],"body_light")])
tip=bone("tail_tip",[0,14.5,22.5],"tail_base")
tip["cubes"].append(cube([-2.05,12.9,22.9],[4.1,2.9,4.1],"body"))
for i,(x,y) in enumerate([(2.15,14.3),(-2.15,14.3),(1.7,12.9),(-1.7,12.9)],1):
 b=bone("tail_spike_"+str(i),[x,y,25.5],"tail_tip")
 b["cubes"].append(cube([x-.8,y-.75,25],[1.6,1.5,7.2],"spike"))
bone("saddle_anchor",[0,20.5,-.7],"body")
def write_geometry(path,arr,identifier):
 data={"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":identifier,"texture_width":W,"texture_height":H,"visible_bounds_width":5,"visible_bounds_height":4,"visible_bounds_offset":[0,1.5,0]},"bones":arr}]}
 path.write_text(json.dumps(data,separators=(",",":")),encoding="utf-8")
write_geometry(OUT/"geo/stegosaurus_chibi.geo.json",bones,"geometry.prodigychibi.natural")
with_saddle=json.loads(json.dumps(bones))
sb={"name":"saddle","pivot":[0,20.5,-.7],"parent":"saddle_anchor","cubes":[]}
for origin,size,kind in [
 ([-2.15,25.1,-4.6],[4.3,1.3,8.4],"saddle"),
 ([-2.2,26.3,2.5],[4.4,3.1,1.1],"saddle_trim"),
 ([-2.15,26.1,-4.4],[4.3,1.3,1.0],"saddle_trim"),
 ([2.15,24.8,-4],[1,2.4,7.4],"saddle_trim"),
 ([-3.15,24.8,-4],[1,2.4,7.4],"saddle_trim"),
 ([7.55,16.8,-2.7],[.7,8.5,1],"strap"),
 ([-8.25,16.8,-2.7],[.7,8.5,1],"strap")]:sb["cubes"].append(cube(origin,size,kind))
with_saddle.append(sb)
write_geometry(OUT/"geo/stegosaurus_chibi_saddled.geo.json",with_saddle,"geometry.prodigychibi.saddled")
print("Generated",sum(len(b.get('cubes',[])) for b in bones),"cubes, saddle",len(sb["cubes"]))
