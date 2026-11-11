"""Huevos de carnotauro: 4 colores x 4 etapas (64x64, mismo modelo que el huevo de estegosaurio) + icono 16x16."""
import random, sys
import numpy as np
from PIL import Image
import os
ROOT=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..','src','main','resources','assets','prodigycreatures','textures')+os.sep
CUBES=[((6,2,6),(0,0)),((8,2,8),(24,0)),((10,4,10),(0,10)),((8,2,8),(0,24)),((6,2,6),(32,24)),((4,1,4),(0,34))]
# nombre: base clara, base oscura, mancha A, mancha B, veta/acento, borde-icono
PAL={
 'scarlet':[(176,52,40),(138,36,30),(236,170,120),(96,24,22),(250,206,150)],
 'green':  [(98,142,62),(66,106,46),(218,210,152),(30,60,36),(246,232,150)],
 'steel':  [(120,130,144),(92,102,116),(190,200,212),(52,60,72),(150,225,255)],
 'arena':  [(196,176,126),(168,148,102),(150,40,60),(112,36,70),(232,92,70)],
}
def faces(u,v,w,h,d):
    return {"top":(u+d,v,w,d),"bottom":(u+d+w,v,w,d),"west":(u,v+d,d,h),"front":(u+d,v+d,w,h),"east":(u+d+w,v+d,d,h),"back":(u+2*d+w,v+d,w,h)}
def base_img(name):
    rnd=random.Random(sum(map(ord,name))*31+5); p=PAL[name]
    img=Image.new('RGBA',(64,64),(0,0,0,0))
    for (w,h,d),(u,v) in CUBES:
        for fname,(x,y,fw,fh) in faces(u,v,w,h,d).items():
            for j in range(fh):
                for i in range(fw):
                    r=rnd.random(); gx,gy=x+i,y+j
                    c=p[0] if r<.55 else p[1]
                    blob=((gx//2)*7+(gy//2)*13+len(name)*5)%11
                    if name=='scarlet':
                        if (gx+gy*2)%7 in (0,1) and r<.8: c=p[3]          # rayas oscuras de tigre
                        elif blob==4 and r<.6: c=p[2]
                    elif name=='green':                                     # verde dinosaurio: moteado crema y manchas oscuras
                        if blob in (0,1) and r<.7: c=p[2]
                        elif blob in (5,6) and r<.65: c=p[3]
                    elif name=='steel':
                        if (gx%5==0 or gy%5==0): c=p[3]                      # junta de placas
                        elif blob in (0,1) and r<.7: c=p[2]
                    else:  # arena: crema con manchas carmesi y moteado
                        if blob in (0,1) and (i+j)%3!=0: c=p[2]
                        elif blob==5 and r<.6: c=p[3]
                    img.putpixel((gx,gy),c+(255,))
    # marca de garra (3 aranazos) en la cara frontal del cubo grande
    (w,h,d),(u,v)=CUBES[2]; fx,fy,fw,fh=faces(u,v,w,h,d)["front"]
    for k,ox in enumerate((2,5,8)):
        for j in range(4):
            x=fx+ox-(j//2); y=fy+j
            if fx<=x<fx+fw and fy<=y<fy+fh: img.putpixel((x,y),p[4]+(255,))
    return img
def cracks(stage,name):
    img=Image.new('RGBA',(64,64),(0,0,0,0)); rnd=random.Random(99)
    col=(30,24,20,255) if name in ('scarlet','arena') else (245,245,235,255)
    for (w,h,d),(u,v) in CUBES[1:5]:
        for fname in ("front","east","west","back"):
            x,y,fw,fh=faces(u,v,w,h,d)[fname]
            for _ in range(stage*2):
                cx=x+rnd.randrange(fw); cy=y+rnd.randrange(fh)
                for k in range(rnd.randint(2,3+stage)):
                    if x<=cx<x+fw and y<=cy<y+fh: img.putpixel((cx,cy),col)
                    cx+=rnd.choice((-1,0,1)); cy+=1
    return img
# ---- icono 16x16
W={1:4,2:6,3:8,4:10,5:10,6:12,7:12,8:12,9:12,10:12,11:10,12:10,13:8,14:6}
mask=np.zeros((16,16),bool)
for y,w in W.items(): mask[y,8-w//2:8-w//2+w]=True
edge=np.zeros_like(mask)
for y in range(16):
    for x in range(16):
        if mask[y,x]:
            for dy,dx in ((1,0),(-1,0),(0,1),(0,-1)):
                yy,xx=y+dy,x+dx
                if not(0<=yy<16 and 0<=xx<16) or not mask[yy,xx]: edge[y,x]=True
SPOTS=[(4,6),(5,6),(10,5),(11,5),(11,6),(3,9),(4,9),(12,9),(12,10),(7,12),(8,12),(8,11)]
CLAW=[(6,6),(7,7),(6,8),(9,6),(10,7),(9,8)]   # dos arañazos
def icon(name):
    p=PAL[name]; im=np.zeros((16,16,4),np.uint8)
    for y in range(16):
        for x in range(16):
            if not mask[y,x]: continue
            t=(x-8)*0.5+(y-8)*0.5
            c=p[1] if t>2.2 else p[0]
            if (x,y) in [(6,3),(5,4),(6,4),(5,5)]: c=tuple(min(255,int(v*1.25+25)) for v in p[0])
            if (x,y) in SPOTS: c=p[2]
            if (x,y) in CLAW: c=p[4]
            if edge[y,x]: c=tuple(int(v*0.62) for v in p[1])
            im[y,x]=(*c,255)
    return Image.fromarray(im)
if __name__=='__main__':
    out=sys.argv[1] if len(sys.argv)>1 else ROOT
    for name in PAL:
        b=base_img(name); b.save(out+'entity/carnotaurus_egg_%s_0.png'%name)
        for st in (1,2,3):
            o=b.copy(); o.alpha_composite(cracks(st,name)); o.save(out+'entity/carnotaurus_egg_%s_%d.png'%(name,st))
        icon(name).save(out+'item/carnotaurus_egg_%s.png'%name)
    sheet=Image.new('RGBA',(4*136+4*72,4*136),(40,40,40,255))
    for i,name in enumerate(PAL):
        for st in range(4):
            sheet.alpha_composite(Image.open(out+'entity/carnotaurus_egg_%s_%d.png'%(name,st)).resize((128,128),Image.NEAREST),(i*136+4,st*136+4))
        sheet.alpha_composite(icon(name).resize((64,64),Image.NEAREST),(4*136+i*0+4,i*136+4)) if False else None
    ic=Image.new('RGBA',(4*136,140),(198,198,198,255))
    for i,name in enumerate(PAL): ic.alpha_composite(icon(name).resize((128,128),Image.NEAREST),(i*136+4,6))
    sheet=sheet.crop((0,0,4*136,4*136)); full=Image.new('RGBA',(4*136,4*136+140),(40,40,40,255)); full.paste(sheet,(0,0)); full.paste(ic,(0,4*136))
    full.save(os.path.join(os.path.dirname(os.path.abspath(__file__)),'eggsheet_preview.png'))
