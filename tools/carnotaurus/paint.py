"""Pintor de texturas del carnotauro (3 variantes) + brillo + silla. Patrones en 3D en pose de reposo => sin costuras entre caras/partes."""
import numpy as np, math, sys
import os, zlib
sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
import rig
from PIL import Image
W=512
def ss(a,b,x):
    t=np.clip((x-a)/(b-a),0.0,1.0); return t*t*(3-2*t)
def lerp(a,b,t):
    t=np.asarray(t,float)[...,None] if np.ndim(t)==1 else t
    return np.asarray(a,float)*(1-t)+np.asarray(b,float)*t
# ---------------------------------------------------------------- ruido
def hsh(ix,iy,iz,seed):
    h=(ix.astype(np.int64)*73856093)^(iy.astype(np.int64)*19349663)^(iz.astype(np.int64)*83492791)^(int(seed)*2654435761)
    h&=0xFFFFFFFF; h=(((h>>13)^h)*1274126177)&0xFFFFFFFF; h=(h^(h>>16))&0xFFFFFFFF
    return h/4294967296.0
def vnoise(P,seed):
    ip=np.floor(P); f=P-ip; f=f*f*(3-2*f); out=0
    for dx in (0,1):
        for dy in (0,1):
            for dz in (0,1):
                wgt=(f[:,0] if dx else 1-f[:,0])*(f[:,1] if dy else 1-f[:,1])*(f[:,2] if dz else 1-f[:,2])
                out=out+wgt*hsh(ip[:,0]+dx,ip[:,1]+dy,ip[:,2]+dz,seed)
    return out
def fbm(P,seed,octv=3):
    a=0.5; t=0; tot=0
    for o in range(octv):
        t=t+a*vnoise(P*(2**o),seed+o*7); tot+=a; a*=0.5
    return t/tot
def voronoi(P,seed):
    ip=np.floor(P); d1=np.full(len(P),9.0); d2=np.full(len(P),9.0); rid=np.zeros(len(P))
    for dx in (-1,0,1):
        for dy in (-1,0,1):
            for dz in (-1,0,1):
                c=ip+np.array([dx,dy,dz],float)
                pt=c+np.stack([hsh(c[:,0],c[:,1],c[:,2],seed),hsh(c[:,0],c[:,1],c[:,2],seed+11),hsh(c[:,0],c[:,1],c[:,2],seed+23)],-1)
                d=np.linalg.norm(P-pt,axis=1); m=d<d1
                d2=np.where(m,d1,np.minimum(d2,d)); rid=np.where(m,hsh(c[:,0],c[:,1],c[:,2],seed+57),rid); d1=np.minimum(d,d1)
    return d1,d2,rid
# ---------------------------------------------------------------- marcos de reposo (marco de diseno)
def Rx(a): c,s=math.cos(a),math.sin(a); return np.array([[1,0,0],[0,c,-s],[0,s,c]])
def Ry(a): c,s=math.cos(a),math.sin(a); return np.array([[c,0,s],[0,1,0],[-s,0,c]])
def Rz(a): c,s=math.cos(a),math.sin(a); return np.array([[c,-s,0],[s,c,0],[0,0,1]])
def frames(spec):
    fr={}
    for p in spec:
        rx,ry,rz=p['rot']; R=Rz(-rz)@Ry(-ry)@Rx(rx); piv=np.array(p['piv'],float)
        if p['parent']=='root': fr[p['name']]=(R,piv)
        else:
            Rp,Tp=fr[p['parent']]; fr[p['name']]=(Rp@R,Tp+Rp@piv)
    return fr
# ---------------------------------------------------------------- texels de una region
def region_geom(c,face,r):
    x0,y0,x1,y1=rig.prect(r)
    jj,ii=np.mgrid[y0:y1,x0:x1]
    fu=np.clip((ii+0.5-r[0])/max(1e-6,(r[2]-r[0])),0,1); fv=np.clip((jj+0.5-r[1])/max(1e-6,(r[3]-r[1])),0,1)
    cx,cy,cz,w,h,d=c['x0'],c['y0'],c['z0'],c['w'],c['h'],c['d']
    if face=='xmin': x=cx+0*fu; z=cz+fu*d; y=cy+h-fv*h; n=(-1,0,0)
    elif face=='xmax': x=cx+w+0*fu; z=cz+d-fu*d; y=cy+h-fv*h; n=(1,0,0)
    elif face=='north': z=cz+d+0*fu; x=cx+fu*w; y=cy+h-fv*h; n=(0,0,1)
    elif face=='south': z=cz+0*fu; x=cx+w-fu*w; y=cy+h-fv*h; n=(0,0,-1)
    elif face=='up': y=cy+h+0*fu; x=cx+w-fu*w; z=cz+d-fv*d; n=(0,1,0)
    else: y=cy+0*fu; x=cx+w-fu*w; z=cz+fv*d; n=(0,-1,0)
    return (x0,y0,x1,y1),np.stack([x.ravel(),y.ravel(),z.ravel()],-1),np.array(n,float),ii.ravel(),jj.ravel(),fu.ravel(),fv.ravel()
