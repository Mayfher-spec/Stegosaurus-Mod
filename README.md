# Stegosaurus for ROR v3.0.0

Mod original para **Minecraft 1.20.1, Forge 47.4.0 y GeckoLib 4.8.4**, integrado como mod adicional a ROR.

## Actualizacion GeckoLib

- Modelo de 48 huesos y 1121 cubos geometrizados en `assets/prodigycreatures/geo/stegosaurus.geo.json` (formato Bedrock para Blockbench).
- Cinco animaciones: idle, walk, sit, eat y attack (coletazo disparado desde servidor).
- Cuatro variantes conservadas: cybernetic, golden, magma, glacial.
- Mascara luminosa `_glowmask.png` de GeckoLib para cada skin; no proyecta luz en el terreno.
- Mantiene el ID interno `prodigycreatures` para compatibilidad con criaturas guardadas; nombre visible fijo `Stegosaurus for ROR`.
- 270 HP, 27 daño, armadura 14 y domesticación con `ror:redwood_salad` sin cambios.

## Instalacion

1. Mantener ROR beta 0.71 del modpack.
2. Instalar **GeckoLib 4.8.4 para Forge 1.20.1** en `mods` (obligatorio).
3. Eliminar el JAR antiguo de Stegosaurus for ROR para evitar duplicados.
4. Instalar `Stegosaurus-for-ROR-3.0.0.jar` y probar en un mundo plano.

## Desarrollo

`python3 generate_textures.py && python3 make_gecko_model.py`

`gradle --no-daemon build`

La geometría se puede abrir en Blockbench. Los PNG son texturas pixel-art originales; los artes de referencia solo guían la apariencia. No se copian recursos de ROR.

**Notas:** La integración necesita pruebas visuales dentro de Minecraft; que compile no garantiza que las animaciones, pivotes o brillos funcionen con cada shader. La animación de comer se incluye como recurso para futuras mejoras de IA y aún no se activa de forma automática.
