#!/usr/bin/env python3
"""Build editable Blockbench Carnotaurus models + pixel texture variants.
Project: Prodigy Creatures | Minecraft Forge 1.20.1
Generated from custom procedural geometry, not copied assets.
"""
from __future__ import annotations
import base64, io, json, math, os, random, uuid, zipfile
from collections import defaultdict
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np
import cv2

BASE = Path('/mnt/data/Prodigy_Carnotaurus_BBModel')
BASE.mkdir(exist_ok=True)
TEX_SIZE=512
TILE=32
COLS=16
GUID=uuid.UUID('c41b313b-bd8e-42a1-9aea-e478b8382701')
CATS = [
    'skin','skin_light','skin_dark','skin_stripes','skin_scales','skin_shadow','head','head_eye_left',
    'head_eye_right','snout','snout_nose','neck','belly','belly_light','back','spine',
    'tail','tail_stripes','leg','leg_stripes','forearm','foot','claw','horn',
    'horn_tip','mouth','tongue','tooth','jaw','jaw_under','brow','eyelid',
    'skin_side','muzzle_top','spine_top','tail_tip','underside','cheek','pupil','accent',
    'eye_big_left','eye_big_right','skin_black','jaw_side','horn_base','foot_toe','crest','spine_dark'
]
CATINDEX={n:i for i,n in enumerate(CATS)}
assert len(CATS)<16*16

PALETTES={
 'Natural': dict(skin='#a33a2c',light='#d45b3b',dark='#5d2430',stripe='#511d27', belly='#dec4a7',belly_dark='#b38e72',spine='#692b36',spine_light='#e16a40',horn='#f3d2a6',horn_shadow='#ac8a67',eye='#f8ba32',mouth='#4b1c29',tongue='#cf5b62',teeth='#f8edda',accent='#e87643',claw='#f3dfc5'),
 'Magma': dict(skin='#353038',light='#4d3b37',dark='#181b25',stripe='#18151c',belly='#9a8172',belly_dark='#574b50',spine='#3c2828',spine_light='#ff7524',horn='#c4b2a1',horn_shadow='#695f5e',eye='#ffb02d',mouth='#290f1c',tongue='#c54c3e',teeth='#ffe7c7',accent='#ff6321',claw='#ddd1b7'),
 'Glacial': dict(skin='#2f568a',light='#427cba',dark='#172c54',stripe='#1a2e57',belly='#d3e8e9',belly_dark='#8aaec7',spine='#25457b',spine_light='#73dcff',horn='#effafd',horn_shadow='#aacbd5',eye='#68e7ff',mouth='#1c2146',tongue='#8b6ea9',teeth='#ecfafe',accent='#47cfff',claw='#e9faff')
}

def rgb(h):
 h=h.lstrip('#');return tuple(int(h[i:i+2],16) for i in [0,2,4])
def mix(a,b,fac):
 a=rgb(a) if isinstance(a,str) else a
 b=rgb(b) if isinstance(b,str) else b
 return tuple(max(0,min(255,int(a[i]*(1-fac)+b[i]*fac))) for i in range(3))
def clipadd(c,shift):return tuple(max(0,min(255,v+shift)) for v in c)

