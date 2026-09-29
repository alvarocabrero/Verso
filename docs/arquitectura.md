# Arquitectura

Verso es una app de una sola actividad con Jetpack Compose. Está organizada en tres
capas con dependencias en un solo sentido:

```
 ui (Compose + ViewModel)  ──►  data (Room, preferencias)
          │
          └────────────────►  analisis (Kotlin puro)
```

- **`analisis`** no conoce Android ni el resto de la app. Recibe texto y devuelve datos.
  Así se puede probar con JUnit en la JVM, sin emulador.
- **`data`** guarda las notas (Room) y los ajustes (SharedPreferences).
- **`ui`** une las dos cosas: los ViewModel leen y escriben en `data` y llaman a `analisis`.

## Índice de archivos

| Archivo | Papel |
|---|---|
| `VersoApp.kt` | `Application`. Crea la base de datos, el repositorio, las preferencias y `scopeApp` |
| `MainActivity.kt` | Única actividad. Aplica el tema y define la navegación |
| `data/Nota.kt` | Entidad Room `Nota` y enum `TipoNota` |
| `data/NotaDao.kt` | Consultas: todas, búsqueda, obtener, insertar, actualizar, borrar |
| `data/VersoDatabase.kt` | Base de datos Room `verso.db` (versión 1) |
| `data/NotasRepositorio.kt` | Fachada sobre el DAO: `guardar()` inserta o actualiza |
| `data/Preferencias.kt` | Ajustes como estado de Compose: seseo y mostrar análisis |
| `ui/notas/NotasViewModel.kt` | Búsqueda con debounce y lista de notas como `StateFlow` |
| `ui/notas/NotasScreen.kt` | Barra de búsqueda, cuadrícula escalonada, tarjetas, estado vacío |
| `ui/editor/EditorViewModel.kt` | Estado de la nota, autoguardado y análisis en segundo plano |
| `ui/editor/EditorScreen.kt` | Barra superior, tipo, título, versos y panel |
| `ui/editor/AnalisisEditor.kt` | Campo de versos con margen y resaltado, y panel de análisis |
| `ui/theme/Tema.kt` | Colores claro/oscuro y `EstiloVerso` (tipografía de los versos) |
| `ui/theme/ColoresNota.kt` | `PaletaNotas` (7 colores) y `fondoNota(indice)` |
| `analisis/*.kt` | Motor de análisis, ver [motor-de-analisis.md](motor-de-analisis.md) |

## Inyección de dependencias

No hay Hilt ni Koin. `VersoApp` hace de contenedor:

```kotlin
class VersoApp : Application() {
    val scopeApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val baseDeDatos by lazy { VersoDatabase.crear(this) }
    val repositorio by lazy { NotasRepositorio(baseDeDatos.notaDao()) }
    val preferencias by lazy { Preferencias(this) }
}
```

Cada ViewModel tiene un `Factory` (`viewModelFactory { initializer { … } }`) que saca lo
que necesita de `this[APPLICATION_KEY] as VersoApp`. Para añadir una dependencia nueva:
créala como `lazy` en `VersoApp` y pásala en el `initializer` del ViewModel que la use.

## Navegación

`MainActivity` define un `NavHost` con dos rutas:

| Ruta | Pantalla | Notas |
|---|---|---|
| `notas` | `NotasScreen` | Inicio |
| `editor/{id}` | `EditorScreen` | `id` es `Long`; `0` significa nota nueva |

El editor lee `id` desde su `SavedStateHandle`. Al volver se usa `navigateUp()`.
La actividad usa `enableEdgeToEdge()` y `windowSoftInputMode="adjustResize"`; los
márgenes del teclado se gestionan en Compose con `imePadding()`.

## Modelo de datos

```kotlin
@Entity(tableName = "notas")
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String = "",
    val contenido: String = "",
    val tipo: TipoNota = TipoNota.POEMA,     // POEMA o CANCION
    val color: Int = 0,                      // índice en PaletaNotas (0 = sin color)
    val fijada: Boolean = false,
    val creada: Long,                        // epoch ms
    val modificada: Long                     // epoch ms
)
```

- Orden de la lista: `fijada DESC, modificada DESC`.
- Búsqueda: `LIKE '%texto%'` sobre título y contenido. SQLite no distingue mayúsculas
  en ASCII, pero sí en letras con tilde.
