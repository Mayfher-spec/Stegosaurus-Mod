"""Armadura ligera del carnotauro (v1.14): modelo PROPIO con atlas propio (no toca la malla ni las pieles).
Yelmo, tejas dorsales, hombrera octogonal, arnes de cuero con hebilla y estandarte. Mismos huesos que el carnotauro adulto.
Uso: python3 armor2.py <repo>   (genera CarnotaurusArmorLayers.java y carnotaurus_armor_<nivel>.png)"""
import sys, os, math, zlib, numpy as np
HERE=os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0,HERE)
import rig, paint
AW=256
def _c(x0,y0,z0,w,h,d,kind,**o): return (x0,y0,z0,w,h,d,'armor',dict(o,kind=kind))
def _mx(x0,w,sgn): return x0 if sgn>0 else -(x0+w)
def geometry():
    """dict hueso -> cubos (unidades de diseno del hueso) y lista de partes extra (hijas fijas con giro)."""
    A={}; extra=[]
    def add(part,*cs): A.setdefault(part,[]).extend(cs)
    # ---- yelmo: tapa del craneo en dos pisos, placa del hocico, sienes y cejas
    add('head',_c(-14.8,13.6,-8.6,29.6,2.6,27.2,'plate',r='crown'),
               _c(-10.5,16.2,0.0,21.0,1.8,15.0,'plate',r='crown2'),
               _c(-11.4,10.2,24.0,22.8,2.2,12.0,'plate',r='snout'),
               _c(-10.0,8.2,36.0,20.0,2.2,9.4,'plate',r='snout'),
               _c(-15.4,7.6,6.0,30.8,2.2,5.0,'plate',r='brow'))
    for sgn in (1,-1):
        add('head',_c(_mx(13.7,1.7,sgn),-2.0,-6.4,1.7,13.0,17.0,'plate',r='temple',side=sgn),
                   _c(_mx(11.0,1.5,sgn),-1.0,25.0,1.5,9.5,15.0,'plate',r='snoutside',side=sgn))
    # ---- tejas dorsales: 3 en el pecho y 2 en el cuerpo, con alas que caen por los flancos (partes hijas con giro)
    def shingle(bone,top,z0,k,hw=15.6,wlen=9.0,d=9.0,wings=True):
        add(bone,_c(-hw,top,z0,2*hw,2.4,d,'plate',r='shingle',k=k))
        if not wings: return
        for sgn in (1,-1):
            extra.append(dict(name='wing_%s_%d_%s'%(bone,k,'l' if sgn>0 else 'r'),parent=bone,piv=(sgn*hw,top+1.2,z0+d/2.0),rotz=sgn,
                              cubes=[_c(0 if sgn>0 else -wlen,-2.4,-d/2.0,wlen,2.4,d,'plate',r='wing',k=k,side=sgn)],grp='body'))
    # espalda: 3 tejas en el pecho y 8 en el cuerpo (hasta la grupa)
    for k,z0 in enumerate((12.6,6.0,-0.6)): shingle('chest',22.2+0.3*k,z0,k)
    for k,z0 in enumerate((13.0,6.4,-0.2,-6.8,-13.4,-20.0,-26.6,-33.2)): shingle('body',24.4+0.1*k,z0,k+3)
    # cola: tejas con alas en el primer tramo, tejas con costados en el segundo y tercero, remate en el cuarto
    for k,z0 in enumerate((-4.0,-10.6,-17.2,-23.8)): shingle('tail1',14.3+0.1*k,z0,k+20,hw=15.0,wlen=11.0,d=8.6)
    for nm,hw,top,zs,sy in (('tail2',11.0,10.3,(-2.0,-8.6,-15.2,-21.8),8.0),('tail3',8.0,7.3,(-4.0,-10.6,-17.2),5.6)):
        for k,z0 in enumerate(zs): shingle(nm,top+0.1*k,z0,k+30,hw=hw,d=8.4,wings=False)
        for sgn in (1,-1):
            add(nm,_c(_mx(hw-0.9,1.4,sgn),-sy,zs[-1],1.4,2*sy,zs[0]+8.4-zs[-1],'plate',r='tailside',side=sgn))
    add('tail4',_c(-5.6,4.9,-22.0,11.2,1.6,22.0,'plate',r='tail'))
    # piernas: placa del muslo, grebas por tramos y tobillera
    for sgn,nn in ((1,'l'),(-1,'r')):
        add('thigh_'+nn,_c(_mx(12.0,1.6,sgn),-28.0,-12.0,1.6,24.0,24.0,'plate',r='thigh',side=sgn),
                        _c(_mx(12.4,1.4,sgn),-30.5,-8.0,1.4,3.2,16.0,'plate',r='knee',side=sgn))
        add('shin_'+nn,_c(-8.8,-8.0,-8.8,17.6,7.0,17.6,'plate',r='greave'),
                        _c(-8.8,-23.0,-8.8,17.6,6.0,17.6,'plate',r='greave'),
                        _c(-5.0,-22.0,8.9,10.0,19.0,1.4,'plate',r='shinfront'))
        add('meta_'+nn,_c(-6.9,-20.0,-6.9,13.8,5.0,13.8,'plate',r='anklet'),_c(-6.9,-8.0,-6.9,13.8,4.0,13.8,'plate',r='anklet'))
    # ---- hombrera octogonal (3 losas escalonadas) en cada costado del pecho
    for sgn in (1,-1):
        add('chest',_c(_mx(19.1,2.6,sgn),-2.0,5.5,2.6,20.0,9.0,'plate',r='pad1',side=sgn),
                    _c(_mx(19.1,2.2,sgn),3.0,0.0,2.2,10.0,20.0,'plate',r='pad2',side=sgn),
                    _c(_mx(19.1,1.8,sgn),0.5,2.0,1.8,15.0,16.0,'plate',r='pad3',side=sgn))
    # ---- arnes: dos cinchas en anillo alrededor del cuerpo + hebilla dorada
    for k,z0 in enumerate((24.0,3.5)):
        add('body',_c(-19.6,24.6 if z0<20 else 20.6,z0,39.2,1.5,3.4,'strap',r='top'),
                   _c(-19.6,-24.0,z0,39.2,1.5,3.4,'strap',r='bottom'))
        for sgn in (1,-1):
            add('body',_c(_mx(19.1,1.6,sgn),-24.0,z0,1.6,45.0 if z0<20 else 41.0,3.4,'strap',r='side',side=sgn))
    for sgn in (1,-1):
        add('body',_c(_mx(20.5,1.2,sgn),-9.0,22.8,1.2,7.0,6.2,'gold',r='buckle',side=sgn))
        # estandarte colgado entre las cinchas (tela fina + barra de cuero)
        add('body',_c(_mx(19.7,0.9,sgn),-22.0,7.0,0.9,34.0,15.0,'banner',r='banner',side=sgn),
                   _c(_mx(19.4,1.8,sgn),11.6,6.0,1.8,2.4,17.0,'strap',r='bar',side=sgn))
    return A,extra
