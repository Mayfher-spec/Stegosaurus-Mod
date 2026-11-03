#!/usr/bin/env python3
"""Generate original Blockbench/GeckoLib Bedrock geo and animation files for Stegosaurus for ROR.
    All dimensions Minecraft model pixels (16 px = 1 block); no ROR geometry or assets copied.
"""
from pathlib import Path
import json,math
R=Path(__file__).resolve().parent
OUT=R/'src/main/resources/assets/prodigycreatures'
(OUT/'geo').mkdir(parents=True,exist_ok=True)
(OUT/'animations').mkdir(parents=True,exist_ok=True)
mat={'skin':0,'armor':1,'shadow':2,'belly':3,'ridge':4,'rim':5,'core':6,'glow':7,'accent':8,'light':9,'bone':10,'eye':11,'scar':12,'vein':13,'muzzle':14,'joint':15}
bones=[]
info={}

def bone(name,parent=None,at=(0,0,0),rotation=None):
    # at: Java ModelPart global coordinates (x, y down from top, z). 24 = grounded floor.
    o={'name':name,'pivot':[round(at[0],4), round(24-at[1],4),round(at[2],4)],'cubes':[]}
    if parent:o['parent']=parent
    if rotation:o['rotation']=[round(math.degrees(k),3) for k in rotation]
    bones.append(o);info[name]=(o,at);return name