- `exportSchema = false` y versión 1. **Cualquier cambio en `Nota` necesita subir la
  versión y escribir una `Migration`**; si no, la app fallará al abrirse en instalaciones
  existentes.

## Pantalla de notas

`NotasViewModel` expone `busqueda: StateFlow<String>` y
`notas: StateFlow<List<Nota>?>`:

```kotlin
_busqueda
    .debounce { if (it.isEmpty()) 0L else 200L }   // vaciar la búsqueda es inmediato
    .flatMapLatest { repo.notas(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

`null` significa "cargando" y evita que el estado vacío parpadee al abrir. Las tarjetas
muestran como mucho 8 versos, respetan los saltos de línea y solo tienen borde si la
nota no tiene color.

## Editor y autoguardado

`EditorViewModel` guarda el estado de la nota como estado de Compose (`mutableStateOf`)
y lo persiste sin que el usuario tenga que hacer nada:

1. Cada cambio (`cambiarTitulo`, `cambiarContenido`, `cambiarTipo`, `cambiarColor`,
   `alternarFijada`) marca `hayCambios` y programa un guardado a los **600 ms**,
   cancelando el anterior.
2. `guardar()` inserta la nota si es nueva (y guarda el `id` que devuelve Room) o la
   actualiza. No guarda notas vacías (título y contenido en blanco).
3. Al salir de la pantalla, `onCleared()` hace un último guardado. Como `viewModelScope`
   ya está cancelado en ese momento, usa `scopeApp`, que vive lo mismo que la app.
   Si la nota quedó vacía y ya existía, la borra (comportamiento de Keep).
4. `borrar()` marca la nota como borrada (para que `onCleared` no la resucite) y la
   elimina en `scopeApp`.

Dos detalles evitan notas duplicadas:
- Un `Mutex` serializa todos los guardados y borrados.
- La inserción corre en `withContext(NonCancellable)`: si el guardado programado se
  cancela justo mientras Room inserta, el `id` nuevo no se pierde y el siguiente
  guardado actualiza en vez de insertar otra vez.

## Análisis en segundo plano

El análisis se recalcula en el ViewModel a partir del estado de Compose:

```kotlin
snapshotFlow { Triple(contenido, preferencias.seseo, preferencias.mostrarAnalisis) }
    .filter { it.third }                 // si el análisis está oculto, no se calcula
    .debounce(300)                       // espera a que se deje de escribir
    .mapLatest { (texto, seseo) ->       // cancela el cálculo anterior si llega texto nuevo
        withContext(Dispatchers.Default) { AnalisisPoema.analizar(texto, seseo) }
    }
    .collect { r -> analisis = r; /* conserva el recurso elegido si sigue existiendo */ }
```

El resultado (`AnalisisPoema.Resultado`) incluye el texto con el que se calculó. La
interfaz lo usa para saber si va por detrás de lo que hay escrito (ver
[editor.md](editor.md)).

## Preferencias

`Preferencias` envuelve un `SharedPreferences` llamado `preferencias` y expone cada ajuste
como estado de Compose, así que cambiarlo redibuja la interfaz y dispara el análisis:

| Clave | Propiedad | Por defecto | Efecto |
|---|---|---|---|
| `seseo` | `seseo` | `false` | s = z = c(e,i) en rimas y aliteraciones |
| `mostrar_analisis` | `mostrarAnalisis` | `true` | Margen, panel y cálculo del análisis |

Los ajustes son globales, no de cada nota.

## Hilos

| Trabajo | Dónde corre |
|---|---|
| Consultas Room | Hilo de Room (`suspend` y `Flow` de Room) |
| Análisis del texto | `Dispatchers.Default`, con debounce de 300 ms |
| Guardado al salir | `scopeApp` (`Dispatchers.Default`) |
| Dibujo del margen y resaltado | Hilo principal, a partir del resultado ya calculado |

## Tema y diseño

"Cuaderno de tinta": índigo sobre papel frío en claro (`#2E3A6E` sobre `#F6F7F9`) y tinta
clara sobre papel nocturno en oscuro. Los versos usan una serif (`FontFamily.Serif`, 18 sp
con 30 sp de interlineado); la interfaz, la sans del sistema. Las tarjetas son planas, sin
sombra. La paleta de notas tiene siete entradas (Sin color, Salvia, Lavanda, Cielo, Trigo,
Rosa, Grafito), cada una con su versión oscura; `fondoNota(i)` elige según el tema.
