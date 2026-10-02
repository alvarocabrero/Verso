# Registro de cambios

[English](CHANGELOG.md) · **Español**

## 0.2.0 (2026-10-02)

### Novedades
- **Sección de audios.** La pantalla principal tiene una barra abajo para cambiar entre
  *Notas* y *Audios* (se recuerda la última usada).
  - Grabar en la app con pausa, reanudar y medidor de nivel. Si la app pasa a segundo plano,
    la grabación se pausa, no se pierde.
  - Adjuntar audios del móvil (mp3, m4a, wav, ogg, flac…); se copian dentro de la app, así
    que siguen ahí aunque se borre el original.
  - Escucharlos ahí mismo con barra de avance, cambiarles el nombre y borrarlos (con
    confirmación).
  - Vincular audios y notas, de muchos a muchos, desde los dos lados: la lista de audios
    muestra las notas de cada uno y el editor tiene un chip *Audios* para escucharlos,
    desvincularlos y vincular otros. Las tarjetas de nota muestran 🎧 con el número. Una nota
    con audios pero sin texto no se borra.
  - Los audios se quedan en el dispositivo.
- **Botón ⓘ en cada recurso literario**: qué es, un ejemplo clásico, dónde se ha encontrado
  y, en las aliteraciones, por qué cuenta como clara o posible.
- **Rima interna** como nuevo recurso (solo rimas consonantes).
- **Colorear rimas** (botón del pincel): un color de rotulador por grupo de rima en la
  terminación que rima, de una paleta apta para daltonismo, más suave en las asonantes, con
  leyenda en el panel de análisis. Se recuerda entre sesiones.

### Cambios
- Código y documentación traducidos al inglés. El motor de análisis conserva sus nombres en
  español y la app sigue en español.
- Base de datos en versión 2 (tablas nuevas para los audios). Al actualizar desde la 0.1.0
  se conservan todas las notas.

### Corregido
- El teclado ya no se abre solo en la pantalla principal al cerrar una nota.
- En modo oscuro, el panel de análisis de una nota **con color** se lee bien (sus títulos
  salían en negro).

### Otros
- Licencia [GNU GPL v3.0](LICENSE).

## 0.1.0 (2026-09-29)

Primera versión, como APK firmado.

- Notas para poemas y canciones: título, tipo, siete colores de papel, fijar, búsqueda,
  cuadrícula escalonada y autoguardado.
- Análisis en tiempo real en el editor: sílabas métricas por verso ajustadas al metro
  dominante, esquema de rima (consonante y asonante, seseo opcional) y recursos literarios
  (aliteración, anáfora, epífora, anadiplosis, epanadiplosis, geminación, polisíndeton,
  asíndeton, paralelismo, estribillo), con resaltado y panel de resumen.
