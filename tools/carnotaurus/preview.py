"""Vista previa (rasterizador de puntos) del atlas del carnotauro. Uso: python3 preview.py <variante|atlas.png> <adult|chibi> <salida.png> [yaw_grados] [elev_grados]"""
import sys, os, math, numpy as np
sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
import rig, paint
from PIL import Image
def render(atlas,stage='adult',yaw=90.0,elev=12.0,scale=2.6,size=(1000,660)):
    out,H=rig.get_all(); spec,cubes=out[stage]; fr=paint.frames(spec)
    ya,ea=math.radians(yaw),math.radians(elev)
    # camara: gira alrededor de Y (yaw=90 => mira desde +x), luego inclina
    Ryaw=paint.Ry(-(ya-math.pi/2)); 
    cam=paint.Rx(-ea)
    look=np.array([math.sin(ya),0,math.cos(ya)])  # direccion hacia la camara
    look=np.array([math.sin(ya),math.sin(ea),math.cos(ya)*0+math.cos(ea)*0])
    # base de camara explicita
    to_cam=np.array([math.cos(ea)*math.sin(ya),math.sin(ea),math.cos(ea)*math.cos(ya)]); to_cam/=np.linalg.norm(to_cam)
    up0=np.array([0,1,0.0]); right=np.cross(up0,to_cam); right/=np.linalg.norm(right); up=np.cross(to_cam,right)
    Wd,Hd=size; img=np.zeros((Hd,Wd,4),np.uint8); zb=np.full((Hd,Wd),-1e9)
    img[...,:3]=(38,40,56); img[...,3]=255
    ctr=np.array([0,82.0,-29.0] if stage=='adult' else [0,90.0,-1.0]); cx=Wd*0.5-(ctr@right)*scale; cy=Hd*0.5+(ctr@up)*scale
    for c in cubes:
        if c['role'] in rig.SADDLE_ROLES or c['role'] in rig.ARMOR_ROLES: continue
        R,T=fr[c['part']]
        for face,r in rig.regions(c).items():
            rect,loc,nl,ii,jj,fu,fv=paint.region_geom(c,face,r)
            Nw=R@nl
            if Nw@to_cam<=-0.05: continue
            P=loc@R.T+T
            sx=P@right; sy=P@up; dp=P@to_cam
            x0,y0,x1,y1=rect; fw,fh=paint.face_dims(c,face)
            tw=max(fw/max(1,x1-x0),fh/max(1,y1-y0)); sp=int(math.ceil(tw*scale*0.95))+1
            px=(cx+sx*scale-sp/2).astype(int); py=(cy-sy*scale-sp/2).astype(int)
            cols=atlas[jj,ii]
            for k in range(len(px)):
                if cols[k,3]==0: continue
                xa,ya_=px[k],py[k]
                xs=slice(max(0,xa),min(Wd,xa+sp)); ys=slice(max(0,ya_),min(Hd,ya_+sp))
                if xs.start>=xs.stop or ys.start>=ys.stop: continue
                m=zb[ys,xs]<dp[k]
                if m.any():
                    sh=0.78+0.22*max(0.0,float(Nw@to_cam))   # sombreado simple
                    blk=img[ys,xs]; blk[m,:3]=(cols[k,:3]*sh).astype(np.uint8); zb[ys,xs][m]=dp[k]
    return img
if __name__=='__main__':
    a=sys.argv[1]; st=sys.argv[2]; o=sys.argv[3]; yaw=float(sys.argv[4]) if len(sys.argv)>4 else 90.0; el=float(sys.argv[5]) if len(sys.argv)>5 else 12.0
    if os.path.exists(a): atl=np.array(Image.open(a).convert('RGBA'))
    else: atl,_=paint.make_atlas(a)
    Image.fromarray(render(atl,st,yaw,el)).save(o)
