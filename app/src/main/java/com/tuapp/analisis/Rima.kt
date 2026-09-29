package com.tuapp.analisis

/**
 * Detección de rima en español.
 *
 * La rima empieza en la última vocal tónica del verso:
 * - CONSONANTE: coinciden todos los sonidos desde ahí (cielo / suelo).
 * - ASONANTE: solo coinciden las vocales (cielo / lejos). Se cuenta la tónica y
 *   la de la última sílaba; en diptongos cuenta la fuerte; en posición final
 *   átona, i ≈ e y u ≈ o (fácil / calle, Venus / tenso).
 *
 * La comparación consonante es fonética: b = v, h muda, yeísmo (ll = y),
 * y opcionalmente seseo (s = z = c ante e/i), útil para letras latinoamericanas.
 */
object Rima {

    enum class Tipo { CONSONANTE, ASONANTE }

    data class Terminacion(
        val palabra: String,
        val texto: String,        // terminación escrita: "ántaro"
        val consonante: String,   // clave fonética: "antaro"
        val asonante: String      // vocales: "ao"
    )

    /** letra: 'A', 'B'... o '-' si el verso no rima con ninguno. */
    data class RimaVerso(val letra: Char, val tipo: Tipo?, val terminacion: Terminacion)

    // ---------- Vocal tónica dentro de una sílaba ----------

    /** Índice de la vocal que lleva el acento dentro de [silaba]. */
    private fun indiceVocalTonica(silaba: String): Int? {
        val s = silaba.lowercase()
        s.indexOfFirst { it in "áéíóú" }.let { if (it >= 0) return it }
        s.indexOfFirst { it in "aeo" }.let { if (it >= 0) return it }
        // Solo vocales débiles (cui-da, ciu-dad, muy): cuenta la última,
        // saltando la u muda de qu/gu ante e/i
        var idx: Int? = null
        s.forEachIndexed { i, c ->
            if (c in "iuüy") {
                val uMuda = c == 'u' && i > 0 && s[i - 1] in "qg" && s.getOrNull(i + 1)?.let { it in "eéií" } == true
                if (!uMuda) idx = i
            }
        }
        return idx
    }

    private fun sinTilde(c: Char): Char = when (c) {
        'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú', 'ü' -> 'u'; 'y' -> 'i'; else -> c
    }

    // ---------- Normalización fonética ----------

    internal fun fonetica(t: String, seseo: Boolean): String {
        val z = if (seseo) "s" else "θ"
        var s = t.replace("ch", "ç").replace("h", "")
        s = s.replace(Regex("g(?=[eéií])"), "j")          // gente → jente
        s = s.replace(Regex("qu(?=[eéií])"), "k")         // queso → keso
        s = s.replace(Regex("gu(?=[eéií])"), "g")         // guerra → gerra
        s = s.replace("ü", "u")
        s = s.replace(Regex("c(?=[eéií])"), z).replace("z", z).replace("c", "k")
        s = s.replace("v", "b").replace("ll", "y")
        s = s.replace(Regex("y(?![aeiouáéíóú])"), "i")    // hoy → oi
        return s.replace("ç", "ch").map { if (it == 'y') 'y' else sinTilde(it) }.joinToString("")
    }

    // ---------- API pública ----------

    fun terminacion(verso: String, seseo: Boolean = false): Terminacion? {
        val palabra = Silabeador.palabrasDe(verso).lastOrNull() ?: return null
        val silabas = Silabeador.silabear(palabra)
        val t = Silabeador.silabaTonica(palabra, silabas)
        val iv = indiceVocalTonica(silabas[t]) ?: return null

        val inicio = silabas.take(t).sumOf { it.length } + iv
        val texto = palabra.lowercase().substring(inicio)

        val tonica = sinTilde(texto[0])
        val asonante = if (t == silabas.lastIndex) "$tonica" else {
            val ult = silabas.last()
            val f = indiceVocalTonica(ult)?.let { sinTilde(ult.lowercase()[it]) }
            val eq = when (f) { 'i' -> 'e'; 'u' -> 'o'; else -> f }
            "$tonica${eq ?: ""}"
        }
        return Terminacion(palabra, texto, fonetica(texto, seseo), asonante)
    }

    /** ¿Riman dos versos? Devuelve el tipo o null. */
    fun comparar(a: String, b: String, seseo: Boolean = false): Tipo? {
        val ta = terminacion(a, seseo) ?: return null
        val tb = terminacion(b, seseo) ?: return null
        return when {
            ta.consonante == tb.consonante -> Tipo.CONSONANTE
            ta.asonante == tb.asonante -> Tipo.ASONANTE
            else -> null
        }
    }

    /**
     * Esquema de rima de un texto. Devuelve un elemento por línea
     * (null en líneas vacías). Las letras se asignan en orden de aparición.
     * Primero se agrupan las rimas consonantes; los versos sueltos se unen
     * por asonancia a un grupo existente o forman uno nuevo.
     * [minusculas]: úsalo para arte menor (versos de 8 sílabas o menos).
     */
    fun esquema(lineas: List<String>, seseo: Boolean = false, minusculas: Boolean = false): List<RimaVerso?> {
        val ts = lineas.map { if (it.isBlank()) null else terminacion(it, seseo) }
        val indices = ts.indices.filter { ts[it] != null }

        class Grupo(val asonante: String, val miembros: MutableMap<Int, Tipo>)
        val grupos = mutableListOf<Grupo>()

        val porConsonante = indices.groupBy { ts[it]!!.consonante }
        porConsonante.values.filter { it.size >= 2 }.forEach { v ->
            grupos += Grupo(ts[v[0]]!!.asonante, v.associateWith { Tipo.CONSONANTE }.toMutableMap())
        }
        val sueltos = indices.filter { porConsonante[ts[it]!!.consonante]!!.size < 2 }
        sueltos.groupBy { ts[it]!!.asonante }.forEach { (aso, v) ->
            val destino = grupos.firstOrNull { it.asonante == aso }
            when {
                destino != null -> v.forEach { destino.miembros[it] = Tipo.ASONANTE }
                v.size >= 2 -> grupos += Grupo(aso, v.associateWith { Tipo.ASONANTE }.toMutableMap())
            }
        }
        grupos.sortBy { g -> g.miembros.keys.min() }

        val resultado = MutableList<RimaVerso?>(lineas.size) { i -> ts[i]?.let { RimaVerso('-', null, it) } }
        grupos.forEachIndexed { n, g ->
            val letra = ('A' + n).let { if (minusculas) it.lowercaseChar() else it }
            g.miembros.forEach { (i, tipo) -> resultado[i] = RimaVerso(letra, tipo, ts[i]!!) }
        }
        return resultado
    }

    /** "ABBA ABBA": las líneas vacías se muestran como espacio. */
    fun esquemaComoTexto(esquema: List<RimaVerso?>): String =
        esquema.joinToString("") { it?.letra?.toString() ?: " " }
}