def box(b,material,x,y,z,w,h,d):
    o,p=info[b]
    # Cube's origin = minimum coordinate in Bedrock world space with Y upward.
    pos=[round(p[0]+x,4),round(24-(p[1]+y+h),4),round(p[2]+z,4)]
    idx=mat[material] if isinstance(material,str) else material
    o['cubes'].append({'origin':pos,'size':[round(w,4),round(h,4),round(d,4)],'uv':[(idx%4)*256+12,(idx//4)*256+12]})

def plate(parent,name,x,y,z,height,depth,side=1,steps=8):
    at0=info[parent][1];at=(at0[0]+x,at0[1]+y,at0[2]+z)
    child=bone(name,parent,at,rotation=(side*.06,side*.06,side*.04))
    profile=[.51,.79,1.,.98,.86,.65,.44,.20]
    height=float(height);tier=height/steps
    box(child,'ridge',-1.9,-.65,-depth*.33,3.8,2.3,depth*.66)
    for n in range(steps):
        wid=depth*profile[n]
        yy=-(n+1)*tier
        thick=2.9-n*.1
        box(child,'rim',-thick/2,yy,-wid/2,thick,tier+.15,wid)
        if n<7 and wid>2:
            for s in [-1,1]:
                xx=thick/2-.05 if s>0 else -thick/2-.35
                box(child,'glow' if n>4 else 'core',xx,yy+.22,-wid*.37,.4,max(.28,tier-.45),wid*.74)
        if n in (2,4,6):
            for s in [-1,1]:
                xx=thick/2+.28 if s>0 else -thick/2-.49
                box(child,'light',xx,yy+.3,-wid*.19,.19,max(.3,tier*.29),wid*.38)
    return child

bone('root',at=(0,24,0))
bone('torso','root',(0,11,0))
for material,x,y,z,w,h,d in [
('skin',-13.5,-9,-25,27,18,50),('skin',-12,-11,-20,24,4,41),('belly',-11,3,-25,22,6,49),
('belly',-9,6,-20,18,4,43),('skin',-14,-9,-29,28,18,20),('armor',-13,-11,-26,26,5,15),
('skin',-15,-10,12,30,20,22),('armor',-13.5,-12,16,27,6,17),('ridge',-11,-12,-15,22,2.8,36),
('skin',-11,-6,26,22,13,13),('skin',-12,-6,-35,24,13,14)]:box('torso',material,x,y,z,w,h,d)
for side in [-1,1]:
    sx=side*13.7
    for i in range(6):
        z=-23+i*8.6;y=-5+(i%2)*1.1
        box('torso','armor',sx-(1.3 if side<0 else 0),y,z,1.3,7.8,7.2)
        box('torso','accent',sx-(1.52 if side<0 else -.25),y+1,z+1.5,.5,1.5,4.7)
    sx2=15.8 if side>0 else -16.45
    for (y,z,h,d) in [(-4,11,2.8,9),(-4,11,9,2.8),(2,15,2.5,6)]:box('torso','accent',sx2,y,z,.65,h,d)
    box('torso','ridge',13.2 if side>0 else -15,-9,-24,1.8,12,12)
    box('torso','armor',14 if side>0 else -16,-6,17,2,12,13)
    edge=15.1 if side>0 else -15.45
    for k in range(4):box('torso','vein',edge,-6+k*1.65,17+k*3.3,.35,.9,5.7)
    box('torso','scar',edge,-1,22,.45,1.4,7.5)
    box('torso','vein',14.9 if side>0 else -15.2,-3,-22,.40,1.15,11)
for i,height in enumerate([7,10,14,18,23,28,30,29,25,20,15,10]):
    z=-34+i*6.5;span=4.4 if i<3 or i>9 else 5.6
    for side in [-1,1]:plate('torso',f'plate_{i}_{"l" if side<0 else "r"}',side*span,-11.2,z+side*1.45,height,13.6 if 2<i<10 else 9,side)
for i in range(5):plate('torso',f'neck_crest_{i}',0,-10,-42+i*3.2,4+i*2,4.5,1)

bone('neck','root',(0,11,-30))
for m,x,y,z,w,h,d in [('skin',-9,-6,-13,18,13,21),('skin',-7,-6,-25,14,11,20),('belly',-6,2,-26,12,5,26),('armor',-7.2,-6,-20,14.4,2.5,19)]:box('neck',m,x,y,z,w,h,d)
for i in range(4):box('neck','ridge',-5,-7.2,-25+i*5,10,2,4)
bone('head','neck',(0,13,-54))
for m,x,y,z,w,h,d in [('muzzle',-5.8,-6,-14,11.6,10.5,18),('armor',-6,-7,-12,12,3.4,11),('belly',-5.5,2.5,-22,11,3.8,24),('skin',-6.1,-3,-4,12.2,8,8),('muzzle',-4.3,-2.9,-24.5,8.6,5,11),('bone',-4.1,0,-25.8,8.2,2,3),('armor',-4.8,2.5,-22,9.6,1.3,17),('armor',-5.7,-7.3,-16.5,11.4,2,6)]:box('head',m,x,y,z,w,h,d)
for side in [-1,1]:
    x=5.55 if side>0 else -6.45
    for m,xx,yy,zz,w,h,d in [('shadow',x,-4.9,-12,.9,3.4,5),('eye',x+(.35 if side>0 else -.35),-4.15,-10.7,.45,1.8,2.6),('armor',x,-1.4,-8,.9,3.5,8),('shadow',4.2 if side>0 else -5,-2.3,-22,.8,1.3,2.1),('accent',x,2.3,-15,.6,.9,8.5)]:box('head',m,xx,yy,zz,w,h,d)

bone('tail','root',(0,11,31))
for m,x,y,z,w,h,d in [('skin',-10,-7,-7,20,14,20),('armor',-8,-8,6,16,4.5,18),('belly',-8,3,-5,16,5,18),('skin',-8,-6,11,16,12,20)]:box('tail',m,x,y,z,w,h,d)
for i in range(4):plate('tail',f'tail_plate_{i}',0,-8.5,7+i*5.7,7-i,5.8,1)
bone('tail_mid','tail',(0,11,58))
for m,x,y,z,w,h,d in [('skin',-6,-5,-3,12,10,25),('armor',-5,-6,2,10,2.8,23),('skin',-4.4,-4.2,20,8.8,8,15)]:box('tail_mid',m,x,y,z,w,h,d)
for i in range(6):
    rad=4.9-i*.42
    box('tail_mid','ridge',-rad,-6+i*.28,2+i*5,rad*2,2.1,3.1)
bone('tail_tip','tail_mid',(0,11,89))
for m,x,y,z,w,h,d in [('skin',-3.7,-3.7,-4,7.4,7.4,24),('armor',-2.8,-2.8,15,5.6,5.6,18)]:box('tail_tip',m,x,y,z,w,h,d)
for i in range(4):
    side=-1 if i%2==0 else 1
    z=15 if i<2 else 25
    spike=bone(f'spike_{i}','tail_tip',(side*2,10,89+z),rotation=(-.13,side*.96,side*.18))
    for m,x,y,z,w,h,d in [('joint',-2.3,-2.3,-1,4.6,4.6,5),('bone',-2,-2,2,4,4,11),('bone',-1.6,-1.6,12,3.2,3.2,9),('bone',-1,-1,21,2,2,8),('light',-.5,-.5,29,1,1,4)]:box(spike,m,x,y,z,w,h,d)
for id,side,z,front in [(0,-1,-20,True),(1,1,-20,True),(2,-1,22,False),(3,1,22,False)]:
    t=5.9 if front else 7; x=side*(12 if front else 13.5)
    leg=bone(f'leg_{id}','root',(x,7.5,z))
    for m,xx,y,zz,w,h,d in [('ridge',-t-1,-4,-t-1,2*t+2,8,2*t+2),('skin',-t,-1,-t,2*t,10,2*t),('armor',-t+1,-3,-t+1,2*t-2,4,2*t-2),('skin',-t+1,7,-t+1,2*t-2,8.4,2*t-2),('armor',-t,12,-t,2*t,3.8,2*t),('skin',-t-1,15,-t-3,2*t+2,2,2*t+5),('ridge',t-.25 if side>0 else -t-1.4,2,-t+1,1.6,8.4,2*t-2),('accent',t+1.05 if side>0 else -t-1.65,3.5,-t+2,.65,2.1,2*t-5)]:box(leg,m,xx,y,zz,w,h,d)
    for k in range(3):
        footX=(k-1)*(t*.59)-1.6
        box(leg,'bone',footX,15,-t-6.4,3.2,2.4,6.3)
        box(leg,'bone',footX+.48,15.3,-t-8.4,2.15,1.9,3.2)

geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.prodigycreatures.stegosaurus','texture_width':1024,'texture_height':1024,'visible_bounds_width':12,'visible_bounds_height':5.5,'visible_bounds_offset':[0,2,0]},'bones':bones}]}
(OUT/'geo/stegosaurus.geo.json').write_text(json.dumps(geo,indent=2,ensure_ascii=False)+'\n')

def rotation(keys):return {'rotation':{str(k):list(v) for k,v in keys}}
def pos(keys):return {'position':{str(k):list(v) for k,v in keys}}
idle={'animation_length':4.0,'loop':True,'bones':{
  'torso':pos([(0,(0,0,0)),(2,(0,.65,0)),(4,(0,0,0))]),
  'neck':rotation([(0,(0,0,0)),(2,(-3,2,0)),(4,(0,0,0))]),
  'head':rotation([(0,(0,0,0)),(1,(2,-4,0)),(2,(0,0,0)),(3,(-3,4,0)),(4,(0,0,0))]),
  'tail':rotation([(0,(0,-6,0)),(1,(0,0,0)),(2,(0,6,0)),(3,(0,0,0)),(4,(0,-6,0))]),
  'tail_mid':rotation([(0,(0,-7,0)),(2,(0,7,0)),(4,(0,-7,0))])}}
walk={'animation_length':1.6,'loop':True,'bones':{
  'torso':pos([(0,(0,-.45,0)),(.4,(0,.35,0)),(.8,(0,-.45,0)),(1.2,(0,.35,0)),(1.6,(0,-.45,0))]),
  'leg_0':rotation([(0,(20,0,0)),(.4,(0,0,0)),(.8,(-20,0,0)),(1.2,(0,0,0)),(1.6,(20,0,0))]),
  'leg_3':rotation([(0,(20,0,0)),(.4,(0,0,0)),(.8,(-20,0,0)),(1.2,(0,0,0)),(1.6,(20,0,0))]),
  'leg_1':rotation([(0,(-20,0,0)),(.4,(0,0,0)),(.8,(20,0,0)),(1.2,(0,0,0)),(1.6,(-20,0,0))]),
  'leg_2':rotation([(0,(-20,0,0)),(.4,(0,0,0)),(.8,(20,0,0)),(1.2,(0,0,0)),(1.6,(-20,0,0))]),
  'tail':rotation([(0,(0,-6,0)),(.8,(0,6,0)),(1.6,(0,-6,0))]),
  'tail_mid':rotation([(0,(0,-8,0)),(.8,(0,8,0)),(1.6,(0,-8,0))]),
  'head':rotation([(0,(-2,0,0)),(.8,(2,0,0)),(1.6,(-2,0,0))])}}
attack={'animation_length':1.0,'loop':False,'bones':{
  'torso':rotation([(0,(0,0,0)),(.22,(0,-8,0)),(.48,(0,12,0)),(1,(0,0,0))]),
  'tail':rotation([(0,(0,-10,0)),(.22,(0,-40,0)),(.48,(0,55,0)),(.74,(0,15,0)),(1,(0,0,0))]),
  'tail_mid':rotation([(0,(0,-12,0)),(.22,(0,-28,0)),(.48,(0,42,0)),(.74,(0,10,0)),(1,(0,0,0))]),
  'tail_tip':rotation([(0,(0,-15,0)),(.22,(0,-25,0)),(.48,(0,35,0)),(.74,(0,8,0)),(1,(0,0,0))]),
  'neck':rotation([(0,(0,0,0)),(.48,(0,-6,0)),(1,(0,0,0))])}}
sit={'animation_length':1.5,'loop':False,'bones':{
  'torso':pos([(0,(0,0,0)),(.9,(0,-4.5,0)),(1.5,(0,-4.5,0))]),
  'leg_0':rotation([(0,(0,0,0)),(.9,(-26,0,8)),(1.5,(-26,0,8))]),
  'leg_1':rotation([(0,(0,0,0)),(.9,(-26,0,-8)),(1.5,(-26,0,-8))]),
  'leg_2':rotation([(0,(0,0,0)),(.9,(23,0,9)),(1.5,(23,0,9))]),
  'leg_3':rotation([(0,(0,0,0)),(.9,(23,0,-9)),(1.5,(23,0,-9))]),
  'neck':rotation([(0,(0,0,0)),(1,(8,0,0)),(1.5,(8,0,0))])}}
eat={'animation_length':2.6,'loop':True,'bones':{
  'neck':rotation([(0,(0,0,0)),(.6,(30,0,0)),(1.8,(30,0,0)),(2.6,(0,0,0))]),
  'head':rotation([(0,(0,0,0)),(.9,(-9,0,0)),(1.3,(6,0,0)),(1.7,(-6,0,0)),(2.6,(0,0,0))])}}
animations={'format_version':'1.8.0','animations':{'animation.stegosaurus.idle':idle,'animation.stegosaurus.walk':walk,'animation.stegosaurus.attack':attack,'animation.stegosaurus.sit':sit,'animation.stegosaurus.eat':eat}}
(OUT/'animations/stegosaurus.animation.json').write_text(json.dumps(animations,indent=2,ensure_ascii=False)+'\n')
print(f'Built {len(bones)} bones, {sum(len(b.get("cubes",[])) for b in bones)} cubes, {len(animations["animations"])} animations; all outputs valid JSON.')