# ---------------------------------------------------------------- paletas
PAL={
 'scarlet':dict(dark=(176,48,34),mid=(222,86,46),stripe=(122,32,32),belly=(232,200,150),belly2=(206,166,118),horn_a=(236,224,186),horn_b=(112,90,66),spike_a=(100,72,58),spike_b=(54,40,38),claw_a=(44,36,36),claw_b=(176,160,130),tooth=(242,238,222),gum=(168,66,76),tongue=(196,78,94),eye=(255,196,40),pupil=(30,14,10),scar_a=(150,62,52),scar_b=(246,200,164)),
 'green':dict(dark=(48,92,44),mid=(124,160,66),stripe=(16,40,28),belly=(216,210,148),belly2=(178,174,110),horn_a=(230,222,174),horn_b=(94,74,46),spike_a=(64,80,38),spike_b=(28,36,22),claw_a=(36,32,26),claw_b=(206,198,160),tooth=(240,236,214),gum=(150,60,62),tongue=(190,82,92),eye=(255,184,28),pupil=(28,14,6),scar_a=(196,170,100),scar_b=(240,228,176)),
 'steel':dict(dark=(86,95,108),mid=(152,163,178),stripe=(46,52,63),belly=(200,208,218),belly2=(158,166,178),horn_a=(150,160,174),horn_b=(238,244,252),spike_a=(84,92,106),spike_b=(216,224,234),claw_a=(30,34,42),claw_b=(204,212,222),tooth=(238,242,246),gum=(96,74,82),tongue=(150,92,102),eye=(120,214,255),pupil=(8,18,30),scar_a=(168,178,192),scar_b=(240,246,252)),
 'arena':dict(dark=(122,106,70),mid=(188,171,122),stripe=(90,40,84),belly=(216,202,162),belly2=(184,168,124),horn_a=(230,216,174),horn_b=(98,74,52),spike_a=(226,210,164),spike_b=(112,86,58),claw_a=(38,30,26),claw_b=(228,216,182),tooth=(240,230,198),gum=(112,26,40),tongue=(188,80,94),eye=(255,178,30),pupil=(34,12,4),scar_a=(136,58,54),scar_b=(226,178,152)),
}
SCALE_K={'arena':1.0,'scarlet':0.85,'green':0.85,'steel':0.4}
def tooth_tex(role,face,ii,jj,fu,fv,pal,big):
    col=np.tile(np.array(pal['tooth'],float),(len(ii),1)); gl=np.zeros_like(col)
    gum=(fv<(0.22 if big else 0.34)) if role=='tooth' and face in('xmin','xmax','north','south') else np.zeros(len(ii),bool)
    gap=(((ii%4)==3) if big else ((ii%3)==2))&(fv>0.3)&np.isin(np.full(len(ii),face),('xmin','xmax'))
    col[gum]=pal['gum']; col[gap]=np.array(pal['gum'],float)*0.55
    shade=0.92+0.08*np.cos(ii*2.1); col*=shade[:,None]
    return col,gl

def mouth_tex(kind,loc,c,pal,ctx):
    """Interior de la boca: paladar con crestas, suelo con encia y lengua con surco; se oscurece hacia la garganta."""
    gum=np.array(pal['gum'],float); tg=np.array(pal['tongue'],float)
    z=loc[:,2]; xr=(loc[:,0]-(c['x0']+c['w']/2.0))/max(1e-6,c['w']/2.0)
    t=np.clip((z+4.0)/50.0,0,1)                      # 0 = garganta, 1 = punta del hocico
    n=fbm(loc/max(0.3,ctx['qs']*0.9),23,2)
    base=tg if kind=='tongue' else gum
    deep=0.30+0.70*ss(0.02,0.60,t)                   # garganta oscura
    edge=1.0-0.30*ss(0.55,1.0,np.abs(xr))            # bordes en sombra
    col=np.tile(base,(len(z),1))*(deep*edge)[:,None]
    if kind=='palate':
        rug=0.5+0.5*np.sin(z*1.15)                   # crestas transversales
        col*=(0.86+0.20*rug*ss(0.1,0.5,t))[:,None]
        col*=(1.0+0.10*np.exp(-(xr/0.10)**2))[:,None] # rafe central
    elif kind=='floor':
        col*=(0.80+0.10*np.sin(z*0.9+1.0))[:,None]
    elif kind=='tongue':
        col*=(1.0-0.22*np.exp(-(xr/0.12)**2))[:,None]  # surco central
        col*=(1.04+0.16*ss(0.45,1.0,t))[:,None]        # punta mas clara
        col*=(0.94+0.12*np.sin(z*2.3)*np.cos(loc[:,0]*2.1))[:,None]  # papilas
    col*=(0.93+0.14*n)[:,None]
    return np.clip(col,0,255)
SCARS=[('body',(25,14,10),(25,-12,-18),1.1,1),('body',(25,16,4),(25,-9,-24),1.0,1),('body',(25,18,-2),(25,-6,-28),0.9,1),
       ('head',(19,9,6),(19,-6,24),1.1,1),('head',(19,11,10),(19,-4,28),0.9,1),('head',(-19,2,22),(-19,-9,34),1.0,-1),('neck1',(18,10,4),(18,-6,16),0.9,1)]
