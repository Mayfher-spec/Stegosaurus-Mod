import numpy as np, wave
def load(p):
    w=wave.open(p); sr=w.getframerate(); a=np.frombuffer(w.readframes(w.getnframes()),dtype=np.int16).astype(float)/32768; return a,sr
def f0_track(a,sr,fmin=80,fmax=420):
    fr=int(0.04*sr); hop=int(0.01*sr); out=[]
    lo,hi=int(sr/fmax),int(sr/fmin)
    for i in range(0,len(a)-fr-hi,hop):
        x=a[i:i+fr+hi]; seg=x[:fr]
        if np.sqrt((seg**2).mean())<0.02: continue
        seg=seg-seg.mean()
        ac=np.array([np.dot(seg,x[k:k+fr]) for k in range(lo,hi)])
        n0=np.dot(seg,seg)+1e-9
        k=int(np.argmax(ac)); r=ac[k]/n0
        if r>0.55: out.append(sr/(lo+k))
    return np.array(out)
if __name__=='__main__':
    import sys
    for p in sys.argv[1:]:
        a,sr=load(p); f=f0_track(a,sr); print(p,sr,len(f),'frames voiced; median f0',round(float(np.median(f)),1) if len(f) else None, 'p10-p90',np.percentile(f,[10,90]).round(0) if len(f) else None)
