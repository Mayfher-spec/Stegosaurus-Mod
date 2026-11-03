# Prodigy Creatures v0.1.0 — Stegosaurus (prototipo jugable)

Para Minecraft **1.20.1 / Forge 47.x / Java 17**. Proyecto **independiente** del mod Prodigy Blueprints. **Requiere Raxiores Of Raxiore (modid `ror`)**, y funciona por ID de su ítem, sin importar/copiar clases o recursos de ROR.

## Lo que incluye este primer prototipo

- Estegosaurio con **modelo cúbico original y articulado (borrador visual)**; no es todavía un modelo final al nivel de ROR.
- 3 **texturas originales**: normal (88 %), albino (9 %), magma (3 %).
- Huevo de aparición (creativo): `/give @p prodigycreatures:stegosaurus_spawn_egg`.
- Vida 270, armadura 14, daño de cola 27, velocidad 0.19, resistencia al empuje 0.85.
- Los salvajes se defienden al ser atacados; domesticados protegen al dueño.
- Se domestica con **1 Redwood Salad** (`ror:redwood_salad`) con probabilidad 100%; se consume en supervivencia.
- La misma ensalada cura **40 puntos de vida** a un ejemplar domesticado del que eres dueño.
- Clic derecho vacío con dueño: alternar entre quedarse sentado y seguirlo.
- Variante magma inmune al daño etiquetado como fuego; su textura incluye grietas/cicatrices volcánicas.
- Animación inicial de patas, cabeza y cola; sonidos provisionales nativos de Minecraft.
- **No hay aparición natural**, huevos/incubación, cría, montura ni ataques de área todavía (por seguridad y por etapas).

## Comprobación de la ensalada (hecha con el JAR beta del usuario)

Dentro de `ror-0.71(1).jar` está `data/ror/recipes/redwood_salad.json`: receta sin forma de 9 ingredientes:

`ror:great_horsetail`, `ror:gold_mushroom`, `ror:snow_flower`, `ror:stream_violet`, `ror:brown_mushroom`, `ror:trillium`, `ror:resin`, `minecraft:bowl` y `ror:redwood_pinecone`.

El objeto y su receta existen en esa compilación; algunas plantas e ingredientes pueden ser difíciles de obtener según el mundo. El addon **no modifica** esa receta.

## Cómo compilar

1. Crea un repositorio nuevo **`Prodigy-Creatures`** en GitHub (no subas esto a Prodigy Blueprints).
2. Sube todos los archivos y carpetas **de la raíz de este proyecto**, incluida `.github`.
3. En Actions, selecciona `Build Prodigy Creatures`, luego `Run workflow`.
4. Descarga el artifact ZIP; dentro encontrarás un `.jar` si la compilación pasa.
5. Prueba en un mundo de test: instala este mod **junto a ROR**, nunca en lugar de él.

**Estado:** código fuente prototipo. No se ha compilado con Gradle/Forge en este entorno; podrían requerirse correcciones de API. No es todavía un `.jar` listo para jugar.

## Objetivos próximos

- **v0.2**: rediseñar modelo en Blockbench con geometría más fiel al estilo de ROR y animaciones completas (incluido golpe de cola).
- **v0.3**: crecimiento, huevos, crianza, domesticación avanzada y aparición natural equilibrada.
- **v0.4**: montura, armadura, ataques en arco y configuración de IA por órdenes.

**Copyright:** modelo, animaciones y texturas originales. No contiene recursos del mod ROR. Referencia de compatibilidad y balance únicamente.
