#!/usr/bin/env python3
"""Sintetiza los sonidos propios del carnotauro (estilo T-Rex de pelicula): rugidos graves con rasposidad, gruñidos, pasos pesados.
Uso: python3 sounds.py <carpeta de salida>   (escribe .ogg mono 44.1 kHz; requiere numpy, scipy y ffmpeg).
Es determinista (semilla fija): volver a ejecutarlo da los mismos archivos."""
import os, subprocess, sys, tempfile
import numpy as np
from scipy import signal

SR = 44100
rng = np.random.default_rng(7)

# ---------------------------------------------------------------- utilidades
def T(sec): return np.arange(int(sec * SR)) / SR
def env(n, a, d, s, r, peak=1.0):
    """ADSR en fracciones de n muestras (a,d,r) y nivel s."""
    e = np.ones(n) * s
    na, nd, nr = int(n * a), int(n * d), int(n * r)
    e[:na] = np.linspace(0, peak, na, endpoint=False)
    e[na:na + nd] = np.linspace(peak, s, nd, endpoint=False)
    e[n - nr:] = np.linspace(s, 0, nr)
    return e
def curve(t, pts):
    """Contorno lineal por tramos: pts = [(segundo, valor), ...]; suavizado."""
    xs, ys = zip(*pts)
    c = np.interp(t, xs, ys)
    k = int(0.02 * SR) | 1
    return np.convolve(np.pad(c, (k, k), mode='edge'), np.ones(k) / k, mode='same')[k:-k]
def lp(x, f, order=4): return signal.sosfilt(signal.butter(order, f, 'low', fs=SR, output='sos'), x)
def hp(x, f, order=4): return signal.sosfilt(signal.butter(order, f, 'high', fs=SR, output='sos'), x)
def bp(x, f0, f1, order=2): return signal.sosfilt(signal.butter(order, [f0, f1], 'band', fs=SR, output='sos'), x)
def noise(n): return rng.standard_normal(n)
def formants(x, fs_bw, gains):
    """Banco de resonadores (vocal): fs_bw = [(frecuencia, ancho de banda), ...]."""
    out = np.zeros_like(x)
    for (f, bw), g in zip(fs_bw, gains):
        out += g * signal.sosfilt(signal.butter(2, [max(30, f - bw / 2), f + bw / 2], 'band', fs=SR, output='sos'), x)
    return out
def reverb(x, sec=0.6, wet=0.25):
    n = int(sec * SR)
    ir = noise(n) * np.exp(-np.arange(n) / (n / 5.0))
    ir = lp(ir, 3500)
    wetx = signal.fftconvolve(x, ir)[:len(x)]
    wetx *= np.max(np.abs(x)) / (np.max(np.abs(wetx)) + 1e-9)
    return x * (1 - wet) + wetx * wet
def drive(x, g): return np.tanh(g * x) / np.tanh(g)

def voice(t, f0, growl=30.0, growl_depth=0.6, jitter=0.02, rasp=0.25, vowels=None, vow_t=None, tilt=2800.0):
    """Voz de reptil: tren de pulsos con jitter + modulacion de gruñido + ruido + formantes. f0 es un arreglo (Hz)."""
    n = len(t)
    j = np.cumsum(rng.standard_normal(n)) * 0.0
    jit = lp(rng.standard_normal(n), 40) * jitter * 6
    ph = np.cumsum(f0 * (1 + jit)) / SR
    saw = 2 * (ph % 1.0) - 1
    pulse = saw - 0.6 * np.roll(saw, int(SR / 400))          # pulso glotal mas "abierto"
    src = lp(pulse, tilt)
    am = 1 - growl_depth * (0.5 + 0.5 * np.sin(2 * np.pi * (growl + 4 * lp(rng.standard_normal(n), 3)) * t)) ** 2
    src = src * am
    src += rasp * lp(noise(n), 2500) * (0.4 + 0.6 * am)
    if vowels is None: vowels = ([(420, 150), (900, 250), (2100, 500)], [0.9, 0.7, 0.35])
    y = formants(src, *vowels)
    if vow_t is not None:                                      # transicion a otra vocal
        y2 = formants(src, *vow_t[0:2])
        mix = np.clip((t - vow_t[2]) / max(1e-3, vow_t[3] - vow_t[2]), 0, 1)
        y = y * (1 - mix) + y2 * mix
    return y + 0.35 * lp(src, 300)

