import numpy as np, math
from PIL import Image
import bb2java as B
def rgb(h): h=h.lstrip('#'); return np.array([int(h[i:i+2],16) for i in (0,2,4)],float)
GLOW_KEYS={'magma':['accent','spine_light','eye'],'emerald':['accent','spine_light','eye']}
for var in ('scarlet','magma','emerald'):
    a,pal=B.bake(var); a.save(B.TEX+'carnotaurus_%s.png'%var); a.save(B.OUT+'atlas_%s.png'%var)
    if var in GLOW_KEYS:
        arr=np.array(a).astype(float); rgbv=arr[...,:3]; alpha=arr[...,3]
        d=np.full(alpha.shape,1e9)
        for k in GLOW_KEYS[var]:
            t=rgb(pal[k]); d=np.minimum(d,np.linalg.norm(rgbv-t,axis=2))
            t2=(t+255)/2; d=np.minimum(d,np.linalg.norm(rgbv-t2,axis=2))
        m=(d<62)&(alpha>0)
        g=np.zeros_like(arr,dtype=np.uint8); g[m,:3]=arr[m,:3].astype(np.uint8); g[m,3]=255
        Image.fromarray(g,'RGBA').save(B.TEX+'carnotaurus_%s_glow.png'%var); Image.fromarray(g,'RGBA').save(B.OUT+'glow_%s.png'%var)
        print(var,'glow px',int(m.sum()),'de',int((alpha>0).sum()))
        comp=a.copy(); comp.alpha_composite(Image.fromarray(g,'RGBA')); comp.save(B.OUT+'atlas_%s_comp.png'%var)
# silla: capa superior del torso (cara up) + correas en los costados del torso
sad=Image.new('RGBA',(B.ATLAS,B.ATLAS),(0,0,0,0)); px=sad.load()
cub={(c['stage'],c['name']):c for c in B.allcubes}
leather=(112,72,42,255); dk=(70,42,26,255); strap=(58,34,22,255); metal=(176,170,160,255)
top=cub[('adult','torso_capa_superior')]; R=B.regions(top)
x0,y0,x1,y1=B.prect(R['up'])   # u -> ancho (x), v -> profundidad (z de -15 a 18)
for yy in range(y0,y1):
    zz=yy-y0-15  # z relativa al centro (z=0 en yy offset 15)
    for xx in range(x0,x1):
        if -9<=zz<=9 and 1<=xx-x0<=(x1-x0-2):
            px[xx,yy]=leather if (-8<=zz<=8 and 2<=xx-x0<=(x1-x0-3)) else dk
for k in (-1,1):
    pass
main=cub[('adult','torso_principal')]; R=B.regions(main)
for fn in ('east','west'):
    x0,y0,x1,y1=B.prect(R[fn])
    for yy in range(y0,y1):
        for xx in range(x0,x1):
            col=xx-x0
            if y0<=yy<y0+ (y1-y0) and (14<=col<=17 or 22<=col<=24) : px[xx,yy]=strap
    # hebilla
    for xx in range(x0+14,x0+18):
        px[xx,y0+ (y1-y0)-3]=metal
sad.save(B.TEX+'carnotaurus_saddle.png')
print('listo')