def loc_sign(face): return 1 if face=='xmax' else (-1 if face=='xmin' else 0)
def seg_mask(loc,p0,p1,wd):
    a=np.array(p0,float); b=np.array(p1,float); ab=b-a
    t=np.clip(((loc-a)@ab)/(ab@ab),0,1)
    d=np.linalg.norm(loc-(a+t[:,None]*ab),axis=1)
    return np.clip(1.0-d/wd,0,1)
def base_skin(var,P,N,ly,ctx,face):
    """color de piel + brillo para un conjunto de texels."""
    pal=PAL[var]; n=len(P); q=P/ctx['qs']
    n1=fbm(q*0.045,3); d1,d2,rid=voronoi(P/3.4,5)
    cell=(0.94+0.12*rid)*(1-0.22*ss(0.10,0.0,d2-d1))
    wave=np.sin(q[:,2]*0.30+2.2*fbm(q*0.035+7,11,2)+0.5*np.sin(q[:,1]*0.07))
    st=ss(0.22,0.58,wave)
    opt=ctx['opt']; grp=ctx['grp']
    dw=ss(0.18,0.62,ly)
    if grp=='leg': dw=dw*0.55 if ctx['part'].startswith('thigh') else dw*0.0
    if grp=='arm': dw=dw*0.3
    if opt.get('head') or opt.get('brow') or opt.get('nostril'): dw=dw*0.35
    if face=='up': dw=np.ones(n)*(0.6 if grp=='leg' else 1.0)*(0.35 if opt.get('head') else 1.0)
    b=np.zeros(n)
    if opt.get('bf'): b=1-ss(0.10,0.36,ly)
    if opt.get('jaw'): b=1-ss(0.35,0.70,ly)
    if face=='down' and not opt.get('leg') and grp!='arm' and grp!='horn': b=np.ones(n)
    if ctx['role']=='belly': b=np.ones(n)
    col=lerp(pal['dark'],pal['mid'],n1)*cell[:,None]
    gl=np.zeros((n,3))
    col=lerp(col,np.array(pal['stripe'])*cell[:,None],(st*dw*0.9))
    if var=='green':
        # verde natural: moteado claro en flancos y lomo, mas suave en cabeza y patas
        spk=ss(0.60,0.78,fbm(q*0.14+9,23,2)); col=lerp(col,np.array(pal['mid'])*1.14,spk*0.42*dw)
    if var=='steel':
        # placas de acero: costuras oscuras entre placas grandes, brillo suave en el centro y rebordes
        d1p,d2p,rp=voronoi(P/7.5,17)
        seam=ss(0.16,0.02,d2p-d1p); sheen=ss(0.55,0.0,d1p)
        col=col*(0.90+0.18*rp)[:,None]*(1-0.42*seam)[:,None]*(0.96+0.12*sheen)[:,None]
    lg=ctx['stage']=='adult'
    if lg or var=='arena':
        # escamas grandes y arrugas (referencia fotografica): fuertes en Arena, mas suaves en el resto
        d1p,d2p,rp=voronoi(q/4.0,53)
        seam=ss(0.14,0.0,d2p-d1p)
        wr=np.abs(np.sin(q[:,2]*0.85+3.0*fbm(q*0.08,5,2)+q[:,1]*0.25))
        scl=(0.92+0.16*rp)*(1-0.30*seam)*(0.94+0.06*wr)
        col=col*(1.0+(scl-1.0)*SCALE_K[var])[:,None]
        part=ctx['part']
        pf=1.0
        if opt.get('head') or opt.get('brow') or opt.get('nostril'): pf=0.5
        if grp=='leg': pf=0.6 if part.startswith('thigh') else 0.0
        if grp=='arm': pf=0.25
        top=np.where(face=='up',1.0,ly)*pf
        nz=fbm(q*0.045+5,41,3)-0.5
        sk=(0.80+0.34*rp)*(1-0.38*seam)*cell*(0.92+0.16*n1)
        if var=='arena':
            # lomo carmesi que baja a morado por los flancos
            pur=ss(0.26,0.52,top+0.35*nz); cri=ss(0.52,0.80,top+0.35*nz)
            blot=ss(0.56,0.74,fbm(q*0.05+11,9,3))*pf*ss(0.15,0.5,top)
            col=lerp(col,np.array((112,46,96))*sk[:,None],pur*0.88)
            col=lerp(col,lerp((132,28,46),(190,50,68),np.clip(n1*1.4-0.2,0,1))*sk[:,None],cri*0.94)
            col=lerp(col,np.array((78,32,74))*sk[:,None],blot*0.5)
        elif var=='scarlet':
            dd=ss(0.42,0.80,top+0.30*nz)
            col=lerp(col,np.array((118,24,30))*sk[:,None],dd*0.62)
        elif var=='steel':
            dd=ss(0.40,0.80,top+0.30*nz)
            col=col*(1.0-0.30*dd)[:,None]
        # cicatrices de batalla (solo en el adulto; coordenadas locales de cada pieza)
        if lg:
            sc_=np.zeros(n)
            for (pn,p0,p1,wd,sgn) in SCARS:
                if part!=pn: continue
                if (loc_sign(face)!=sgn): continue
                sc_=np.maximum(sc_,seg_mask(ctx['loc'],p0,p1,wd))
            if sc_.any():
                col=lerp(col,np.array(pal['scar_a']),np.clip(sc_*1.6,0,1)*0.9)
                col=lerp(col,np.array(pal['scar_b']),np.clip(sc_*2.2-1.0,0,1))
    # vientre con escudos transversales
    sc=((P[:,2]/3.8)%1.0<0.16)
    bcol=lerp(pal['belly'],pal['belly2'],np.where(sc,0.8,fbm(q*0.1+2,13,2)*0.5))*(0.97+0.06*rid)[:,None]
    col=lerp(col,bcol,b)
    return col,gl