def tile_img(category,pal,seed):
 r=random.Random(seed)
 im=Image.new('RGB',(32,32),rgb(pal['skin']))
 d=ImageDraw.Draw(im)
 c=category
 base=(pal['skin'] if c in ('skin','skin_scales','skin_side','skin_stripes','skin_light','skin_dark','skin_shadow','cheek','head','head_eye_left','head_eye_right','eye_big_left','eye_big_right','snout','snout_nose','neck','leg','leg_stripes','forearm','foot','brow','eyelid','skin_black')
       else pal['belly'] if c in ('belly','belly_light','jaw_under','underside')
       else pal['spine'] if c in ('back','spine','spine_top','spine_dark','crest')
       else pal['horn'] if c in ('horn','horn_tip','horn_base')
       else pal['claw'] if c in ('claw','foot_toe')
       else pal['teeth'] if c=='tooth'
       else pal['mouth'] if c=='mouth' else pal['tongue'] if c=='tongue'
       else pal['accent'] if c in ('accent','pupil') else pal['skin'] )
 bas=rgb(base)
 if c=='skin_light':bas=mix(bas,pal['light'],.58)
 if c=='skin_dark':bas=mix(bas,pal['dark'],.70)
 if c in ('skin_shadow','skin_black'):bas=mix(bas,pal['dark'],.85)
 if c in ('jaw','jaw_side'): bas=mix(rgb(pal['skin']),rgb(pal['belly']),.38)
 if c=='mouth': bas=rgb(pal['mouth'])
 if c=='tongue':bas=rgb(pal['tongue'])
 for y in range(0,32,4):
  for x in range(0,32,4):
   shift=r.choice([-18,-10,-4,0,0,0,7,13,19])
   if x in [0,28] or y in [0,28]:shift-=8
   d.rectangle((x,y,x+3,y+3),fill=clipadd(bas,shift))
 # tiled geometry edge pixel sheen
 d.line((0,0,31,0),fill=clipadd(bas,24),width=2)
 d.line((0,0,0,31),fill=clipadd(bas,12),width=2)
 d.line((0,31,31,31),fill=clipadd(bas,-24),width=2)
 d.line((31,0,31,31),fill=clipadd(bas,-19),width=2)
 if c in ['skin_stripes','leg_stripes','tail_stripes','skin_side','head','neck','tail']:
  sc=rgb(pal['stripe']);acc=rgb(pal['accent'])
  # zigzag, broken stripes typical of animal skin, rendered at pixel scale
  for xx,yy in [(4,5),(20,14),(3,24)]:
   d.polygon([(xx,yy),(xx+7,yy+1),(xx+12,yy+9),(xx+7,yy+10)],fill=sc)
   d.rectangle((xx+2,yy+1,xx+4,yy+3),fill=mix(sc,acc,.20))
  if pal['skin'].lower() in ['#353038','#2f568a'] and c in ('skin_stripes','leg_stripes','tail_stripes'):
   d.line((3,28,11,20,18,24,28,10), fill=mix(pal['accent'],pal['dark'],.16), width=2)
 if c in ['back','spine','spine_top','spine_dark','crest']:
  d.polygon([(4,27),(10,8),(16,1),(22,7),(27,27)],fill=mix(pal['spine'],pal['spine_light'],.34 if c=='back' else .72))
  d.polygon([(11,24),(16,5),(20,10),(17,23)],fill=mix(pal['spine'],pal['spine_light'],.78))
  if c=='spine_dark': d.rectangle((1,1,6,30),fill=rgb(pal['dark']))
 if c in ('horn','horn_tip','horn_base'):
  d.polygon([(2,30),(17,2),(31,1),(28,12),(12,31)],fill=mix(pal['horn'],pal['horn_shadow'],.18 if c!='horn_tip' else .05))
  d.line((8,29,24,4),fill=rgb(pal['horn']),width=3)
 if c=='snout_nose':
  d.rectangle((7,8,11,13), fill=rgb(pal['dark']))
  d.rectangle((22,9,26,13), fill=rgb(pal['dark']))
 if c in ('head_eye_left','head_eye_right','eye_big_left','eye_big_right'):
  big=c.startswith('eye_big')
  # Characteristic animal eye painted directly into the head texture.
  if big:
   d.rectangle((10,6,24,25),fill=rgb(pal['dark']))
   d.rectangle((12,8,22,23),fill=mix(pal['teeth'],pal['belly'],.17))
   d.rectangle((15,9,22,22),fill=rgb(pal['eye']))
   d.rectangle((18,11,21,20),fill=(15,16,22))
   d.rectangle((17,10,18,12),fill=(255,255,255))
   d.line((9,6,24,6),fill=rgb(pal['stripe']),width=3)
  else:
   d.rectangle((13,9,24,21),fill=rgb(pal['dark']))
   d.rectangle((15,11,23,20),fill=mix(pal['teeth'],pal['belly'],.34))
   d.rectangle((17,12,23,20),fill=rgb(pal['eye']))
   d.rectangle((20,13,23,19),fill=(17,16,20))
   d.rectangle((19,12,20,13),fill=(255,255,255))
   d.line((12,9,24,9),fill=rgb(pal['stripe']),width=3)
 if c=='muzzle_top':
  d.rectangle((7,16,10,18),fill=rgb(pal['dark']))
  d.rectangle((22,16,25,18),fill=rgb(pal['dark']))
 if c=='tongue':
  d.line((8,26,15,6,24,20),fill=mix(pal['tongue'],'#ffffff',.3),width=2)
 if c=='tooth':d.line((5,2,25,25),fill=mix(pal['teeth'],'#ffffff',.25),width=3)
 if c in ('belly','belly_light','underside','jaw_under'):
  for y in range(7,32,9):d.line((0,y,31,y),fill=mix(pal['belly'],pal['belly_dark'],.22),width=2)
 if c=='accent':
  d.line((3,26,26,3),fill=rgb(pal['accent']),width=5)
  d.line((3,26,26,3),fill=mix(pal['accent'],'#ffffff',.4),width=2)
 return im

