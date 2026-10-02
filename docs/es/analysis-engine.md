# Motor de análisis

[English](../analysis-engine.md) · **Español**

Paquete `com.tuapp.analisis`. Kotlin puro, sin dependencias de Android: se prueba con
JUnit en la JVM (`./gradlew testDebugUnitTest`).

| Objeto | Responsabilidad |
|---|---|
| [`Silabeador`](#silabeador) | Divide palabras en sílabas y encuentra la tónica |
| [`Metrica`](#métrica) | Cuenta sílabas métricas de un verso y elige el metro del poema |
| [`Rima`](#rima) | Terminaciones, comparación de rimas y esquema (ABBA…) |
| [`Recursos`](#recursos-literarios) | Detecta recursos literarios de sonido, repetición y estructura |
| [`AnalisisPoema`](#analisispoema-el-análisis-completo) | Junta todo para el editor y calcula qué resaltar |

Los ejemplos de este documento están sacados de los tests, así que son el comportamiento
real del código.

---

## Silabeador

Silabeo **ortográfico** (gramatical) del español: las sílabas de la palabra aislada, sin
tener en cuenta la sinalefa entre palabras (eso lo hace `Metrica`).

### API

```kotlin
Silabeador.silabear("murciélago")          // [mur, cié, la, go]
Silabeador.silabaTonica("murciélago")      // 1
Silabeador.analizar("murciélago")          // Palabra(texto, silabas, tonica = 1, tipo = ESDRUJULA)
Silabeador.palabrasDe("¡Ay, qué dolor!")   // [Ay, qué, dolor]
```

- `TipoAcentual`: `AGUDA`, `LLANA`, `ESDRUJULA`, `SOBRESDRUJULA`, según cuántas sílabas
  hay tras la tónica (0, 1, 2, 3 o más).
- `palabrasDe` extrae secuencias de letras (`\p{L}+`): ignora puntuación **y números**.
- Las sílabas conservan mayúsculas y tildes del original.

### Algoritmo

**1. Tokenización.** La palabra se convierte en unidades que son vocal o consonante:

| Caso | Tratamiento | Ejemplo |
|---|---|---|
| `ch`, `ll`, `rr` | Una sola consonante | pe-**rr**o, **ch**o-co-la-te |
| `qu`, `gu` ante e/i | Una consonante; la u es muda | **qu**e-so, **gu**i-ta-rra |
| `gü` | La ü es vocal | pin-**güi**-no |
| `y` sin vocal detrás | Vocal (como i) | ho**y**, re**y**, **y** |
| `y` con vocal detrás | Consonante | re-**y**es |

**2. Núcleos vocálicos.** Las vocales seguidas se agrupan en un mismo núcleo (diptongo o
triptongo) salvo que formen **hiato**:

| Combinación | Resultado | Ejemplos |
|---|---|---|
| Débil + débil distintas (i, u) | Diptongo | **ciu**-dad, **cui**-da-do, cons-**truí** |
| Débil + débil iguales | Hiato | chi-i-ta |
| Débil tildada (í, ú) junto a fuerte | Hiato | rí-o, ma-íz, Ra-úl, o-í-do |
| Fuerte + fuerte (a, e, o) | Hiato | po-e-ta, le-er, a-é-re-o |
| Fuerte + débil átona | Diptongo | **ai**-re, **Eu**-ro-pa, **hue**-vo |
| Débil + fuerte + débil | Triptongo | **buey**, U-ru-**guay** |

La h entre vocales no rompe nada: bú-ho, a-hu-mar.

**3. Reparto de consonantes** entre dos núcleos:

| Consonantes entre vocales | Corte | Ejemplos |
|---|---|---|
| 0 o 1 | Todas a la sílaba siguiente | ca-sa, e-xa-men |
| 2 | Entre las dos, salvo grupo inseparable | ac-ción, in-fla-mar; a-**br**a-zo |
| 3 o más | Las dos últimas a la siguiente si son inseparables; si no, solo la última | ins-**tr**u-men-to, obs-tá-cu-lo, trans-**pl**an-te |

Grupos inseparables: `pr br cr gr fr kr tr dr` y `pl bl cl gl fl kl`. `tl` no lo es:
at-le-ta.

**4. Sílaba tónica.**
1. Si una sílaba lleva tilde, es la tónica.
2. Si la palabra tiene una sola sílaba, esa.
3. Si termina en vocal, *n* o *s*, la penúltima (llana); si no, la última (aguda).

### Limitaciones

- Silabeo ortográfico: no contempla pronunciaciones dialectales (p. ej. *ti-a* frente a
  *tia*), que en métrica se tratan como licencias.
- Palabras extranjeras o siglas se silabean con las reglas del español.
- Monosílabos sin tilde se consideran tónicos al calcular el tipo acentual; para la
  sinalefa, `Metrica` usa su propia lista de átonos.

---

## Métrica

Cuenta sílabas **métricas**, que no coinciden con las gramaticales por dos razones:

- **Sinalefa**: la vocal final de una palabra se une a la inicial de la siguiente
  (*no‿hay*, *se‿ha-ce*).
- **Ley del acento final**: si el verso termina en aguda se suma una sílaba; si termina
  en esdrújula (o sobresdrújula) se resta una.

### Un rango, no un número

La sinalefa es opcional en la práctica poética (el poeta puede romperla: *dialefa*), así
que cada verso no tiene un recuento sino un **rango**:

- `maximo` = sílabas gramaticales + ajuste final (ninguna sinalefa).
- `minimo` = `maximo` − número de sinalefas posibles (todas aplicadas).

Como cada sinalefa resta exactamente una sílaba, cualquier valor del rango es alcanzable.

```kotlin
val v = Metrica.Verso("Escrito está en mi alma vuestro gesto")
v.minimo                       // 10
v.maximo                       // 13
v.admite(11)                   // true
v.silabasPara(11)              // Es-cri-to‿es-tá-en-mi‿al-ma-vues-tro-ges-to
v.silabasPara(8)               // null: no puede medir 8
```

### Cuándo hay sinalefa posible

Entre dos palabras seguidas cuando:
- la primera **termina en vocal** (la *y* final de *rey*, *hoy*, *soy* no cuenta: es
  semiconsonante), y
- la segunda **empieza por vocal**, también tras *h* muda (*no‿hay*, *se‿ha-ce*), salvo
  *hie-* y *hue-* (*la hierba*, *el hueso*: la h suena como consonante). La conjunción
  *y* cuenta como vocal.

### Qué sinalefas se rompen primero

Cuando hay que alargar un verso para que encaje en un metro, `silabasPara(metro)` rompe
primero las sinalefas más "resistentes":

| Resistencia | Situación | Ejemplo |
|---|---|---|
| 2 | La vocal final es **tónica** | *es-tá-en* (se rompe) |
| 1 | La vocal inicial siguiente es tónica | *mi-al-ma* |
| 0 | Ninguna es tónica | *mi‿al-ma* (se mantiene) |

A igual resistencia se rompen antes las que están más al final del verso. Para decidir si
un monosílabo es tónico se usa una lista de átonos: artículos (*el, la, los, las, lo, un,
una…*), contracciones (*al, del*), preposiciones (*a, de, en, con, por, sin, so*),
conjunciones (*y, e, o, u, ni, que*), pronombres átonos (*me, te, se, le, les, nos, os*)
y posesivos antepuestos (*mi, tu, su, mis, tus, sus*).

Las sinalefas aplicadas se marcan con `‿` en la salida.

### Metro dominante

```kotlin
val versos = Metrica.analizarTexto(texto)   // una Verso por línea (las vacías no cuentan)
val metro = Metrica.metroDominante(versos)  // 11
Metrica.nombreMetro(metro!!)                // "endecasílabo"
```

`metroDominante` prueba todos los valores entre el mínimo y el máximo de los versos y se
queda con el que **admiten más versos**. Los empates se resuelven por frecuencia en la
tradición: 11, 8, 7, 14, 6, 5, 9, 12, 10, 13, 4, 3, 2.

Nombres: bisílabo (2), trisílabo, tetrasílabo, pentasílabo, hexasílabo, heptasílabo,
octosílabo, eneasílabo, decasílabo, endecasílabo, dodecasílabo, tridecasílabo y
alejandrino (14). Fuera de ese rango: "N sílabas".

### Ejemplos (de los tests)

| Verso | Rango | Medida |
|---|---|---|
| Verde que te quiero verde | 8–8 | Ver-de-que-te-quie-ro-ver-de |
| Caminante, no hay camino | 8–9 | Ca-mi-nan-te-no‿hay-ca-mi-no |
| la hierba verde | 5–5 | la-hier-ba-ver-de |
| se hace camino al andar | 8–10 | se‿ha-ce-ca-mi-no‿al-an-dar (aguda +1) |
| dame la mano, murciélago | 8–8 | da-me-la-ma-no-mur-cié-la-go (esdrújula −1) |
| En tanto que de rosa y azucena | 10–12 | En-tan-to-que-de-ro-sa‿y-a-zu-ce-na (11) |
| Puedo escribir los versos más tristes esta noche | 14–15 | Pue-do‿es-cri-bir-… (14) |
| Yo soy un hombre sincero | 8–8 | Yo-soy-un-hom-bre-sin-ce-ro |

### Limitaciones

- No detecta **diéresis** (*sü-a-ve*) ni **sinéresis** (*poe-ta* en una sílaba).
- No trata los **hemistiquios** del alejandrino (7 + 7, con ley del acento en cada mitad).
- Un único metro para todo el texto: en poemas polimétricos, los versos de otra medida
  quedan como "no encajan".

---

## Rima

La rima empieza en la **última vocal tónica** del verso.

- **Consonante**: coinciden todos los sonidos desde ahí (*cielo / suelo*).
- **Asonante**: solo coinciden las vocales (*cielo / lejos*).

### API

```kotlin
Rima.terminacion("agua del cántaro")
// Terminacion(palabra = "cántaro", texto = "ántaro", consonante = "antaro", asonante = "ao")

Rima.comparar("cielo", "suelo")                // CONSONANTE
Rima.comparar("casa", "caza")                  // ASONANTE
Rima.comparar("casa", "caza", seseo = true)    // CONSONANTE
Rima.comparar("casa", "perro")                 // null

val e = Rima.esquema(lineas, seseo = false, minusculas = false)  // List<RimaVerso?>
Rima.esquemaComoTexto(e)                                         // "ABBA ABBA"
```

### Terminación

1. Se toma la última palabra del verso y su sílaba tónica.
2. Dentro de esa sílaba, la vocal tónica es: la que lleva tilde; si no, la primera
   fuerte (a, e, o); si solo hay débiles, la última (*cui-da* → i, *ciu-dad* → u),
   saltando la u muda de *qu/gu*.
3. La terminación escrita va desde esa vocal hasta el final: *cántaro* → *ántaro*.

### Clave consonante (fonética)

La terminación se normaliza para comparar **sonidos**, no letras:

| Regla | Ejemplo |
|---|---|
| h muda | *hoy* → *oi* |
| g ante e/i suena j | *gente* → *jente* |
| qu ante e/i → k; gu ante e/i → g | *queso* → *keso*, *guerra* → *gerra* |
| ü → u | |
| c ante e/i y z → θ (con seseo → s) | *caza* → *kaθa* / *kasa* |
| c en el resto → k | |
| v → b | *vive / libre*: asonantes; *b* = *v* |
| ll → y (yeísmo) | |
| y final o ante consonante → i | *hoy* → *oi* |
| Tildes fuera | *ántaro* → *antaro* |

Así riman en consonante *guerra / tierra*, *gente / fuente*, *hoy / voy*, *fuego / juego*.

### Clave asonante

- Vocal tónica + vocal de la **última sílaba** (en diptongo, la fuerte).
- En la última sílaba átona, **i ≈ e** y **u ≈ o**: *fácil / calle*, *Venus / tenso*.
- Si la palabra es aguda, la clave es solo la vocal tónica (*amor* → *o*).
- Ejemplos: *cántaro / pájaro* (a-o), *sangre / hambre* (a-e). *mar / montaña* no riman
  (aguda frente a llana).

### Esquema

`esquema(lineas)` devuelve un elemento por línea: `null` para líneas vacías y
`RimaVerso(letra, tipo, terminacion)` para las demás.

1. Se agrupan los versos con la **misma clave consonante** (grupos de 2 o más).
2. Los versos sueltos se unen por **asonancia** a un grupo existente con la misma clave
   asonante; si no hay ninguno, forman grupo nuevo si son 2 o más.
3. Los grupos se ordenan por su primera aparición y reciben letras A, B, C…
   (a, b, c… con `minusculas = true`, usado para arte menor).
4. Los versos que no riman con ninguno llevan `-`.

Ejemplo: un romance (*-a-a-a*) o un soneto (*ABBA ABBA*). En `esquemaComoTexto` las líneas
vacías se muestran como un espacio: *"AA AA"*.

---

## Recursos literarios

```kotlin
val recursos: List<Recursos.Recurso> = Recursos.detectar(texto, seseo = false)
```

```kotlin
data class Recurso(
    val tipo: Tipo,                  // ALITERACION, ANAFORA, …
    val lineas: List<Int>,           // índices de línea (0 = primera línea del texto)
    val evidencia: String,           // texto breve para mostrar: «temprano», sonido «s» ×6…
    val palabras: List<String>,      // palabras implicadas (puede estar vacía)
    val intensidad: Double? = null   // solo en aliteraciones
) { val clara: Boolean }             // intensidad == null || intensidad >= 4,5
```

El resultado se ordena por primera línea y, dentro de ella, por tipo.

### Estrofas

La mayoría de detectores trabajan **dentro de cada estrofa** (bloques de líneas
consecutivas no vacías). Una línea en blanco corta rachas: la anáfora no salta de una
estrofa a otra. El estribillo es la excepción: se busca en todo el texto.

### Palabras átonas

Para no llenar la lista de falsos positivos se ignoran repeticiones de palabras vacías:
artículos (*el, la, lo, los, las, un, una, unos, unas, le, les*), conjunciones (*y, e, ni,
o, u*) y otras átonas (*al, del, de, a, en, con, por, sin, que, se, me, te, nos, os, mi,
tu, su, mis, tus, sus*).

### Detectores de repetición

| Recurso | Condición | Palabras resaltadas |
|---|---|---|
| **Anáfora** | 2+ versos seguidos que empiezan igual. Si lo común es una sola palabra átona, hacen falta 3+ versos | El prefijo común |
| **Polisíndeton** (inicio) | Como la anáfora, pero la palabra inicial es una conjunción | La conjunción |
| **Polisíndeton** (verso) | 2+ conjunciones (*y, e, ni, o, u*) en un mismo verso | Las conjunciones |
| **Epífora** | 2+ versos seguidos que terminan igual (no vale una sola átona) | El sufijo común |
| **Anadiplosis** | Un verso termina con la palabra con que empieza el siguiente (no átona) | Última del primero y primera del segundo |
| **Epanadiplosis** | Verso de 3+ palabras que empieza y termina con la misma palabra (no átona) | Primera y última |
| **Geminación** | La misma palabra (2+ letras) dos veces seguidas | Las repeticiones contiguas |
| **Estribillo** | El mismo verso (2+ palabras) aparece 2+ veces en cualquier parte | Los versos enteros |

Versos completamente iguales no cuentan como anáfora ni epífora (eso es estribillo).

### Rima interna

Una palabra del **interior** de un verso (no la última) que rima con otra. Todas las
palabras de una estrofa que comparten una rima forman **un solo** recurso, con las palabras
como evidencia (`«soneto» · «aprieto»`) y resaltadas; `Recurso.rima` indica de qué tipo es.

**Rimas consonantes** (`CONSONANTE`), con la misma clave consonante que la rima final (así
que el seseo cuenta: *casa / caza*):
- una palabra interior con otra palabra interior del mismo verso (*la **luna** sobre la
  **laguna** se dormía*);
- una palabra interior con la última palabra de **cualquier verso de la estrofa** (*tu
  **corazón** es mi **canción***; *Un **soneto** me manda hacer Violante, / que en mi vida
  me he visto en tanto **aprieto***);
- una palabra interior con una palabra interior del verso siguiente (*la noche **oscura** se
  cierra / con **amargura** en el alma*).

Dos palabras interiores solo cuentan si las dos tienen dos sílabas o más: las palabras
cortas (*es / tres*) riman por casualidad a todas horas. Una palabra corta interior sí puede
rimar con un final de verso.

**Rimas asonantes** (`ASONANTE`, se muestran como *Rima interna · asonante*). La asonancia
aparece por azar mucho más a menudo, así que las reglas son más estrictas:
- solo una palabra interior con la última palabra de **su propio verso, del anterior o del
  siguiente** (*la **casa blanca** de la **plaza***);
- solo palabras llanas o esdrújulas (clave de dos vocales, *a-a*); las agudas tienen una
  clave de una sola vocal (*volverán / colgar*) que coincide con demasiadas palabras.

Siempre se ignoran: las palabras átonas, las terminaciones consonantes de una sola letra y
la misma palabra repetida, también en plural o en una forma más larga (*verde / verdes*).

Aun así, algunas rimas internas asonantes serán casuales; por eso llevan su etiqueta y se
colorean más suave.

### Detectores de estructura

| Recurso | Condición |
|---|---|
| **Asíndeton** | El verso, partido por comas o punto y coma, da 3+ segmentos de 3 palabras o menos, el último no empieza por conjunción y no son todos iguales |
| **Paralelismo** | Versos seguidos con el mismo número de palabras (4+), no idénticos, que coinciden en al menos 3 posiciones y en al menos la mitad. Para comparar, los artículos determinados, indeterminados y posesivos cuentan como una misma clase (*el mar* ≈ *la luna*) |

### Aliteración

Es el detector más elaborado, porque en español cualquier verso repite consonantes:
lo que importa es que un sonido aparezca **mucho más de lo normal**.

**1. Sonidos, no letras.** Cada palabra pasa por la misma normalización fonética que la
rima (*c/qu/k* = k, *b/v* = b, *ll/y* = y, *g/j* ante e/i = j…). La *r* fuerte (inicial,
tras n/l/s, o *rr*) se distingue de la suave.

**2. Solo consonantes en ataque**, es decir, al inicio de sílaba (seguidas de vocal o
formando grupo inseparable como *pr*, *bl*). Las consonantes de final de sílaba (la *s*
de *más*) apenas se perciben en la aliteración y distorsionan el recuento.

**3. Frecuencia esperada.** Cada sonido se compara con su frecuencia normal en ataque en
prosa española:

| Sonido | Frecuencia | Sonido | Frecuencia |
|---|---|---|---|
| t | 10,4 % | s | 6,2 % (9,1 % con seseo) |
| k (c/qu) | 10,0 % | θ (z/c) | 2,9 % |
| d | 9,5 % | rr | 2,4 % |
| r | 9,1 % | y/ll | 2,2 % |
| m | 8,6 % | g | 2,2 % |
| p | 8,4 % | f | 2,0 % |
| b/v | 7,7 % | ñ | 1,3 % |
| n | 7,5 % | j/g | 1,1 % |
| l | 7,5 % | ch | 0,7 % |

Otros sonidos: 1,5 %. **Intensidad** = (apariciones / total de ataques) ÷ frecuencia
esperada.

**4. Condiciones** para aceptar un sonido en una ventana de versos:

- Intensidad ≥ **3,5** (≥ **4,5** es aliteración *clara*; por debajo, *posible*).
- Al menos 3 apariciones en un verso, o 4 en un par de versos.
- En al menos 3 **raíces distintas** (primeras 4 letras): *caminante / camino* cuenta una
  vez.
- En ventanas de dos versos, el sonido debe aparecer **en los dos**.
- Los artículos cuentan para el total pero no como apariciones.

**5. Ventanas y fusión.** Se analiza cada verso por separado y cada par de versos
consecutivos de la estrofa. Los pares solapados con el mismo sonido se fusionan en una
sola aliteración (versos 1–2 y 2–3 → versos 1–3), y los versos sueltos ya cubiertos por una
aliteración más amplia del mismo sonido se descartan.

Ejemplos de los tests:

| Texto | Resultado |
|---|---|
| bajo el ala aleve del leve abanico | b/v ×4 (4,7×, **clara**) y l ×3 (3,6×, **posible**) |
| en el silencio sólo se escuchaba / un susurro de abejas que sonaba | s ×6 en 2 versos |
| mis manos buscan tu mirada muda / mientras la madrugada muere mansa | una sola aliteración de m en los versos 1–2 |
| Volverán las oscuras golondrinas / en tu balcón sus nidos a colgar | nada |
| Puedo escribir los versos más tristes esta noche | nada |

La evidencia muestra la grafía del sonido: `sonido «b/v» ×4`, `sonido «z/c» ×3`,
`sonido «s» ×6 en 2 versos`.

### Qué no detecta

Los recursos **semánticos** (metáfora, símil, personificación, hipérbole, antítesis…) no
se pueden reconocer con reglas de forma fiable; haría falta un modelo de lenguaje. Tampoco
hay detección de "casi rimas", hipérbaton ni onomatopeyas.

---

## AnalisisPoema: el análisis completo

Es la entrada que usa el editor. Hace un solo recorrido y devuelve todo lo necesario
para pintar la pantalla.

```kotlin
val r = AnalisisPoema.analizar(texto, seseo = false)

r.texto        // el texto analizado (para saber si la interfaz va por detrás)
r.metro        // metro dominante, o null si no hay versos
r.lineas       // una Linea por línea del texto, incluidas las vacías
r.recursos     // lo mismo que Recursos.detectar
r.esquema      // "ABBA ABBA": letras por verso, un espacio entre estrofas
```

```kotlin
data class Linea(
    val inicio: Int,           // posición del primer carácter en el texto
    val fin: Int,              // posición del salto de línea (o del final del texto)
    val silabas: Int?,         // null en líneas sin palabras
    val encaja: Boolean,       // ¿el verso admite el metro dominante?
    val rima: Rima.RimaVerso?  // null en líneas sin palabras
)
```

- **Sílabas mostradas**: si el verso admite el metro, el metro; si no, el valor de su
  rango más cercano al metro (y `encaja = false`). Sin metro, el mínimo.
- **Minúsculas** en el esquema si el metro es de 8 sílabas o menos (arte menor).
- Las líneas se separan solo por `\n`.

### Qué resaltar de cada recurso

`AnalisisPoema.rangos(r, recurso)` devuelve rangos de caracteres (`IntRange`) sobre
`r.texto`:

| Recurso | Qué se resalta en cada verso |
|---|---|
| Anáfora | Las primeras *k* palabras (*k* = palabras del prefijo común), no otras apariciones de la palabra en el verso |
| Epífora | Las últimas *k* palabras |
| Anadiplosis | Última palabra del primer verso y primera del segundo |
| Epanadiplosis | Primera y última palabra |
| Geminación | Solo las apariciones contiguas de la palabra repetida |
| Aliteración, polisíndeton | Todas las palabras del verso que están en `palabras` |
| Paralelismo, asíndeton, estribillo | El verso entero, de la primera a la última letra (sin la puntuación de los extremos) |

La comparación no distingue mayúsculas.

### Tramos para colorear las rimas

`AnalisisPoema.tramosDeRima(r)` devuelve los trozos de texto que hay que colorear cuando se
activa el coloreado de rimas, como `TramoRima(rango, grupo, tipo)`:

- la **terminación que rima** (desde la vocal tónica hasta el final de la palabra) de cada
  verso que rima con otro, con el grupo de su letra (A = 0, B = 1…) y su tipo
  (`CONSONANTE` o `ASONANTE`). Los versos sueltos (`-`) no se colorean;
- las palabras de cada **rima interna**, con su propio tipo (las asonantes, más suaves): con
  el grupo del verso cuya rima final comparten (*soneto* toma el color del grupo de
  *aprieto*) o, si no hay ninguno, con un grupo nuevo tras los de las letras.

Ejemplo (cuarteto de Lope): *son**eto*** (interna, B), *Viol**ante*** (A),
*apri**eto*** (B), *v**ersos*** (interna asonante, B), *son**eto*** (B), *del**ante*** (A).

---

## Tests

`app/src/test/java/com/tuapp/analisis/`:

| Clase | Tests | Qué cubre |
|---|---|---|
| `SilabeadorTest` | 9 | Silabeo básico, dígrafos, u muda y diéresis, diptongos y triptongos, hiatos, y griega, grupos consonánticos, tipo acentual, extracción de palabras |
| `MetricaTest` | 8 | Sinalefa (con h), ley del acento final, dialefa en vocal tónica, alejandrino, y final, metro no admitido, metro dominante de un soneto |
| `RimaTest` | 8 | Terminaciones, consonantes, asonantes, no rimas, seseo, esquemas de soneto y romance, fusión asonante/consonante |
| `RecursosTest` | 24 | Cada tipo de recurso, aliteración en uno y dos versos, fusión, intensidad, rimas internas (consonantes y asonantes) y ausencia de falsos positivos en versos conocidos |
| `AnalisisPoemaTest` | 13 | Metro y esquema de un cuarteto, arte menor, verso que no encaja, posiciones, texto vacío, rangos de resaltado, tramos para colorear rimas |

Para añadir un caso: usa versos reales (de autores conocidos cuando sea posible) y anota
en un comentario por qué el resultado es el esperado.
