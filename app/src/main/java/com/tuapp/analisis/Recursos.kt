package com.tuapp.analisis

/**
 * Detector de recursos literarios basados en patrones (sonido, repetición,
 * estructura). Los recursos semánticos (metáfora, símil, personificación...)
 * no se pueden detectar con reglas: para eso hace falta un modelo de lenguaje.
 */
object Recursos {

    enum class Tipo(val nombre: String, val descripcion: String) {
        ALITERACION("Aliteración", "Repetición de un mismo sonido consonántico"),
        ANAFORA("Anáfora", "Varios versos empiezan con las mismas palabras"),
        EPIFORA("Epífora", "Varios versos terminan con las mismas palabras"),
        ANADIPLOSIS("Anadiplosis", "Un verso empieza con la palabra con que acaba el anterior"),
        EPANADIPLOSIS("Epanadiplosis", "El verso empieza y termina con la misma palabra"),
        GEMINACION("Geminación", "Una palabra se repite seguida"),
        POLISINDETON("Polisíndeton", "Uso repetido de conjunciones"),
        ASINDETON("Asíndeton", "Enumeración sin conjunciones"),
        PARALELISMO("Paralelismo", "Versos consecutivos con la misma estructura"),
        ESTRIBILLO("Estribillo", "Verso que se repite a lo largo del texto")
    }

    /**
     * [lineas]: índices de línea implicados. [palabras]: palabras a resaltar.
     * [intensidad]: solo en aliteraciones; cuántas veces supera el sonido su
     * frecuencia normal en español. [clara] distingue "clara" de "posible".
     */
    data class Recurso(
        val tipo: Tipo,
        val lineas: List<Int>,
        val evidencia: String,
        val palabras: List<String> = emptyList(),
        val intensidad: Double? = null
    ) {
        val clara: Boolean get() = intensidad == null || intensidad >= UMBRAL_CLARA
    }

    /** A partir de aquí la aliteración se considera clara, no solo posible. */
    const val UMBRAL_CLARA = 4.5

    private val CONJUNCIONES = setOf("y", "e", "ni", "o", "u")
    private val ARTICULOS = setOf("el", "la", "lo", "los", "las", "un", "una", "unos", "unas", "le", "les")
    private val ATONOS = ARTICULOS + CONJUNCIONES + setOf(
        "al", "del", "de", "a", "en", "con", "por", "sin", "que", "se", "me", "te",
        "nos", "os", "mi", "tu", "su", "mis", "tus", "sus"
    )

    fun detectar(texto: String, seseo: Boolean = false): List<Recurso> = detectar(texto.lines(), seseo)

    fun detectar(lineas: List<String>, seseo: Boolean = false): List<Recurso> {
        val pal = lineas.map { l -> Silabeador.palabrasDe(l).map { it.lowercase() } }
        val bloques = bloques(pal)
        val out = mutableListOf<Recurso>()

        bloques.forEach { b ->
            out += anaforaYPolisindeton(b, pal)
            out += epifora(b, pal)
            out += anadiplosis(b, pal)
            out += paralelismo(b, pal)
            out += aliteraciones(b, lineas, seseo)
        }
        pal.forEachIndexed { i, w ->
            if (w.isEmpty()) return@forEachIndexed
            epanadiplosis(i, w)?.let { out += it }
            geminacion(i, w)?.let { out += it }
            polisindetonEnVerso(i, w)?.let { out += it }
            asindeton(i, lineas[i])?.let { out += it }
        }
        out += estribillos(pal)
        return out.sortedWith(compareBy({ it.lineas.first() }, { it.tipo.ordinal }))
    }

    // ---------- Utilidades ----------

    /** Grupos de líneas consecutivas no vacías (estrofas). */
    private fun bloques(pal: List<List<String>>): List<List<Int>> {
        val res = mutableListOf<MutableList<Int>>()
        var actual = mutableListOf<Int>()
        pal.forEachIndexed { i, w ->
            if (w.isEmpty()) { if (actual.isNotEmpty()) res += actual; actual = mutableListOf() }
            else actual += i
        }
        if (actual.isNotEmpty()) res += actual
        return res
    }

