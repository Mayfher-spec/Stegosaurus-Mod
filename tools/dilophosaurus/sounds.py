#!/usr/bin/env python3
"""Sonidos propios del dilofosaurio: mas agudos y roncos que los del carnotauro (chillidos y siseos de depredador agil), sintetizados desde cero.
Reutiliza las utilidades de tools/carnotaurus/sounds.py. Uso: python3 sounds.py <carpeta de salida> (.ogg mono 44.1 kHz; numpy, scipy, ffmpeg). Determinista."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'carnotaurus'))
import numpy as np
import sounds as C
from sounds import T, env, curve, lp, hp, bp, noise, voice, thump, norm, fade, mix, drive, reverb, SR, write_ogg

VA = ([(600, 160), (1400, 300), (2800, 600)], [0.9, 0.8, 0.45])
VB = ([(900, 220), (1900, 350), (3200, 700)], [0.9, 0.9, 0.5])

def roar(sec=1.5, pitch=1.0):
    t = T(sec)
    f0 = curve(t, [(0, 190 * pitch), (0.10, 360 * pitch), (0.40, 420 * pitch), (0.9, 330 * pitch), (sec - 0.1, 200 * pitch), (sec, 150 * pitch)])
    v = voice(t, f0, growl=44, growl_depth=0.5, rasp=0.6, vowels=VA, vow_t=(VB[0], VB[1], 0.1, 0.5), tilt=4200)
    hiss = hp(noise(len(t)), 2500) * 0.16 * np.exp(-((t - 0.35) / 0.4) ** 2)
    y = (v + hiss) * env(len(t), 0.04, 0.12, 0.8, 0.3)
    return fade(norm(reverb(drive(norm(y), 2.4), 0.6, 0.22)))

def frenzy():
    sec = 2.4; t = T(sec)
    f0 = curve(t, [(0, 200), (0.15, 520), (0.6, 640), (1.4, 520), (sec, 230)])
    v = voice(t, f0, growl=52, growl_depth=0.6, rasp=0.7, vowels=VB, vow_t=(VA[0], VA[1], 0.8, 1.6), tilt=5000)
    sub = voice(t, f0 * 0.5, growl=30, growl_depth=0.5, rasp=0.2, tilt=900)
    hiss = hp(noise(len(t)), 3000) * 0.2 * np.exp(-((t - 0.5) / 0.6) ** 2)
    y = (v + 0.7 * sub + hiss) * env(len(t), 0.05, 0.1, 0.85, 0.3)
    return fade(norm(reverb(drive(norm(y), 2.8), 1.1, 0.3)))

def hurt(i=0, baby=False):
    sec = 0.4 if baby else 0.5; t = T(sec)
    if baby:
        f0 = curve(t, [(0, 800), (0.07, 1400), (0.22, 1000), (sec, 620)]); tilt = 6000
    else:
        f0 = curve(t, [(0, 260), (0.06, 520 - 60 * i), (0.22, 340), (sec, 190)]); tilt = 4500
    v = voice(t, f0, growl=50, growl_depth=0.4, rasp=0.45 if not baby else 0.12, vowels=VA, tilt=tilt)
    return fade(norm(drive(norm(v * env(len(t), 0.04, 0.2, 0.7, 0.4)), 2.0)))

def death():
    sec = 1.9; t = T(sec)
    f0 = curve(t, [(0, 380), (0.2, 430), (0.8, 300), (1.5, 170), (sec, 110)])
    v = voice(t, f0, growl=36, growl_depth=0.6, rasp=0.6, vowels=VA, vow_t=([(450, 140), (900, 220), (2200, 450)], [0.9, 0.5, 0.2], 0.4, 1.7), tilt=3800)
    wheeze = bp(noise(len(t)), 900, 3200) * 0.22 * np.clip((t - 1.0) / 0.3, 0, 1) * np.exp(-(t - 1.0) * 1.5)
    return fade(norm(reverb(drive(norm((v + wheeze) * env(len(t), 0.03, 0.1, 0.8, 0.5)), 1.8), 0.8, 0.22)))

def bite():
    t = T(0.3)
    snap = hp(noise(len(t)), 1500) * np.exp(-t * 80) * 0.9
    crunch = bp(noise(len(t)), 400, 3000) * np.exp(-t * 28) * 0.6
    body = thump(0.3, 220, 80, 16) * 0.6
    return fade(norm(drive(norm(snap + crunch + body), 1.8)))

def claw():
    t = T(0.5)
    sw = bp(noise(len(t)), 1200, 5500) * np.sin(np.pi * np.clip(t / 0.2, 0, 1)) * 0.7
    sw2 = np.roll(sw, int(0.16 * SR)) * 0.8
    tick = hp(noise(len(t)), 3000) * (np.exp(-np.clip(t - 0.06, 0, None) * 90) * (t > 0.06) + np.exp(-np.clip(t - 0.22, 0, None) * 90) * (t > 0.22)) * 0.5
    return fade(norm(drive(norm(sw + sw2 + tick), 1.5), 0.85))

def pounce():
    sec = 0.9; t = T(sec)
    whoosh = bp(noise(len(t)), 300, 2500) * np.exp(-((t - 0.35) / 0.25) ** 2) * 0.7
    shriek = voice(t, curve(t, [(0, 300), (0.15, 560), (0.5, 420), (sec, 240)]), growl=46, growl_depth=0.4, rasp=0.5, vowels=VB, tilt=4500) * env(len(t), 0.06, 0.15, 0.7, 0.4) * 0.8
    land = thump(sec, 180, 55, 11) * (t > 0.0) * np.exp(-np.clip(t - 0.62, 0, None) * 0) * 0
    return fade(norm(drive(norm(whoosh + shriek), 2.0)))

def frill_open():
    t = T(0.7)
    snap = hp(noise(len(t)), 1800) * (np.exp(-t * 14) * 0.5 + 0.5 * np.exp(-np.abs(t - 0.22) * 40)) * 0.6
    rattle = bp(noise(len(t)), 800, 4500) * (0.5 + 0.5 * np.sin(2 * np.pi * 38 * t)) * np.sin(np.pi * np.clip(t / 0.5, 0, 1)) * 0.7
    return fade(norm(snap + rattle, 0.8))

def frill_close():
    t = T(0.45)
    sw = bp(noise(len(t)), 600, 3000) * np.sin(np.pi * np.clip(t / 0.35, 0, 1)) * 0.6
    tick = hp(noise(len(t)), 2500) * np.exp(-np.abs(t - 0.3) * 70) * 0.7
    return fade(norm(sw + tick, 0.75))

def ambient(i):
    sec = [1.0, 1.3, 0.8][i]; t = T(sec)
    base = [230, 200, 270][i]
    f0 = curve(t, [(0, base * 0.9), (sec * 0.35, base * 1.3), (sec, base * 0.8)])
    v = voice(t, f0, growl=[20, 26, 16][i], growl_depth=0.6, rasp=0.5, vowels=VA, tilt=3500)
    hiss = hp(noise(len(t)), 3000) * 0.07
    return fade(norm(drive(norm((v + hiss) * env(len(t), 0.2, 0.1, 0.8, 0.4)), 1.6), 0.7))

def baby_ambient(i):
    sec = [0.45, 0.6][i]; t = T(sec)
    pts = [[(0, 900), (0.1, 1500), (0.25, 1100), (sec, 800)], [(0, 1100), (0.15, 850), (0.35, 1500), (sec, 900)]][i]
    v = voice(t, curve(t, pts), growl=55, growl_depth=0.3, rasp=0.08, tilt=6500, vowels=([(900, 250), (2400, 500), (3500, 700)], [0.9, 0.7, 0.4]))
    return fade(norm(v * env(len(t), 0.1, 0.2, 0.7, 0.35), 0.7))

def step(i):
    t = T(0.3)
    th = thump(0.3, 150 + 20 * i, 60, 18) * 0.7
    gr = lp(hp(noise(len(t)), 500), 3200 - 400 * i) * np.exp(-t * 35) * 0.5
    return fade(norm(th + gr, 0.7))

def passthrough(name): return getattr(C, name)

SOUNDS = {
    'roar': lambda: roar(1.5), 'frenzy': frenzy, 'hurt1': lambda: hurt(0), 'hurt2': lambda: hurt(1), 'hurt_baby': lambda: hurt(0, True),
    'death': death, 'bite': bite, 'claw': claw, 'pounce': pounce, 'frill_open': frill_open, 'frill_close': frill_close,
    'ambient1': lambda: ambient(0), 'ambient2': lambda: ambient(1), 'ambient3': lambda: ambient(2),
    'baby_ambient1': lambda: baby_ambient(0), 'baby_ambient2': lambda: baby_ambient(1),
    'step1': lambda: step(0), 'step2': lambda: step(1), 'step3': lambda: step(2),
    'breathe': lambda: fade(norm(C.breathe() * 0.8, 0.5)), 'sit': C.sit, 'sleep': C.sleep, 'wake': C.wake, 'happy': C.happy,
    'eat': C.eat, 'evolution': C.evolution, 'revert': C.revert,
}
# sit/sleep/wake/happy/breathe/eat reutilizan la base grave del carnotauro pero se tocan mas agudos al reproducirlos (pitch en Java) y se mezclan aqui
def lift(fn, ratio):
    def g():
        x = fn()
        n = int(len(x) / ratio)
        return fade(norm(np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x), 0.85))
    return g
for k, r in (('sit', 1.5), ('sleep', 1.45), ('wake', 1.5), ('happy', 1.6), ('eat', 1.2), ('breathe', 1.3)):
    SOUNDS[k] = lift(SOUNDS[k], r)

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'dilophosaurus_sounds'
    os.makedirs(out, exist_ok=True)
    for name, fn in SOUNDS.items():
        C.rng = np.random.default_rng(sum(map(ord, 'dilo' + name)))
        x = fn()
        write_ogg(x, os.path.join(out, name + '.ogg'))
        print('%-14s %.2fs' % (name, len(x) / SR))
