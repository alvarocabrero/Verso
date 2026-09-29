# Desarrollo

Guía para preparar el entorno, compilar, probar y contribuir.

## Versiones

| Pieza | Versión |
|---|---|
| Gradle (wrapper) | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 (con el plugin de Compose) |
| KSP | 2.0.21-1.0.28 |
| Compose BOM | 2024.12.01 (Material 3) |
| Room | 2.6.1 |
| Navigation Compose | 2.8.5 |
| Lifecycle | 2.8.7 |
| JUnit | 4.13.2 |
| JDK | 17 |
| minSdk / compileSdk / targetSdk | 26 / 35 / 35 |

Todas las versiones de librerías están en `gradle/libs.versions.toml` (catálogo de
versiones). Son de finales de 2024 y se pueden actualizar; al hacerlo, sube Kotlin, KSP y
el plugin de Compose a la vez, porque van emparejados.

## Preparar el entorno

### Con Android Studio (lo más sencillo)

1. Instala Android Studio y ábrelo una vez para que descargue el SDK.
2. *File → Open* sobre la carpeta del proyecto. Android Studio crea `local.properties` y
   usa su propio JDK 17.
3. Ejecuta la configuración `app` en un emulador o un móvil.

### Solo con la línea de comandos (Windows)

Así se preparó el entorno actual del proyecto:

