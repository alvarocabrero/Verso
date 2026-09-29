package com.tuapp.analisis

import com.tuapp.analisis.Silabeador.TipoAcentual

/**
 * Métrica de versos en español.
 *
 * Como la sinalefa es opcional en poesía, cada verso no tiene un único recuento
 * sino un RANGO: [minimo] (todas las sinalefas) .. [maximo] (ninguna).
 * Cada sinalefa resta exactamente una sílaba, así que cualquier valor del rango
 * es alcanzable. [metroDominante] elige el valor que encaja con más versos.
 */
object Metrica {

    /** Palabras átonas: al ser monosílabos sin acento no bloquean la sinalefa. */
    private val ATONOS = setOf(
        "el", "la", "lo", "los", "las", "un", "una", "unos", "unas", "al", "del",
        "de", "a", "en", "con", "por", "sin", "so", "y", "e", "o", "u", "ni", "que",
        "se", "me", "te", "le", "les", "nos", "os", "mi", "tu", "su", "mis", "tus", "sus"
    )
    private const val VOCALES = "aeiouáéíóúü"

    /**
     * Sinalefa entre la palabra [indice] y la siguiente.
     * [resistencia]: 2 si la vocal final es tónica (está‿en), 1 si lo es la
     * inicial siguiente (mi‿alma), 0 si ninguna. Las más resistentes se
     * rompen primero al ajustar un verso a un metro.
     */
    data class Sinalefa(val indice: Int, val resistencia: Int)

    class Verso(val texto: String) {
        val palabras: List<Silabeador.Palabra> =
            Silabeador.palabrasDe(texto).map { Silabeador.analizar(it) }

        val sinalefas: List<Sinalefa> = (0 until (palabras.size - 1).coerceAtLeast(0))
            .filter { terminaEnVocal(palabras[it].texto) && empiezaConVocal(palabras[it + 1].texto) }
            .map { j ->
                val a = palabras[j]
                val b = palabras[j + 1]
                val finalTonico = if (a.silabas.size == 1) a.texto.lowercase() !in ATONOS
                                  else a.tipo == TipoAcentual.AGUDA
                val inicioTonico = if (b.silabas.size == 1) b.texto.lowercase() !in ATONOS
                                   else b.tonica == 0
                Sinalefa(j, if (finalTonico) 2 else if (inicioTonico) 1 else 0)
            }

        /** Ley del acento final: aguda +1, llana 0, esdrújula/sobresdrújula −1. */
        val ajusteFinal: Int = when (palabras.lastOrNull()?.tipo) {
            TipoAcentual.AGUDA -> 1
            TipoAcentual.ESDRUJULA, TipoAcentual.SOBRESDRUJULA -> -1
            else -> 0
        }

        val silabasGramaticales: Int = palabras.sumOf { it.silabas.size }
        val maximo: Int = if (palabras.isEmpty()) 0 else silabasGramaticales + ajusteFinal
        val minimo: Int = maximo - sinalefas.size

        fun admite(metro: Int) = metro in minimo..maximo

        /**
         * Sílabas para mostrar en pantalla, ajustadas al [metro] pedido.
         * Las sinalefas aplicadas se marcan con "‿". Devuelve null si el
         * verso no puede medir eso. Por defecto aplica todas las sinalefas.
         */
        fun silabasPara(metro: Int = minimo): List<String>? {
            if (palabras.isEmpty() || !admite(metro)) return null
            val rotas = sinalefas
                .sortedWith(compareByDescending<Sinalefa> { it.resistencia }.thenByDescending { it.indice })
                .take(metro - minimo)
                .map { it.indice }.toSet()
            val activas = sinalefas.map { it.indice }.toSet() - rotas

            val out = mutableListOf<String>()
            palabras.forEachIndexed { j, p ->
                if ((j - 1) in activas) {
                    out[out.lastIndex] = out.last() + "‿" + p.silabas.first()
                    out += p.silabas.drop(1)
                } else out += p.silabas
            }
            return out
        }
    }

    private fun terminaEnVocal(palabra: String): Boolean {
        val l = palabra.lowercase()
        return l == "y" || l.last() in VOCALES   // "rey", "hoy": la y final no hace sinalefa
    }

    private fun empiezaConVocal(palabra: String): Boolean {
        var l = palabra.lowercase()
        if (l == "y") return true
        if (l.startsWith("h")) {
            l = l.drop(1)
            if (l.startsWith("ie") || l.startsWith("ue")) return false  // hielo, hueso
        }
        return l.isNotEmpty() && l[0] in VOCALES
    }

    // ---------- Poema completo ----------

    /** Metros más habituales, para desempatar. */
    private val PREFERENCIA = listOf(11, 8, 7, 14, 6, 5, 9, 12, 10, 13, 4, 3, 2)

    /** Metro que encaja con más versos (null si no hay versos). */
    fun metroDominante(versos: List<Verso>): Int? {
        val validos = versos.filter { it.palabras.isNotEmpty() }
        if (validos.isEmpty()) return null
        val candidatos = (validos.minOf { it.minimo }..validos.maxOf { it.maximo })
        return candidatos.maxWithOrNull(
            compareBy<Int> { m -> validos.count { it.admite(m) } }
                .thenByDescending { m -> PREFERENCIA.indexOf(m).let { if (it < 0) 99 else it } }
        )
    }

    fun analizarTexto(texto: String): List<Verso> =
        texto.lines().map { Verso(it) }   // las líneas vacías separan estrofas

    fun nombreMetro(n: Int): String = when (n) {
        2 -> "bisílabo"; 3 -> "trisílabo"; 4 -> "tetrasílabo"; 5 -> "pentasílabo"
        6 -> "hexasílabo"; 7 -> "heptasílabo"; 8 -> "octosílabo"; 9 -> "eneasílabo"
        10 -> "decasílabo"; 11 -> "endecasílabo"; 12 -> "dodecasílabo"
        13 -> "tridecasílabo"; 14 -> "alejandrino"
        else -> "$n sílabas"
    }
}
