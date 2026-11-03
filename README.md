# Stegosaurus for ROR v2.0.0

Un mod independiente para **Minecraft Forge 1.20.1** que requiere **Raxiores of Raxiore** (`ror`).

## Modelo v2.0

Rehecho desde cero: cuerpo articulado y musculoso, cabeza estrecha, cuello conectado, patas con garras, once placas dorsales escalonadas unidas al cuerpo y cuatro púas de cola con varias articulaciones. La geometría **no procede** de ROR ni se extrajo de las ilustraciones. Las imágenes facilitadas se utilizan como **referencias visuales**, no como texturas listas para utilizar.

**Variantes visuales (4)**:

- **Cibernético (83 %):** negro azulado, líneas turquesas y placas luminosas en apariencia (sin luz dinámica).
- **Albino dorado (9 %):** piel marfil, patrones dorados y placas color oro.
- **Magma superviviente (3 %):** piel de carbón, lava en placas, cicatrices y resistencia al fuego.
- **Cristal glacial (5 %):** escamas azul noche y placas de hielo turquesa y blanco.

Los colores evocan los conceptos que elegimos, pero son nuevas texturas pixeladas originales. Por ahora **las placas no emiten luz real ni animación de brillo**.

**Conservamos**: 270 HP, daño base 27, armadura 14, velocidad 0.19, defensa a su dueño, domesticación usando `ror:redwood_salad` con éxito garantizado. No hay cría/montura natural en v2.0; esto es una gran revisión **visual** del prototipo, no una expansión de mecánicas.

## Compatibilidad con mundos existentes

El nombre público es **Stegosaurus for ROR** y el nombre del archivo compilado `Stegosaurus-for-ROR-2.0.0.jar`. El identificador técnico sigue siendo **`prodigycreatures`**, y el ID de entidad `prodigycreatures:stegosaurus`: así no desaparecen los estegosaurios de una partida existente. Los valores antiguos de NBT `SkinVariant` se conservan: 0 ahora cibernético, 1 albino dorado, 2 magma; el nuevo valor 3 es cristal glacial.

El huevo aparece en **Huevos de aparición** en creativo y se puede obtener con:
`/give @p prodigycreatures:stegosaurus_spawn_egg`.

## Compilación

1. La automatización de GitHub Actions ejecuta `python3 generate_textures.py` (solo biblioteca estándar de Python; sin paquetes externos).
2. Después usa Java 17 y Gradle 8.8 para compilar.
3. Descarga el artefacto desde **Actions** y extrae `Stegosaurus-for-ROR-2.0.0.jar`.
4. Instálalo junto a ROR, sustituyendo el JAR viejo. No instales dos versiones del mismo mod a la vez.

Las texturas son generadas por `generate_textures.py`. Al compilar **localmente** desde el repositorio, ejecuta ese script antes de Gradle; el ZIP del proyecto también incluye las cuatro texturas ya generadas.

**Estado:** código nuevo listo para compilar en GitHub Actions y verificar en mundo de pruebas; aún no es una garantía de que el render final sea exactamente igual a las imágenes ni de que funcionen todos los shaders. Haz copia de tu mundo antes de probar.
