import sys, numpy as np
import mrig, mpaint
cp=mpaint.cp
(spec,cubes),H=mrig.get_all(); fr=cp.frames(spec)
pts=[]
for c in cubes:
    R,T=fr[c['part']]
    for x in (c['x0'],c['x0']+c['w']):
        for y in (c['y0'],c['y0']+c['h']):
            for z in (c['z0'],c['z0']+c['d']): pts.append(T+R@np.array([x,y,z]))
pts=np.array(pts); lo=pts.min(0); hi=pts.max(0)
print('lo',lo.round(1),'hi',hi.round(1)); L=hi[2]-lo[2]; Hh=hi[1]-lo[1]; Wd=hi[0]-lo[0]
print('largo',L.round(1),'alto',Hh.round(1),'ancho',Wd.round(1),'H atlas',H)
for sc in (0.17,0.18,0.19,0.2): print(sc,'largo bloques',round(L*sc/16,3),'alto',round(Hh*sc/16,3),'ancho',round(Wd*sc/16,3))
