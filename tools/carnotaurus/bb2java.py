"""Convierte los .bbmodel de Carnotauro (adulto y chibi) a ModelPart de Java y recompone la textura (UV por cara -> UV de caja)."""
import json, math, sys, os
import numpy as np
from PIL import Image
Z='/tmp/claude-0/-home-claude/31363d96-ed19-5f2f-8a93-9e8e6fc457e3/scratchpad/bb_zip/Prodigy_Carnotaurus/'
OUT='/tmp/claude-0/-home-claude/31363d96-ed19-5f2f-8a93-9e8e6fc457e3/scratchpad/bb/'
TEX='/home/claude/stegosaurus-mod/src/main/resources/assets/prodigycreatures/textures/entity/'
src=open(Z+'build_carnotaurus.py').read()
head=src[:src.index('class Sculpt')].replace('import cv2','').replace("['#353038','#2f568a']","['#353038','#2f568a','#1b3a30']").replace("BASE = Path('/mnt/data/Prodigy_Carnotaurus_BBModel')","BASE = Path('/tmp')").replace('BASE.mkdir(exist_ok=True)','')
ns={}; exec(head,ns)
CATS=ns['CATS']; tile_img=ns['tile_img']; PALETTES=ns['PALETTES']
PALETTES['Emerald']=dict(skin='#1b3a30',light='#2b5c47',dark='#0b1914',stripe='#0c1d17',belly='#6c8f7c',belly_dark='#46665a',spine='#143a2c',spine_light='#3df08a',horn='#bff5d6',horn_shadow='#79c9a0',eye='#8dffb0',mouth='#0d2a22',tongue='#2fa86a',teeth='#e6fff0',accent='#2fe880',claw='#d4f7e3')
VARS={'scarlet':'Natural','magma':'Magma','emerald':'Emerald'}
FILES={'adult':'Carnotauro_adulto_Natural.bbmodel','chibi':'Carnotauro_chibi_Natural.bbmodel'}
ATLAS=512
# ---------------- carga
def load(stage):
    d=json.load(open(Z+FILES[stage]))
    G={g['uuid']:g for g in d['groups']}; E={e['uuid']:e for e in d['elements']}
    groups=[]; cubes=[]
    def walk(o,parent):
        g=G[o['uuid']]; groups.append(dict(name=g['name'],parent=parent,origin=g['origin']))
        for c in o.get('children',[]):
            if isinstance(c,str):
                e=E[c]; cats={}
                for fn,f in e['faces'].items():
                    x0,y0=f['uv'][0],f['uv'][1]; cats[fn]=CATS[int(y0//32)*16+int(x0//32)]
                cubes.append(dict(stage=stage,name=e['name'],group=g['name'],fr=e['from'],to=e['to'],org=e['origin'],rot=e['rotation'],cats=cats))
            else: walk(c,g['name'])
    for o in d['outliner']: walk(o,None)
    return groups,cubes
M={s:load(s) for s in FILES}
allcubes=[c for s in M for c in M[s][1]]
# horns: el sentido del BB (hacia dentro) no es el buscado; ambos cuernos hacia fuera y hacia atras
for c in allcubes:
    if 'cuerno' in c['name'] or 'punta' in c['name']:
        side=1 if c['org'][0]>0 else -1
        if c['stage']=='adult': c['rot']=[14, 0, 15*side]   # BB: x>0 inclina atras; z>0 en lado +x inclina hacia fuera (se comprueba en la vista previa)
        else: c['rot']=[0,0,14*side]
def dims(c): return [c['to'][i]-c['fr'][i] for i in range(3)]
# ---------------- empaquetado
items=[]
for c in allcubes:
    w,h,d=dims(c); cw,ch,cd=math.ceil(w-1e-6),math.ceil(h-1e-6),math.ceil(d-1e-6)
    c['alloc']=(2*(cd+cw)+1,cd+ch+1); items.append(c)
items.sort(key=lambda c:-c['alloc'][1])
x=y=rowh=0
for c in items:
    fw,fh=c['alloc']
    if x+fw>ATLAS: x=0; y+=rowh; rowh=0
    c['uv']=(x,y); x+=fw; rowh=max(rowh,fh)
used=y+rowh
assert used<=ATLAS,('atlas',used)
print('atlas filas usadas',used,'de',ATLAS)
def regions(c):
    u,v=c['uv']; w,h,d=dims(c)
    # nombres de region de Java; la cara BB 'east' va en la region de Java 'west' (u) y viceversa
    return {
     'east':(u,v+d,u+d,v+d+h),           # BB east  -> region u
     'north':(u+d,v+d,u+d+w,v+d+h),
     'west':(u+d+w,v+d,u+2*d+w,v+d+h),   # BB west  -> region u+d+w
     'south':(u+2*d+w,v+d,u+2*d+2*w,v+d+h),
     'up':(u+d,v,u+d+w,v+d),
     'down':(u+d+w,v,u+d+2*w,v+d)}
def prect(r):
    x0=int(math.floor(r[0]+1e-6)); y0=int(math.floor(r[1]+1e-6))
    x1=max(x0+1,int(math.ceil(r[2]-1e-6))); y1=max(y0+1,int(math.ceil(r[3]-1e-6)))
    return x0,y0,x1,y1
def bake(var):
    pal=PALETTES[VARS[var]]; tiles={}
    for i,cat in enumerate(CATS):
        s=sum(ord(a)*(j+1) for j,a in enumerate(cat))+23
        tiles[cat]=tile_img(cat,pal,s).convert('RGBA')
    atlas=Image.new('RGBA',(ATLAS,ATLAS),(0,0,0,0))
    for c in allcubes:
        R=regions(c)
        for fn in ('up','down','north','south','east','west'):
            x0,y0,x1,y1=prect(R[fn]); t=tiles[c['cats'][fn]]
            atlas.paste(t.resize((x1-x0,y1-y0),Image.LANCZOS),(x0,y0))
    return atlas,pal
# ---------------- Java
def fl(v):
    s=('%.3f'%v).rstrip('0').rstrip('.')
    return ('0' if s in('','-0') else s)+'F'
def emit(stage):
    groups,cubes=M[stage]; org={g['name']:g['origin'] for g in groups}
    L=[]; by={}
    for c in cubes: by.setdefault(c['group'],[]).append(c)
    for g in groups:
        n=g['name']; o=g['origin']; par=g['parent']
        pv='root' if par is None else 'part_'+par
        if par is None: pose='PartPose.offset(0F, 24F, 0F)'
        else:
            po=org[par]; pose='PartPose.offset(%s, %s, %s)'%(fl(-(o[0]-po[0])),fl(-(o[1]-po[1])),fl(o[2]-po[2]))
        cb=''; rotated=[]
        for c in by.get(n,[]):
            if any(abs(q)>1e-9 for q in c['rot']): rotated.append(c); continue
            w,h,d=dims(c)
            cb+='.texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)'%(c['uv'][0],c['uv'][1],fl(o[0]-c['to'][0]),fl(o[1]-c['to'][1]),fl(c['fr'][2]-o[2]),fl(w),fl(h),fl(d))
        L.append('        PartDefinition part_%s = %s.addOrReplaceChild("%s", CubeListBuilder.create()%s, %s);'%(n,pv,n,cb,pose))
        for k,c in enumerate(rotated):
            w,h,d=dims(c); co=c['org']
            off=(-(co[0]-o[0]),-(co[1]-o[1]),co[2]-o[2])
            rx,ry,rz=[math.radians(q) for q in c['rot']]
            L.append('        PartDefinition part_%s_r%d = part_%s.addOrReplaceChild("r%d", CubeListBuilder.create().texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s), PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));'%(
              n,k,n,k,c['uv'][0],c['uv'][1],fl(co[0]-c['to'][0]),fl(co[1]-c['to'][1]),fl(c['fr'][2]-co[2]),fl(w),fl(h),fl(d),
              fl(off[0]),fl(off[1]),fl(off[2]),fl(-rx),fl(-ry),fl(rz)))
    return L
JAVA='''package com.prodigytown.creatures.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERADO a partir de Carnotauro_adulto/chibi_Natural.bbmodel (Blockbench). No editar a mano: la geometria se cambia en el .bbmodel. */
final class CarnotaurusLayers {
    private CarnotaurusLayers() {}
    static final int TEX = %d;

%s
}
'''
def method(stage,name):
    body='\n'.join(emit(stage))
    return '    static LayerDefinition %s() {\n        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n%s\n        return LayerDefinition.create(mesh, TEX, TEX);\n    }\n'%(name,body)
if __name__=='__main__':
    open(OUT+'adult_parts.txt','w').write('\n'.join(emit('adult')))
    open(OUT+'chibi_parts.txt','w').write('\n'.join(emit('chibi')))
    open(OUT+'CarnotaurusLayers.java','w').write(JAVA%(ATLAS,method('adult','adult')+'\n'+method('chibi','chibi')))
    for var in VARS:
        a,pal=bake(var); a.save(OUT+'atlas_%s.png'%var)
    print('ok')
