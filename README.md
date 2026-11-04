# Stegosaurus for ROR — v4.1.0 (Forge 1.20.1)

## What was updated
- Uses the user's latest `estegosaurio_ror_variantes-2.zip` Blockbench *Modded Entity* geometry, **64 cubes and 63 named bone groups**, UV atlas 512×512.
- Four newly authored textures: **Natural (green), Cybernetic, Magma, Golden** with separate emissive layers, copied byte-identically.
- Retains **Glacial** as **legacy saved variant 3**, recolored from the new cyber texture because the user ZIP contains no new Glacial atlas.
- Native Minecraft keyframe definitions converted directly from **six supplied Blockbench animations**: idle (4 s), walk (2 s), run (1 s), tail_whip (1.8 s), bite (1.2 s), roar (3 s).
- `run` used for fast movement, `walk` for slower movement, `tail_whip` (80%)/`bite` (20%) triggered on a successful hit, `roar` occasionally while hostile. Combat timing and collision are the v4 rules and have **not** been visually verified in Minecraft.
- **No GeckoLib**. Forge native `ModelPart`/`AnimationDefinition` preserved.
- Variant IDs stable for old saves: `0=Cyb`, `1=Golden`, `2=Magma`, `3=Glacial`, **`4=Natural`**.
- New natural spawn mix: Natural 65%, Cybernetic 20%, Golden 9%, Magma 3%, Glacial 3% across existing Badlands/Savanna spawn rules (weight=2, group 1–2).
- Keeps existing entity ID, stats (270 HP, 27 attack), Redwood Salad taming, sounds and biome modifiers. ROR remains mandatory in mods.toml.
- Source includes script `tools_generate_native.py` for regenerating native model and animations from the green .bbmodel.

## Test checklist
1. Back up your save and remove the previous Stegosaurus JAR from `mods`.
2. Use Minecraft 1.20.1, Forge 47.4.x, ROR beta 0.71. GeckoLib is not needed **by this mod**.
3. Test each variant in a creative test world. Summon `prodigycreatures:stegosaurus` and specify NBT `SkinVariant` 0..4.
4. Look from front/side/behind/above; especially zero-thickness dorsal plates which require double-sided alpha rendering. Test glowing textures during darkness.
5. Check the jaw and head pitch, walk/run leg motion, tail attack, bite and occasional roar; animation-blend quality is not yet proven in-game.
6. Test natural spawns in Badlands/Savanna and Redwood Salad tame. Check old saved cyber/gold/magma/glacial dinosaurs.

## Implementation limits
- No chibi/juvenile form, RPG progression, mount, armor or skill tree implemented yet.
- Entity hitbox remains 4x5 blocks, much smaller than the body + tail; to be revisited.
- Build success does not guarantee correct animation appearance; validate in Minecraft before migrating your main world.
- The original ROR creatures/assets are not bundled here.
