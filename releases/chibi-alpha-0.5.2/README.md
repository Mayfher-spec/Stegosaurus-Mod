# Prodigy Chibi Alpha 0.5.2 — Caminata sin patinaje

**Descarga:** [Prodigy_Chibi_Alpha_0.5.2_Caminata_Sin_Patinaje_Forge_1.20.1.jar](./Prodigy_Chibi_Alpha_0.5.2_Caminata_Sin_Patinaje_Forge_1.20.1.jar)

Minecraft **1.20.1**, Forge **47+**, GeckoLib **4.4.9+**. Mod ID: `prodigychibi`.

## Corrección

- Da prioridad a `WALK` al desplazarse: las cuatro patas se animan; `RUN` permanece como recurso JSON pero no se selecciona.
- También utiliza velocidad horizontal efectiva para detectar movimiento.
- Las expresiones y reacciones se reproducen estando quieto, sin interrumpir la locomoción.
- Reduce la transición del controlador de **6** a **2 ticks**.
- Versión interna: `0.5.2-alpha.1`.
- Conserva las cuatro variantes, modelo (70 cubos/17 huesos), texturas, hitbox, IA, sonidos y recursos. Glow adicional desactivado para evitar el fallo de inicio de 0.5.0.

## Instalación

Quitar otros JARs de `Prodigy Chibi` de la carpeta `mods`; colocar este JAR y conservar GeckoLib compatible. Realizar copia del mundo antes de actualizar.

## Verificación

- Se comprobó el ZIP/JAR sin errores y se compararon los cambios contra Alpha 0.5.1.
- Pruebas Java de selección WALK/idle: correctas.
- **No se ha ejecutado una prueba de inicio de Minecraft ni una compilación ForgeGradle de 0.5.2 en esta publicación.** El usuario confirmó que 0.5.1 inicia, pero no se presupone que 0.5.2 esté validada en juego.

## Fuentes y reproducibilidad

`patch-src/ChibiAnimationSupport.java` contiene la lógica Java que está compilada en el JAR. `tests/TestChibiNoSlide.java` verifica la selección.

**El JAR 0.5.2 se creó modificando bytecode de Alpha 0.5.1** porque no se disponía del proyecto fuente completo de 0.5.1. El cambio en `ChibiStego.class` (6 → 2 ticks) y la actualización de `mods.toml` están descritos aquí, pero no están reproducidos mediante la compilación del proyecto antiguo de esta rama. Por eso, el código alojado en `chibi-alpha/` sigue correspondiendo a una alfa previa y **no debe considerarse fuente de compilación exacta del JAR 0.5.2**.

**SHA-256 del JAR:** `0c46b3f86fde5c7226d387b7a704a4063de71ffce9e0d0f9e9a5ce30b2a8eadd`

**Git blob SHA-1:** `422cbb0681006ca6e8cf2bd23d0472f76b8bd080`