def horn_tex(var,ly,ctx,ii,jj):
    pal=PAL[var]; k=ctx['opt'].get('k',0); g=(k+ly)/3.0
    ridge=np.where((jj%3)==0,0.88,1.0)
    gl=np.zeros((len(ly),3))
    if var=='scarlet':
        col=lerp(pal['horn_a'],pal['horn_b'],g**1.3)*ridge[:,None]
    elif var=='green':
        col=lerp(pal['horn_a'],pal['horn_b'],g**1.2)*ridge[:,None]
    elif var=='arena':
        col=lerp(pal['horn_a'],pal['horn_b'],g**1.25)*ridge[:,None]
    else:
        col=lerp(pal['horn_a'],pal['horn_b'],g**1.1)*ridge[:,None]
    if ctx['stage']=='adult':
        # adulto: puntas de los cuernos incandescentes en el color de sus ojos
        t=ss(0.80,1.0,g)*0.6; gc=np.array(pal['eye'],float); col=lerp(col,gc,t*0.8); gl=gc*t[:,None]
    return col,gl
def spike_tex(var,ly,ctx,ii,jj):
    pal=PAL[var]; col=lerp(pal['spike_a'],pal['spike_b'],ly**1.2); gl=np.zeros_like(col)
    return col,gl
def claw_tex(var,fu,fv,ctx,face,loc):
    pal=PAL[var]; t=ctx['claw_t'](loc)
    col=lerp(pal['claw_a'],pal['claw_b'],t**1.5); return col,np.zeros_like(col)
def eye_tex(var,loc,ctx,col,gl):
    """ojo redondeado: 8 texels (adulto) / 7 (cria), aro oscuro, iris, pupila vertical y brillo."""
    pal=PAL[var]; sx,sy,sz=ctx['sv']; r={'chibi':3.6,'adult':4.5}[ctx['stage']]
    cz,cy=17.0*sz,3.2*sy            # centro en la cabeza (el cuerno/ceja quedan por encima)
    dz=loc[:,2]-cz; dy=loc[:,1]-cy
    e2=(dz/r)**2+(dy/r)**2
    m=e2<=1.0; ring=m&(e2>0.58); iris=m&~ring
    out=col.copy(); g=gl.copy()
    out[m]=pal['pupil']
    out[iris]=pal['eye']
    pup=iris&(np.abs(dz)<0.7)&(np.abs(dy)<r*0.78)
    out[pup]=pal['pupil']
    glint=iris&((dz-1.3)**2+(dy-1.4)**2<0.9)
    out[glint]=np.array(out[glint])*0.35+np.array([255,255,240])*0.65
    if var!='scarlet' or ctx['stage']=='adult':
        g[iris&~pup]=pal['eye']; g[glint]=(255,255,240)
    return out,g
# ---------------------------------------------------------------- ensamblado
def claw_axis(c):
    if c['part'].startswith('arm2'): return lambda loc:1-(loc[:,1]-c['y0'])/max(1e-6,c['h'])
    return lambda loc:(loc[:,2]-c['z0'])/max(1e-6,c['d'])
