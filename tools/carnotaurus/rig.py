"""Carnotauro v3: esqueleto articulado propio (estilo ROR: cuello en S, patas de 4 tramos, cola de 5), atlas UV, texturas y brillo."""
import math, numpy as np, os, sys
from PIL import Image
ATLAS=512
# ---------------------------------------------------------------- esqueleto (marco de diseno: x lateral, y arriba, z hacia delante)
def build_spec(stage='adult'):
    B=(stage=='adult')   # el adulto (Brutus): mas espinas y dientes mas grandes que la cria
    P=[]
    def part(name,parent,piv,rot=(0,0,0),cubes=(),grp='body'): P.append(dict(name=name,parent=parent,piv=piv,rot=rot,cubes=list(cubes),grp=grp))
    part('body','root',(0,90,0),(0.08,0,0),[
      (-19,-22,-38,38,42,68,'skin',dict(bf=1)),
      (-14,19,-34,28,5,56,'skin',dict()),
      (-15,-27,-30,30,6,52,'belly',dict()),
      (-18,-20,-52,36,38,16,'skin',dict(bf=1)),
      (-2,23.5,18,4,6,4,'spike',dict()),(-2,23.5,11,4,5,4,'spike',dict()),(-2,23.5,-30,4,6,4,'spike',dict()),(-2,23.5,-36,4,6,4,'spike',dict()),(-2,17.5,-44,4,6,4,'spike',dict())]+([
      (-2.5,19.5,24,5,8,5,'spike',dict()),(7,19.5,14,4,6,4,'spike',dict()),(-11,19.5,14,4,6,4,'spike',dict()),(7,19.5,-34,4,6,4,'spike',dict()),(-11,19.5,-34,4,6,4,'spike',dict()),
      (-2,17.5,-50,4,6,4,'spike',dict()),(11,10,-44,4,6,4,'spike',dict()),(-15,10,-44,4,6,4,'spike',dict())] if B else []))
    # cola de 5 tramos
    part('tail1','body',(0,4,-50),(-0.03,0,0),[(-14,-14,-30,28,28,32,'skin',dict(bf=1)),(-2,13.5,-18,4,5,4,'spike',dict()),(-2,13.5,-6,4,5,4,'spike',dict())]+([(-2,13.5,-28,4,5,4,'spike',dict()),(-2,13.5,-1,4,5,4,'spike',dict())] if B else []),'tail')
    part('tail2','tail1',(0,-1,-28),(-0.05,0,0),[(-10,-10,-28,20,20,30,'skin',dict(bf=1)),(-2,9.5,-14,4,5,4,'spike',dict())]+([(-2,9.5,-26,4,5,4,'spike',dict()),(-2,9.5,-3,4,5,4,'spike',dict())] if B else []),'tail')
    part('tail3','tail2',(0,-1,-26),(-0.06,0,0),[(-7,-7,-26,14,14,28,'skin',dict(bf=1)),(-1.6,6.5,-12,3.2,4,3.2,'spike',dict())]+([(-1.6,6.5,-24,3.2,4,3.2,'spike',dict()),(-1.6,6.5,-2,3.2,4,3.2,'spike',dict())] if B else []),'tail')
    part('tail4','tail3',(0,0,-24),(-0.05,0,0),[(-4.5,-4.5,-24,9,9,26,'skin',dict(bf=1))],'tail')
    part('tail5','tail4',(0,0,-22),(-0.04,0,0),[(-2.6,-2.6,-22,5.2,5.2,24,'skin',dict(bf=1))],'tail')
    # pecho, cuello en S y cabeza
    part('chest','body',(0,4,26),(0.0,0,0),[(-19,-20,-8,38,42,28,'skin',dict(bf=1)),(-13,-26,-4,26,6,20,'belly',dict())]+([(-2.5,21.5,0,5,8,5,'spike',dict()),(-2.5,21.5,11,5,8,5,'spike',dict())] if B else []),'body')
    part('neck1','chest',(0,12,18),(-0.95,0,0),[(-14,-13,0,28,28,26,'skin',dict(bf=1))]+([(-2.5,14.5,3,5,8,5,'spike',dict()),(-2.5,14.5,14,5,8,5,'spike',dict()),(11,2,4,5,5,5,'spike',dict()),(-16,2,4,5,5,5,'spike',dict())] if B else []),'neck')
    part('neck2','neck1',(0,2,22),(0.85,0,0),[(-12,-12,0,24,25,24,'skin',dict(bf=1)),(-9,-14,2,18,3,20,'belly',dict())]+([(-2,12.5,6,4,7,4,'spike',dict()),(-2,12.5,15,4,7,4,'spike',dict())] if B else []),'neck')
    part('head','neck2',(0,0,20),(0.10,0,0),[
      (-13.5,-13,-8,27,27,32,'skin',dict(head=1)),     # craneo (profundo y corto)
      (-10.5,-7,24,21,17,12,'skin',dict(head=1,snout=1)), # hocico
      (-8.5,-6,36,17,14,9,'skin',dict(head=1,snout=1)), # punta del hocico
      (-14.5,8,2,29,6,16,'skin',dict(brow=1)),       # cejas
      (-9.4,-12 if B else -10,24,18.8,5 if B else 3,21,'tooth',dict(upper=1)),   # dientes superiores
      (-6,7.5,39,12,3,6,'skin',dict(nostril=1))]+([(12.5,-7,-7,4,4,6,'spike',dict()),(12.5,-1,-3,3.5,3.5,5,'spike',dict()),(-16.5,-7,-7,4,4,6,'spike',dict()),(-16,-1,-3,3.5,3.5,5,'spike',dict()),(-2.5,13,-4,5,6,5,'spike',dict()),(-2,13,6,4,5,4,'spike',dict())] if B else []),'head')
    part('jaw','head',(0,-9,2),(0.0,0,0),[
      (-8.8,-7,-4,17.6,7,46,'belly',dict(jaw=1)),
      (-8.1,0,4,16.2,4 if B else 2,38,'tooth',dict(lower=1)),
      (-5.8,0.3,4,11.6,1.6,30,'mouth',dict())],'head')
    for sgn,n in ((1,'l'),(-1,'r')):
        # cuernos sobre los ojos (3 tramos, cortos, hacia fuera y atras)
        part('horn_'+n,'head',(sgn*10,13,14),(-0.40,0,sgn*0.62),[(-4.2,0,-4.2,8.4,8,8.4,'horn',dict(k=0))],'horn')
        part('horn2_'+n,'horn_'+n,(0,7.5,0),(-0.14,0,sgn*0.14),[(-3.2,0,-3.2,6.4,8,6.4,'horn',dict(k=1))],'horn')
        part('horn3_'+n,'horn2_'+n,(0,7.5,0),(-0.12,0,sgn*0.12),[(-2.2,0,-2.2,4.4,8,4.4,'horn',dict(k=2))],'horn')
        # brazos diminutos
        part('arm_'+n,'chest',(sgn*19,-6,14),(0.5,0,sgn*-0.1),[(-3.5,-11,-3.5,7,13,7,'skin',dict())],'arm')
        part('arm2_'+n,'arm_'+n,(0,-10,0),(0.9,0,0),[(-2.8,-8,-2.8,5.6,9,5.6,'skin',dict()),(-2.6,-12,0.2,2.2,5,3,'claw',dict()),(0.4,-12,0.2,2.2,5,3,'claw',dict())],'arm')
        # pata: muslo, tibia, metatarso, pie y 3 dedos (mas robusta y corta)
        part('thigh_'+n,'body',(sgn*19,-2,-14),(-0.40,0,0),[(-12,-32,-16,24,36,32,'skin',dict(leg=1)),(-10,-33,-10,20,4,20,'skin',dict())]+([(-2.5,-18,16,5,6,7,'spike',dict())] if B else []),'leg')
        part('shin_'+n,'thigh_'+n,(0,-29,2),(0.95,0,0),[(-7.5,-26,-7.5,15,30,15,'skin',dict(leg=1))],'leg')
        part('meta_'+n,'shin_'+n,(0,-25,-1),(-0.60,0,0),[(-5.8,-22,-5.8,11.6,26,11.6,'skin',dict(leg=1))],'leg')
        part('foot_'+n,'meta_'+n,(0,-21,0),(0.10,0,0),[(-8.5,-7,-10,17,8,24,'skin',dict(leg=1,foot=1))],'leg')
        for k,(tx,yaw) in enumerate(((0,0),(6.3,-0.30),(-6.3,0.30))):
            part('toe%d_%s'%(k,n),'foot_'+n,(sgn*tx,-3,14),(0,sgn*yaw,0),[(-3.4,-3.5,0,6.8,7,14,'skin',dict(toe=1)),(-2.2,-2.6,14,4.4,5.2,8,'claw',dict())],'leg')
    if B:   # montura 3D del Brutus (se pinta solo en carnotaurus_saddle.png: sin silla no se ve)
        byn={p['name']:p for p in P}
        byn['body']['cubes']+=_fin(saddle_body()); byn['chest']['cubes']+=_fin(saddle_chest())
    return P