def thump(sec, f_start=110, f_end=38, decay=9.0):
    t = T(sec)
    f = f_end + (f_start - f_end) * np.exp(-t * 28)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * decay)

def norm(x, peak=0.89):
    x = x - np.mean(x)
    return x / (np.max(np.abs(x)) + 1e-9) * peak
def fade(x, ms=8):
    k = int(ms / 1000 * SR); x = x.copy(); x[:k] *= np.linspace(0, 1, k); x[-k:] *= np.linspace(1, 0, k); return x
def mix(*parts):
    n = max(len(p[0]) + int(p[1] * SR) for p in parts); out = np.zeros(n)
    for x, at in parts: s = int(at * SR); out[s:s + len(x)] += x
    return out

# ---------------------------------------------------------------- sonidos
def roar(sec=2.3, low=1.0, big=1.0):
    t = T(sec)
    f0 = curve(t, [(0, 70 * low), (0.12, 150 * low), (0.45, 175 * low), (1.2, 135 * low), (sec - 0.2, 70 * low), (sec, 48 * low)])
    A = ([(420, 140), (880, 260), (2200, 520)], [0.9, 0.8, 0.4])
    B = ([(700, 200), (1250, 300), (2600, 600)], [0.9, 0.9, 0.5])
    v = voice(t, f0, growl=34, growl_depth=0.55, rasp=0.45, vowels=A, vow_t=(B[0], B[1], 0.15, 0.7))
    v2 = voice(t, f0 * 0.5, growl=22, growl_depth=0.5, rasp=0.15, tilt=900)
    scream = hp(noise(len(t)), 1500) * 0.10 * np.exp(-((t - 0.45) / 0.35) ** 2)
    y = (v + 0.9 * v2 + scream) * env(len(t), 0.05, 0.12, 0.8, 0.30)
    y = drive(norm(y), 2.2 * big)
    return fade(norm(reverb(y, 0.9, 0.28)))

def hurt(variant=0, baby=False):
    sec = 0.55 if not baby else 0.45; t = T(sec)
    if baby:
        f0 = curve(t, [(0, 520), (0.08, 980), (0.25, 760), (sec, 420)])
        v = voice(t, f0, growl=45, growl_depth=0.25, rasp=0.15, tilt=5000,
                  vowels=([(900, 250), (2300, 500), (3400, 700)], [0.9, 0.7, 0.4]))
    else:
        f0 = curve(t, [(0, 100), (0.07, 190 - 25 * variant), (0.25, 120), (sec, 62)])
        v = voice(t, f0, growl=38, growl_depth=0.45, rasp=0.4,
                  vowels=([(520, 160), (1000, 260), (2300, 520)], [0.9, 0.7, 0.35]))
    y = v * env(len(t), 0.05, 0.2, 0.7, 0.4)
    return fade(norm(drive(norm(y), 2.0)))

def death():
    sec = 2.6; t = T(sec)
    f0 = curve(t, [(0, 150), (0.25, 170), (1.0, 95), (2.0, 52), (sec, 36)])
    v = voice(t, f0, growl=28, growl_depth=0.6, rasp=0.5,
              vowels=([(520, 160), (1000, 260), (2300, 520)], [0.9, 0.7, 0.35]), vow_t=([(380, 120), (700, 200), (2000, 400)], [0.9, 0.5, 0.2], 0.5, 2.2))
    wheeze = bp(noise(len(t)), 500, 1800) * 0.25 * np.clip((t - 1.4) / 0.4, 0, 1) * np.exp(-(t - 1.4) * 1.2)
    y = (v + wheeze) * env(len(t), 0.03, 0.1, 0.8, 0.55)
    return fade(norm(reverb(drive(norm(y), 1.8), 1.0, 0.25)))

