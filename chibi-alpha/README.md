# Prodigy Chibi Alpha 0.2 — Baby

Mod independiente para Forge 1.20.1 y GeckoLib 4.4.9. ID: `prodigychibi`. No modifica Stegosaurus for ROR adulto.

**El chibi NO se monta.** Se eliminó la silla, el control de jinete y la geometría de montura.

## Cuatro variantes (mismo modelo)

- Natural: `/summon prodigychibi:stegosaurus_chibi ~ ~ ~ {ChibiVariant:0}`
- Cibernético: `/summon prodigychibi:stegosaurus_chibi ~ ~ ~ {ChibiVariant:1}`
- Magma: `/summon prodigychibi:stegosaurus_chibi ~ ~ ~ {ChibiVariant:2}`
- Albino Dorado: `/summon prodigychibi:stegosaurus_chibi ~ ~ ~ {ChibiVariant:3}`

Al aparecer con huevo, el chibi elige una variante. Interacción con trigo si está herido. Animaciones idle y caminar.

Las máscaras `*_glow.png` están preparadas como recursos, pero **el efecto emisivo aún no se implementó en el renderer**; se probará después.

El atlas de las variantes se dibuja específicamente para la geometría de nuestra Alpha 0.1 usando como referencia la paleta del ZIP fase1 aportado por el usuario. No se copiaron sus UV ni se sustituyó la geometría preferida.

El crecimiento, monturas adultas, animaciones especiales, domesticación y crianza quedan para versiones posteriores.
