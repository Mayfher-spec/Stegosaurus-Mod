#!/usr/bin/env python3
"""Rebuild native Forge 1.20.1 geometry/animations from user Blockbench Modded Entity file.
No GeckoLib, no copy of ROR code or artwork. Uses shared UV-compatible variant geometry.
"""
import json, math, pathlib, collections
ROOT=pathlib.Path(__file__).parent
DATA=json.loads((ROOT/'custom_assets/estegosaurio_verde.bbmodel').read_text(encoding='utf8'))
BASE=ROOT/'src/main/java/com/prodigytown/creatures/client'
BASE.mkdir(parents=True,exist_ok=True)


def num(n,precision=5):
    n=round(float(n),precision)
    if not n:return '0F'
    s=f'{n:.{precision}f}'.rstrip('0').rstrip('.')
    return s+'F'

GROUPS={};ELEMENTS={e['uuid']:e for e in DATA['elements']}

def traverse(node,parent='root'):
    assert isinstance(node,dict)
    name=node['name']
    assert name not in GROUPS,name
    GROUPS[name]=(node,parent)
    for child in node.get('children',[]):
        if isinstance(child,dict):traverse(child,name)
for child in DATA['outliner']:traverse(child)

parts=[]
for i,(name,(g,parent)) in enumerate(GROUPS.items()):
    par=[0,0,0] if parent=='root' else GROUPS[parent][0]['origin']
    here=g.get('origin',[0,0,0]);r=g.get('rotation',[0,0,0]) or [0,0,0]
    x=here[0]-par[0];y=par[1]-here[1];z=here[2]-par[2]
    cubes=[ELEMENTS[v] for v in g.get('children',[]) if isinstance(v,str)]
    chain='CubeListBuilder.create()'
    for c in cubes:
        lo=c['from'];hi=c['to'];tex=c.get('uv_offset',[0,0]);mir=bool(c.get('mirror_uv',False))
        # Original coordinates point upward for Y, Minecraft ModelPart Y points down.
        xmin=lo[0]-here[0];ymin=here[1]-hi[1];zmin=lo[2]-here[2]
        dx=hi[0]-lo[0];dy=hi[1]-lo[1];dz=hi[2]-lo[2]
        assert dx>=0 and dy>=0 and dz>=0
        chain+=f'\n      .mirror({str(mir).lower()}).texOffs({int(tex[0])}, {int(tex[1])}).addBox({num(xmin)}, {num(ymin)}, {num(zmin)}, {num(dx)}, {num(dy)}, {num(dz)})'
    rx=-math.radians(r[0]);ry=math.radians(r[1]);rz=-math.radians(r[2])
    # Minecraft EntityModel origin is at the feet: the base ALL bone starts +24 px.
    if parent == "root": y += 24.0
    pose=f'PartPose.offset({num(x)}, {num(y)}, {num(z)})' if not any(abs(v)>1e-6 for v in (rx,ry,rz)) else f'PartPose.offsetAndRotation({num(x)}, {num(y)}, {num(z)}, {num(rx)}, {num(ry)}, {num(rz)})'
    pvar='root' if parent=='root' else 'part_'+str(list(GROUPS).index(parent))
    parts.append(f'    PartDefinition part_{i} = {pvar}.addOrReplaceChild("{name}", {chain}, {pose});')