def build():
    """(spec, cubes, H): esqueleto del adulto con SOLO los cubos de armadura, ya escalado, y su atlas propio."""
    A,extra=geometry()
    spec=rig.build_spec('adult')
    for p in spec: p['cubes']=list(A.get(p['name'],[]))
    byn={p['name']:p for p in spec}
    for e in extra:
        par=byn[e['parent']]; sgn=e['rotz']
        # giro del ala: debe caer hacia fuera (probar el signo contra el marco real)
        a=0.78
        for sg in (a,-a):
            R=paint.Rz(-sg); v=R@np.array([sgn,0,0.0])
            if v[1]<0: rz=sg; break
        q=dict(name=e['name'],parent=e['parent'],piv=e['piv'],rot=(0,0,rz),cubes=e['cubes'],grp=e['grp']); spec.append(q); byn[q['name']]=q
    sc=rig.STAGES['adult']
    for p in spec: p['sv']=rig._v3(sc[p['grp']]); p['s']=p['sv'][1]
    for nm,rot in rig.ROT_OVERRIDE['adult'].items(): byn[nm]['rot']=rot
    for p in spec:
        par=byn.get(p['parent'])
        if par: p['piv']=tuple(q*k for q,k in zip(p['piv'],par['sv']))
        sx,sy,sz=p['sv']
        p['cubes']=[(c[0]*sx,c[1]*sy,c[2]*sz,c[3]*sx,c[4]*sy,c[5]*sz,c[6],c[7]) for c in p['cubes']]
    cubes=[]
    for p in spec:
        for i,c in enumerate(p['cubes']):
            cubes.append(dict(part=p['name'],grp=p['grp'],s=p['s'],sv=p['sv'],i=i,x0=c[0],y0=c[1],z0=c[2],w=c[3],h=c[4],d=c[5],role=c[6],opt=c[7],stage='adult'))
    # empaquetado en un atlas propio de AW px de ancho
    for c in cubes:
        w,h,d=c['w'],c['h'],c['d']; cw,ch,cd=math.ceil(w-1e-6),math.ceil(h-1e-6),math.ceil(d-1e-6); c['alloc']=(2*(cd+cw)+1,cd+ch+1)
    items=sorted(cubes,key=lambda c:-c['alloc'][1]); x=y=rowh=0
    for c in items:
        fw,fh=c['alloc']
        if x+fw>AW: x=0; y+=rowh; rowh=0
        c['uv']=(x,y); x+=fw; rowh=max(rowh,fh)
    H=int(math.ceil((y+rowh)/32.0)*32)
    return spec,cubes,H