    /** Recorre rachas de líneas consecutivas donde [clave] coincide. */
    private fun rachas(b: List<Int>, clave: (Int) -> String?, accion: (List<Int>) -> Unit) {
        var i = 0
        while (i < b.size) {
            val k = clave(b[i])
            var j = i
            while (k != null && j + 1 < b.size && clave(b[j + 1]) == k) j++
            if (j > i) accion(b.subList(i, j + 1))
            i = j + 1
        }
    }

    private fun prefijoComun(ls: List<List<String>>): List<String> {
        var k = 0
        while (ls.all { it.size > k && it[k] == ls[0][k] }) k++
        return ls[0].take(k)
    }

    private fun todasIguales(run: List<Int>, pal: List<List<String>>) = run.all { pal[it] == pal[run[0]] }

    // ---------- Repetición ----------

    private fun anaforaYPolisindeton(b: List<Int>, pal: List<List<String>>): List<Recurso> {
        val res = mutableListOf<Recurso>()
        rachas(b, { pal[it].firstOrNull() }) { run ->
            if (todasIguales(run, pal)) return@rachas
            val pref = prefijoComun(run.map { pal[it] })
            val w = pref[0]
            when {
                w in CONJUNCIONES ->
                    res += Recurso(Tipo.POLISINDETON, run, "«$w» al inicio de ${run.size} versos", listOf(w))
                pref.size == 1 && w in ATONOS && run.size < 3 -> Unit   // "la… / la…" es demasiado común
                else -> res += Recurso(Tipo.ANAFORA, run, "«${pref.joinToString(" ")}»", pref)
            }
        }
        return res
    }

    private fun epifora(b: List<Int>, pal: List<List<String>>): List<Recurso> {
        val res = mutableListOf<Recurso>()
        rachas(b, { pal[it].lastOrNull() }) { run ->
            if (todasIguales(run, pal)) return@rachas
            val suf = prefijoComun(run.map { pal[it].reversed() }).reversed()
            if (suf.size == 1 && suf[0] in ATONOS) return@rachas
            res += Recurso(Tipo.EPIFORA, run, "«${suf.joinToString(" ")}»", suf)
        }
        return res
    }

    private fun anadiplosis(b: List<Int>, pal: List<List<String>>): List<Recurso> =
        b.zipWithNext().mapNotNull { (x, y) ->
            val w = pal[x].last()
            if (w == pal[y].first() && w !in ATONOS) Recurso(Tipo.ANADIPLOSIS, listOf(x, y), "«$w»", listOf(w))
            else null
        }

    private fun epanadiplosis(i: Int, w: List<String>): Recurso? =
        if (w.size >= 3 && w.first() == w.last() && w.first() !in ATONOS)
            Recurso(Tipo.EPANADIPLOSIS, listOf(i), "«${w.first()}»", listOf(w.first()))
        else null

    private fun geminacion(i: Int, w: List<String>): Recurso? =
        w.zipWithNext().firstOrNull { (a, b) -> a == b && a.length >= 2 }?.let { (a, _) ->
            Recurso(Tipo.GEMINACION, listOf(i), "«$a, $a»", listOf(a))
        }

    private fun estribillos(pal: List<List<String>>): List<Recurso> =
        pal.indices.filter { pal[it].size >= 2 }
            .groupBy { pal[it].joinToString(" ") }
            .filter { it.value.size >= 2 }
            .map { (texto, idx) -> Recurso(Tipo.ESTRIBILLO, idx, "«$texto» (×${idx.size})") }

    // ---------- Conjunciones y enumeraciones ----------

    private fun polisindetonEnVerso(i: Int, w: List<String>): Recurso? {
        val conj = w.filter { it in CONJUNCIONES }
        return if (conj.size >= 2)
            Recurso(Tipo.POLISINDETON, listOf(i), "${conj.size} conjunciones en el verso", conj.distinct())
        else null
    }

