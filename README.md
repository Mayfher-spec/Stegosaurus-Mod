# Stegosaurus for ROR v2.1.0

Un mod original, independiente para **Minecraft Java 1.20.1 / Forge 47.x**,
con dependencia de **Raxiores of Raxiore** (`ror`).

## Revisión completa del modelo (v2.1)

En comparación con las versiones 0.1/2.0, esta versión reconstruye el modelo
3D renderizado **dentro de Minecraft**; las imágenes de concepto NO son texturas.

- Volumen de cuerpo, hombros y caderas más macizo, vientre escalonado y placas de armadura lateral.
- Cabeza conectada con hocico, mandíbula, cejas, ojos, pómulos y narinas.
- **Dos filas asimétricas** de placas dorsales tipo hoja: 24 piezas altas más las placas de transición.
  Cada placa posee 8 niveles de geometría, borde oscuro, cristal interior y detalles.
- Patas pesadas con rodillas blindadas y tres garras delanteras por pie.
- Cola articulada en tres segmentos, cuatro púas escalonadas y crestas.
- Cuatro texturas **1024 × 1024** generadas con pixel-art original, escamas,
  vetas, grietas y detalles propios de cada variante.
- **Segunda pasada emisiva**: solo las placas, líneas seleccionadas y ojos se ven
  iluminados incluso a oscuras. **No producen luz real sobre el terreno**.

Los conceptos artísticos son la **dirección visual** y no un modelo exacto de
Blockbench. Las proporciones y el shader pueden verse diferentes en Minecraft;
la prueba del `.jar` en el mundo plano es el último paso para evaluar fidelidad.

### Variantes

- Cibernético (83 %), base negro azulada, placas turquesa.
- Albino dorado (9 %), marfil, placas y adornos dorados.
- Magma superviviente (3 %), carbón, cicatrices y venas naranjas; inmune al fuego.
- Cristal glacial (5 %), azul noche y cristal celeste.

Las rarezas se aplican al aparecer. Sin crianza ni montura en esta versión.

## Mecánicas conservadas

- 270 HP, ataque 27, armadura 14, velocidad 0.19 y golpe defensivo de cola.
- Domesticación con `ror:redwood_salad` (1 ensalada, éxito garantizado).
- Huevo de aparición en la pestaña **Huevos de aparición**.
- Comando `/give @p prodigycreatures:stegosaurus_spawn_egg`.
- IDs de entidad/ítem **`prodigycreatures:*` intactos** y números NBT de
  variantes 0=cibernético, 1=albino, 2=magma, 3=glacial.

## Compilación

GitHub Actions genera las 8 texturas (base y emisiva) con `python3 generate_textures.py`
y compila usando Java 17 y Gradle 8.8. El archivo resultante es
**`Stegosaurus-for-ROR-2.1.0.jar`**.

Para compilar localmente:

```sh
python3 generate_textures.py
gradle --no-daemon build
```

Para instalar: desinstala cualquier JAR anterior de este mod, coloca el nuevo
junto a ROR y pruébalo primero en un mundo de pruebas. Haz respaldo del mundo
antes de usarlo en The Prodigy Town. No es necesario borrar los dinosaurios
antiguos: conservan sus identificadores y variante persistida.

## Notas de autoría

Geometría, UV, texturas y código nuevos. No se ha copiado código, modelos,
animaciones ni texturas de ROR. Se utilizan imágenes de referencia para
orientar estilo y proporciones, no como recursos listos para exportar.