# ---------------------------------------------------------------- pintura
C=lambda *v: np.array(v,float)
LEATHER=dict(base=C(112,70,38),light=C(160,108,60),dark=C(62,38,20),stitch=C(214,172,110))
GOLD=dict(base=C(222,176,62),light=C(255,228,130),dark=C(128,90,30))
TIERS=('iron','gold','diamond','netherite')
PLATE={
 'iron':     dict(base=C(134,138,152),light=C(200,204,216),dark=C(58,62,76),banner=C(158,30,42),banner_d=C(96,16,26)),
 'gold':     dict(base=C(222,168,40),light=C(255,232,122),dark=C(120,74,16),banner=C(120,26,38),banner_d=C(72,14,22)),
 'diamond':  dict(base=C(52,176,188),light=C(164,246,246),dark=C(18,80,94),banner=C(32,88,156),banner_d=C(16,48,100)),
 'netherite':dict(base=C(86,74,100),light=C(146,130,160),dark=C(32,26,40),banner=C(92,36,136),banner_d=C(48,16,78)),
}
SKULL=["...########.....",
       "..###########...",
       ".####..#########",
       "#####..#########",
       "###############.",
       "#.#.#.#.####....",
       ".#.#.#.#........",
       "..##########....",
       "...########....."]
def _stamp(bits,u,v,fw,fh,cs,flip):
    bh,bw=len(bits),len(bits[0]); ex0=(fw-bw*cs)/2.0; ey0=(fh-bh*cs)/2.0
    cu=np.floor((u-ex0)/cs).astype(int); cv=np.floor((v-ey0)/cs).astype(int)
    if flip: cu=bw-1-cu
    ok=(cu>=0)&(cu<bw)&(cv>=0)&(cv<bh); m=np.zeros(len(u),bool)
    for k in np.where(ok)[0]: m[k]=bits[cv[k]][cu[k]]=='#'
    return m
def _h(a,b,seed): return paint.hsh(np.asarray(a,float),np.asarray(b,float),np.zeros(len(a)),seed)
def _scratches(u,v,seed,dens=0.22):
    """aranazos finos en diagonal: cada celda de 7x7 puede traer uno"""
    cu=np.floor(u/7.0); cv=np.floor(v/7.0); hh=_h(cu,cv,seed)
    ph=_h(cu,cv,seed+5)*7.0
    lin=((u*0.85+v*0.55+ph*3.0)%6.0)<0.40
    seg=_h(cu,cv,seed+9)>0.35
    return (hh<dens)&lin&seg
