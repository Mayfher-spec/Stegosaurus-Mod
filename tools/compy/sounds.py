#!/usr/bin/env python3
"""Sonidos del Compy: gorjeos y chirridos agudos y breves (animal diminuto), sintetizados desde cero con las utilidades del carnotauro.
Uso: python3 sounds.py <carpeta de salida> (.ogg mono 44.1 kHz; numpy, scipy, ffmpeg). Determinista."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'carnotaurus'))
import numpy as np
import sounds as C
from sounds import T, env, curve, lp, hp, bp, noise, norm, fade, SR, write_ogg

def tone(t, f0, harm=(1.0, 0.45, 0.25, 0.12), vib=0.0, vibf=28.0):
    f = f0 * (1.0 + vib * np.sin(2 * np.pi * vibf * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return sum(h * np.sin((k + 1) * ph) for k, h in enumerate(harm))

def chirp(pts, sec, a=0.05, r=0.35, vib=0.02, rasp=0.05):
    t = T(sec); f0 = curve(t, pts)
    y = tone(t, f0, vib=vib) + rasp * bp(noise(len(t)), 2500, 6000)
    return y * env(len(t), a, 0.2, 0.7, r)

def ambient(i):
    seqs = [
        [((0, 2200), (0.05, 3200), (0.12, 2600)), 0.14, ((0, 2400), (0.04, 3500), (0.10, 2800)), 0.12],
        [((0, 1800), (0.06, 2900), (0.14, 2200)), 0.16, ((0, 2600), (0.05, 3300), (0.1, 2400)), 0.10],
        [((0, 2800), (0.04, 3600), (0.09, 3000)), 0.11, ((0, 2300), (0.05, 3000), (0.11, 2500)), 0.13]][i]
    p1, d1, p2, d2 = seqs
    a = chirp(list(p1), d1); b = chirp(list(p2), d2)
    gap = np.zeros(int(0.045 * SR))
    return fade(norm(np.concatenate([a, gap, b]), 0.8))

def hurt():
    return fade(norm(chirp([(0, 3600), (0.05, 4200), (0.16, 2400), (0.24, 1800)], 0.26, a=0.02, r=0.4, vib=0.05, rasp=0.15), 0.85))

def death():
    a = chirp([(0, 3400), (0.08, 3900), (0.30, 2000), (0.55, 1100)], 0.58, a=0.02, r=0.6, vib=0.06, rasp=0.18)
    b = chirp([(0, 1500), (0.15, 900), (0.3, 600)], 0.32, a=0.05, r=0.7, vib=0.08, rasp=0.2)
    return fade(norm(np.concatenate([a, np.zeros(int(0.05 * SR)), b]), 0.85))

def bite():
    t = T(0.22)
    snap = hp(noise(len(t)), 2000) * np.exp(-t * 48) * 0.9
    c = chirp([(0, 3000), (0.05, 2000), (0.12, 1400)], 0.22, a=0.01, r=0.5, rasp=0.3) * 0.45
    click = np.zeros(len(t)); n5 = int(0.005 * SR); click[int(0.045 * SR):int(0.045 * SR) + n5] = np.hanning(n5)
    return fade(norm(snap + c + lp(click, 6000) * 0.8, 0.85))

def eat():
    parts = []
    for k in range(3):
        t = T(0.07)
        parts.append((bp(noise(len(t)), 700, 3500) * np.exp(-t * 55) + 0.4 * tone(t, 900 + 120 * k, harm=(1, .3)) * np.exp(-t * 40)))
        parts.append(np.zeros(int(0.06 * SR)))
    return fade(norm(np.concatenate(parts), 0.8))

def happy():
    parts = []
    for f in (2600, 3100, 3600, 3300):
        parts.append(chirp([(0, f * 0.9), (0.04, f), (0.08, f * 1.05)], 0.09, a=0.1, r=0.4, vib=0.01, rasp=0.02)); parts.append(np.zeros(int(0.03 * SR)))
    return fade(norm(np.concatenate(parts), 0.8))

def step(i):
    t = T(0.05)
    y = bp(noise(len(t)), 900 + 300 * i, 4200) * np.exp(-t * 90)
    return fade(norm(y, 0.7), 2)

SOUNDS = {'ambient1': lambda: ambient(0), 'ambient2': lambda: ambient(1), 'ambient3': lambda: ambient(2), 'hurt': hurt, 'death': death, 'bite': bite,
          'eat': eat, 'happy': happy, 'step1': lambda: step(0), 'step2': lambda: step(1), 'step3': lambda: step(2)}

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'compy_sounds'
    os.makedirs(out, exist_ok=True)
    for name, fn in SOUNDS.items():
        C.rng = np.random.default_rng(sum(map(ord, 'compy' + name)))
        x = fn()
        write_ogg(x, os.path.join(out, name + '.ogg'))
        print('%-10s %.2fs' % (name, len(x) / SR))
