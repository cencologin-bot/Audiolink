# Audio Link

## Decisiones fijadas
- Comando: solo `/audio` exacto (un espacio opcional). Activación instantánea; si el texto deja de coincidir, la barra se cierra.
- Al enviar, `/audio` se reemplaza en el campo por `[audio ID]`.
- Quien recibe el `[audio ID]` queda autorizado.
- Detección mediante Servicio de Accesibilidad. Sin teclado propio. La barra no es movible.

## Stack (Etapa 1)
Kotlin 2.0 · Jetpack Compose + Material 3 · AGP 8.7 · minSdk 26 · targetSdk 35 (edge-to-edge).
Diseño: barra de una fila de 52 dp, sin logo, que toma el tono del teclado (colores dinámicos en Android 12+; gris claro/oscuro estándar en versiones anteriores), sin borde, transiciones animadas entre estados, respuesta háptica. Android no permite leer el color real del teclado: es una aproximación.
Las versiones son las conocidas al escribir esto; Android Studio sugerirá actualizaciones.

## Estado de las etapas
| Etapa | Contenido | Estado |
|---|---|---|
| 1 | Pantalla principal, barra, grabación, vista previa, escucha, navegación | Código entregado (v3, Compose), **sin compilar ni probar** |
| 2 | Activación por accesibilidad: reconocimiento exacto de /audio, tarjeta encima del campo, cierre con X, desaparece al borrar | Código entregado, **sin compilar ni probar** |
| 3 | Grabación real | Pendiente |
| 4 | Identificadores | Pendiente |
| 5 | Comunicación | Pendiente |
| 6 | Seguridad y pruebas | Pendiente |

## Etapa 2: notas técnicas
- La tarjeta es una ventana `TYPE_ACCESSIBILITY_OVERLAY` del propio servicio (no necesita el permiso de superposición).
- Se coloca con el borde inferior justo encima del campo y se reposiciona cuando aparece el teclado.
- Solo en Escuchar la ventana pide foco (para escribir el identificador); puede quitárselo un momento al campo de la app anfitriona. Requiere pruebas.
- Android 13+: si instalas el APK fuera de Play, hay que permitir los ajustes restringidos de la app antes de activar la accesibilidad.
- Pruebas unitarias de la regla del comando: `CommandMatcherTest` (no ejecutadas aún).

## Compilar en GitHub
1. Crea un repositorio nuevo en GitHub y sube **el contenido** de esta carpeta (incluida `.github/`).
2. Abre la pestaña **Actions**. El flujo "Build Audio Link" se ejecuta solo con cada push (o con "Run workflow").
3. Cuando termine en verde, entra en la ejecución y descarga el artefacto **AudioLink-debug-apk** (contiene `app-debug.apk`).
4. Instálalo en el móvil (permite instalar apps de origen desconocido).

Línea de comandos:
```
git init && git add . && git commit -m "Audio Link Etapa 2"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/AudioLink.git
git push -u origin main
```
No incluye `gradlew`: el flujo instala Gradle 8.10.2 por sí mismo. Android Studio lo generará al abrir el proyecto.

## Abrir el proyecto
Android Studio (última versión estable) > Open > carpeta `AudioLink` > Run en un dispositivo o emulador (Android 8.0+).
