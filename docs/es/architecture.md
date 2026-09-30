# Arquitectura

[English](../architecture.md) · **Español**

Verso es una app de una sola actividad con Jetpack Compose. Está organizada en tres
capas con dependencias en un solo sentido:

```
 ui (Compose + ViewModel)  ──►  data (Room, preferencias)
          │
          └────────────────►  analisis (Kotlin puro)
```

- **`analisis`** (motor de análisis) no conoce Android ni el resto de la app. Recibe
  texto y devuelve datos, así que se puede probar con JUnit en la JVM, sin emulador. Está
  escrito en español porque modela la métrica española; ver
  [analysis-engine.md](analysis-engine.md).
- **`data`** guarda las notas (Room) y los ajustes (SharedPreferences).
- **`ui`** une las dos cosas: los ViewModel leen y escriben en `data` y llaman a `analisis`.

## Índice de archivos

| Archivo | Papel |
|---|---|
| `VersoApp.kt` | `Application`. Crea la base de datos, el repositorio, las preferencias y `appScope` |
| `MainActivity.kt` | Única actividad. Aplica el tema y define la navegación |
| `data/Note.kt` | Entidad Room `Note`, enum `NoteType` y sus `Converters` |
| `data/NoteDao.kt` | Consultas: todas, búsqueda, obtener, insertar, actualizar, borrar |
| `data/VersoDatabase.kt` | Base de datos Room `verso.db` (versión 1) |
| `data/NotesRepository.kt` | Fachada sobre el DAO: `save()` inserta o actualiza |
| `data/Preferences.kt` | Ajustes como estado de Compose: seseo y mostrar análisis |
| `ui/notes/NotesViewModel.kt` | Búsqueda con debounce y lista de notas como `StateFlow` |
| `ui/notes/NotesScreen.kt` | Barra de búsqueda, cuadrícula escalonada, tarjetas, estado vacío |
| `ui/editor/EditorViewModel.kt` | Estado de la nota, autoguardado y análisis en segundo plano |
| `ui/editor/EditorScreen.kt` | Barra superior, tipo, título, versos y panel |
| `ui/editor/AnalysisEditor.kt` | Campo de versos con margen y resaltado, y panel de análisis |
| `ui/theme/Theme.kt` | Colores claro/oscuro y `VerseStyle` (tipografía de los versos) |
| `ui/theme/NoteColors.kt` | `NotePalette` (7 colores) y `noteBackground(index)` |
| `analisis/*.kt` | Motor de análisis, ver [analysis-engine.md](analysis-engine.md) |

## Inyección de dependencias

No hay Hilt ni Koin. `VersoApp` hace de contenedor:

```kotlin
class VersoApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { VersoDatabase.create(this) }
    val repository by lazy { NotesRepository(database.noteDao()) }
    val preferences by lazy { Preferences(this) }
}
```

Cada ViewModel tiene un `Factory` (`viewModelFactory { initializer { … } }`) que saca lo
que necesita de `this[APPLICATION_KEY] as VersoApp`. Para añadir una dependencia nueva:
créala como `lazy` en `VersoApp` y pásala en el `initializer` del ViewModel que la use.

## Navegación

`MainActivity` define un `NavHost` con dos rutas:

| Ruta | Pantalla | Notas |
|---|---|---|
| `notes` | `NotesScreen` | Inicio |
| `editor/{id}` | `EditorScreen` | `id` es `Long`; `0` significa nota nueva |

El editor lee `id` desde su `SavedStateHandle`. Al volver se usa `navigateUp()`.
La actividad usa `enableEdgeToEdge()` y `windowSoftInputMode="adjustResize"`; los
márgenes del teclado se gestionan en Compose con `imePadding()`.

## Modelo de datos

```kotlin
@Entity(tableName = "notas")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "titulo") val title: String = "",
    @ColumnInfo(name = "contenido") val content: String = "",
    @ColumnInfo(name = "tipo") val type: NoteType = NoteType.POEM,   // POEM o SONG
    val color: Int = 0,                                              // índice en NotePalette (0 = sin color)
    @ColumnInfo(name = "fijada") val pinned: Boolean = false,
    @ColumnInfo(name = "creada") val created: Long,                  // epoch ms
    @ColumnInfo(name = "modificada") val modified: Long              // epoch ms
)
```

- **La tabla y las columnas conservan los nombres en español de la versión 0.1.0**, y
  `Converters` guarda `NoteType` como `"POEMA"` / `"CANCION"`, igual que la 0.1.0. Así el
  código se ha podido pasar al inglés sin migrar la base de datos: al actualizar desde la
  0.1.0 se conservan todas las notas.
- Orden de la lista: `fijada DESC, modificada DESC` (fijadas primero y luego las más
  recientes).