def texture_atlas(variant):
 pal=PALETTES[variant]
 atlas=Image.new('RGBA',(TEX_SIZE,TEX_SIZE),(0,0,0,0))
 for i,category in enumerate(CATS):
  s=sum(ord(a)* (j+1) for j,a in enumerate(category))+23
  tile=tile_img(category,pal,s)
  atlas.paste(tile,((i%COLS)*TILE,(i//COLS)*TILE))
 # fill blank cells so texture editing is less confusing
 return atlas

class Sculpt:
 def __init__(self,stage):
  self.stage=stage;self.elements=[];self.grp={};self.members=defaultdict(list);self.parents={};self.n=0
  self.newgroup('root', [0,0,0])
  for name,pivot,parent in (
     ('body',[0,24,0] if stage=='adulto' else [0,12,0],'root'),
     ('chest',[0,27,-17] if stage=='adulto' else [0,13,-7],'body'),
     ('neck',[0,32,-25] if stage=='adulto' else [0,17,-14],'chest'),
     ('head',[0,35,-37] if stage=='adulto' else [0,18,-20],'neck'),
     ('jaw',[0,28,-36] if stage=='adulto' else [0,13,-22],'head'),
     ('horns',[0,42,-34] if stage=='adulto' else [0,23,-19],'head'),
     ('tail_1',[0,26,22] if stage=='adulto' else [0,13,10],'body'),
     ('tail_2',[0,24,41] if stage=='adulto' else [0,12,20],'tail_1'),
     ('tail_3',[0,22,56] if stage=='adulto' else [0,11,27],'tail_2'),
     ('leg_left',[-7,23,15] if stage=='adulto' else [-4,8,6],'body'),
     ('leg_right',[7,23,15] if stage=='adulto' else [4,8,6],'body'),
     ('arm_left',[-8,25,-18] if stage=='adulto' else [-6,12,-10],'chest'),
     ('arm_right',[8,25,-18] if stage=='adulto' else [6,12,-10],'chest')):
    self.newgroup(name,pivot,parent)

 def newgroup(self,name,pivot,parent=None):
  uid=str(uuid.uuid5(GUID, f'carnotaurus/{self.stage}/group/{name}'))
  self.grp[name]={'name':name,'uuid':uid,'origin':pivot,'rotation':[0,0,0], 'visibility':True, 'locked':False,'export':True}
  self.parents[name]=parent
  return uid

 def cube(self,name,xyz,group,kind='skin',faces=None,rotation=None,origin=None, shade=True):
  self.n+=1
  a,b=xyz
  ax,ay,az=a;bx,by,bz=b
  if min(bx-ax,by-ay,bz-az)<=0:raise ValueError(f'Bad cube {name}: {xyz}')
  unique=str(uuid.uuid5(GUID,f'carnotaurus/{self.stage}/cube/{self.n:03}/{name}'))
  t=faces or {}
  fs={}
  for face in ['north','east','south','west','up','down']:
   kindface=t.get(face,kind)
   idx=CATINDEX[kindface]
   xx=(idx%COLS)*TILE; yy=(idx//COLS)*TILE
   fs[face]={'uv':[xx,yy,xx+TILE,yy+TILE],'texture':'__TEXTURE_UUID__'}
  e={'name':name,'type':'cube','uuid':unique,
     'from':list(a),'to':list(b),'origin':origin or [(ax+bx)/2,(ay+by)/2,(az+bz)/2],
     'rotation':rotation or [0,0,0],'faces':fs,'box_uv':False,'shade':shade, 'visibility':True}
  self.elements.append(e);self.members[group].append(unique)
  return e
 def sym(self,name,xrange_,yr,zr,group,kind='skin',**kwargs):
  # mirrored cubes about X=0
  a,b=xrange_;y0,y1=yr;z0,z1=zr
  self.cube(f'{name}_L',[[a,y0,z0],[b,y1,z1]],group+'_left',kind,**kwargs)
  self.cube(f'{name}_R',[[-b,y0,z0],[-a,y1,z1]],group+'_right',kind,**kwargs)

 def build(self):
  if self.stage=='adulto': self.adulto()
  else:self.chibi()
  return self
 def adulto(self):
  C=self.cube
  C('torso_principal',[[-9,20,-17],[9,35,19]],'body','skin_stripes',{'up':'skin_scales','down':'belly','north':'skin_side','south':'skin_stripes'})
  C('torso_capa_superior',[[-8,34,-15],[8,37,18]],'body','back')
  C('torso_vientre',[[-8,17,-15],[8,22,19]],'body','belly')
  C('cadera_trasera',[[-9.5,19,12],[9.5,34,28]],'body','skin_stripes',{'up':'back'})
  C('vientre_trasero',[[-7.3,16,14],[7.3,22,26]],'body','belly')
  C('banda_cadera',[[-8,29,17],[8,33,25]],'body','skin_dark')
  C('pecho',[[-8.5,21,-25],[8.5,35,-13]],'chest','skin_scales',{'north':'skin_light','down':'belly'})
  C('pecho_costa',[[-6,20,-29],[6,29,-22]],'chest','belly')
  C('base_cuello',[[-6.8,25,-33],[6.8,36,-22]],'neck','skin_stripes',{'north':'neck','down':'belly'})
  C('cogote',[[-6.2,32,-31],[6.2,39,-24]],'neck','neck')
  C('cuello_ventral',[[-5.8,22.8,-32],[5.8,28,-23]],'neck','belly')
  C('craneo',[[-6.3,31,-42],[6.3,42,-30]],'head','head',{'east':'head_eye_right','west':'head_eye_left','up':'skin_scales'})
  C('morro_superior',[[-5.7,31.5,-49],[5.7,38.5,-40]],'head','snout',{'north':'snout_nose','up':'muzzle_top','east':'snout','west':'snout'})
  C('morro_puente',[[-5.4,37.5,-45],[5.4,40,-37]],'head','snout')
  C('nariz',[[-4.9,34,-50],[4.9,37.7,-46]],'head','snout_nose')
  C('mejilla_izq',[[-6.4,29.5,-38],[-4.2,34,-31]],'head','cheek')
  C('mejilla_der',[[4.2,29.5,-38],[6.4,34,-31]],'head','cheek')
  C('ceja_izquierda',[[-7,39.5,-42],[-3.8,42.7,-33]],'head','brow')
  C('ceja_derecha',[[3.8,39.5,-42],[7,42.7,-33]],'head','brow')
  C('cavidad_boca',[[-4.8,28.4,-47],[4.8,31.5,-36]],'head','mouth')
  C('mandibula_inferior',[[-5.3,25,-48],[5.3,28.5,-35]],'jaw','jaw',{'north':'jaw_side','down':'jaw_under','up':'mouth'})
  C('barbilla',[[-5.2,24.5,-45],[5.2,27.5,-39]],'jaw','jaw_under')
  C('lengua',[[-2.6,28.5,-46],[2.6,29.2,-38]],'jaw','tongue')
  for side in [-1,1]:
   for i in range(5):
    z=-47+i*2.35
    x=4.6 if side>0 else -5.6
    C(f'colmillo_superior_{side}_{i}',[[x,29.2,z],[x+1.0,31.5,z+0.9]],'head','tooth')
   for i in range(4):
    z=-45+i*2.5;x=4.4 if side>0 else -5.5
    C(f'colmillo_inferior_{side}_{i}',[[x,28.2,z],[x+1.1,30.4,z+1.0]],'jaw','tooth')
  for side in [-1,1]:
   xc=3.75*side
   a,b=sorted([xc-2*side,xc+2*side])
   C(f'cuerno_base_{side}',[[a,41.3,-37.5],[b,44.5,-32]],'horns','horn_base')
   C(f'cuerno_alto_{side}',[[a+.45,44.1,-37.2],[b-.45,48.4,-33.3]],'horns','horn',rotation=[-14*side,0,15*side],origin=[xc,42.5,-35])
   C(f'punta_cuerno_{side}',[[xc-1.1,48,-35.9],[xc+1.1,51.6,-34.1]],'horns','horn_tip',rotation=[-14*side,0,15*side],origin=[xc,44,-35])
  # Legs articulated by separate parent groups for animation
  for side in [-1,1]:
   g='leg_left' if side==-1 else 'leg_right'
   x=(-11,-5) if side==-1 else (5,11)
   a,b=x
   C(f'muslo_{side}',[[a-0.7,14.3,9],[b+0.7,28,22]],g,'leg_stripes')
   C(f'rodilla_{side}',[[a,11.5,10],[b,19,19]],g,'leg')
   C(f'pantorrilla_{side}',[[a+0.3,3.5,8],[b-0.3,13,15.5]],g,'leg_stripes')
   C(f'tobillo_{side}',[[a+0.5,2.4,6],[b-0.5,7.5,14]],g,'skin_dark')
   C(f'pie_{side}',[[a-0.5,0,0],[b+0.5,4.2,13]],g,'foot',{'up':'leg','north':'foot_toe'})
   for j in range(3):
    px=a+(j+.5)*(b-a)/3
    C(f'garra_trasera_{side}_{j}',[[px-0.8,0.4,-2.3],[px+0.8,3.0,1.5]],g,'claw')
   C(f'escama_cadera_{side}',[[a-0.7,24,10],[b+0.7,29.8,19]],g,'skin_dark')
   C(f'talón_{side}',[[a+.3,1.0,12],[b-.3,6.5,16]],g,'skin_shadow')
  for side in [-1,1]:
   g='arm_left' if side==-1 else 'arm_right'
   a,b=(-10.4,-7.8) if side==-1 else (7.8,10.4)
   C(f'hombro_brazo_{side}',[[a-1.2,21,-23],[b+1.2,29,-15]],g,'skin_stripes')
   C(f'brazo_corto_{side}',[[a,17,-26],[b,22.6,-21]],g,'forearm',rotation=[-15,0,0])
   C(f'manita_{side}',[[a+.15,16,-27.4],[b-.15,18.2,-23.5]],g,'skin_dark')
   for j in range(2):
    px=a+.4+j*1.5
    C(f'unita_{side}_{j}',[[px,15.5,-28.7],[px+.7,17,-26]],g,'claw')
  # Tail: segments taper smoothly and have ridges
  tailpieces=[('cola_base',[[-7,22,25],[7,31,41]],'tail_1','tail_stripes'),
     ('cola_media',[[-5.5,22,40],[5.5,29,54]],'tail_2','tail_stripes'),
     ('cola_fina',[[-4,20.8,53],[4,26,65]],'tail_3','tail'),
     ('cola_extremo',[[-2.8,20,64],[2.8,24.2,74]],'tail_3','tail_stripes'),
     ('cola_punta',[[-1.6,20.5,73],[1.6,22.8,80]],'tail_3','tail_tip')]
  for n,v,g,k in tailpieces:C(n,v,g,k,{'down':'underside','up':'back'})
  for i,z in enumerate(range(-14,33,5)):
   height=3.6 if i%3==0 else 2.8
   C(f'escama_dorsal_{i}',[[-1.6,36 if z<=12 else 32.2,z],[1.6,36+height if z<=12 else 32.2+height,z+2.9]],'body' if z<=20 else 'tail_1','spine',{'east':'spine_top','west':'spine_top'})
  for i,z in enumerate(range(34,73,7)):
   y=30.2 if z<43 else 27.6 if z<55 else 24.9
   C(f'escama_cola_{i}',[[-1.0,y,z],[1.0,y+2.0,z+2.1]],'tail_1' if z<40 else 'tail_2' if z<55 else 'tail_3','spine')
  # skin seams side layer blocks around tail & torso
  for side in [-1,1]:
   x1,x2=(-9.9,-9.25) if side==-1 else (9.25,9.9)
   for i,z in enumerate([-11,-3,6,16]):
    C(f'banda_costado_{side}_{i}',[[x1,27+(i%2),z],[x2,32,z+3.4]],'body','skin_dark')

 def chibi(self):
  C=self.cube
  C('cuerpo_redondo',[[-6.5,8,-9],[6.5,18,10]],'body','skin_stripes',{'up':'back','down':'belly'})
  C('barriguita',[[-6.0,6.5,-8],[6.0,10.5,9]],'body','belly')
  C('lomo',[[-5.5,17,-7],[5.5,19.5,10]],'body','back')
  C('cadera',[[-5.8,7,6],[5.8,16,14]],'body','skin_stripes')
  C('pecho_bebe',[[-5.8,8,-13],[5.8,18,-6]],'chest','skin_light',{'down':'belly'})
  C('cuellito',[[-5.7,11,-17],[5.7,19.5,-11]],'neck','neck',{'down':'belly'})
  C('cabeza_grande',[[-6.5,12.7,-24],[6.5,24,-13]],'head','head',{'east':'eye_big_right','west':'eye_big_left','up':'skin_scales'})
  C('morro_cuadrado',[[-5.7,13,-29],[5.7,19.8,-21]],'head','snout',{'north':'snout_nose','up':'muzzle_top'})
  C('puente_nariz',[[-5.2,18.8,-27],[5.2,21.3,-20]],'head','snout')
  C('carrillo_izq',[[-6.55,12.2,-22],[-4.8,16.5,-15]],'head','cheek')
  C('carrillo_der',[[4.8,12.2,-22],[6.55,16.5,-15]],'head','cheek')
  C('boca_bebé',[[-4.5,11.6,-27],[4.5,13.4,-19]],'head','mouth')
  C('mandibula_bebe',[[-5.2,10.2,-27.8],[5.2,12.4,-19]],'jaw','jaw',{'down':'jaw_under','up':'mouth'})
  C('menton_bebe',[[-4.6,9.8,-26],[4.6,10.9,-22]],'jaw','belly')
  C('lengüita',[[-2,12.5,-26],[2,13.0,-21]],'jaw','tongue')
  for side in [-1,1]:
   g='horns'
   xc=3.8*side
   a,b=sorted([xc-1.8,xc+1.8])
   C(f'cuerno_cortito_{side}',[[a,23,-20.2],[b,26.4,-16.6]],g,'horn_base',rotation=[0,0,14*side],origin=[xc,23,-18])
   C(f'punta_bebe_{side}',[[xc-.8,26.1,-19.4],[xc+.8,28.4,-17.7]],g,'horn_tip',rotation=[0,0,14*side],origin=[xc,23,-18])
  for side in [-1,1]:
   g='leg_left' if side==-1 else 'leg_right'
   a,b=(-7.2,-2.2) if side==-1 else (2.2,7.2)
   C(f'muslo_cortito_{side}',[[a,4,3],[b,12.0,10.3]],g,'leg_stripes')
   C(f'pie_gordito_{side}',[[a-.6,0,-1],[b+.6,4.7,11.4]],g,'foot')
   for j in range(3):
    x=a+j*(b-a)/3+.4
    C(f'uña_bebe_{side}_{j}',[[x,0.2,-2.6],[x+.92,2.5,1]],g,'claw')
   g='arm_left' if side==-1 else 'arm_right'
   a,b=(-7.5,-5.7) if side==-1 else (5.7,7.5)
   C(f'brazo_mini_{side}',[[a,8,-13],[b,13,-9]],g,'forearm')
   C(f'manita_mini_{side}',[[a,7.1,-15],[b,9.3,-12]],g,'skin_dark')
   for j in range(2):
    x=a+j*.95
    C(f'garrita_{side}_{j}',[[x,7,-16],[x+.5,8.3,-14.5]],g,'claw')
  for n,coord,grp,kind in [
   ('colita_base',[[-4.7,10,10],[4.7,16,18]],'tail_1','tail_stripes'),
   ('colita_media',[[-3.2,10.5,18],[3.2,14,25]],'tail_2','tail_stripes'),
   ('colita_punta',[[-1.3,10.4,24],[1.3,12.6,31]],'tail_3','tail_tip')]:
   C(n,coord,grp,kind,{'down':'underside'})
  for i,z in enumerate([-6,-2,2,6,10,14,18,23]):
   y=18.6 if z<10 else 15.8 if z<17 else 13.8
   size=2.2 if i<4 else 1.6
   C(f'espinita_{i}',[[-1.15,y,z],[1.15,y+size,z+2.0]],'body' if z<10 else 'tail_1' if z<18 else 'tail_2','spine')
  for i,z in enumerate([-7,0,7]):
   C(f'marca_lateral_izq_{i}',[[-6.85,12,z],[-6.48,15,z+2]],'body','skin_dark')
   C(f'marca_lateral_der_{i}',[[6.48,12,z],[6.85,15,z+2]],'body','skin_dark')

 def make_bbmodel(self,variant,atlas):
  tex_uuid=str(uuid.uuid5(GUID, f'carnotaurus/texture/{variant}'))
  buf=io.BytesIO();atlas.save(buf,format='PNG',optimize=True)
  b64=base64.b64encode(buf.getvalue()).decode('ascii')
  elems=json.loads(json.dumps(self.elements))
  for e in elems:
   for f in e['faces'].values():f['texture']=tex_uuid
  def outliner_node(name):
   u=self.grp[name]['uuid']
   childs=[outliner_node(n) for n,par in self.parents.items() if par==name]
   childs.extend(self.members[name])
   return {'uuid':u,'isOpen':True,'children':childs}
  return {
   'meta':{'format_version':'5.0','model_format':'free','box_uv':False},
   'name':f'Carnotauro_{self.stage}_{variant}',
   'resolution':{'width':TEX_SIZE,'height':TEX_SIZE},
   'elements':elems,
   'groups':list(self.grp.values()),
   'outliner':[outliner_node('root')],
   'textures':[{'name':f'carnotauro_{self.stage}_{variant.lower()}.png',
                'id':'0','uuid':tex_uuid,'particle':False,'visible':True,
                'mode':'bitmap','uv_width':TEX_SIZE,'uv_height':TEX_SIZE,
                'source':'data:image/png;base64,'+b64}],
   'credit':'Prodigy Creatures - Carnotauro, Minecraft Forge 1.20.1',
   'model_identifier':f'carnotauro_{self.stage.lower()}'
   }

# Perspective-free ortho projected renders, with actual pixel texture tiles
# draw faces painter-sorted; final images for shape validation and sharing.
FACES={
 'north':(0,1,2,3),'south':(4,5,6,7),'west':(0,4,7,3),
 'east':(1,5,6,2),'up':(3,2,6,7),'down':(0,1,5,4)}

def cube_corners(e):
 a=e['from'];b=e['to']
 pt=np.array([[a[0],a[1],a[2]], [b[0],a[1],a[2]], [b[0],b[1],a[2]], [a[0],b[1],a[2]],
              [a[0],a[1],b[2]], [b[0],a[1],b[2]], [b[0],b[1],b[2]], [a[0],b[1],b[2]]],dtype=float)
 if e.get('rotation') and any(e['rotation']):
  ax,ay,az=np.radians(e['rotation']);cx,sx=np.cos(ax),np.sin(ax);cy,sy=np.cos(ay),np.sin(ay);cz,sz=np.cos(az),np.sin(az)
  rx=np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]])
  ry=np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])
  rz=np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])
  pt=(pt-np.array(e['origin']))@(rz@ry@rx).T+np.array(e['origin'])
 return pt