    private fun asindeton(i: Int, linea: String): Recurso? {
        val segs = linea.split(Regex("[,;]")).map { Silabeador.palabrasDe(it).map { w -> w.lowercase() } }
            .filter { it.isNotEmpty() }
        val ok = segs.size >= 3 &&
                segs.all { it.size <= 3 } &&
                segs.last().first() !in CONJUNCIONES &&
                segs.distinct().size > 1                  // "palabras, palabras, palabras" es geminación
        return if (ok) Recurso(Tipo.ASINDETON, listOf(i), "${segs.size} elementos sin conjunción") else null
    }

    // ---------- Estructura ----------

    private fun clase(w: String): String = when (w) {
        "el", "la", "lo", "los", "las" -> "ART"
        "un", "una", "unos", "unas" -> "IND"
        "mi", "tu", "su", "mis", "tus", "sus" -> "POS"
        else -> w
    }

    private fun paralelos(a: List<String>, b: List<String>): Boolean {
        if (a.size != b.size || a.size < 4 || a == b) return false
        val coincidencias = a.indices.count { clase(a[it]) == clase(b[it]) }
        return coincidencias >= 3 && coincidencias * 2 >= a.size
    }

    private fun paralelismo(b: List<Int>, pal: List<List<String>>): List<Recurso> {
        val res = mutableListOf<Recurso>()
        var run = mutableListOf<Int>()
        for ((x, y) in b.zipWithNext()) {
            if (paralelos(pal[x], pal[y])) {
                if (run.isEmpty()) run += x
                run += y
            } else {
                if (run.isNotEmpty()) res += Recurso(Tipo.PARALELISMO, run, "${run.size} versos con la misma estructura")
                run = mutableListOf()
            }
        }
        if (run.isNotEmpty()) res += Recurso(Tipo.PARALELISMO, run, "${run.size} versos con la misma estructura")
        return res
    }

    // ---------- Aliteración ----------

    /**
     * Frecuencia de cada consonante en posición de ataque (inicio de sílaba)
     * en prosa española corriente. θ = z/ce/ci; R = rr fuerte.
     */
    private val FRECUENCIA_BASE = mapOf(
        "t" to .104, "k" to .100, "d" to .095, "r" to .091, "m" to .086, "p" to .084,
        "b" to .077, "n" to .075, "l" to .075, "s" to .062, "θ" to .029, "R" to .024,
        "y" to .022, "g" to .022, "f" to .020, "ñ" to .013, "j" to .011, "ch" to .007
    )
    private val GRAFIA = mapOf(
        "k" to "c/qu", "θ" to "z/c", "R" to "rr", "b" to "b/v", "y" to "y/ll", "j" to "j/g"
    )
    private const val UMBRAL = 3.5
    private val INSEPARABLES = setOf("pr", "br", "kr", "gr", "fr", "tr", "dr", "pl", "bl", "kl", "gl", "fl")

    /** Consonantes en posición de ataque (no a final de sílaba) de una palabra. */
    private fun ataques(palabra: String, seseo: Boolean): List<String> {
        var f = Rima.fonetica(palabra.lowercase(), seseo)
        f = f.replace(Regex("^r"), "R").replace(Regex("(?<=[nls])r"), "R").replace("rr", "R")
        val toks = mutableListOf<String>()
        var i = 0
        while (i < f.length) {
            if (f.startsWith("ch", i)) { toks += "ch"; i += 2 } else { toks += f[i].toString(); i++ }
        }
        return toks.indices.mapNotNull { k ->
            val t = toks[k]
            val sig = toks.getOrNull(k + 1)
            val esConsonante = t !in listOf("a", "e", "i", "o", "u") && (t[0].isLetter() || t == "θ")
            if (esConsonante && sig != null && (sig in listOf("a", "e", "i", "o", "u") || t + sig in INSEPARABLES)) t
            else null
        }
    }

