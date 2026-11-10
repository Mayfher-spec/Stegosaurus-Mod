"""Exporta el carnotauro: CarnotaurusLayers.java + texturas (4 colores x 2 mallas, brillo, silla). Uso: python3 export.py <repo>"""
import sys, os, numpy as np
HERE=os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0,HERE)
import rig, paint
from PIL import Image
repo=sys.argv[1] if len(sys.argv)>1 else '.'
tex=os.path.join(repo,'src/main/resources/assets/prodigycreatures/textures/entity')
out,H=rig.get_all()
hy={st:paint.fit_hy(st) for st in out}
def layer(st):
    spec,cubes=out[st]
    L=rig.java_lines(spec,cubes,hy[st])
    return '    static LayerDefinition %s() {\n        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n%s\n        return LayerDefinition.create(mesh, TEX_W, TEX_H);\n    }\n'%(st,'\n'.join(L))
src='''package com.prodigytown.creatures.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERADO por tools/carnotaurus/export.py (esqueleto propio, inspirado en la anatomia de los dinos de alta calidad). No editar a mano. */
final class CarnotaurusLayers {
    private CarnotaurusLayers() {}
    static final int TEX_W = 512;
    static final int TEX_H = %d;
    /** Altura (unidades de modelo) del pivote del cuerpo sobre el suelo en reposo. */
    static final float ADULT_BODY_Y = %sF;
    static final float CHIBI_BODY_Y = %sF;

%s
%s}
'''%(H,('%.3f'%hy['adult']),('%.3f'%hy['chibi']),layer('adult'),layer('chibi'))
open(os.path.join(repo,'src/main/java/com/prodigytown/creatures/client/CarnotaurusLayers.java'),'w').write(src)
for var in ('scarlet','magma','steel','arena'):
    a,g=paint.make_atlas(var)
    Image.fromarray(paint.dilate(a)).save(os.path.join(tex,'carnotaurus_%s.png'%var),optimize=True)
    Image.fromarray(g).save(os.path.join(tex,'carnotaurus_%s_glow.png'%var),optimize=True)
Image.fromarray(paint.make_saddle()).save(os.path.join(tex,'carnotaurus_saddle.png'),optimize=True)
print('H',H,'hy',hy)