def bite():
    t = T(0.45)
    snap = hp(noise(len(t)), 900) * np.exp(-t * 55) * 0.8
    crunch = bp(noise(len(t)), 200, 1800) * np.exp(-t * 18) * 0.7
    body = thump(0.45, 140, 50, 12) * 0.9
    f0 = curve(t, [(0, 110), (0.15, 90), (0.45, 60)])
    gr = voice(t, f0, rasp=0.5, growl=40) * np.exp(-t * 9) * 0.35
    return fade(norm(drive(norm(snap + crunch + body + gr), 1.8)))

def step(i):
    t = T(0.55)
    f_hi = 120 + 18 * i
    th = thump(0.55, f_hi, 34 + 3 * i, 8.0)
    gravel = lp(hp(noise(len(t)), 300), 2200 - 300 * i) * np.exp(-t * 22) * 0.45
    rumble = lp(noise(len(t)), 120) * np.exp(-t * 7) * 0.5
    return fade(norm(th + gravel + rumble))

def ambient(i):
    sec = [1.7, 2.1, 1.4][i]; t = T(sec)
    base = [78, 64, 90][i]
    f0 = curve(t, [(0, base * 0.9), (sec * 0.4, base * 1.15), (sec, base * 0.75)])
    v = voice(t, f0, growl=[12, 18, 9][i], growl_depth=0.7, rasp=0.3, tilt=1200,
              vowels=([(300, 110), (700, 220), (1700, 400)], [0.9, 0.6, 0.2]))
    breath = bp(noise(len(t)), 150, 900) * 0.08
    y = (v + breath) * env(len(t), 0.25, 0.1, 0.8, 0.4)
    return fade(norm(drive(norm(y), 1.6), 0.7))

def breathe():
    sec = 2.4; t = T(sec)
    inh = np.exp(-((t - 0.55) / 0.30) ** 2); exh = np.exp(-((t - 1.55) / 0.42) ** 2)
    n = noise(len(t))
    y = bp(n, 120, 700) * (0.8 * inh + 1.0 * exh) + lp(n, 90) * 0.7 * exh
    y *= 1 + 0.15 * np.sin(2 * np.pi * 26 * t)
    return fade(norm(y, 0.6))

def sit():
    t = T(0.9)
    th = thump(0.9, 90, 32, 6.5)
    puff = bp(noise(len(t)), 120, 800) * np.exp(-((t - 0.25) / 0.2) ** 2) * 0.5
    gr = voice(t, curve(t, [(0, 70), (0.3, 60), (0.9, 45)]), growl=20, growl_depth=0.7, rasp=0.3, tilt=900) * np.exp(-((t - 0.3) / 0.25) ** 2) * 0.5
    return fade(norm(th + puff + gr, 0.85))

def sleep():
    sec = 4.0; t = T(sec)
    n = noise(len(t))
    cyc = 0.5 + 0.5 * np.sin(2 * np.pi * t / 2.0 - np.pi / 2)  # dos ronquidos
    snore = bp(n, 80, 600) * cyc ** 2
    rattle = lp(n, 160) * (0.5 + 0.5 * np.sin(2 * np.pi * 34 * t)) * cyc ** 3
    deep = voice(t, curve(t, [(0, 55), (sec, 52)]), growl=17, growl_depth=0.8, rasp=0.1, tilt=500) * cyc ** 2 * 0.9
    return fade(norm((snore * 0.9 + rattle * 0.8 + deep) * env(len(t), 0.12, 0.05, 1.0, 0.35), 0.7))

