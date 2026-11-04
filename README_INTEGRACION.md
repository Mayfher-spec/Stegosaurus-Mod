# Prodigy Creatures — Beta Integrada v4.2.0

**Minecraft Java 1.20.1 · Forge 47.x · Java 17 · GeckoLib 4.4.9+ · ROR beta 0.71**

One Forge JAR includes two mod ids (to preserve existing saves):
- Adult `prodigycreatures:stegosaurus`: 5 variants, native Java model, original Blockbench geometry, rarer Badlands/Savannas spawning, Redwood Salad taming.
- Chibi `prodigychibi:stegosaurus_chibi`: 4 variants, 70 cubes, 17 bones, GeckoLib animation resources, Alpha 0.5.4 corrected UV-eye textures.

**Installation:** remove *both* old adult and chibi JARs before installing the single new integrated JAR. Keep **ROR** and **GeckoLib** installed. Back up the world before testing.

## Adult animation adaptation
- Faster physical movement (0.19 → 0.26) and WALK/RUN playback, grounded quadruped animation.
- Full-body IDLE no longer overlaps WALK/RUN (helps avoid plate collisions).
- Added procedural interpretations of the chibi `look_around`, `sniff`, `startle`, `stomp`, `yawn`, `sleep`, `happy` and `tail_swipe`, mapped to native adult bones, without modifying dorsal-plate bones.
- Original `bite`, `roar` and `tail_whip` are retained. Movement and combat stay responsive.

## Build in GitHub Actions
Use the workflow **Build Prodigy Creatures Integrated**. It compiles the adult's source using ForgeGradle, retrieves the pinned Alpha 0.5.2 chibi class JAR from GitHub, updates the chibi's eye textures to the Alpha 0.5.4 artwork, and merges both into one compatible JAR with two `[[mods]]` entries.

**Limitation:** the chibi 0.5.2 portion comes from the earlier, validated compiled JAR, not a fresh full GeckoLib source build. Its source is not fully reproduced in the previous branch. This is an integration beta, not yet a complete baby-to-adult transformation or a Minecraft startup validation.