def make_atlas(var):
    out,H=rig.get_all()
    fr={st:frames(out[st][0]) for st in out}
    atlas=np.zeros((H,W,4),np.uint8); glow=np.zeros((H,W,4),np.uint8)
    pal=PAL[var]
    for st,(spec,cubes) in out.items():
        qs={'adult':1.3,'chibi':0.6}[st]
        for c in cubes:
            R,T=fr[st][c['part']]
            for face,r in rig.regions(c).items():
                rect,loc,nl,ii,jj,fu,fv=region_geom(c,face,r)
                P=loc@R.T+T; Nw=nl@R.T
                ly=np.clip((loc[:,1]-c['y0'])/max(1e-6,c['h']),0.0,1.0)
                ctx=dict(qs=qs,opt=c['opt'],grp=c['grp'],part=c['part'],role=c['role'],s=c['s'],sv=c['sv'],stage=st,claw_t=claw_axis(c),loc=loc)
                role=c['role']
                if role in rig.SADDLE_ROLES or role in rig.ARMOR_ROLES: continue
                if role in('skin','belly'): col,gl=base_skin(var,P,Nw,ly,ctx,face)
                elif role=='horn': col,gl=horn_tex(var,ly,ctx,ii,jj)
                elif role=='spike': col,gl=spike_tex(var,ly,ctx,ii,jj)
                elif role=='claw': col,gl=claw_tex(var,fu,fv,ctx,face,loc)
                elif role=='tooth': col,gl=tooth_tex('tooth',face,ii,jj,fu,fv,pal,st=='adult')
                else:
                    col=np.tile(np.array(pal['tongue'],float),(len(ii),1))*(0.9+0.1*np.cos(loc[:,0]*0.9))[:,None]; gl=np.zeros_like(col)
                op=c['opt']
                if role=='tooth' and op.get('upper') and face=='down': col=mouth_tex('palate',loc,c,pal,ctx)
                elif role=='tooth' and op.get('lower') and face=='up': col=mouth_tex('floor',loc,c,pal,ctx)
                elif role=='mouth': col=mouth_tex('tongue',loc,c,pal,ctx)
                elif role in('skin','belly') and ((op.get('head') and face=='down') or (op.get('jaw') and face=='up')): col=mouth_tex('palate' if op.get('head') else 'floor',loc,c,pal,ctx)
                if c['part']=='head' and c['i']==0 and face in('xmin','xmax'): col,gl=eye_tex(var,loc,ctx,col,gl)
                x0,y0,x1,y1=rect
                atlas[jj,ii,:3]=np.clip(col,0,255).astype(np.uint8); atlas[jj,ii,3]=255
                if gl.any():
                    lit=gl.sum(axis=1)>6
                    glow[jj[lit],ii[lit],:3]=np.clip(gl[lit],0,255).astype(np.uint8); glow[jj[lit],ii[lit],3]=255
    return atlas,glow
# ---------------------------------------------------------------- montura 3D del Brutus (solo se pinta en carnotaurus_saddle.png)
C=lambda *v: np.array(v,float)
LEA=C(126,78,46); LEA_L=C(156,102,60); LEA_D=C(86,52,32); PIPE=C(54,32,22); STITCH=C(220,190,134)
CLOTH=C(46,58,126); CLOTH_D=C(30,38,94); CLOTH_L=C(74,88,166); TRIM=C(224,186,86); CREAM=C(238,224,190)
BRASS=C(212,168,68); BRASS_L=C(250,222,128); BRASS_D=C(128,92,34)
IRON=C(106,110,120); IRON_L=C(152,156,168); IRON_D=C(58,60,70); WINE=C(98,26,30)
def face_dims(c,face):
    w,h,d=c['w'],c['h'],c['d']
    return {'xmin':(d,h),'xmax':(d,h),'north':(w,h),'south':(w,h),'up':(w,d),'down':(w,d)}[face]
def _diam(a,b,period,rad):
    da=np.abs(a%period-period/2.0); db=np.abs(b%period-period/2.0); return da+db<rad