1. **JDK 17**:
   ```bash
   winget install --id EclipseAdoptium.Temurin.17.JDK -e
   ```
   Queda en `C:\Program Files\Eclipse Adoptium\jdk-17…`. Si `java` no está en el PATH,
   define `JAVA_HOME` antes de usar Gradle:
   ```bash
   export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
   ```
   (En PowerShell: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"`.)

2. **Android SDK**: descarga las *command-line tools* de
   [developer.android.com/studio](https://developer.android.com/studio#command-line-tools-only)
   y descomprímelas en `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`. Después:
   ```bash
   sdkmanager --licenses
   sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
   ```
   Gradle descargará por su cuenta otras build-tools que necesite (por ejemplo, la 34).

3. **`local.properties`** en la raíz (no se sube a git):
   ```properties
   sdk.dir=C:/Users/<usuario>/AppData/Local/Android/Sdk
   ```
   Usa barras normales `/`; las invertidas deben escaparse (`C\:\\Users\\…`).

### macOS / Linux

Igual, con el JDK 17 de tu gestor de paquetes y el SDK en `~/Library/Android/sdk` o
`~/Android/Sdk`. El archivo `.gitattributes` mantiene `gradlew` con finales de línea Unix
para que sea ejecutable.

## Tareas de Gradle

| Comando | Qué hace |
|---|---|
| `./gradlew assembleDebug` | Compila el APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) |
| `./gradlew installDebug` | Compila e instala en el dispositivo conectado |
| `./gradlew testDebugUnitTest` | Ejecuta los tests JUnit del motor |
| `./gradlew :app:compileDebugKotlin --rerun` | Recompila Kotlin para ver avisos |
| `./gradlew clean` | Borra lo compilado |

En Windows se usa `gradlew.bat` (o `./gradlew.bat` desde Git Bash).

Informe de tests en HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.
Resultados en XML: `app/build/test-results/testDebugUnitTest/`.

El proyecto compila **sin avisos**; conviene mantenerlo así.

## Emulador

### Aceleración por hardware (Windows)

El emulador necesita la **Plataforma del hipervisor de Windows** (WHPX). Para activarla,
en una terminal de administrador:

```bash
Dism /Online /Enable-Feature /FeatureName:HypervisorPlatform /All
```

y reinicia. La virtualización también debe estar activada en la BIOS/UEFI (AMD-V o
VT-x). Para comprobarlo:

```bash
emulator -accel-check
```

Debe decir *"WHPX … is installed and usable"*.

### Crear y arrancar el dispositivo virtual

```bash
sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n Verso_API35 -k "system-images;android-35;google_apis;x86_64" -d pixel_7
emulator -avd Verso_API35 -no-audio -no-boot-anim
```

Esperar al arranque e instalar:

```bash
adb wait-for-device
adb shell getprop sys.boot_completed      # 1 cuando ha terminado
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tuapp.verso/com.tuapp.MainActivity
```

El `applicationId` es `com.tuapp.verso`; el paquete del código, `com.tuapp`.

### Probar a mano con adb

| Acción | Comando |
|---|---|
| Captura de pantalla | `adb exec-out screencap -p > captura.png` |
| Tocar | `adb shell input tap X Y` |
| Escribir (sin tildes ni ñ) | `adb shell input text 'hola%smundo'` (`%s` = espacio) |
| Intro / Escape / fin de línea | `adb shell input keyevent 66` / `111` / `123` |
| Modo oscuro | `adb shell cmd uimode night yes` (y `no` para volver) |
| Árbol de la interfaz | `adb shell uiautomator dump /sdcard/ui.xml` y `adb shell cat /sdcard/ui.xml` |
| Errores de la app | `adb logcat -b crash` |

Consejos:
- `adb input text` no admite caracteres fuera de ASCII: para probar tildes, escribe a
  mano en el emulador o usa versos sin ellas.
- Gboard puede abrir un tutorial de lápiz al recibir texto por adb y tragarse lo escrito.
  Desactívalo con `adb shell settings put secure stylus_handwriting_enabled 0`.
- Desde Git Bash en Windows, antepón `MSYS_NO_PATHCONV=1` para que rutas como
  `/sdcard/ui.xml` no se conviertan en rutas de Windows.

### Lista de comprobación manual del editor

- [ ] El margen queda alineado con cada verso, también en versos partidos en varias líneas.
- [ ] Escribir al final de un poema largo mantiene el cursor visible sobre el teclado.
- [ ] El panel se pliega al abrir el teclado.
- [ ] Tocar un recurso lo resalta y desplaza la pantalla; tocarlo otra vez lo quita.
- [ ] Aliteración clara con subrayado sólido; posible, punteado.
- [ ] Modo oscuro legible (margen, resaltado, panel).
- [ ] Salir y volver a entrar conserva la nota; una nota vaciada desaparece.

## Convenciones

- **Idioma**: nombres de clases, funciones, variables, comentarios, commits e interfaz en
  español (`alCambiar`, `resaltadoDe`, `Preferencias`). Los nombres de parámetros de
  callbacks siguen el patrón `alHacerAlgo`.
- **Motor sin Android**: nada en `analisis/` puede importar `android.*` ni
  `androidx.*`. Si algo necesita Android, va en `ui/` o `data/`.
- **Tests**: todo cambio en el motor va con tests, preferiblemente con versos reales.
- **Estado**: las pantallas leen estado de Compose del ViewModel; los cálculos pesados
  van fuera del hilo principal.
- **Textos**: tono sencillo y cercano, sin tecnicismos en la interfaz (*"No hay recursos
  detectados todavía."*).
- **Diseño**: versos en serif con interlineado amplio; interfaz en la sans del sistema;
  sin sombras; colores del tema, nunca fijos (para que funcione el modo oscuro).

## Cómo añadir un recurso literario

1. Añade el valor al enum `Recursos.Tipo` con su nombre visible y descripción.
2. Escribe el detector como función privada en `Recursos.kt` y llámalo desde `detectar()`
   (dentro del bucle de estrofas si depende de versos consecutivos, o en el de líneas si
   es de un solo verso). Devuelve `Recurso(tipo, lineas, evidencia, palabras)`.
3. Si el resaltado debe ser distinto del genérico (todas las palabras de `palabras` o el
   verso entero si está vacía), añade un caso en `AnalisisPoema.rangos`.
4. Tests en `RecursosTest` (que lo detecta y que **no** da falsos positivos en versos
   conocidos) y, si tocaste `rangos`, en `AnalisisPoemaTest`.
5. Documenta la regla en [motor-de-analisis.md](motor-de-analisis.md).

## Cómo cambiar el modelo de datos

`VersoDatabase` está en la versión 1 sin esquema exportado. Para añadir un campo a `Nota`
(por ejemplo, etiquetas):

1. Añade el campo con valor por defecto.
2. Sube `version` a 2 y registra una migración:
   ```kotlin
   val MIGRACION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE notas ADD COLUMN etiquetas TEXT NOT NULL DEFAULT ''")
       }
   }
   Room.databaseBuilder(…).addMigrations(MIGRACION_1_2).build()
   ```
3. Plantéate activar `exportSchema = true` y guardar los esquemas en el repositorio para
   poder probar las migraciones.

## Resolución de problemas

| Síntoma | Causa y solución |
|---|---|
| `java: command not found` / Gradle no encuentra Java | Define `JAVA_HOME` apuntando al JDK 17 |
| `SDK location not found` | Falta `local.properties` con `sdk.dir` |
| `gradlew` no se ejecuta en Linux/macOS | `chmod +x gradlew`; comprueba que tiene finales de línea LF |
| `Android Emulator hypervisor driver is not installed` / el emulador no arranca | Activa la Plataforma del hipervisor de Windows y reinicia |
| Error de API experimental (`This foundation API is experimental…`) | Añade `@OptIn(ExperimentalFoundationApi::class)` o `ExperimentalLayoutApi` en la función |
| El texto escrito con `adb input` no aparece | Tutorial de lápiz de Gboard abierto; ver [Consejos](#probar-a-mano-con-adb) |
| La app se cierra al abrir tras cambiar `Nota` | Falta la migración de Room (ver arriba) |
