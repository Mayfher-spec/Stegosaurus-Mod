"""Pintor de texturas del carnotauro (3 variantes) + brillo + silla. Patrones en 3D en pose de reposo => sin costuras entre caras/partes."""
import numpy as np, math, sys
import os
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
 'scarlet':dict(dark=(176,48,34),mid=(222,86,46),stripe=(122,32,32),belly=(232,200,150),belly2=(206,166,118),horn_a=(236,224,186),horn_b=(112,90,66),spike_a=(100,72,58),spike_b=(54,40,38),claw_a=(44,36,36),claw_b=(176,160,130),tooth=(242,238,222),gum=(168,66,76),tongue=(196,78,94),eye=(255,196,40),pupil=(30,14,10)),
 'magma':dict(dark=(30,26,30),mid=(70,54,54),stripe=(14,12,16),belly=(92,66,56),belly2=(66,48,42),horn_a=(40,32,36),horn_b=(16,12,16),spike_a=(36,30,34),spike_b=(14,12,16),claw_a=(26,22,24),claw_b=(90,70,60),tooth=(236,226,205),gum=(150,50,30),tongue=(255,110,30),eye=(255,150,30),pupil=(40,8,0)),
 'steel':dict(dark=(86,95,108),mid=(152,163,178),stripe=(46,52,63),belly=(200,208,218),belly2=(158,166,178),horn_a=(150,160,174),horn_b=(238,244,252),spike_a=(84,92,106),spike_b=(216,224,234),claw_a=(30,34,42),claw_b=(204,212,222),tooth=(238,242,246),gum=(96,74,82),tongue=(150,92,102),eye=(120,214,255),pupil=(8,18,30)),
 'brutus':dict(dark=(122,106,70),mid=(188,171,122),stripe=(90,40,84),belly=(216,202,162),belly2=(184,168,124),horn_a=(230,216,174),horn_b=(98,74,52),spike_a=(226,210,164),spike_b=(112,86,58),claw_a=(38,30,26),claw_b=(228,216,182),tooth=(240,230,198),gum=(112,26,40),tongue=(188,80,94),eye=(255,178,30),pupil=(34,12,4)),
}
GLOWC={'magma':((190,40,8),(255,130,24),(255,228,120))}
def tooth_tex(role,face,ii,jj,fu,fv,pal,var):
    col=np.tile(np.array(pal['tooth'],float),(len(ii),1)); gl=np.zeros_like(col)
    big=(var=='brutus')
    gum=(fv<(0.22 if big else 0.34)) if role=='tooth' and face in('xmin','xmax','north','south') else np.zeros(len(ii),bool)
    gap=(((ii%4)==3) if big else ((ii%3)==2))&(fv>0.3)&np.isin(np.full(len(ii),face),('xmin','xmax'))
    col[gum]=pal['gum']; col[gap]=np.array(pal['gum'],float)*0.55
    shade=0.92+0.08*np.cos(ii*2.1); col*=shade[:,None]
    return col,gl
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
    if var=='magma':
        a_,bc,cc=GLOWC['magma']
        e=(lambda dd:(dd[1]-dd[0])*11.0)(voronoi(q/11.0,31)[:2])
        crack=ss(1.2,0.38,e)*ss(0.30,0.52,fbm(q*0.07+3,19,2))*(0.35+0.65*dw)
        core=ss(0.7,1.0,crack)
        gc=lerp(lerp(a_,bc,ss(0.0,0.6,crack)),cc,core)
        wgt=crack*(1-0.55*b)
        col=lerp(col,gc,wgt); gl=gc*wgt[:,None]
    if var=='steel':
        # placas de acero: costuras oscuras entre placas grandes, brillo suave en el centro y rebordes
        d1p,d2p,rp=voronoi(P/7.5,17)
        seam=ss(0.16,0.02,d2p-d1p); sheen=ss(0.55,0.0,d1p)
        col=col*(0.90+0.18*rp)[:,None]*(1-0.42*seam)[:,None]*(0.96+0.12*sheen)[:,None]
    if var=='brutus':
        # escamas grandes y arrugas como en la referencia; lomo carmesi que baja a morado por los flancos
        d1p,d2p,rp=voronoi(P/5.2,53)
        seam=ss(0.14,0.0,d2p-d1p)
        wr=np.abs(np.sin(q[:,2]*0.85+3.0*fbm(q*0.08,5,2)+q[:,1]*0.25))
        col=col*(0.92+0.16*rp)[:,None]*(1-0.30*seam)[:,None]*(0.94+0.06*wr)[:,None]
        part=ctx['part']
        pf=1.0
        if opt.get('head') or opt.get('brow') or opt.get('nostril'): pf=0.5
        if grp=='leg': pf=0.6 if part.startswith('thigh') else 0.0
        if grp=='arm': pf=0.25
        top=np.where(face=='up',1.0,ly)*pf
        nz=fbm(q*0.045+5,41,3)-0.5
        pur=ss(0.26,0.52,top+0.35*nz); cri=ss(0.52,0.80,top+0.35*nz)
        blot=ss(0.56,0.74,fbm(q*0.05+11,9,3))*pf*ss(0.15,0.5,top)
        sk=(0.80+0.34*rp)*(1-0.38*seam)*cell*(0.92+0.16*n1)
        col=lerp(col,np.array((112,46,96))*sk[:,None],pur*0.88)
        col=lerp(col,lerp((132,28,46),(190,50,68),np.clip(n1*1.4-0.2,0,1))*sk[:,None],cri*0.94)
        col=lerp(col,np.array((78,32,74))*sk[:,None],blot*0.5)
        # cicatrices de batalla (coordenadas locales de cada pieza)
        sc_=np.zeros(n)
        for (pn,p0,p1,wd,sgn) in SCARS:
            if part!=pn: continue
            if (loc_sign(face)!=sgn): continue
            sc_=np.maximum(sc_,seg_mask(ctx['loc'],p0,p1,wd))
        if sc_.any():
            col=lerp(col,np.array((136,58,54)),np.clip(sc_*1.6,0,1)*0.9)
            col=lerp(col,np.array((226,178,152)),np.clip(sc_*2.2-1.0,0,1))
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
    elif var=='magma':
        col=lerp(pal['horn_a'],pal['horn_b'],g)*ridge[:,None]
        t=ss(0.55,0.95,g); gc=lerp(GLOWC['magma'][0],GLOWC['magma'][2],ss(0.7,1.0,g)*0.9+0.1); col=lerp(col,gc,t); gl=gc*t[:,None]
    elif var=='brutus':
        col=lerp(pal['horn_a'],pal['horn_b'],g**1.25)*ridge[:,None]
        t=ss(0.80,1.0,g)*0.6; gc=np.array((255,170,48)); col=lerp(col,gc,t*0.8); gl=gc*t[:,None]
    else:
        col=lerp(pal['horn_a'],pal['horn_b'],g**1.1)*ridge[:,None]
    return col,gl