def saddle_tex(c,face,loc,ii,jj,fu,fv):
    o=c['opt']; role=c['role']; n=len(ii)
    fw,fh=face_dims(c,face)
    eu=np.minimum(fu*fw,(1-fu)*fw); ev=np.minimum(fv*fh,(1-fv)*fh); edge=np.minimum(eu,ev)
    x,y,z=loc[:,0],loc[:,1],loc[:,2]
    nz=fbm(loc*0.9+3.0,41)
    chk=((ii+jj)%2)==0
    side=o.get('side',0)
    outer=(face=='xmax' and side>0) or (face=='xmin' and side<0)
    mask=np.ones(n,bool)
    big=min(fw,fh)>=4.0
    if role=='cloth':
        col=np.tile(CLOTH,(n,1))*(0.93+0.07*(((ii%2)^(jj%2)))[:,None])*(0.94+0.12*nz)[:,None]
        pc=o.get('piece')
        if pc in('top','shelf') and face=='up':
            lat=_diam(x,z,9.0,7.4); col[lat]=CLOTH_L*(0.94+0.1*nz[lat])[:,None]
            col[_diam(x,z,9.0,3.4)]=CLOTH_D; col[_diam(x,z,9.0,1.3)]=TRIM
            if pc=='top':
                col[(edge>=0.9)&(edge<2.1)]=TRIM; col[(edge>=2.7)&(edge<3.3)]=CREAM
            else:
                col[(edge>=0.6)&(edge<1.5)]=TRIM
        elif pc=='drape' and outer:
            lat=_diam(z,y,9.0,7.4); col[lat]=CLOTH_L*(0.94+0.1*nz[lat])[:,None]
            col[_diam(z,y,9.0,3.6)]=CLOTH_D; col[_diam(z,y,9.0,1.4)]=TRIM
            fb=(1-fv)*fh                                         # distancia al borde inferior
            col[(eu>=1.1)&(eu<2.1)]=TRIM
            col[(fb>=2.4)&(fb<3.6)]=TRIM; col[(fb>=3.6)&(fb<4.2)]=CREAM; col[(fb>=4.2)&(fb<5.2)]=CLOTH_D
            fr=fb<2.4; col[fr]=np.where(((ii[fr]%2)==0)[:,None],TRIM,CLOTH_D*0.8)
            mask=~((fb<1.4)&((ii%2)==1))                         # flecos
        elif pc=='riser': col*=0.78
        elif pc=='bedroll':
            col=np.tile(C(198,168,114),(n,1))*(0.93+0.07*(((ii%2)^(jj%2)))[:,None])*(0.92+0.16*nz)[:,None]
            if face in('xmin','xmax'):
                zc=c['z0']+c['d']/2; yc=c['y0']+c['h']/2; r=np.hypot((z-zc)/(c['d']/2),(y-yc)/(c['h']/2))
                col[(np.floor(r*4.0)%2)==0]*=0.8
            else: col[(np.floor((z if face=='up' or face=='down' else x)*0.5)%2)==0]*=0.94
        else:
            col*=0.9
            if pc in('top','shelf','drape'): col[(ev>=0.7)&(ev<1.6)&(fh<5)]=TRIM
        return col,mask
    if role=='leather':
        col=np.tile(LEA_L if o.get('seat') else LEA,(n,1))*(0.90+0.2*nz)[:,None]
        if o.get('strap'):
            col=np.tile(LEA_D,(n,1))*(0.90+0.2*nz)[:,None]
            along_u=fw>=fh; cu=np.abs(fv-0.5)*fh if along_u else np.abs(fu-0.5)*fw
            t=(fu*fw if along_u else fv*fh)
            col[(cu<0.55)&chk]=STITCH
            col[(((t%6.0)>2.4)&((t%6.0)<3.6))&(cu<1.2)]=BRASS
            col[edge<0.5]=PIPE
        elif o.get('seat') and face=='up':
            u=(x+z)/4.4; v=(x-z)/4.4; fu_=u-np.floor(u); fv_=v-np.floor(v)
            ln=(fu_<0.16)|(fv_<0.16); col[ln]=LEA_D*(0.95+0.1*nz[ln])[:,None]; col[(fu_<0.16)&(fv_<0.16)]=BRASS
            col[edge<0.7]=PIPE
        elif o.get('cantle') and face=='south':
            col*=0.92
            ym=38.6; R=3.1; r=np.hypot(x,y-ym)
            col[r<R]=LEA_D; col[(r<R)&(r>=R-1.3)]=BRASS
            dx=np.abs(x); dy=y-ym
            hornm=(r<R-1.3)&(np.abs(dx*0.9-(dy+1.4)*0.75)<0.55)&(dy>-1.6)
            col[hornm]=BRASS_L
            stud=((ii%3)==0)&((jj%3)==0)&(edge>=1.3)&(edge<2.6)&(r>R+1.0); col[stud]=BRASS
            col[(edge<0.7)]=PIPE
        elif o.get('skirt') and outer:
            col[(edge<0.8)]=PIPE; col[(edge>=1.3)&(edge<2.0)&chk]=STITCH
            lat=_diam(z,y,7.0,4.6); col[lat&(edge>=2.4)]=col[lat&(edge>=2.4)]*0.78
            rc=np.hypot(z-(c['z0']+c['d']/2),y-(c['y0']+c['h']/2)); col[rc<2.7]=BRASS_D; col[rc<2.0]=BRASS; col[rc<0.9]=BRASS_L
        elif big:
            col[edge<0.7]=PIPE; col[(edge>=1.2)&(edge<1.9)&chk]=STITCH
        return col,mask
    # metal
    if o.get('iron'):
        col=np.tile(IRON,(n,1))*(0.88+0.24*nz)[:,None]; col[edge<0.6]=IRON_D
        if face=='up': col[edge>=0.6]=IRON_L*(0.92+0.1*nz[edge>=0.6])[:,None]
        return col,mask
    col=np.tile(BRASS,(n,1))*(0.90+0.2*nz)[:,None]
    if face=='up': col=np.tile(BRASS_L,(n,1))*(0.92+0.1*nz)[:,None]
    col[edge<0.6]=BRASS_D
    if o.get('buckle') and outer:
        col=np.tile(LEA_D,(n,1)); col[edge<1.3]=BRASS; col[edge<0.5]=BRASS_D; col[np.abs(fv-0.5)*fh<0.65]=BRASS_L
    if o.get('medal') and outer:
        zc=c['z0']+c['d']/2; yc=c['y0']+c['h']/2
        u=(z-zc)/(c['d']/2.0); v=(y-yc)/(c['h']/2.0); r=np.hypot(u,v)
        mask=r<1.12
        col=np.tile(BRASS_D,(n,1)); col[r<0.86]=BRASS_L; col[r<0.7]=WINE
        horn=(r<0.7)&(np.abs(np.abs(u)*1.5-(v+0.45)*0.9)<0.30)&(v<0.55)
        col[horn]=BRASS_L; col[(r<0.7)&(np.abs(u)<0.16)&(v<0.1)]=BRASS
    return col,mask