- Búsqueda: `LIKE '%texto%'` sobre título y contenido. `LIKE` en SQLite no distingue
  mayúsculas solo en letras ASCII, no en las que llevan tilde.
- `exportSchema = false` y versión 1. **Cualquier cambio en `Note` necesita subir la
  versión y escribir una `Migration`**; si no, la app fallará al abrirse en instalaciones
  existentes. Ver [development.md](development.md#cómo-cambiar-el-modelo-de-datos).

## Pantalla de notas

`NotesViewModel` expone `query: StateFlow<String>` y `notes: StateFlow<List<Note>?>`:

```kotlin
_query
    .debounce { if (it.isEmpty()) 0L else 200L }   // vaciar la búsqueda es inmediato
    .flatMapLatest { repo.notes(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

`null` significa "cargando" y evita que el estado vacío parpadee al abrir. Las tarjetas
muestran como mucho 8 versos, respetan los saltos de línea y solo tienen borde si la
nota no tiene color.

## Editor y autoguardado

`EditorViewModel` guarda el estado de la nota como estado de Compose (`mutableStateOf`)
y lo persiste sin que el usuario tenga que hacer nada:

1. Cada cambio (`updateTitle`, `updateContent`, `updateType`, `updateColor`,
   `togglePinned`) marca `hasChanges` y programa un guardado a los **600 ms**,
   cancelando el anterior.
2. `save()` inserta la nota si es nueva (y guarda el `id` que devuelve Room) o la
   actualiza. No guarda notas vacías (título y contenido en blanco).
3. Al salir de la pantalla, `onCleared()` hace un último guardado. Como `viewModelScope`
   ya está cancelado en ese momento, usa `appScope`, que vive lo mismo que la app.
   Si la nota quedó vacía y ya existía, la borra (comportamiento de Keep).
4. `delete()` marca la nota como borrada (para que `onCleared` no la resucite) y la
   elimina en `appScope`.

Dos detalles evitan notas duplicadas:
- Un `Mutex` serializa todos los guardados y borrados.
- La inserción corre en `withContext(NonCancellable)`: si el guardado programado se
  cancela justo mientras Room inserta, el `id` nuevo no se pierde y el siguiente
  guardado actualiza en vez de insertar otra vez.

## Análisis en segundo plano

El análisis se recalcula en el ViewModel a partir del estado de Compose:

```kotlin
snapshotFlow { Triple(content, preferences.seseo, preferences.showAnalysis) }
    .filter { it.third }                 // si el análisis está oculto, no se calcula
    .debounce(300)                       // espera a que se deje de escribir
    .mapLatest { (text, seseo) ->        // cancela el cálculo anterior si llega texto nuevo
        withContext(Dispatchers.Default) { AnalisisPoema.analizar(text, seseo) }
    }
    .collect { r -> analysis = r; /* conserva el recurso elegido si sigue existiendo */ }
```

El resultado (`AnalisisPoema.Resultado`) incluye el texto con el que se calculó. La
interfaz lo usa para saber si va por detrás de lo que hay escrito (ver
[editor.md](editor.md)).

## Preferencias

`Preferences` envuelve un `SharedPreferences` llamado `preferencias` y expone cada ajuste
como estado de Compose, así que cambiarlo redibuja la interfaz y dispara el análisis:

| Clave | Propiedad | Por defecto | Efecto |
|---|---|---|---|
| `seseo` | `seseo` | `false` | s = z = c(e,i) en rimas y aliteraciones |
| `mostrar_analisis` | `showAnalysis` | `true` | Margen, panel y cálculo del análisis |

Las claves conservan sus nombres de la 0.1.0. Los ajustes son globales, no de cada nota.

## Hilos

| Trabajo | Dónde corre |
|---|---|
| Consultas Room | Hilo de Room (`suspend` y `Flow` de Room) |
| Análisis del texto | `Dispatchers.Default`, con debounce de 300 ms |
| Guardado al salir | `appScope` (`Dispatchers.Default`) |
| Dibujo del margen y resaltado | Hilo principal, a partir del resultado ya calculado |

## Tema y diseño

"Cuaderno de tinta": índigo sobre papel frío en claro (`#2E3A6E` sobre `#F6F7F9`) y tinta
clara sobre papel nocturno en oscuro. Los versos usan una serif (`FontFamily.Serif`, 18 sp
con 30 sp de interlineado); la interfaz, la sans del sistema. Las tarjetas son planas, sin
sombra. La paleta de notas tiene siete entradas (Sin color, Salvia, Lavanda, Cielo, Trigo,
Rosa, Grafito), cada una con su versión oscura; `noteBackground(i)` elige según el tema.

## Idioma

El código de la app está en inglés, salvo el motor de análisis (`analisis/`), cuyos
nombres vienen de la terminología de la métrica española. Todo lo que ve el usuario
(textos de la interfaz y resultados del análisis) está en español.
