import numpy as np, onnxruntime as ort, wave, base64, sys
M='sherpa-onnx-whisper-base/'
def mel_filters(sr=16000,n_fft=400,n_mels=80):
    def hz2mel(f): 
        f=np.asarray(f,float); lin=f/(200/3); lt=1000/(200/3); lg=np.log(6.4)/27
        return np.where(f>=1000, lt+np.log(np.maximum(f,1e-9)/1000)/lg, lin)
    def mel2hz(m):
        m=np.asarray(m,float); lt=1000/(200/3); lg=np.log(6.4)/27
        return np.where(m>=lt, 1000*np.exp(lg*(m-lt)), m*(200/3))
    fft_f=np.linspace(0,sr/2,1+n_fft//2)
    mel_f=mel2hz(np.linspace(hz2mel(0),hz2mel(sr/2),n_mels+2))
    fdiff=np.diff(mel_f); ramps=mel_f[:,None]-fft_f[None,:]
    w=np.zeros((n_mels,len(fft_f)))
    for i in range(n_mels):
        lower=-ramps[i]/fdiff[i]; upper=ramps[i+2]/fdiff[i+1]
        w[i]=np.maximum(0,np.minimum(lower,upper))
    enorm=2.0/(mel_f[2:n_mels+2]-mel_f[:n_mels]); return w*enorm[:,None]
def logmel(a):
    a=np.pad(a,(0,480000-len(a))) if len(a)<480000 else a[:480000]
    win=np.hanning(401)[:-1]; pad=np.pad(a,(200,200),mode='reflect')
    n=1+(len(pad)-400)//160
    fr=np.stack([pad[i*160:i*160+400]*win for i in range(n)])
    spec=np.abs(np.fft.rfft(fr,axis=1))**2
    mel=mel_filters()@spec.T
    mel=mel[:,:3000]
    l=np.log10(np.maximum(mel,1e-10)); l=np.maximum(l,l.max()-8.0); return ((l+4)/4).astype(np.float32)
def load(p):
    w=wave.open(p); return np.frombuffer(w.readframes(w.getnframes()),dtype=np.int16).astype(np.float32)/32768
toks={int(l.split()[1]):base64.b64decode(l.split()[0]) for l in open(M+'base-tokens.txt')}
enc=ort.InferenceSession(M+'base-encoder.onnx',providers=['CPUExecutionProvider']); dec=ort.InferenceSession(M+'base-decoder.onnx',providers=['CPUExecutionProvider'])
def transcribe(a,lang=50262):
    ck,cv=enc.run(None,{'mel':logmel(a)[None]})
    sk=np.zeros((6,1,448,512),np.float32); sv=np.zeros_like(sk)
    t=[50258,lang,50359]; off=0; out=[]
    sup=[int(x) for x in ort.InferenceSession(M+'base-encoder.onnx').get_modelmeta().custom_metadata_map['non_speech_tokens'].split(',')]
    for step in range(120):
        lg,sk,sv=dec.run(None,{'tokens':np.array([t],np.int64),'in_n_layer_self_k_cache':sk,'in_n_layer_self_v_cache':sv,'n_layer_cross_k':ck,'n_layer_cross_v':cv,'offset':np.array([off],np.int64)})
        off+=len(t); l=lg[0,-1].copy(); l[sup]=-1e9; l[50258:50363]=-1e9
        if step==0: l[50257]=-1e9
        n=int(l.argmax())
        if n==50257: break
        out.append(n); t=[n]
    return b''.join(toks.get(i,b'') for i in out).decode('utf-8','ignore')
if __name__=='__main__':
    a=load(sys.argv[1])
    for lang,name in ((50262,'es'),(50259,'en')):
        print(name,'|',transcribe(a,lang))