def make_saddle():
    out,H=rig.get_all(); sad=np.zeros((H,W,4),np.uint8)
    spec,cubes=out['adult']
    for c in cubes:
        if c['role'] not in rig.SADDLE_ROLES: continue
        for face,r in rig.regions(c).items():
            rect,loc,nl,ii,jj,fu,fv=region_geom(c,face,r)
            col,m=saddle_tex(c,face,loc,ii,jj,fu,fv)
            if m.any():
                sad[jj[m],ii[m],:3]=np.clip(col[m],0,255).astype(np.uint8); sad[jj[m],ii[m],3]=255
    return sad
def saddle_mask():
    """texels de la malla ocupados por cubos de la montura (en la piel deben quedar transparentes)."""
    out,H=rig.get_all(); m=np.zeros((H,W),bool)
    for c in out['adult'][1]:
        if c['role'] in rig.SADDLE_ROLES:
            for face,r in rig.regions(c).items():
                x0,y0,x1,y1=rig.prect(r); m[y0:y1,x0:x1]=True
    return m

# ---------------------------------------------------------------- armadura 3D del Brutus (se pinta solo en carnotaurus_armor_<nivel>.png)
ARMOR_TIERS=('iron','gold','diamond','netherite')
APAL={
 'iron':     dict(base=C(150,155,166),light=C(204,209,220),dark=C(78,82,94),trim=C(98,102,116),rivet=C(222,226,234),gem=C(232,52,52),gem_d=C(120,16,24),tint=(1.0,0.28,0.26)),
 'gold':     dict(base=C(228,182,54),light=C(255,232,126),dark=C(142,98,22),trim=C(176,122,30),rivet=C(255,240,170),gem=C(64,222,124),gem_d=C(14,100,56),tint=(0.26,1.0,0.50)),
 'diamond':  dict(base=C(74,198,198),light=C(176,252,244),dark=C(26,104,118),trim=C(38,138,152),rivet=C(220,255,252),gem=C(84,226,255),gem_d=C(16,96,140),tint=(0.34,0.90,1.0)),
 'netherite':dict(base=C(70,60,76),light=C(122,110,132),dark=C(32,26,38),trim=C(176,132,72),rivet=C(222,184,110),gem=C(196,84,255),gem_d=C(74,20,112),tint=(0.78,0.34,1.0)),
}
def _axis_t(c,loc):
    """posicion 0..1 a lo largo del eje del pincho (0 = base, 1 = punta)."""
    ax=c['opt'].get('ax','y'); x,y,z=loc[:,0],loc[:,1],loc[:,2]
    pos={'x':(x-c['x0'])/c['w'],'y':(y-c['y0'])/c['h'],'z':(z-c['z0'])/c['d']}[ax[0]]
    pos=np.clip(pos,0,1)
    return 1-pos if ax.endswith('n') else pos
