"""Genera el texto de CompyLayers.java a partir del esqueleto de mrig.py."""
import numpy as np, mrig, os, sys
from mrig import crig
sys.path.insert(0, os.path.join(mrig.HERE, '..', 'carnotaurus'))
import paint as cpaint

def fit_hy():
    """altura del pivote del cuerpo para que el punto mas bajo de la pose de reposo toque el suelo."""
    (spec, cubes), H = mrig.get_all(); fr = cpaint.frames(spec); lo = 1e9
    for c in cubes:
        R, T = fr[c['part']]
        for x in (c['x0'], c['x0'] + c['w']):
            for y in (c['y0'], c['y0'] + c['h']):
                for z in (c['z0'], c['z0'] + c['d']):
                    lo = min(lo, (T + R @ np.array([x, y, z]))[1])
    return mrig.BODY_Y - lo

def layers_java():
    (spec, cubes), H = mrig.get_all()
    hy = fit_hy()
    L = crig.java_lines(spec, cubes, hy)
    src = '''package com.prodigytown.creatures.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERADO por tools/compy/mexport.py (esqueleto propio del Compy: cuello en S, cola de 6 tramos, patas de 4 tramos + 3 dedos). No editar a mano. */
final class CompyLayers {
    private CompyLayers() {}
    static final int TEX_W = %d;
    static final int TEX_H = %d;
    /** Altura (unidades de modelo) del pivote del cuerpo sobre el suelo en reposo. */
    static final float BODY_Y = %sF;

    static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
%s
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }
}
''' % (crig.ATLAS, H, '%.3f' % hy, '\n'.join(L))
    return src, hy, H
