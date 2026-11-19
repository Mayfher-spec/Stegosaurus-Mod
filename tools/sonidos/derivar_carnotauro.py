"""Deriva los sonidos 'complementarios' del carnotauro a partir de las 3 grabaciones del usuario (roar.ogg, bite.ogg, ambient4.ogg).
Cambia tono/duracion, filtra, recorta y mezcla; NO usa material externo. Uso: python3 derivar_carnotauro.py <carpeta sounds/carnotaurus>
Escribe: hurt1, hurt2, hurt_baby, death, ambient1-3, baby_ambient1-3, happy, roar_ult, charge_hit (las fuentes roar/bite/ambient4 no se tocan)."""
import sys, os, subprocess, numpy as np
SR=44100
D=sys.argv[1] if len(sys.argv)>1 else '.'
def load(name):
    raw=subprocess.run(['ffmpeg','-v','error','-i',os.path.join(D,name+'.ogg'),'-ac','1','-ar',str(SR),'-f','f32le','-'],capture_output=True).stdout
    return np.frombuffer(raw,np.float32).copy()
def fx(x,af):
    p=subprocess.run(['ffmpeg','-v','error','-f','f32le','-ar',str(SR),'-ac','1','-i','-','-af',af,'-f','f32le','-ar',str(SR),'-ac','1','-'],input=x.astype(np.float32).tobytes(),capture_output=True)
    if p.returncode: raise RuntimeError(p.stderr.decode())
    return np.frombuffer(p.stdout,np.float32).copy()
def cut(x,t0,t1): return x[int(t0*SR):int(t1*SR)]
def pitch(x,st,tempo=1.0): return fx(x,'rubberband=pitch=%.5f:tempo=%.4f'%(2**(st/12.0),tempo))
def lp(x,hz): return fx(x,'lowpass=f=%d'%hz)
def hp(x,hz): return fx(x,'highpass=f=%d'%hz)
def fade(x,fi=0.01,fo=0.08):
    x=x.copy(); n=len(x); a=int(fi*SR); b=int(fo*SR)
    if a: x[:a]*=np.linspace(0,1,a)
    if b: x[-b:]*=np.linspace(1,0,b)
    return x
def mix(*parts):
    n=max(len(p) for p,_ in parts); out=np.zeros(n,np.float32)
    for p,g in parts: out[:len(p)]+=p*g
    return out
def save(name,x,peak_db):
    x=x/max(1e-6,np.abs(x).max())*(10**(peak_db/20.0))
    p=subprocess.run(['ffmpeg','-v','error','-y','-f','f32le','-ar',str(SR),'-ac','1','-i','-','-c:a','libvorbis','-q:a','5',os.path.join(D,name+'.ogg')],input=x.astype(np.float32).tobytes(),capture_output=True)
    if p.returncode: raise RuntimeError(p.stderr.decode())
    print('%-14s %.2fs  pico %.1f dB'%(name,len(x)/SR,peak_db))
R=load('roar'); B=load('bite'); A=load('ambient4')
# ---- dolor: gruñidos cortos
save('hurt1',fade(hp(pitch(cut(R,0.15,0.75),1.5),110),0.008,0.12),-0.6)
save('hurt2',fade(hp(pitch(cut(A,0.40,0.95),2.5),110),0.008,0.12),-0.7)
save('hurt_baby',fade(hp(pitch(cut(R,0.20,0.60),7.0),200),0.006,0.10),-0.3)
# ---- muerte: el grito largo mas grave y mas lento, apagandose
save('death',fade(lp(pitch(cut(A,0.0,2.5),-3.0,0.9),4500),0.01,0.6),-1.0)
# ---- ambiente adulto: gruñidos graves y suaves
save('ambient1',fade(lp(pitch(cut(A,0.50,2.00),-1.0),3500),0.04,0.25),-3.0)
save('ambient2',fade(lp(pitch(cut(R,0.20,2.30),-2.0),3000),0.05,0.30)*0.8,-2.8)
save('ambient3',fade(lp(pitch(cut(A,1.00,2.40),1.0),3500),0.04,0.25),-3.0)
# ---- cria: versiones agudas y cortas
save('baby_ambient1',fade(hp(pitch(cut(A,0.50,1.05),8.0),250),0.01,0.10),-3.0)
save('baby_ambient2',fade(hp(pitch(cut(R,0.20,0.90),9.0),250),0.01,0.12),-3.1)
save('baby_ambient3',fade(hp(pitch(cut(B,0.10,0.55),7.0),250),0.01,0.10),-3.4)
# ---- contento: gruñido grave y amortiguado
save('happy',fade(lp(pitch(cut(A,0.20,2.10),-2.0),1800),0.08,0.40),-2.2)
# ---- rugido definitivo: el rugido mas grave y lento, en capas, con eco de cueva
r1=pitch(R,-3.0,0.85); r2=pitch(R,-6.0,0.85)
ult=fx(mix((r1,1.0),(r2,0.55)),'aecho=0.7:0.5:90|180:0.30|0.18')
save('roar_ult',fade(ult[:int(3.8*SR)],0.01,0.4),-0.7)
# ---- golpe de embestida: mordida grave + impacto sordo
t=np.arange(int(1.0*SR))/SR
thump=np.sin(2*np.pi*(40+55*np.exp(-t*14))*t)*np.exp(-t*7.0)
rng=np.random.default_rng(7); noise=lp(rng.standard_normal(len(t)).astype(np.float32),700)*np.exp(-t*18.0)
bite=fade(pitch(cut(B,0.0,0.70),-5.0),0.003,0.15)
save('charge_hit',fade(mix((bite,0.7),(thump.astype(np.float32),0.9),(noise,0.5)),0.002,0.2),-0.6)
