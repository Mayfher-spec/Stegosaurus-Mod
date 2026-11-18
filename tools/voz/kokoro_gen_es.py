import numpy as np, onnxruntime as ort, wave, subprocess, os
V={";":1,":":2,",":3,".":4,"!":5,"?":6," ":16,"A":24,"I":25,"O":31,"W":39,"a":43,"b":44,"d":46,"e":47,"f":48,"h":50,"i":51,"j":52,"k":53,"l":54,"m":55,"n":56,"o":57,"p":58,"s":61,"t":62,"u":63,"v":64,"w":65,"z":68,"ɑ":69,"æ":72,"ð":81,"ə":83,"ɚ":85,"ɛ":86,"ɡ":92,"ɪ":102,"ɹ":123,"ʃ":131,"ʧ":133,"ʊ":135,"ʌ":138,"ˈ":156,"β":75,"x":66,"ɾ":125,"ː":158}
voices=np.load('voices.bin',allow_pickle=True)
sess=ort.InferenceSession('kokoro.onnx',providers=['CPUExecutionProvider'])
def synth(ph,voice,speed=1.0):
    toks=[V[c] for c in ph]
    style=voices[voice][len(toks)]
    out=sess.run(None,{'tokens':np.array([[0]+toks+[0]],dtype=np.int64),'style':style.astype(np.float32),'speed':np.array([speed],dtype=np.float32)})[0]
    return out.reshape(-1)
LINES={
 'hello1':('ˈola!','ef_dora'),                 # ¡Hola!
 'hello2':('kˈe ɡˈusto βˈeɾte!','ef_dora'),     # ¡Qué gusto verte!
 'hello3':('bwˈenas!','ef_dora'),               # ¡Buenas!
 'happy1':('kˈe bjˈen!','ef_dora'),             # ¡Qué bien!
 'happy2':('ɡɾˈasjas!','ef_dora'),              # ¡Gracias!
 'hurt1':('ˈai!','ef_dora'),                    # ¡Ay!
 'hurt2':('ˈautʃ!'.replace('tʃ','ʧ'),'ef_dora'),# ¡Auch!
 'death1':('ˈaːi!','ef_dora'),                  # ¡Aaay!
}
os.makedirs('out_es',exist_ok=True)
for name,(ph,voice) in LINES.items():
    a=synth(ph,voice)
    # recorta silencios y normaliza
    th=0.01*np.max(np.abs(a)); idx=np.where(np.abs(a)>th)[0]; a=a[max(0,idx[0]-1200):idx[-1]+2400]
    a=a/np.max(np.abs(a))*0.89
    fade=np.linspace(1,0,1500); a[-1500:]*=fade
    pcm=(a*32767).astype(np.int16)
    with wave.open(f'out_es/{name}.wav','wb') as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(24000); w.writeframes(pcm.tobytes())
    r=subprocess.run(['ffmpeg','-y','-loglevel','error','-i',f'out_es/{name}.wav','-ac','1','-ar','44100','-c:a','libvorbis','-q:a','4',f'out_es/{name}.ogg'])
    print(name,voice,ph,f'{len(a)/24000:.2f}s','rms',round(float(np.sqrt((a**2).mean())),3),os.path.getsize(f'out_es/{name}.ogg'),'bytes')
