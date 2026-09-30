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

    // ---------- Coloreado de rimas ----------

    /**
     * Terminación que rima (desde la vocal tónica hasta el final de la palabra)
     * con su [grupo] de color y el [tipo] de rima.
     */
    data class TramoRima(val rango: IntRange, val grupo: Int, val tipo: Rima.Tipo)

    /** Rango de la terminación de rima de [palabra], que ocupa [rangoPalabra] en el texto. */
    private fun terminacionEn(palabra: String, rangoPalabra: IntRange, seseo: Boolean): IntRange? {
        val t = Rima.terminacion(palabra, seseo) ?: return null
        val fin = rangoPalabra.last
        return (fin - t.texto.length + 1)..fin
    }

    /**
     * Tramos que hay que colorear para ver las rimas:
     * - la terminación de cada verso que rima con otro, con el grupo de su letra
     *   (A = 0, B = 1…) y su tipo (consonante o asonante);
     * - las dos palabras de cada rima interna: con el grupo del verso cuya rima
     *   final comparten o, si no hay ninguno, con un grupo nuevo tras los de las letras.
     */
    fun tramosDeRima(r: Resultado): List<TramoRima> {
        val out = mutableListOf<TramoRima>()
        val grupoPorClave = mutableMapOf<String, Int>()

        r.lineas.forEachIndexed { i, l ->
            val rima = l.rima ?: return@forEachIndexed
            val tipo = rima.tipo ?: return@forEachIndexed        // verso suelto: no se colorea
            val grupo = rima.letra.lowercaseChar() - 'a'
            val ultima = palabrasConPosicion(r, i).lastOrNull() ?: return@forEachIndexed
            val rango = terminacionEn(ultima.texto, ultima.rango, r.seseo) ?: return@forEachIndexed
            out += TramoRima(rango, grupo, tipo)
            grupoPorClave.putIfAbsent(rima.terminacion.consonante, grupo)
        }

        var siguiente = (out.maxOfOrNull { it.grupo } ?: -1) + 1
        r.recursos.filter { it.tipo == Recursos.Tipo.RIMA_INTERNA }.forEach { recurso ->
            val palabras = rangos(r, recurso).map { r.texto.substring(it) to it }
            val clave = palabras.firstNotNullOfOrNull { (p, _) -> Rima.terminacion(p, r.seseo)?.consonante }
                ?: return@forEach
            val grupo = grupoPorClave.getOrPut(clave) { siguiente++ }
            palabras.forEach { (p, rango) ->
                val t = terminacionEn(p, rango, r.seseo) ?: return@forEach
                if (out.none { it.rango == t }) out += TramoRima(t, grupo, Rima.Tipo.CONSONANTE)
            }
        }
        return out.sortedBy { it.rango.first }
    }
}
