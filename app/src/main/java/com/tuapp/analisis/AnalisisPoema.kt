package com.tuapp.analisis

/**
 * Análisis completo de un texto para mostrarlo en el editor: sílabas de cada
 * verso ajustadas al metro dominante, letra de rima y recursos literarios.
 * También traduce cada recurso a rangos de caracteres para resaltarlo.
 */
object AnalisisPoema {

    /**
     * Una línea del texto. [inicio]/[fin]: posición en el texto completo.
     * [silabas]: recuento para mostrar (null en líneas sin palabras); si el
     * verso admite el metro dominante es ese metro, si no, el valor de su
     * rango más cercano, y [encaja] es false.
     */
    data class Linea(
        val inicio: Int,
        val fin: Int,
        val silabas: Int?,
        val encaja: Boolean,
        val rima: Rima.RimaVerso?
    )

    data class Resultado(
        val texto: String,
        val seseo: Boolean,
        val metro: Int?,
        val lineas: List<Linea>,
        val recursos: List<Recursos.Recurso>
    ) {
        /** "ABBA ABBA": una letra por verso, espacio entre estrofas. */
        val esquema: String
            get() = lineas.joinToString("") { l -> l.rima?.letra?.toString() ?: " " }
                .trim().replace(Regex(" +"), " ")
    }

    fun analizar(texto: String, seseo: Boolean = false): Resultado {
        val lineas = texto.split("\n")
        val versos = lineas.map { Metrica.Verso(it) }
        val metro = Metrica.metroDominante(versos)
        val rimas = Rima.esquema(lineas, seseo, minusculas = metro != null && metro <= 8)

        var inicio = 0
        val res = lineas.mapIndexed { i, l ->
            val v = versos[i]
            val silabas = when {
                v.palabras.isEmpty() -> null
                metro == null -> v.minimo
                else -> metro.coerceIn(v.minimo, v.maximo)
            }
            val linea = Linea(
                inicio = inicio,
                fin = inicio + l.length,
                silabas = silabas,
                encaja = metro == null || v.admite(metro),
                rima = if (v.palabras.isEmpty()) null else rimas[i]
            )
            inicio += l.length + 1
            linea
        }
        return Resultado(texto, seseo, metro, res, Recursos.detectar(lineas, seseo))
    }

    // ---------- Resaltado ----------

    private data class Palabra(val texto: String, val rango: IntRange)

    private fun palabrasConPosicion(r: Resultado, linea: Int): List<Palabra> {
        val l = r.lineas[linea]
        return Regex("\\p{L}+").findAll(r.texto.substring(l.inicio, l.fin)).map {
            Palabra(it.value.lowercase(), (l.inicio + it.range.first)..(l.inicio + it.range.last))
        }.toList()
    }

    /**
     * Rangos de caracteres (en [Resultado.texto]) que ocupa el [recurso]:
     * las palabras concretas cuando las hay y, si no, los versos enteros
     * (paralelismo, asíndeton, estribillo).
     */
    fun rangos(r: Resultado, recurso: Recursos.Recurso): List<IntRange> {
        val buscadas = recurso.palabras.map { it.lowercase() }.toSet()
        return recurso.lineas.filter { it in r.lineas.indices }.flatMapIndexed { orden, li ->
            val ps = palabrasConPosicion(r, li)
            if (ps.isEmpty()) return@flatMapIndexed emptyList()
            val k = recurso.palabras.size
            val elegidas = when (recurso.tipo) {
                Recursos.Tipo.ANAFORA -> ps.take(k)
                Recursos.Tipo.EPIFORA -> ps.takeLast(k)
                Recursos.Tipo.ANADIPLOSIS -> listOf(if (orden == 0) ps.last() else ps.first())
                Recursos.Tipo.EPANADIPLOSIS -> listOf(ps.first(), ps.last())
                Recursos.Tipo.GEMINACION -> ps.indices
                    .filter { j -> ps[j].texto in buscadas &&
                        (ps.getOrNull(j - 1)?.texto == ps[j].texto || ps.getOrNull(j + 1)?.texto == ps[j].texto) }
                    .map { ps[it] }
                else -> if (buscadas.isEmpty()) return@flatMapIndexed listOf(ps.first().rango.first..ps.last().rango.last)
                        else ps.filter { it.texto in buscadas }
            }
            elegidas.map { it.rango }
        }
    }
}
