package com.tuapp.analisis

/**
 * Silabeador ortográfico del español.
 *
 * - Divide palabras en sílabas gramaticales (diptongos, triptongos, hiatos,
 *   dígrafos ch/ll/rr, "qu"/"gu" mudos, "y" vocálica, grupos inseparables).
 * - Detecta la sílaba tónica y el tipo acentual (aguda, llana, esdrújula...).
 *
 * No depende de Android: se puede probar con JUnit puro.
 */
object Silabeador {

    enum class TipoAcentual { AGUDA, LLANA, ESDRUJULA, SOBRESDRUJULA }

    data class Palabra(
        val texto: String,
        val silabas: List<String>,
        val tonica: Int,             // índice (desde 0) de la sílaba tónica
        val tipo: TipoAcentual
    )

    // ---------- Clasificación de letras ----------

    private const val FUERTES = "aeoáéó"
    private const val DEBILES_TILDADAS = "íú"
    private const val DEBILES = "iuü"
    private const val VOCALES = FUERTES + DEBILES_TILDADAS + DEBILES
    private const val TILDES = "áéíóú"

    /** Grupos consonánticos que nunca se separan (pr, bl, tr...). */
    private val INSEPARABLES: Set<String> =
        "pbcgfktd".map { "${it}r" }.toSet() + "pbcgfk".map { "${it}l" }.toSet()

    private enum class Clase { FUERTE, DEBIL, DEBIL_TILDADA }

    private fun clase(v: Char): Clase = when (v) {
        in FUERTES -> Clase.FUERTE
        in DEBILES_TILDADAS -> Clase.DEBIL_TILDADA
        else -> Clase.DEBIL
    }

    private fun base(v: Char): Char = when (v) {
        'í' -> 'i'; 'ú', 'ü' -> 'u'; else -> v
    }

    // ---------- Tokenización ----------

    /**
     * Unidad fonológica: una vocal o una consonante (que puede ser un dígrafo).
     * [original] conserva mayúsculas y tildes; [norm] es la forma en minúscula
     * usada para el análisis ("y" vocálica se normaliza como "i").
     */
    private data class Unidad(val esVocal: Boolean, val original: String, val norm: String)

    private fun tokenizar(palabra: String): List<Unidad> {
        val l = palabra.lowercase()
        val out = mutableListOf<Unidad>()
        var i = 0
        while (i < l.length) {
            val c = l[i]
            val n = l.getOrNull(i + 1)
            val nn = l.getOrNull(i + 2)
            val par = if (i + 1 < l.length) l.substring(i, i + 2) else ""

            when {
                par == "ch" || par == "ll" || par == "rr" -> {
                    out += Unidad(false, palabra.substring(i, i + 2), par); i += 2
                }
                // "qu" y "gu" ante e/i: la u es muda
                (c == 'q' || c == 'g') && n == 'u' && nn != null && nn in "eéií" -> {
                    out += Unidad(false, palabra.substring(i, i + 2), par); i += 2
                }
                // "y" es vocal si no va seguida de vocal (rey, hoy, y)
                c == 'y' -> {
                    val esVocal = n == null || n !in VOCALES
                    out += Unidad(esVocal, palabra[i].toString(), if (esVocal) "i" else "y"); i++
                }
                else -> {
                    out += Unidad(c in VOCALES, palabra[i].toString(), c.toString()); i++
                }
            }
        }
        return out
    }

    // ---------- Diptongo / hiato ----------

    /** ¿La vocal [actual] rompe el núcleo actual (hiato)? */
    private fun esHiato(nucleo: List<Unidad>, actual: Unidad): Boolean {
        val prev = nucleo.last().norm[0]
        val cur = actual.norm[0]
        val p = clase(prev)
        val c = clase(cur)

        // Dos débiles: diptongo (ciudad, construí) salvo que sean iguales (chiita)
        if (p != Clase.FUERTE && c != Clase.FUERTE) {
            if (p == Clase.DEBIL_TILDADA && c == Clase.DEBIL_TILDADA) return true
            return base(prev) == base(cur)
        }
        // Débil tildada junto a fuerte: hiato (río, maíz)
        if (p == Clase.DEBIL_TILDADA || c == Clase.DEBIL_TILDADA) return true
        // Dos fuertes en el mismo núcleo: hiato (poeta, leer)
        if (c == Clase.FUERTE && nucleo.any { clase(it.norm[0]) == Clase.FUERTE }) return true
        return false
    }

    // ---------- API pública ----------

    fun silabear(palabra: String): List<String> {
        val t = tokenizar(palabra)
        val nucleos = mutableListOf<List<Unidad>>()
        val consonantes = mutableListOf(mutableListOf<Unidad>()) // antes, entre y después de núcleos

        var i = 0
        while (i < t.size) {
            if (!t[i].esVocal) {
                consonantes.last() += t[i]; i++
            } else {
                val nuc = mutableListOf(t[i]); i++
                while (i < t.size && t[i].esVocal && !esHiato(nuc, t[i])) {
                    nuc += t[i]; i++
                }
                nucleos += nuc
                consonantes += mutableListOf<Unidad>()
            }
        }
        if (nucleos.isEmpty()) return listOf(palabra)

        val silabas = MutableList(nucleos.size) { StringBuilder() }
        consonantes[0].forEach { silabas[0].append(it.original) }

        for (k in nucleos.indices) {
            nucleos[k].forEach { silabas[k].append(it.original) }
            val cs = consonantes[k + 1]

            if (k == nucleos.lastIndex) {           // consonantes finales
                cs.forEach { silabas[k].append(it.original) }
                break
            }
            val corte = when (cs.size) {
                0, 1 -> 0                                               // ca-sa
                2 -> if (cs[0].norm + cs[1].norm in INSEPARABLES) 0 else 1  // a-bra-zo / ac-ción
                else -> if (cs[cs.size - 2].norm + cs.last().norm in INSEPARABLES)
                    cs.size - 2 else cs.size - 1                        // ins-tru / obs-tá
            }
            cs.subList(0, corte).forEach { silabas[k].append(it.original) }
            cs.subList(corte, cs.size).forEach { silabas[k + 1].append(it.original) }
        }
        return silabas.map { it.toString() }
    }

    /** Índice de la sílaba tónica según las reglas de acentuación. */
    fun silabaTonica(palabra: String, silabas: List<String> = silabear(palabra)): Int {
        silabas.forEachIndexed { i, s -> if (s.lowercase().any { it in TILDES }) return i }
        if (silabas.size == 1) return 0
        val ultima = palabra.lowercase().last()
        return if (ultima in "aeiouns") silabas.size - 2 else silabas.lastIndex
    }

    fun analizar(palabra: String): Palabra {
        val silabas = silabear(palabra)
        val tonica = silabaTonica(palabra, silabas)
        val tipo = when (silabas.size - 1 - tonica) {
            0 -> TipoAcentual.AGUDA
            1 -> TipoAcentual.LLANA
            2 -> TipoAcentual.ESDRUJULA
            else -> TipoAcentual.SOBRESDRUJULA
        }
        return Palabra(palabra, silabas, tonica, tipo)
    }

    /** Extrae las palabras de un verso ignorando signos de puntuación. */
    fun palabrasDe(verso: String): List<String> =
        Regex("\\p{L}+").findAll(verso).map { it.value }.toList()
}
