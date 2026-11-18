import numpy as np, wave, subprocess, os, glob
from scipy.signal import welch
import sys, os; sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from f0 import load, f0_track
REF,rsr=load('v.wav')
def ltas(a,sr,gate=0.5):
    fr=int(0.03*sr); hop=fr//2
    e=np.array([np.sqrt((a[i:i+fr]**2).mean()) for i in range(0,len(a)-fr,hop)]); th=gate*np.median(e[e>0.01*e.max()])
    segs=[a[i*hop:i*hop+fr] for i,v in enumerate(e) if v>th]
    P=np.zeros(fr//2+1)
    for s in segs: P+=np.abs(np.fft.rfft(s*np.hanning(fr)))**2
    f=np.fft.rfftfreq(fr,1/sr); return f,P/len(segs)
def smooth_log(f,P,pts):
    # promedio en bandas de 1/3 de octava, interpolado
    c=pts; out=[]
    for fc in c:
        m=(f>=fc/2**(1/6))&(f<fc*2**(1/6)); out.append(10*np.log10(P[m].mean()+1e-14) if m.any() else np.nan)
    return np.array(out)
bands=np.array([100*2**(i/3) for i in range(0,20)]); bands=bands[bands<7600]
fr_,Pr=ltas(REF,rsr); Lr=smooth_log(fr_,Pr,bands)
Lr-=np.nanmean(Lr)
ref_f0=float(np.median(f0_track(REF,rsr)))
RATIO=min(1.32,ref_f0/178.0)
print('f0 ref',round(ref_f0,1),'-> ratio',round(RATIO,3))
def process(src,dst):
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',src,'-af',f'rubberband=pitch={RATIO}:formant=preserved:pitchq=quality','tmp.wav'],check=True)
    a,sr=load('tmp.wav')
    f,P=ltas(a,sr,gate=0.3); Ls=smooth_log(f,P,bands); Ls-=np.nanmean(Ls)
    g=np.clip(0.5*(Lr-Ls),-3,3); g=np.nan_to_num(g)
    # curva de ganancia suave en frecuencia
    N=len(a); F=np.fft.rfftfreq(N,1/sr)
    curve=np.interp(np.log(np.maximum(F,bands[0])),np.log(bands),g)
    a2=np.fft.irfft(np.fft.rfft(a)*10**(curve/20),N)
    a2=a2/np.max(np.abs(a2))*0.89
    a2[-1200:]*=np.linspace(1,0,1200)
    with wave.open(dst,'wb') as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr); w.writeframes((a2*32767).astype(np.int16).tobytes())
    return g
os.makedirs('out_ref',exist_ok=True)
for f in sorted(glob.glob('../tts/out_es/*.wav')):
    n=os.path.basename(f)[:-4]; g=process(f,f'out_ref/{n}.wav')
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',f'out_ref/{n}.wav','-ac','1','-ar','44100','-c:a','libvorbis','-q:a','4',f'out_ref/{n}.ogg'],check=True)
    a,sr=load(f'out_ref/{n}.wav'); ff=f0_track(a,sr)
    print(n,f'{len(a)/sr:.2f}s','f0',round(float(np.median(ff)),0) if len(ff) else None,'eq max',np.abs(g).max().round(1),'dB')