def spike_tex(var,ly,ctx,ii,jj):
    pal=PAL[var]; col=lerp(pal['spike_a'],pal['spike_b'],ly**1.2); gl=np.zeros_like(col)
    if var=='magma':
        t=ss(0.45,1.0,ly); gc=lerp(GLOWC['magma'][0],GLOWC['magma'][1],ly); col=lerp(col,gc,t); gl=gc*t[:,None]
    return col,gl
def claw_tex(var,fu,fv,ctx,face,loc):
    pal=PAL[var]; t=ctx['claw_t'](loc)
    col=lerp(pal['claw_a'],pal['claw_b'],t**1.5); return col,np.zeros_like(col)
def eye_tex(var,loc,ctx,col,gl):
    """ojo redondeado: 8 texels (adulto) / 7 (cria), aro oscuro, iris, pupila vertical y brillo."""
    pal=PAL[var]; sx,sy,sz=ctx['sv']; r={'chibi':3.6,'adult':4.0,'brutus':4.5}[ctx['stage']]
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
    if var!='scarlet':
        g[iris&~pup]=pal['eye']; g[glint]=(255,255,240)
    return out,g
# ---------------------------------------------------------------- ensamblado
def claw_axis(c):
    if c['part'].startswith('arm2'): return lambda loc:1-(loc[:,1]-c['y0'])/max(1e-6,c['h'])
    return lambda loc:(loc[:,2]-c['z0'])/max(1e-6,c['d'])
def stages_for(var):
    """etapas que usa cada variante: la legendaria usa su malla propia de adulto y la cria normal; las demas, adulto y cria normales."""
    return ('chibi','brutus') if var=='brutus' else ('adult','chibi')
