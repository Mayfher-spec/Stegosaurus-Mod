# Stegosaurus for ROR v4.0.0 (Minecraft Forge 1.20.1)

**Native Minecraft ModelPart / Java model; GeckoLib no longer required.**

Built from the user's `estegosaurio_ror_variantes.zip` (`Blockbench Modded Entity`), which includes **3 identical 52-cube, 50-bone geometries** (cybernetic, magma, golden), 512x512 user-authored texture and emissive PNG for each, and Blockbench animations `idle`, `walk`, `tail_whip`.

### Technical implementation
- `StegosaurusModel.java`: native baked `LayerDefinition`, complete 50-bone hierarchy, box UV layout, 10 zero-thickness dorsal plates, double-sided alpha via standard entity model render type.
- `StegosaurusAnimations.java`: native 1.20.1 `AnimationDefinition`/`Keyframe` conversion, preserving timings and Catmull-Rom interpolation.
- `StegosaurusRenderer.java`: vanilla/Forge `MobRenderer` with shadow radius 3.0.
- `StegosaurusGlowLayer.java`: glowmask on native RenderType.eyes (fullbright, but NOT dynamic actual light).
- Entity behavior, saved NBT (`SkinVariant:0..3`), egg, 270 HP, Redwood Salad, and spawn weights in Badlands/Savannas preserved.
- New entity hitbox **4.0 wide x 5.0 high** (from supplied README). **WARNING:** this matches the intended T-Rex hitbox, but does not cover full 15-block tail; vanilla mobs only have one hitbox.
- Glacial skin was NOT in user ZIP: derived from 512x512 cyber atlas, matching its UVs; mark it generated.
- `ror` remains mandatory runtime dependency; `geckolib` is removed from dependencies and source.

### Test plan
Load in isolated Forge 47.4.x + ROR beta 0.71 (without GeckoLib if other mods do not require it); check spawn egg and `/summon prodigycreatures:stegosaurus`; verify 3 authored skins + generated glacial; check body directions, transparent dorsal plates, head/leg/tail animation, strike on hit, glow, collision/hitbox, natural spawn in Badlands and Savanna.

### Build
With a Java 17 toolchain and ForgeGradle 6: `./gradlew build` if wrapper present, else `gradle build`.
The source ZIP includes all editable Blockbench models in `custom_assets/`.
