# Cronista Mutante — integración aislada (Forge 1.20.1)

Rama de desarrollo: `feature/cronista-mutante-quests`.

## Objetivo
Integrar un NPC humanoide de misiones inspirado en las referencias originales: túnica oscura, rostro humanoide y **solo una cola de dinosaurio**. El NPC aceptará misiones y entregará recompensas sin afectar las criaturas existentes.

## Estado
- La rama está separada de `main`.
- Existe un prototipo fuente local denominado `Prodigy_Cronista_Modelo_y_Misiones_v02.zip`, que incluye `.bbmodel`, geometría GeckoLib, textura, animaciones y Java.
- **No hay todavía un JAR compilado y verificado ni código fusionado en el mod.**
- El código GitHub de Stegosaurus-Mod está en versión 0.1.0, mientras que el JAR usado actualmente por el usuario es una versión posterior (1.4.1). No sustituir esa versión instalada con un build de este repositorio sin reconciliar diferencias.

## Compilación pendiente
1. Migrar las fuentes y recursos del proyecto del Cronista a un módulo separado o actualizar el código vigente de Prodigy Creatures.
2. Configurar GeckoLib 4.8.4 para Forge 1.20.1 y Java 17.
3. Compilar con Gradle/ForgeGradle y corregir los errores de compilación.
4. Probar spawn, render, animación de cola, aceptación y recompensa de misiones en mundo de pruebas.
5. Publicar el artefacto JAR de GitHub Actions si la compilación y las pruebas son satisfactorias.

No se debe renombrar un ZIP o JAR incompleto como si fuera una versión jugable.
