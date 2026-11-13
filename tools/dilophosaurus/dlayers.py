"""Genera el texto de DilophosaurusLayers.java (adulto + cria) a partir del esqueleto de drig.py."""
import numpy as np, drig, os, sys
from drig import crig
sys.path.insert(0, os.path.join(drig.HERE, '..', 'carnotaurus'))
import paint as cpaint

def fit_hy(stage):
    """altura del pivote del cuerpo para que el punto mas bajo de la pose de reposo toque el suelo."""
    out, H = drig.get_all(); spec, cubes = out[stage]; fr = cpaint.frames(spec); lo = 1e9
    for c in cubes:
        R, T = fr[c['part']]
        for x in (c['x0'], c['x0'] + c['w']):
            for y in (c['y0'], c['y0'] + c['h']):
                for z in (c['z0'], c['z0'] + c['d']):
                    lo = min(lo, (T + R @ np.array([x, y, z]))[1])
    return 66.0 - lo

def layers_java():
    out, H = drig.get_all()
    hy = {st: fit_hy(st) for st in out}
    def layer(st):
        spec, cubes = out[st]
        L = crig.java_lines(spec, cubes, hy[st])
        return '    static LayerDefinition %s() {\n        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n%s\n        return LayerDefinition.create(mesh, TEX_W, TEX_H);\n    }\n' % (st, '\n'.join(L))
    src = '''package com.prodigytown.creatures.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERADO por tools/dilophosaurus/dexport.py (esqueleto propio con doble cresta y membrana de 7 petalos). No editar a mano. */
final class DilophosaurusLayers {
    private DilophosaurusLayers() {}
    static final int TEX_W = 512;
    static final int TEX_H = %d;
    /** Altura (unidades de modelo) del pivote del cuerpo sobre el suelo en reposo. */
    static final float ADULT_BODY_Y = %sF;
    static final float CHIBI_BODY_Y = %sF;

%s
%s}
''' % (H, ('%.3f' % hy['adult']), ('%.3f' % hy['chibi']), layer('adult'), layer('chibi'))
    return src, hy, H