# ---------------------------------------------------------------- montura del Brutus
SADDLE_ROLES=('cloth','leather','metal')
def _fin(cs):
    """cubos dados en unidades FINALES (ya escaladas) -> unidades de diseno de la parte (scaled() los vuelve a multiplicar)."""
    sx,sy,sz=STAGES['adult']['body']
    return [(c[0]/sx,c[1]/sy,c[2]/sz,c[3]/sx,c[4]/sy,c[5]/sz,c[6],c[7]) for c in cs]
def _mir(x0,x1,y0,y1,z0,z1,role,opt=None):
    """cubo a ambos lados del cuerpo (x0..x1 > 0)."""
    o=opt or {}
    return [(x0,y0,z0,x1-x0,y1-y0,z1-z0,role,dict(o,side=1)),(-x1,y0,z0,x1-x0,y1-y0,z1-z0,role,dict(o,side=-1))]
def _box(x0,x1,y0,y1,z0,z1,role,opt=None): return [(x0,y0,z0,x1-x0,y1-y0,z1-z0,role,dict(opt or {}))]
def saddle_body():
    """marco del cuerpo, unidades finales. Lomo: placa dorsal y=29.3 (x +-18.5), hombrera y=24.4 (x +-25.1)."""
    c=[]
    # manta de tela: lomo, escalon de la hombrera y caida por el flanco
    c+=_box(-20.7,20.7,28.9,31.4,-28.4,10.4,'cloth',dict(piece='top'))
    c+=_mir(18.2,20.4,24.1,29.1,-28.0,10.0,'cloth',dict(piece='riser'))
    c+=_mir(18.2,26.9,24.1,26.5,-28.4,10.4,'cloth',dict(piece='shelf'))
    c+=_mir(25.0,27.3,11.0,26.0,-27.6,9.6,'cloth',dict(piece='drape'))
    # cincha delantera por el flanco + hebilla
    c+=_mir(20.9,28.6,26.4,27.8,-1.5,3.5,'leather',dict(strap=1))
    c+=_mir(27.0,28.8,10.0,27.8,-1.5,3.5,'leather',dict(strap=1))
    c+=_mir(27.0,29.2,13.5,18.0,-2.3,4.3,'metal',dict(buckle=1))
    # estribos: pestana bajo el faldon, correa y marco de hierro
    c+=_mir(21.4,29.2,26.7,28.5,-13.0,-5.0,'leather',dict(strap=1))
    c+=_mir(27.5,29.1,22.5,28.5,-11.8,-6.2,'leather',dict(strap=1))
    c+=_mir(28.0,31.4,20.8,22.8,-13.0,-5.0,'metal',dict(iron=1))
    c+=_mir(28.0,31.4,14.3,20.8,-13.0,-11.0,'metal',dict(iron=1))
    c+=_mir(28.0,31.4,14.3,20.8,-7.0,-5.0,'metal',dict(iron=1))
    c+=_mir(28.0,31.4,12.5,14.3,-13.0,-5.0,'metal',dict(iron=1))
    # faldones de cuero
    c+=_mir(16.0,21.8,28.6,34.2,-19.0,7.5,'leather',dict(skirt=1))
    # asiento: armazon, cojin, borrelete trasero (cantle), poste delantero (pomo) y tapa de laton
    c+=_box(-16.0,16.0,30.4,34.0,-22.0,9.5,'leather',dict(tree=1))
    c+=_box(-12.8,12.8,33.8,36.4,-16.0,7.0,'leather',dict(seat=1))
    c+=_box(-15.5,15.5,33.8,43.8,-22.0,-16.0,'leather',dict(cantle=1))
    c+=_box(-16.6,16.6,41.6,44.8,-22.9,-15.1,'leather',dict(roll=1))
    c+=_box(-2.8,2.8,33.8,41.2,3.2,7.6,'leather',dict(pommel=1))
    c+=_box(-3.7,3.7,41.0,43.4,2.5,8.3,'metal',dict(cap=1))
    # petate enrollado detras del cantle, atado con dos correas
    c+=_box(-15.0,15.0,31.0,36.2,-29.6,-22.2,'cloth',dict(piece='bedroll'))
    c+=_mir(6.6,8.8,30.7,36.9,-30.2,-21.6,'leather',dict(strap=1))
    # correas del lomo hacia el collar del pecho
    c+=_mir(5.0,7.4,28.9,30.5,9.5,21.5,'leather',dict(strap=1))
    return c