def armor_tex(tier,c,face,loc,ii,jj,fu,fv):
    """devuelve (color, brillo 0..1, mascara) de los texels de una cara de un cubo de armadura."""
    pal=APAL[tier]; o=c['opt']; n=len(ii); kind=o.get('kind','plate'); r2=o.get('role2','')
    fw,fh=face_dims(c,face)
    eu=np.minimum(fu*fw,(1-fu)*fw); ev=np.minimum(fv*fh,(1-fv)*fh); edge=np.minimum(eu,ev)
    u=fu*fw; v=fv*fh
    x,y,z=loc[:,0],loc[:,1],loc[:,2]
    seed=zlib.crc32(('%s:%d'%(c['part'],c['i'])).encode())%1000
    nz=fbm(loc*0.55+seed*0.37,31+seed%7)
    glow=np.zeros(n); mask=np.ones(n,bool)
    if kind=='gem':
        d1,d2,rid=voronoi(loc/1.5+seed,77)
        facet=0.55+0.45*rid
        col=lerp(pal['gem_d'],pal['gem'],np.clip(facet*1.1,0,1)); col=np.asarray(col)
        col=np.where((ss(0.0,0.35,d2-d1)<0.5)[:,None],col*1.25,col)
        ring=edge<0.75
        col[ring]=pal['trim']*0.8
        glow=np.where(ring,0.0,0.55+0.45*facet)
        return np.clip(col,0,255),glow,mask
    if kind=='spike':
        t=_axis_t(c,loc)
        col=np.asarray(lerp(pal['dark'],pal['light'],t**1.25))*(0.92+0.16*nz)[:,None]
        col[edge<0.5]*=0.7
        return np.clip(col,0,255),glow,mask
    base=pal['trim'] if kind=='band' else pal['base']
    col=np.tile(np.asarray(base,float),(n,1))*(0.90+0.20*nz)[:,None]
    # patron propio de cada material
    if tier=='iron':
        streak=0.94+0.07*hsh(jj*0+0,jj,jj*0,seed)           # vetas horizontales de acero cepillado
        col*=streak[:,None]
    elif tier=='gold':
        lat=_diam(x+0.5*z,y+0.5*z,6.0,3.2); col[lat]=np.asarray(pal['light'])*(0.88+0.1*nz[lat])[:,None]*0.96
        col[_diam(x+0.5*z,y+0.5*z,6.0,1.1)]=pal['dark']
    elif tier=='diamond':
        d1,d2,rid=voronoi(loc/3.6+seed,91)
        col*=(0.86+0.28*rid)[:,None]
        seam=ss(0.14,0.0,d2-d1); col=np.asarray(lerp(col,pal['light'],(seam*0.75)[:,None]*np.ones((n,3))))
    else:
        flk=ss(0.66,0.78,fbm(loc*0.9+seed,13,2)); col=np.asarray(lerp(col,pal['trim']*0.7,(flk*0.55)[:,None]*np.ones((n,3))))
    # laminas transversales en las placas largas (cuello, flancos, cola, vientre, grupa, muslo)
    if kind=='plate' and r2 in ('ntop','nside','lame','tail','tailside','belly','rump','thigh','neck') and face in('up','down','xmin','xmax'):
        along=(z-c['z0'])
        seam=((along%6.4)<0.9)&(along>1.0)
        col[seam]*=0.62
    if kind=='plate' and r2 in ('breast',) and face=='north':
        col[((v%5.0)<0.9)&(v>1.5)]*=0.66
    mn=min(fw,fh)
    # bisel y contorno
    if kind in('plate','band'):
        col[edge<0.7]=pal['dark']
        lit=(face=='up')|((face in('north','xmin') ) & (fu<0.5))
        col[(edge>=0.7)&(edge<1.5)]=np.asarray(pal['light'])*(0.94+0.1*nz[(edge>=0.7)&(edge<1.5)])[:,None] if face=='up' else col[(edge>=0.7)&(edge<1.5)]*1.12
        if mn>=7.0:
            col[(edge>=2.1)&(edge<2.9)]=pal['trim']*(0.9+0.2*nz[(edge>=2.1)&(edge<2.9)])[:,None]
        # remaches en las esquinas y cada ~7 unidades
        if mn>=5.0:
            for cx in (2.2,fw-2.2):
                for cy in (2.2,fh-2.2):
                    rv=(np.abs(u-cx)<0.95)&(np.abs(v-cy)<0.95); col[rv]=pal['rivet']; col[rv&(u<cx)&(v<cy)]=pal['rivet']*1.0
        if fw>=14 and fh>=5.0:
            for k in range(1,int(fw//7.0)):
                cx=k*fw/int(fw//7.0); rv=(np.abs(u-cx)<0.8)&(np.abs(v-fh/2)<0.8); col[rv]=pal['rivet']
    if kind=='band':
        col[(edge>=0.7)&(edge<1.4)]=pal['rivet']*0.85
    return np.clip(col,0,255),glow,mask
def make_armor(tier):
    """atlas (RGBA) de una armadura y su brillo (gemas, en blanco: se tine por nivel al dibujar)."""
    out,H=rig.get_all(); atlas=np.zeros((H,W,4),np.uint8); glow=np.zeros((H,W,4),np.uint8)
    for c in out['adult'][1]:
        if c['role'] not in rig.ARMOR_ROLES: continue
        for face,r in rig.regions(c).items():
            rect,loc,nl,ii,jj,fu,fv=region_geom(c,face,r)
            col,g,m=armor_tex(tier,c,face,loc,ii,jj,fu,fv)
            atlas[jj[m],ii[m],:3]=np.clip(col[m],0,255).astype(np.uint8); atlas[jj[m],ii[m],3]=255
            lit=m&(g>0.02)
            if lit.any():
                gv=np.clip(g[lit]*255,0,255).astype(np.uint8)
                glow[jj[lit],ii[lit],0]=gv; glow[jj[lit],ii[lit],1]=gv; glow[jj[lit],ii[lit],2]=gv; glow[jj[lit],ii[lit],3]=255
    return atlas,glow
def armor_mask():
    """texels de la malla ocupados por cubos de armadura (en la piel deben quedar transparentes)."""
    out,H=rig.get_all(); m=np.zeros((H,W),bool)
    for c in out['adult'][1]:
        if c['role'] in rig.ARMOR_ROLES:
            for face,r in rig.regions(c).items():
                x0,y0,x1,y1=rig.prect(r); m[y0:y1,x0:x1]=True
    return m
def fit_hy(stage):
    """altura del pivote del cuerpo para que el punto mas bajo de la pose de reposo toque el suelo."""
    out,H=rig.get_all(); spec,cubes=out[stage]; fr=frames(spec); lo=1e9
    for c in cubes:
        if c['role'] in rig.ARMOR_ROLES: continue
        R,T=fr[c['part']]
        for x in (c['x0'],c['x0']+c['w']):
            for y in (c['y0'],c['y0']+c['h']):
                for z in (c['z0'],c['z0']+c['d']):
                    lo=min(lo,(T+R@np.array([x,y,z]))[1])
    return 90.0-lo
def dilate(a,passes=2):
    a=a.copy()
    for _ in range(passes):
        al=a[...,3]>0; new=a.copy(); done=al.copy()
        for dy,dx in ((1,0),(-1,0),(0,1),(0,-1)):
            sh=np.roll(a,(dy,dx),(0,1)); sa=np.roll(al,(dy,dx),(0,1))
            m=(~done)&sa; new[m]=sh[m]; done|=m
        a=new
    return a