def make_atlas(var):
    out,H=rig.get_all()
    fr={st:frames(out[st][0]) for st in out}
    atlas=np.zeros((H,W,4),np.uint8); glow=np.zeros((H,W,4),np.uint8)
    pal=PAL[var]
    for st,(spec,cubes) in out.items():
        qs={'adult':1.0,'chibi':0.6,'brutus':1.3}[st]
        if st not in stages_for(var): continue
        for c in cubes:
            R,T=fr[st][c['part']]
            for face,r in rig.regions(c).items():
                rect,loc,nl,ii,jj,fu,fv=region_geom(c,face,r)
                P=loc@R.T+T; Nw=nl@R.T
                ly=np.clip((loc[:,1]-c['y0'])/max(1e-6,c['h']),0.0,1.0)
                ctx=dict(qs=qs,opt=c['opt'],grp=c['grp'],part=c['part'],role=c['role'],s=c['s'],sv=c['sv'],stage=st,claw_t=claw_axis(c),loc=loc)
                role=c['role']
                if role in('skin','belly'): col,gl=base_skin(var,P,Nw,ly,ctx,face)
                elif role=='horn': col,gl=horn_tex(var,ly,ctx,ii,jj)
                elif role=='spike': col,gl=spike_tex(var,ly,ctx,ii,jj)
                elif role=='claw': col,gl=claw_tex(var,fu,fv,ctx,face,loc)
                elif role=='tooth': col,gl=tooth_tex('tooth',face,ii,jj,fu,fv,pal,var)
                else:
                    col=np.tile(np.array(pal['tongue'],float),(len(ii),1))*(0.9+0.1*np.cos(loc[:,0]*0.9))[:,None]; gl=np.zeros_like(col)
                    if var=='magma': gl=col*0.9
                if c['part']=='head' and c['i']==0 and face in('xmin','xmax'): col,gl=eye_tex(var,loc,ctx,col,gl)
                x0,y0,x1,y1=rect
                atlas[jj,ii,:3]=np.clip(col,0,255).astype(np.uint8); atlas[jj,ii,3]=255
                if gl.any():
                    lit=gl.sum(axis=1)>6
                    glow[jj[lit],ii[lit],:3]=np.clip(gl[lit],0,255).astype(np.uint8); glow[jj[lit],ii[lit],3]=255
    return atlas,glow
def make_saddle():
    out,H=rig.get_all(); sad=np.zeros((H,W,4),np.uint8)
    for st in ('adult','brutus'):
        spec,cubes=out[st]
        k=dict((p['name'],p['sv']) for p in spec)['body'][2]      # escala en z del cuerpo
        Z0,Z1=-25.0*k,9.0*k
        B0,B1=-9.0*k,-3.5*k
        for c in cubes:
            if c['part']!='body' or c['i'] not in(0,1,2): continue
            for face,r in rig.regions(c).items():
                rect,loc,nl,ii,jj,fu,fv=region_geom(c,face,r)
                z=loc[:,2]; ly=(loc[:,1]-c['y0'])/max(1e-6,c['h'])
                inz=(z>=Z0)&(z<=Z1)
                col=np.zeros((len(ii),3)); m=np.zeros(len(ii),bool)
                if c['i'] in(0,1):
                    lo=0.0 if c['i']==1 else 0.52
                    if face=='up': m=inz
                    elif face in('xmin','xmax'): m=inz&(ly>lo)
                    elif face in('north','south'): m=(np.abs(z-(Z1 if face=='north' else Z0))<0.6)&(ly>lo)
                    edge=(np.minimum(z-Z0,Z1-z)<1.6)
                    if c['i']==0 and face in('xmin','xmax'): edge=edge|(ly<0.58)
                    col[:]=(118,74,44); col[edge]=(70,42,28)
                    col[((ii+jj)%3==0)&edge]=(176,132,84)
                    if c['i']==0 and face in('xmin','xmax'): col[~edge]=(150,108,70)
                    band=(z>B0)&(z<B1)
                    if c['i']==0 and face in('xmin','xmax'):
                        m=m|(band&(ly>0.12)); col[band]=(78,48,32)
                        buck=band&(ly>0.40)&(ly<0.52)&(z>B0+1.5*k)&(z<B1-1.5*k); col[buck]=(214,190,96); m=m|buck
                    if face=='up': col[band]=(78,48,32)
                else:
                    band=(z>B0)&(z<B1); m=band; col[:]=(78,48,32)
                if m.any():
                    sad[jj[m],ii[m],:3]=np.clip(col[m],0,255).astype(np.uint8); sad[jj[m],ii[m],3]=255
    return sad
def fit_hy(stage):
    """altura del pivote del cuerpo para que el punto mas bajo de la pose de reposo toque el suelo."""
    out,H=rig.get_all(); spec,cubes=out[stage]; fr=frames(spec); lo=1e9
    for c in cubes:
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