def saddle_chest():
    """marco del pecho (pivote del pecho). Pecho: x +-25.1, y -24.4..26.8, z -9.3..23.2."""
    c=[]
    c+=_box(-26.5,26.5,26.4,28.4,3.0,8.2,'leather',dict(strap=1))      # collar: arriba
    c+=_mir(24.9,26.7,-26.2,28.4,3.0,8.2,'leather',dict(strap=1))      # lados
    c+=_box(-26.5,26.5,-26.4,-24.1,3.0,8.2,'leather',dict(strap=1))    # bajo el pecho
    c+=_mir(26.5,28.3,5.0,14.0,2.1,9.1,'metal',dict(medal=1))          # medallones de latón
    c+=_mir(5.0,7.4,26.5,28.0,-8.0,6.0,'leather',dict(strap=1))        # correas del lomo
    return c
# ---------------------------------------------------------------- etapas
STAGES={
 'chibi':dict(body=0.52,tail=0.46,neck=0.55,head=0.82,horn=0.80,arm=0.62,leg=0.52),
 # Adulto (el Brutus): mas ancho, mas hondo y mas largo; piernas gruesas y cortas; cabeza y cuernos enormes (x, y, z)
 'adult':dict(body=(1.32,1.22,1.16),tail=(1.36,1.28,1.12),neck=(1.34,1.28,1.08),head=(1.40,1.32,1.22),horn=(1.55,1.55,1.55),arm=(1.25,1.25,1.25),leg=(1.40,1.14,1.40)),
}
# posturas de reposo propias de cada etapa (adulto: encorvado, cuello mas erguido y cabeza adelantada = actitud agresiva)
ROT_OVERRIDE={'adult':{'body':(0.13,0,0),'neck1':(-1.12,0,0),'neck2':(0.95,0,0),'head':(0.16,0,0),'tail1':(-0.06,0,0)}}
def _v3(s): return tuple(s) if isinstance(s,(tuple,list)) else (s,s,s)
def scaled(stage):
    spec=build_spec(stage); sc=STAGES[stage]; byn={p['name']:p for p in spec}
    for p in spec: p['sv']=_v3(sc[p['grp']]); p['s']=p['sv'][1]
    for nm,rot in ROT_OVERRIDE.get(stage,{}).items(): byn[nm]['rot']=rot
    for p in spec:
        par=byn.get(p['parent'])
        if par: p['piv']=tuple(q*k for q,k in zip(p['piv'],par['sv']))
        sx,sy,sz=p['sv']
        p['cubes']=[(c[0]*sx,c[1]*sy,c[2]*sz,c[3]*sx,c[4]*sy,c[5]*sz,c[6],c[7]) for c in p['cubes']]
    return spec