def wake():
    sec = 1.6; t = T(sec)
    f0 = curve(t, [(0, 50), (0.5, 85), (1.0, 120), (sec, 70)])
    v = voice(t, f0, growl=24, growl_depth=0.5, rasp=0.35, tilt=1800,
              vowels=([(400, 140), (850, 240), (2000, 500)], [0.9, 0.7, 0.3]))
    y = v * env(len(t), 0.45, 0.1, 0.8, 0.3)
    return fade(norm(drive(norm(y), 1.8), 0.85))

def happy():
    sec = 1.9; t = T(sec)
    f0 = curve(t, [(0, 70), (0.5, 88), (1.3, 82), (sec, 60)])
    purr = voice(t, f0, growl=26, growl_depth=0.85, rasp=0.1, tilt=700)
    chirp_f = curve(t, [(0, 400), (0.2, 620), (0.5, 380), (1.0, 380), (1.15, 640), (1.45, 420), (sec, 420)])
    chirp = voice(t, chirp_f, growl=18, growl_depth=0.2, rasp=0.03, tilt=4000,
                  vowels=([(700, 200), (1500, 300), (2500, 500)], [0.9, 0.6, 0.3])) * ((t < 0.5) | ((t > 1.0) & (t < 1.5)))
    return fade(norm((purr + 0.25 * chirp) * env(len(t), 0.2, 0.1, 0.85, 0.35), 0.8))

def eat():
    sec = 1.1; t = T(sec); out = np.zeros(len(t))
    for k, at in enumerate([0.02, 0.33, 0.62]):
        m = (t >= at)
        tt = np.where(m, t - at, 0)
        sn = hp(noise(len(t)), 700) * np.exp(-tt * 45) * m * 0.6
        wet = bp(noise(len(t)), 150, 1200) * np.exp(-tt * 14) * m * 0.8
        th = np.where(m, np.sin(2 * np.pi * (70 - 10 * k) * tt) * np.exp(-tt * 16), 0) * 0.9
        out += sn + wet + th
    return fade(norm(drive(norm(out), 1.6), 0.85))

def charge_hit():
    sec = 1.0; t = T(sec)
    im = thump(sec, 150, 30, 6) * 1.0
    cr = lp(hp(noise(len(t)), 100), 3000) * np.exp(-t * 14) * 0.9
    gr = voice(t, curve(t, [(0, 120), (0.2, 95), (sec, 55)]), growl=36, growl_depth=0.5, rasp=0.5) * np.exp(-t * 4.5) * 0.7
    return fade(norm(drive(norm(im + cr + gr), 2.0)))

def roar_ult():
    sec = 3.8; t = T(sec)
    f0 = curve(t, [(0, 45), (0.3, 120), (0.9, 160), (2.0, 120), (3.0, 85), (sec, 40)])
    v = voice(t, f0, growl=30, growl_depth=0.6, rasp=0.55,
              vowels=([(380, 130), (820, 240), (2100, 520)], [0.9, 0.8, 0.4]), vow_t=([(720, 200), (1300, 320), (2700, 600)], [0.9, 0.9, 0.5], 0.4, 1.4))
    sub = voice(t, f0 * 0.5, growl=20, growl_depth=0.5, rasp=0.1, tilt=500)
    rum = lp(noise(len(t)), 90) * np.exp(-((t - 1.0) / 1.0) ** 2) * 0.8
    y = (v + 1.1 * sub + rum) * env(len(t), 0.06, 0.1, 0.85, 0.35)
    return fade(norm(reverb(drive(norm(y), 2.6), 1.6, 0.35)))

def evolution():
    sec = 4.6; t = T(sec)
    sweep = np.sin(2 * np.pi * np.cumsum(curve(t, [(0, 180), (3.0, 1800), (sec, 2400)])) / SR) * np.clip(t / 3.0, 0, 1) ** 2 * 0.3
    shimmer = sum(np.sin(2 * np.pi * f * t + k) * 0.06 for k, f in enumerate([523.25, 659.25, 783.99, 1046.5, 1318.5])) * np.clip(t / 2.5, 0, 1)
    swell = voice(t, curve(t, [(0, 45), (3.0, 90), (sec, 60)]), growl=14, growl_depth=0.5, rasp=0.2, tilt=800) * np.clip(t / 3.0, 0, 1) ** 1.5
    r = roar(2.2, 0.92)
    out = mix((sweep + shimmer, 0.0), (swell * 0.8, 0.0), (r * 1.1, 2.3))
    out *= env(len(out), 0.03, 0.0, 1.0, 0.2)
    return fade(norm(reverb(out, 1.4, 0.3)))

