# AGENTS.md — TurnoClase Android

## Descripción del repositorio

Aplicación Android de TurnoClase, sistema de gestión de turnos de preguntas para el aula. Contiene dos módulos de aplicación dentro del proyecto Gradle `TurnoClase/`:

- **`turnoclase`** — App del alumno. Muestra el turno asignado y permite unirse a un aula mediante un código. Package: `com.jaureguialzo.turnoclase`.
- **`turnoclaseprofesor`** — App del profesor. Gestiona el aula, asigna turnos y llama al backend para generar códigos. Package: `com.jaureguialzo.turnoclaseprofesor`.

## Stack tecnológico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Backend:** Firebase (Auth, Firestore, App Check con Play Integrity, Cloud Functions)
- **Build:** Gradle 9.x, compileSdk 36, minSdk 23, Java 17
- **Despliegue:** Fastlane (lanes `capturas` y `playstore`)

## Dependencias externas

El fichero `private/android/keystore.properties` (del repositorio `private/`) es necesario para compilar en modo release. Contiene la ruta al keystore y las contraseñas de firma.

## Comandos habituales

Todos los comandos se ejecutan desde `TurnoClase/`.

```bash
# Compilar en debug
./gradlew assembleDebug

# Compilar en release (requiere keystore del repo private/)
./gradlew assembleRelease

# Ejecutar tests unitarios
./gradlew test

# Ejecutar tests de instrumentación (requiere emulador o dispositivo)
./gradlew connectedAndroidTest

# Limpiar build
./gradlew clean
```

### Fastlane (desde `TurnoClase/fastlane/`)

```bash
# Lanzar emulador interactivo
fastlane emulador

# Tomar capturas de pantalla
fastlane capturas

# Subir release a Google Play
fastlane playstore
```

## Estructura del proyecto

```
TurnoClase/
├── build.gradle            # Configuración raíz (versiones SDK compartidas)
├── settings.gradle         # Módulos incluidos
├── turnoclase/             # Módulo app alumno
│   ├── build.gradle
│   └── src/main/kotlin/
├── turnoclaseprofesor/     # Módulo app profesor
│   ├── build.gradle
│   └── src/main/kotlin/
├── libs/                   # Librerías locales (.jar)
└── fastlane/               # Automatización de capturas y despliegue
```

## Convenciones

- El código fuente está en `src/main/kotlin/` dentro de cada módulo (no `java/`).
- Los recursos de Firebase (`google-services.json`) están incluidos en cada módulo.
- Las dependencias de Firebase se gestionan mediante el BOM (`firebase-bom`).
- No modificar `local.properties` ni los ficheros de caché de Gradle (`.gradle/`).

## Commits

Al completar cualquier característica o cambio, crear un commit con:

- **Mensaje en español**, en imperativo y conciso (p.ej. `Añadir soporte para notificaciones push`).
- Un commit por característica o cambio cohesionado; no agrupar cambios no relacionados.
- No incluir ficheros de caché, build artifacts ni ficheros generados (`.gradle/`, `build/`, `local.properties`).

## Consideraciones para agentes

- El repo `private/` es un repositorio separado que debe estar disponible en `../../private/` relativo a `TurnoClase/` para que el firmado release funcione.
- No incluir nunca claves, keystores ni credenciales en el código fuente.
- Los `google-services.json` ya están incluidos y no deben regenerarse sin motivo.
- Al añadir dependencias, usar el BOM de Firebase y el BOM de Compose en lugar de versiones individuales.
- La versión del SDK se define en el `build.gradle` raíz (`ext.compileSdkVersion`, etc.) y se referencia desde los módulos.