# ---------------------------------------------------------------- UV
def pack(allcubes):
    for c in allcubes:
        w,h,d=c['w'],c['h'],c['d']; cw,ch,cd=math.ceil(w-1e-6),math.ceil(h-1e-6),math.ceil(d-1e-6)
        c['alloc']=(2*(cd+cw)+1,cd+ch+1)
    items=sorted(allcubes,key=lambda c:-c['alloc'][1])
    x=y=rowh=0
    for c in items:
        fw,fh=c['alloc']
        if x+fw>ATLAS: x=0; y+=rowh; rowh=0
        c['uv']=(x,y); x+=fw; rowh=max(rowh,fh)
    return y+rowh
def regions(c):
    u,v=c['uv']; w,h,d=c['w'],c['h'],c['d']
    return {'xmin':(u,v+d,u+d,v+d+h),'north':(u+d,v+d,u+d+w,v+d+h),'xmax':(u+d+w,v+d,u+2*d+w,v+d+h),'south':(u+2*d+w,v+d,u+2*d+2*w,v+d+h),
            'up':(u+d,v,u+d+w,v+d),'down':(u+d+w,v,u+d+2*w,v+d)}
def prect(r):
    x0=int(math.floor(r[0]+1e-6)); y0=int(math.floor(r[1]+1e-6))
    return x0,y0,max(x0+1,int(math.ceil(r[2]-1e-6))),max(y0+1,int(math.ceil(r[3]-1e-6)))