def revert():
    sec = 1.7; t = T(sec)
    sweep = np.sin(2 * np.pi * np.cumsum(curve(t, [(0, 2200), (1.2, 380), (sec, 300)])) / SR) * 0.35 * env(len(t), 0.02, 0.2, 0.5, 0.5)
    shimmer = sum(np.sin(2 * np.pi * f * t) * 0.07 for f in [1568, 1318.5, 1046.5]) * np.exp(-t * 2.5)
    squeak = voice(T(0.45), curve(T(0.45), [(0, 640), (0.12, 1000), (0.45, 560)]), growl=40, growl_depth=0.2, rasp=0.1, tilt=5000,
                   vowels=([(900, 250), (2300, 500), (3400, 700)], [0.9, 0.7, 0.4])) * env(int(0.45 * SR), 0.05, 0.2, 0.6, 0.4)
    out = mix((sweep + shimmer, 0.0), (squeak * 0.8, 1.0))
    return fade(norm(reverb(out, 0.7, 0.25), 0.85))

def baby_ambient(i):
    sec = [0.55, 0.7, 0.45][i]; t = T(sec)
    pts = [[(0, 650), (0.1, 1150), (0.25, 900), (sec, 700)], [(0, 900), (0.15, 700), (0.35, 1250), (sec, 800)], [(0, 1000), (0.12, 1400), (sec, 900)]][i]
    v = voice(t, curve(t, pts), growl=[48, 36, 55][i], growl_depth=0.35, rasp=0.08, tilt=6000,
              vowels=([(800, 250), (2200, 500), (3300, 700)], [0.9, 0.7, 0.4])) * env(len(t), 0.1, 0.2, 0.7, 0.35)
    return fade(norm(v, 0.7))

def baby_eat():
    return eat()[: int(0.8 * SR)]

SOUNDS = {
    'roar': roar, 'roar_ult': roar_ult, 'hurt1': lambda: hurt(0), 'hurt2': lambda: hurt(1), 'hurt_baby': lambda: hurt(0, True),
    'death': death, 'bite': bite, 'step1': lambda: step(0), 'step2': lambda: step(1), 'step3': lambda: step(2),
    'ambient1': lambda: ambient(0), 'ambient2': lambda: ambient(1), 'ambient3': lambda: ambient(2),
    'breathe': breathe, 'sit': sit, 'sleep': sleep, 'wake': wake, 'happy': happy, 'eat': eat, 'charge_hit': charge_hit,
    'evolution': evolution, 'revert': revert,
    'baby_ambient1': lambda: baby_ambient(0), 'baby_ambient2': lambda: baby_ambient(1), 'baby_ambient3': lambda: baby_ambient(2),
}

def write_ogg(x, path):
    pcm = (np.clip(x, -1, 1) * 32767).astype('<i2').tobytes()
    with tempfile.NamedTemporaryFile(suffix='.raw', delete=False) as f: f.write(pcm); raw = f.name
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', raw, '-c:a', 'libvorbis', '-q:a', '4', path], check=True)
    os.remove(raw)

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'carnotaurus_sounds'
    os.makedirs(out, exist_ok=True)
    for name, fn in SOUNDS.items():
        rng = np.random.default_rng(abs(hash(name)) % 1000 if False else sum(map(ord, name)))   # semilla estable por nombre
        x = fn()
        write_ogg(x, os.path.join(out, name + '.ogg'))
        print('%-14s %.2fs' % (name, len(x) / SR))
