# Verso: app Android de notas para poesía y letras de canciones

App tipo Google Keep pensada para escribir poemas y canciones en español. Su
rasgo distintivo es el análisis del texto: sílabas métricas por verso, rimas y
recursos literarios. Todo el código, los comentarios y la interfaz están en español.

## Estado actual

- El proyecto compila (`gradlew assembleDebug`, sin avisos) y los 48 tests
  JUnit del motor pasan (`gradlew testDebugUnitTest`). Probado en emulador;
  aún no en un móvil real.
- Entorno de compilación: JDK 17 (Temurin, en `C:\Program Files\Eclipse
  Adoptium`; `java` no está en el PATH, hay que definir `JAVA_HOME`) y Android
  SDK en `%LOCALAPPDATA%\Android\Sdk` (apuntado en `local.properties`).
  Wrapper de Gradle 8.9 generado.
- Las versiones de dependencias (AGP 8.7.3, Kotlin 2.0.21, Compose BOM
  2024.12.01, Room 2.6.1) son de finales de 2024; se pueden actualizar.

## Stack

Kotlin, Jetpack Compose (Material 3), Room con KSP, Navigation Compose,
ViewModel. Sin Hilt: las dependencias se crean a mano en `VersoApp`.
minSdk 26, compileSdk/targetSdk 35, Java 17. Paquete `com.tuapp`.

## Estructura

```
app/src/main/java/com/tuapp/
  VersoApp.kt            Application: base de datos, repositorio y scopeApp
  MainActivity.kt        NavHost: "notas" y "editor/{id}" (id 0 = nota nueva)
  data/                  Nota (entidad Room), NotaDao, VersoDatabase, NotasRepositorio
  ui/theme/              Tema (tinta índigo sobre papel frío), EstiloVerso, PaletaNotas
  ui/notas/              Pantalla principal: cuadrícula escalonada + búsqueda
  ui/editor/             Editor con autoguardado
  analisis/              Motor de análisis, Kotlin puro sin dependencias de Android
app/src/test/java/com/tuapp/analisis/   Tests JUnit del motor
```

## Datos y editor

- `Nota`: id, titulo, contenido, tipo (`TipoNota.POEMA` / `CANCION`), color
  (índice en `PaletaNotas`), fijada, creada, modificada.
- Orden de la lista: fijadas primero, luego por fecha de modificación.
- Autoguardado en `EditorViewModel`: guarda 600 ms tras el último cambio y al
  salir (`onCleared`, usando `scopeApp` porque `viewModelScope` ya está
  cancelado). Una nota que se deja vacía se borra. Un `Mutex` y `NonCancellable`
  evitan inserciones duplicadas.

## Motor de análisis (`com.tuapp.analisis`)

**Silabeador**: `silabear(palabra)`, `silabaTonica()`, `analizar()` → sílabas,
índice de la tónica y tipo acentual (aguda/llana/esdrújula/sobresdrújula);
`palabrasDe(verso)`. Maneja diptongos, triptongos, hiatos, ch/ll/rr, u muda en
qu/gu, ü, y vocálica final y grupos consonánticos inseparables.

**Metrica**: `Verso(texto)` da un **rango** `minimo..maximo`, porque la
sinalefa es opcional (cada sinalefa resta una sílaba). Aplica la ley del acento
final (+1 aguda, −1 esdrújula). `silabasPara(metro)` ajusta el verso rompiendo
primero las sinalefas con vocal tónica y marca las aplicadas con "‿".
`metroDominante(versos)` elige el metro compatible con más versos.
`nombreMetro(n)` → "endecasílabo", etc.

**Rima**: `terminacion(verso)` da claves consonante (fonética: b=v, h muda,
yeísmo, seseo opcional) y asonante (tónica + vocal final; en posición final
átona i≈e, u≈o). `comparar(a, b)` y `esquema(lineas)` → letras ABBA, "-" para
versos sueltos, `minusculas` para arte menor.

**Recursos**: `detectar(texto)` → lista de `Recurso(tipo, lineas, evidencia,
palabras, intensidad)`. Detecta aliteración, anáfora, epífora, anadiplosis,
epanadiplosis, geminación, polisíndeton, asíndeton, paralelismo y estribillo.
La aliteración cuenta solo consonantes en ataque silábico frente a su
frecuencia normal en español (umbral 3,5; `clara` desde 4,5), analiza cada
verso y cada par de versos consecutivos, y fusiona los solapamientos.

**Limitaciones conocidas**: no detecta diéresis/sinéresis ni hemistiquios de
alejandrinos; no hay rimas internas ni "casi rimas"; no detecta recursos
semánticos (metáfora, símil…), que requerirían un modelo de lenguaje.

## Diseño

Cuaderno de tinta: serif (`FontFamily.Serif`) con interlineado amplio para los
versos, sans del sistema para la interfaz. Las tarjetas respetan los saltos de
línea, son planas y sin sombra (borde fino solo si no tienen color). Siete
colores de nota con variante oscura. Textos en español, en tono sencillo.

## Análisis en el editor

- `AnalisisPoema` (en `analisis/`, con tests): `analizar(texto, seseo)` junta
  metro dominante, sílabas por línea (el metro si el verso lo admite; si no,
  el valor más cercano de su rango y `encaja = false`), rima y recursos, con
  la posición de cada línea en el texto. `rangos(resultado, recurso)` da los
  caracteres a resaltar (inicio en anáfora, final en epífora, verso entero en
  paralelismo/asíndeton/estribillo…).
- `EditorViewModel` lo recalcula con `snapshotFlow` + `debounce(300)` +
  `mapLatest` en `Dispatchers.Default`. El resultado guarda el texto
  analizado: el margen se dibuja mientras no cambie el número de líneas y el
  resaltado solo si el texto coincide exactamente.
- `ui/editor/AnalisisEditor.kt`: `EditorVersos` (un `BasicTextField` dentro
  del scroll de la pantalla, sin scroll propio, para que el margen se alinee
  con `TextLayoutResult`; sílabas y letra en la última línea visual de cada
  verso, en rojo si no encaja), resaltado dibujado en `drawBehind` (fondo;
  aliteración clara con subrayado sólido, posible con punteado) y
  `PanelAnalisis` (resumen metro · esquema · nº de recursos, desplegable con
  la lista y el ajuste de seseo). Al elegir un recurso la pantalla se desplaza
  a su primer verso.
- `data/Preferencias` (SharedPreferences como estado de Compose): seseo y
  mostrar/ocultar el análisis (botón en la barra superior).
- Como el campo no tiene scroll propio, `EditorVersos` lleva un
  `TextFieldValue` interno y un `BringIntoViewRequester` para mantener el
  cursor a la vista. El panel se cierra solo al abrirse el teclado.
- Probado en emulador (Pixel 7, API 35, AVD `Verso_API35`): margen alineado
  también en versos partidos, cursor visible, resaltados y modo oscuro.
  En el emulador conviene `settings put secure stylus_handwriting_enabled 0`
  para que Gboard no abra el tutorial de lápiz al escribir con `adb input`.

## Siguiente paso

Etiquetas, papelera con deshacer, exportar/compartir y la opción de detectar
recursos semánticos con un modelo de lenguaje.

## Convenciones

- Nombres de clases, funciones y variables en español, como el código existente.
- El motor de análisis no debe depender de Android; cada cambio en él va con tests.