# ---------------------------------------------------------------- Java
def fl(v):
    s=('%.3f'%v).rstrip('0').rstrip('.')
    return ('0' if s in('','-0') else s)+'F'
def make_stage(stage,hy_target=None):
    spec=scaled(stage); cubes=[]
    for p in spec:
        for i,c in enumerate(p['cubes']):
            cubes.append(dict(part=p['name'],i=i,x0=c[0],y0=c[1],z0=c[2],w=c[3],h=c[4],d=c[5],role=c[6],opt=c[7],stage=stage))
    return spec,cubes
def java_lines(spec,cubes,hy):
    lines=[]; uvmap={(c['part'],c['i']):c['uv'] for c in cubes}
    lines.append('        PartDefinition part_root = root.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));')
    for p in spec:
        par='part_root' if p['parent']=='root' else 'part_'+p['parent']
        px,py,pz=p['piv']
        if p['name']=='body': py=hy
        cb=''
        for i,c in enumerate(p['cubes']):
            x0,y0,z0,w,h,d=c[:6]; u,v=uvmap[(p['name'],i)]
            cb+='.texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)'%(u,v,fl(x0),fl(-(y0+h)),fl(-(z0+d)),fl(w),fl(h),fl(d))
        rx,ry,rz=p['rot']
        pose='PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)'%(fl(px),fl(-py),fl(-pz),fl(rx),fl(ry),fl(rz)) if any(abs(q)>1e-9 for q in p['rot']) else 'PartPose.offset(%s, %s, %s)'%(fl(px),fl(-py),fl(-pz))
        lines.append('        PartDefinition part_%s = %s.addOrReplaceChild("%s", CubeListBuilder.create()%s, %s);'%(p['name'],par,p['name'],cb,pose))
    return lines

# ---------------------------------------------------------------- datos de ambas etapas con un atlas comun
_ALL=None
def get_all():
    global _ALL
    if _ALL is not None: return _ALL
    out={}; allc=[]
    for st in ('chibi','adult'):
        spec=scaled(st); cubes=[]
        for p in spec:
            for i,c in enumerate(p['cubes']):
                cubes.append(dict(part=p['name'],grp=p['grp'],s=p['s'],sv=p['sv'],i=i,x0=c[0],y0=c[1],z0=c[2],w=c[3],h=c[4],d=c[5],role=c[6],opt=c[7],stage=st))
        out[st]=(spec,cubes); allc+=cubes
    used=pack(allc)
    H=int(math.ceil(used/64.0)*64)
    _ALL=(out,H); return _ALL
def parts_text(stage,hy):
    out,H=get_all(); spec,cubes=out[stage]
    return '\n'.join(java_lines(spec,cubes,hy))