def tex(tier,c,face,loc,ii,jj,fu,fv):
    pal=PLATE[tier]; o=c['opt']; kind=o['kind']; r=o.get('r',''); sgn=o.get('side',0)
    n=len(ii); fw,fh=paint.face_dims(c,face); u=fu*fw; v=fv*fh
    eu=np.minimum(u,fw-u); ev=np.minimum(v,fh-v); edge=np.minimum(eu,ev)
    seed=(zlib.crc32(('%s:%d'%(c['part'],c['i'])).encode())%997)
    nz=paint.fbm(loc*0.6+seed*0.31,19)
    big=paint.fbm(loc*0.16+seed*0.13,23,3)
    fine=_h(np.floor(u*1.0),np.floor(v*1.0),seed)               # grano por texel
    mask=np.ones(n,bool)
    outer=True if not sgn else ((face=='xmax' and sgn>0) or (face=='xmin' and sgn<0))
    if kind=='plate':
        base=pal['base']; dark=pal['dark']; light=pal['light']
        col=np.asarray(paint.lerp(dark*0.55+base*0.45,base,np.clip(big*1.4,0,1)))   # manchas de brillo grandes
        # acabado propio de cada material
        if tier=='iron':
            streak=_h(np.floor(v*1.0)*0+np.floor(v),np.floor(u*0.12),seed+3)           # acero cepillado: vetas largas
            col=col*(0.93+0.12*streak)[:,None]
            rust=paint.ss(0.80,0.93,paint.fbm(loc*0.35+seed,41,3))*paint.ss(2.2,0.8,edge)
            col=np.asarray(paint.lerp(col,C(112,70,42),(rust*0.55)[:,None]*np.ones((n,3))))
        elif tier=='gold':
            band=0.5+0.5*np.sin((u*0.55+v*0.85+seed)*0.9)                                # reflejo diagonal pulido
            col=col*(0.92+0.10*band)[:,None]+light*(0.07*paint.ss(0.75,1.0,band))[:,None]
        elif tier=='diamond':
            d1,d2,rid=paint.voronoi(loc/3.2+seed,91)
            col=col*(0.82+0.30*rid)[:,None]
            sm=paint.ss(0.13,0.0,d2-d1); col=np.asarray(paint.lerp(col,light,(sm*0.55)[:,None]*np.ones((n,3))))
        else:
            ember=paint.ss(0.93,0.985,paint.fbm(loc*0.5+seed*0.7,57,3))*paint.ss(0.5,2.5,edge)
            col=col*(0.90+0.18*fine)[:,None]
            col=np.asarray(paint.lerp(col,C(236,110,40),(ember*0.85)[:,None]*np.ones((n,3))))   # vetas de brasa (netherita)
        col=col*(0.94+0.12*fine)[:,None]
        if face=='up': col=col*0.94+light*0.06
        # aranazos
        sc=_scratches(u,v,seed)
        col[sc]=np.asarray(paint.lerp(col[sc],light,0.40))
        # sombra de oclusion junto al borde y bisel desgastado (metal expuesto)
        ao=(edge>=1.6)&(edge<3.2); col[ao]*=0.93
        col[edge<0.75]=dark*0.9
        bv=(edge>=0.75)&(edge<1.6)
        chip=_h(np.floor(u*1.0),np.floor(v*1.0),seed+21)<0.30
        lit_face=face in('up','north')
        col[bv]=(light*(0.90+0.12*nz[bv])[:,None]) if lit_face else col[bv]*1.10
        col[bv&chip]=light*1.04
        # remaches: reflejo arriba-izquierda, sombra abajo-derecha y aro de sombra en la placa
        if min(fw,fh)>=7.0 and face in('up','xmin','xmax'):
            for cx in (2.6,fw-2.6):
                for cy in (2.6,fh-2.6):
                    du=u-cx; dv=v-cy
                    ring=(np.abs(du)<1.7)&(np.abs(dv)<1.7); col[ring]*=0.78
                    rv=(np.abs(du)<0.95)&(np.abs(dv)<0.95); col[rv]=GOLD['base']
                    col[rv&(du<0)&(dv<0)]=GOLD['light']; col[rv&(du>0)&(dv>0)]=GOLD['dark']
        if r=='pad2' and outer and face in('xmin','xmax'):
            fr_=(edge>=2.0)&(edge<2.8); col[fr_]=GOLD['base']*(0.94+0.12*nz[fr_])[:,None]
            col[(edge>=2.8)&(edge<3.3)]*=0.80
        return np.clip(col,0,255),mask
    if kind=='strap':
        d1,d2,rid=paint.voronoi(loc/1.7+seed,13)
        col=np.tile(LEATHER['base'],(n,1))*(0.82+0.30*rid)[:,None]*(0.90+0.16*nz)[:,None]     # grano de cuero
        crack=paint.ss(0.10,0.0,d2-d1); col*=(1.0-0.22*crack)[:,None]
        wear=paint.ss(1.6,0.4,edge)*(_h(np.floor(u),np.floor(v),seed+31)<0.5); col[wear>0.5]=np.asarray(paint.lerp(col[wear>0.5],LEATHER['light'],0.55))
        col[edge<0.6]=LEATHER['dark']
        st=(np.abs(edge-1.5)<0.45)&((np.floor(u+v)%3)==0); col[st]=LEATHER['stitch']
        return np.clip(col,0,255),mask
    if kind=='gold':
        col=np.tile(GOLD['base'],(n,1))*(0.90+0.18*nz)[:,None]
        col=col+GOLD['light']*(0.10*paint.ss(0.6,1.0,0.5+0.5*np.sin((u+v)*0.8)))[:,None]
        sc=_scratches(u,v,seed+2,0.3); col[sc]=GOLD['light']
        col[edge<0.6]=GOLD['dark']
        if face in('xmin','xmax'):
            hole=(u>fw*0.28)&(u<fw*0.72)&(v>fh*0.26)&(v<fh*0.74); col[hole]=LEATHER['dark']
            col[hole&(v<fh*0.34)]=LEATHER['dark']*0.6
            bar=(np.abs(u-fw*0.5)<0.7)&(v>fh*0.18)&(v<fh*0.82); col[bar]=GOLD['light']
        return np.clip(col,0,255),mask
    # estandarte: tejido, pliegues y sombras
    col=np.tile(pal['banner'],(n,1))
    weave=0.96+0.08*(((np.floor(u)+np.floor(v))%2)==0)
    fold=0.5+0.5*np.sin(u*1.15+1.6*nz+0.4*np.sin(v*0.3))
    col=col*(0.80+0.26*fold)[:,None]*weave[:,None]*(0.94+0.10*fine)[:,None]
    col[(edge<1.2)]=GOLD['base']*(0.92+0.14*nz[edge<1.2])[:,None]; col[(edge>=1.2)&(edge<1.9)]=pal['banner_d']
    tcut=np.zeros(n,bool)
    if face in('xmin','xmax'):
        cut=fh-7.0*(1.0-np.abs(2*u/fw-1.0)); tcut=v>cut
        # deshilachado irregular en el borde inferior
        tcut|=(v>cut-0.9)&(_h(np.floor(u),np.floor(v),seed+41)<0.35)
        col[(v>cut-1.5)&(v<=cut-0.2)&~tcut]=GOLD['base']
        cs=min((fw-5.0)/16.0,(fh-12.0)/9.0)
        if cs>0.3:
            m=_stamp(SKULL,u,v-fh*0.02,fw,fh*0.62,cs,face=='xmin'); mm=m&~tcut
            m2=_stamp(SKULL,u+0.9,v-fh*0.02+0.9,fw,fh*0.62,cs,face=='xmin')&~m&~tcut   # sombra proyectada
            col[m2]*=0.62
            col[mm]=GOLD['light']*(0.92+0.12*nz[mm])[:,None]
    mask&=~tcut
    return np.clip(col,0,255),mask