def render_model(sculpt,atlas,view='34',size=(1280,800)):
 # camera toward model from front-left. x camera positive, z negative.
 direction = np.array([1.25,0.73,-1.65],dtype=float)
 if view=='side':direction=np.array([2.0,.55,-.30]);
 if view=='front':direction=np.array([.03,.5,-2.0]);
 direction/=np.linalg.norm(direction)
 forward=-direction; right=np.cross(forward,[0,1,0]);right/=np.linalg.norm(right)
 up=np.cross(right,forward);up/=np.linalg.norm(up)
 P=np.stack([right,up],axis=0)
 allverts=np.concatenate([cube_corners(e) for e in sculpt.elements]);
 p2d=allverts@P.T
 minx,miny=p2d.min(axis=0);maxx,maxy=p2d.max(axis=0)
 W,H=size
 s=min((W-120)/(maxx-minx),(H-110)/(maxy-miny))
 ox=W/2-(minx+maxx)/2*s
 oy=H/2+(miny+maxy)/2*s
 bg=Image.new('RGBA',size,(20,23,31,255))
 draw=ImageDraw.Draw(bg)
 # horizontal subtle texture / soft background vignette
 for i in range(0,1280,32):
  draw.line([(i,0),(i,H)],fill=(31,33,41,255),width=1)
 faces=[]
 for e in sculpt.elements:
  pt=cube_corners(e)
  for name,ixs in FACES.items():
   v=pt[list(ixs)]
   norm=np.cross(v[1]-v[0],v[2]-v[0]);nlen=np.linalg.norm(norm)
   if nlen<.01:continue
   norm/=nlen
   if norm@direction>=-0.001:continue
   pp=v@P.T
   poly=np.array([(p[0]*s+ox,-p[1]*s+oy) for p in pp])
   avgdepth=v.mean(axis=0)@direction
   UV=e['faces'][name]['uv'];txy=[int(UV[0]),int(UV[1])]
   c=atlas.crop((txy[0],txy[1],txy[0]+32,txy[1]+32))
   shade=1 if name=='up' else .87 if name in ('north','south') else .73
   faces.append((avgdepth,poly,c,shade))
 faces.sort(key=lambda t:t[0]) # far first
 canvas=np.array(bg.convert('RGB'))
 for _,poly,tex,shade in faces:
  # homography projects texture into quad of cube face
  if max(poly[:,0])<0 or min(poly[:,0])>=W or max(poly[:,1])<0 or min(poly[:,1])>=H:continue
  x0=max(0,int(np.floor(np.min(poly[:,0])))-2)
  y0=max(0,int(np.floor(np.min(poly[:,1])))-2)
  x1=min(W,int(np.ceil(np.max(poly[:,0])))+3)
  y1=min(H,int(np.ceil(np.max(poly[:,1])))+3)
  if x0>=x1 or y0>=y1:continue
  src=np.float32([[0,0],[31,0],[31,31],[0,31]])
  dst=np.float32(poly-np.array([x0,y0]))
  M=cv2.getPerspectiveTransform(src,dst)
  rgba=np.array(tex.convert('RGB'))
  rgba=np.clip(rgba.astype(np.float32)*shade,0,255).astype('uint8')
  patchW=x1-x0;patchH=y1-y0
  warped=cv2.warpPerspective(rgba,M,(patchW,patchH),flags=cv2.INTER_NEAREST,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
  wmask=cv2.warpPerspective(np.full((32,32),255,dtype='uint8'),M,(patchW,patchH),flags=cv2.INTER_NEAREST)
  sub=canvas[y0:y1,x0:x1]
  sub[wmask>0]=warped[wmask>0]

 bg=Image.fromarray(canvas)
 d=ImageDraw.Draw(bg)
 font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',32)
 d.text((40,24),f'CARNOTAURO  /  {sculpt.stage.upper()}  /  {view}',font=font,fill=(234,206,138))
 return bg

def validate_project(project):
 assert project['meta']['format_version']=='5.0'
 cubes=project['elements'];groups=project['groups']
 elem_uids=[c['uuid'] for c in cubes];group_uids=[g['uuid'] for g in groups]
 assert len(elem_uids)==len(set(elem_uids));assert len(group_uids)==len(set(group_uids))
 all_uids=set(elem_uids)|set(group_uids)
 seen=set()
 def visit(x):
  if isinstance(x,str):
   assert x in set(elem_uids), x
   seen.add(x)
  else:
   assert x['uuid'] in group_uids
   for child in x['children']:visit(child)
 for out in project['outliner']:visit(out)
 assert len(seen)==len(elem_uids),(len(seen),len(elem_uids))
 for e in cubes:
  assert len(e['faces'])==6
  assert all(e['from'][i]<e['to'][i] for i in range(3))
 for t in project['textures']:
  base64.b64decode(t['source'].split(',')[1],validate=True)
 assert len(project['elements'])>=50
 return True

def run():
 summary={}
 for stage in ['adulto','chibi']:
  s=Sculpt(stage).build()
  summary[stage]={'cubes':len(s.elements),'bones':len(s.grp)}
  for variant in ['Natural','Magma','Glacial']:
   atlas=texture_atlas(variant)
   n=f'Carnotauro_{stage}_{variant}'
   atlas.save(BASE/(n+'.png'))
   model=s.make_bbmodel(variant,atlas)
   validate_project(model)
   (BASE/(n+'.bbmodel')).write_text(json.dumps(model,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
   preview=render_model(s,atlas,view='34',size=(1100,710))
   preview.save(BASE/('preview_'+n+'.png'))
   print(n, len(s.elements),'cubes',len(s.grp),'bones',round((BASE/(n+'.bbmodel')).stat().st_size/1024),'KB')
 # mosaic contact sheet
 canvas=Image.new('RGB',(1620,780),(20,23,31));d=ImageDraw.Draw(canvas)
 for i,stage in enumerate(['adulto','chibi']):
  for j,variant in enumerate(['Natural','Magma','Glacial']):
   p=Image.open(BASE/f'preview_Carnotauro_{stage}_{variant}.png').convert('RGB')
   p.thumbnail((532,648))
   x=j*540;y=i*390
   canvas.paste(p,(x,y+17))
   d.rectangle((x,y+17,x+536,y+374),outline=(172,126,50),width=3)
 canvas.save(BASE/'VISTA_PREVIA_SEIS_MODELOS.png')
 # Additional orthogonal inspection renders for modeling reference
 for stage in ['adulto','chibi']:
  sculpt=Sculpt(stage).build(); tex=texture_atlas('Natural')
  ortho=Image.new('RGB',(1530,440),(19,22,31))
  for i,view in enumerate(['34','side','front']):
   picture=render_model(sculpt,tex,view=view,size=(980,700))
   picture.thumbnail((506,426))
   ortho.paste(picture,(i*510,6))
  ortho.save(BASE/f'VISTAS_{stage.upper()}_NATURAL.png')
 readme='''PRODIGY CREATURES — CARNOTAURO (Forge 1.20.1)
===================================================

CONTENIDO
- 6 proyectos editables .bbmodel (adulto + chibi, cada uno Natural, Magma y Glacial)
- 6 atlas de textura en .png (512x512, 32 pixeles por bloque de material)
- 6 vistas previas isométricas y 1 mosaico comparativo
- build_carnotaurus.py (script generador, por si quieres cambiar geometría/colores)

USAR EN BLOCKBENCH
1. Abre Blockbench > File > Open Model (Archivo > Abrir modelo).
2. Selecciona Carnotauro_adulto_Natural.bbmodel o Carnotauro_chibi_Natural.bbmodel.
3. El modelo abrirá con textura EMBEBIDA (también disponible como PNG separado).
4. Los grupos son: root, body, chest, neck, head, jaw, horns, tail_1/2/3, leg_left/right, arm_left/right.
5. Cada variante usa la MISMA estructura; solo cambian las texturas.
6. Los modelos están construidos con cubos; no requieren GeckoLib por su forma.

IMPORTANTE: Estos son archivos fuente de Blockbench, no un .jar compilado.
Para integrarlos al mod Forge 1.20.1 que usa modelos Java nativos, todavía
hay que convertir/exportar la geometría al tipo de entidad del proyecto,
crear la clase de modelo/renderizado y enlazar la textura adecuada.
Las animaciones (caminar, correr, atacar, sentarse, dormir) NO están
implementadas en esta entrega; hay huesos/grupos separados para trabajarlas.
No se ha probado el proyecto dentro de Minecraft ni abierto en Blockbench
(conversión visual pre-renderizada y validación JSON realizadas aquí).

ANATOMÍA
Adulto: cráneo cuadrado con ojos texturizados, dos cuernos marfil,
mandíbula visible, dientes y lengua, torso con bandas oscuras, brazos
pequeños, patas musculosas, uñas marfil, espinas dorsales bajas y cola cónica.
Chibi: cabeza más grande, ojos grandes en textura, cuernos cortos,
cuerpo compacto, patas cortas y cola más corta.

El nombre de paquete y los recursos finales puedes integrarlos bajo:
assets/prodigycreatures/textures/entity/carnotaurus_...

Todos los archivos fueron creados como diseño original para este proyecto.
'''
 (BASE/'LEEME_Instalacion.txt').write_text(readme,encoding='utf8')
 script=Path(__file__).read_text(encoding='utf8')
 (BASE/'build_carnotaurus.py').write_text(script,encoding='utf8')
 zipname=Path('/mnt/data/Prodigy_Carnotaurus_Adulto_Chibi_BBMODEL.zip')
 with zipfile.ZipFile(zipname,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=7) as z:
  for f in sorted(BASE.iterdir()):
   if f.suffix in ['.bbmodel','.png','.txt','.py']:
    z.write(f, arcname='Prodigy_Carnotaurus/'+f.name)
 print('SUMMARY',summary,'ZIP',zipname, round(zipname.stat().st_size/1024/1024,2),'MB')

if __name__=='__main__':run()