model='''package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** From user-authored Blockbench geometry. Four original skins share identical native ModelParts. */
public final class StegosaurusModel<T extends StegosaurusEntity> extends HierarchicalModel<T> {
  public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus"), "main");
  private final ModelPart root;
  public StegosaurusModel(ModelPart root) { this.root = root; }
  @Override public ModelPart root() { return root; }
  @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    root.getAllParts().forEach(ModelPart::resetPose);
    this.animate(entity.idleAnimationState, StegosaurusAnimations.IDLE, ageInTicks);
    if (!entity.isOrderedToSit()) {
      // Keep the two separately-authored gaits. We use horizontal speed to choose between them.
      boolean sprint = entity.getDeltaMovement().horizontalDistanceSqr() > 0.032D;
      this.animateWalk(sprint ? StegosaurusAnimations.RUN : StegosaurusAnimations.WALK,
        limbSwing, limbSwingAmount, 1.0F, 1.0F);
    }
    this.animate(entity.tailWhipAnimationState, StegosaurusAnimations.TAIL_WHIP, ageInTicks);
    this.animate(entity.biteAnimationState, StegosaurusAnimations.BITE, ageInTicks);
    this.animate(entity.roarAnimationState, StegosaurusAnimations.ROAR, ageInTicks);
    ModelPart head = root.getChild("all").getChild("body2").getChild("neck2").getChild("neck").getChild("head");
    head.yRot += netHeadYaw * ((float)Math.PI / 180.0F) * 0.35F;
    head.xRot += headPitch * ((float)Math.PI / 180.0F) * 0.35F;
  }
  @Override public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay,
    float red, float green, float blue, float alpha) {
    root.render(pose, consumer, light, overlay, red, green, blue, alpha);
  }
  public static LayerDefinition createBodyLayer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
'''+ '\n'.join(parts)+'''
    return LayerDefinition.create(mesh, 512, 512);
  }
}
'''
(BASE/'StegosaurusModel.java').write_text(model,encoding='utf8')

animintro='''package com.prodigytown.creatures.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Six native 1.20.1 animations converted from the user's Blockbench animation keyframes. */
public final class StegosaurusAnimations {
  private StegosaurusAnimations() {}
'''

anims=[]
for a in DATA['animations']:
    name=a['name'].upper();length=a['length'];loop=a.get('loop')=='loop'
    meth='create'+''.join(w.title() for w in a['name'].split('_'))
    header=f'  public static final AnimationDefinition {name} = {meth}();'
    anims.append((header,meth,a,loop))

body=[]
for header,meth,a,loop in anims:
    body.append(f'  private static AnimationDefinition {meth}() {{')
    body.append(f'    AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength({num(a["length"])});')
    if loop:body.append('    builder.looping();')
    for animator in a['animators'].values():
        bone=animator['name']
        assert bone in GROUPS, ('missing bone',bone,a['name'])
        channels=collections.defaultdict(list)
        for f in animator.get('keyframes',[]):channels[f['channel']].append(f)
        for chan,frames in channels.items():
            target={'rotation':'ROTATION','position':'POSITION','scale':'SCALE'}.get(chan)
            if target is None:raise ValueError(chan)
            body.append(f'    builder.addAnimation("{bone}", new AnimationChannel(AnimationChannel.Targets.{target},')
            for j,kf in enumerate(sorted(frames,key=lambda k:k['time'])):
                dp=kf['data_points'][0];x=float(dp.get('x',0));y=float(dp.get('y',0));z=float(dp.get('z',0));
                if chan=='rotation':x,y,z=-x,y,-z;vname='degreeVec'
                elif chan=='position':y=-y;vname='posVec'
                else:vname='scaleVec'
                inter={'catmullrom':'CATMULLROM','linear':'LINEAR','step':'LINEAR'}.get(kf.get('interpolation'),'LINEAR')
                suffix=',' if j<len(frames)-1 else ''
                body.append(f'      new Keyframe({num(kf["time"],6)}, KeyframeAnimations.{vname}({num(x,4)}, {num(y,4)}, {num(z,4)}), AnimationChannel.Interpolations.{inter}){suffix}')
            body.append('    ));')
    body.append('    return builder.build();')
    body.append('  }')

animtxt=animintro+'\n'.join(header for header,_,_,_ in anims)+'\n'+'\n'.join(body)+'\n}\n'
(BASE/'StegosaurusAnimations.java').write_text(animtxt,encoding='utf8')
print('generated','groups',len(GROUPS),'cubes',len(DATA['elements']),'java model bytes',len(model),'animation bytes',len(animtxt))
for _,_,a,_ in anims:print('anim',a['name'],'keyframes',sum(len(v['keyframes']) for v in a['animators'].values()))