    /** Recuento de sonidos de ataque en un grupo de versos. */
    private class Estadisticas {
        var total = 0
        val cuenta = mutableMapOf<String, Int>()
        val raices = mutableMapOf<String, MutableSet<String>>()
        val ejemplos = mutableMapOf<String, MutableSet<String>>()
        val enLineas = mutableMapOf<String, MutableSet<Int>>()
    }

    private fun estadisticas(idx: List<Int>, lineas: List<String>, seseo: Boolean): Estadisticas {
        val e = Estadisticas()
        for (li in idx) for (p in Silabeador.palabrasDe(lineas[li])) {
            val a = ataques(p, seseo)
            e.total += a.size
            if (p.lowercase() in ARTICULOS) continue
            for (t in a) {
                e.cuenta[t] = (e.cuenta[t] ?: 0) + 1
                e.raices.getOrPut(t) { mutableSetOf() } += p.lowercase().take(4) // caminante/camino = 1
                e.ejemplos.getOrPut(t) { mutableSetOf() } += p
                e.enLineas.getOrPut(t) { mutableSetOf() } += li
            }
        }
        return e
    }

    private class Candidato(
        val sonido: String, val lineas: List<Int>, val n: Int,
        val intensidad: Double, val ejemplos: Set<String>
    )

    /**
     * Sonidos aliterados en los versos [idx]. En ventanas de varios versos
     * el sonido debe aparecer en todos ellos, para no confundir una
     * aliteración concentrada en un verso con una repartida.
     */
    private fun candidatos(idx: List<Int>, lineas: List<String>, seseo: Boolean, minimo: Int): List<Candidato> {
        val e = estadisticas(idx, lineas, seseo)
        if (e.total == 0) return emptyList()
        return e.cuenta.mapNotNull { (t, n) ->
            val base = if (seseo && t == "s") .091 else (FRECUENCIA_BASE[t] ?: .015)
            val intensidad = n.toDouble() / e.total / base
            val ok = n >= minimo && e.raices[t]!!.size >= 3 && intensidad >= UMBRAL &&
                     e.enLineas[t]!!.size == idx.size
            if (ok) Candidato(t, idx, n, intensidad, e.ejemplos[t]!!) else null
        }
    }

    /**
     * Aliteraciones de una estrofa: se analiza cada verso y cada par de versos
     * consecutivos; los pares solapados con el mismo sonido se fusionan y los
     * versos sueltos ya cubiertos por una aliteración más amplia se descartan.
     */
    private fun aliteraciones(b: List<Int>, lineas: List<String>, seseo: Boolean): List<Recurso> {
        val individuales = b.flatMap { candidatos(listOf(it), lineas, seseo, 3) }
        val ventanas = b.zipWithNext().flatMap { (x, y) -> candidatos(listOf(x, y), lineas, seseo, 4) }

        val fusionadas = mutableListOf<Candidato>()
        for (v in ventanas.sortedWith(compareBy({ it.sonido }, { it.lineas.first() }))) {
            val u = fusionadas.lastOrNull()
            if (u != null && u.sonido == v.sonido && u.lineas.last() >= v.lineas.first()) {
                val union = (u.lineas + v.lineas).distinct().sorted()
                val n = estadisticas(union, lineas, seseo).cuenta[v.sonido] ?: 0
                fusionadas[fusionadas.lastIndex] =
                    Candidato(v.sonido, union, n, maxOf(u.intensidad, v.intensidad), u.ejemplos + v.ejemplos)
            } else fusionadas += v
        }

        val cubiertos = fusionadas.flatMap { f -> f.lineas.map { it to f.sonido } }.toSet()
        val sueltas = individuales.filter { (it.lineas[0] to it.sonido) !in cubiertos }

        return (fusionadas + sueltas).map { c ->
            val donde = if (c.lineas.size > 1) " en ${c.lineas.size} versos" else ""
            Recurso(Tipo.ALITERACION, c.lineas, "sonido «${GRAFIA[c.sonido] ?: c.sonido}» ×${c.n}$donde",
                c.ejemplos.toList(), c.intensidad)
        }
    }
}
