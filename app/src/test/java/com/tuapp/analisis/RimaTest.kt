package com.tuapp.analisis

import com.tuapp.analisis.Rima.Tipo.ASONANTE
import com.tuapp.analisis.Rima.Tipo.CONSONANTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RimaTest {

    private fun rima(a: String, b: String, seseo: Boolean = false) = Rima.comparar(a, b, seseo)

    @Test fun terminaciones() {
        assertEquals("elo", Rima.terminacion("mirando al cielo")!!.texto)
        assertEquals("ida", Rima.terminacion("nadie la cuida")!!.texto)      // diptongo ui → i
        assertEquals("ántaro", Rima.terminacion("agua del cántaro")!!.texto)
        assertEquals("ao", Rima.terminacion("agua del cántaro")!!.asonante)  // esdrújula: á…o
    }

    @Test fun consonantes() {
        assertEquals(CONSONANTE, rima("cielo", "suelo"))
        assertEquals(CONSONANTE, rima("guerra", "tierra"))
        assertEquals(CONSONANTE, rima("fuego", "juego"))
        assertEquals(CONSONANTE, rima("gente", "fuente"))
        assertEquals(CONSONANTE, rima("hoy", "voy"))
        assertEquals(CONSONANTE, rima("amor", "flor"))
    }

    @Test fun asonantes() {
        assertEquals(ASONANTE, rima("cántaro", "pájaro"))
        assertEquals(ASONANTE, rima("vive", "libre"))      // b = v, pero "br" ≠ "b"
        assertEquals(ASONANTE, rima("fácil", "calle"))     // i final ≈ e
        assertEquals(ASONANTE, rima("Venus", "tenso"))     // u final ≈ o
        assertEquals(ASONANTE, rima("sangre", "hambre"))
    }

    @Test fun noRiman() {
        assertNull(rima("casa", "perro"))
        assertNull(rima("mar", "montaña"))                 // aguda ≠ llana
    }

    @Test fun seseo() {
        assertEquals(ASONANTE, rima("casa", "caza"))
        assertEquals(CONSONANTE, rima("casa", "caza", seseo = true))
    }

    private fun esquema(vararg lineas: String, minusculas: Boolean = false) =
        Rima.esquemaComoTexto(Rima.esquema(lineas.toList(), minusculas = minusculas))

    @Test fun sonetoABBA() {
        assertEquals("ABBA", esquema(
            "Escrito está en mi alma vuestro gesto",
            "y cuanto yo escribir de vos deseo;",
            "vos sola lo escribistes, yo lo leo",
            "tan solo, que aun de vos me guardo en esto."
        ))
    }

    @Test fun romances() {
        assertEquals("-a-a", esquema(
            "Que por mayo era, por mayo,",
            "cuando hace la calor,",
            "cuando los trigos encañan",
            "y están los campos en flor",
            minusculas = true
        ))
        val lorca = Rima.esquema(listOf(
            "Verde que te quiero verde.",
            "Verde viento. Verdes ramas.",
            "El barco sobre la mar",
            "y el caballo en la montaña."
        ))
        assertEquals("-A-A", Rima.esquemaComoTexto(lorca))
        assertEquals(ASONANTE, lorca[3]!!.tipo)
    }

    @Test fun asonanteSeUneAGrupoConsonante() {
        val r = Rima.esquema(listOf("amor", "flor", "", "dolor", "sol"))
        assertEquals("AA AA", Rima.esquemaComoTexto(r))
        assertEquals(CONSONANTE, r[3]!!.tipo)
        assertEquals(ASONANTE, r[4]!!.tipo)
    }
}