def make_atlas(tier,spec,cubes,H):
    atlas=np.zeros((H,AW,4),np.uint8)
    for c in cubes:
        for face,r in rig.regions(c).items():
            rect,loc,nl,ii,jj,fu,fv=paint.region_geom(c,face,r)
            col,m=tex(tier,c,face,loc,ii,jj,fu,fv)
            atlas[jj[m],ii[m],:3]=np.clip(col[m],0,255).astype(np.uint8); atlas[jj[m],ii[m],3]=255
    return atlas
JAVA_HEAD="""package com.prodigytown.creatures.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERADO por tools/carnotaurus/armor2.py (armadura ligera del carnotauro: mismos huesos que el adulto, atlas propio). No editar a mano. */
final class CarnotaurusArmorLayers {
    private CarnotaurusArmorLayers() {}
    static final int TEX_W = %d;
    static final int TEX_H = %d;

    static LayerDefinition adult() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
%s
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }
}
"""
def java_source(spec,cubes,H):
    hy=paint.fit_hy('adult')
    return JAVA_HEAD%(AW,H,'\n'.join(rig.java_lines(spec,cubes,hy)))
def export(repo):
    spec,cubes,H=build()
    open(os.path.join(repo,'src/main/java/com/prodigytown/creatures/client/CarnotaurusArmorLayers.java'),'w').write(java_source(spec,cubes,H))
    from PIL import Image
    td=os.path.join(repo,'src/main/resources/assets/prodigycreatures/textures/entity')
    for t in TIERS: Image.fromarray(make_atlas(t,spec,cubes,H)).save(os.path.join(td,'carnotaurus_armor_%s.png'%t),optimize=True)
    print('armadura: %d cubos, atlas %dx%d'%(len(cubes),AW,H))
if __name__=='__main__':
    export(sys.argv[1] if len(sys.argv)>1 else '.')
