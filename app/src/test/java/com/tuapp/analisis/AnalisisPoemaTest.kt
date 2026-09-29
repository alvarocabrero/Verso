package com.tuapp.analisis

import com.tuapp.analisis.Recursos.Tipo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalisisPoemaTest {

    private val lope = """
        Un soneto me manda hacer Violante,
        que en mi vida me he visto en tanto aprieto;
        catorce versos dicen que es soneto;
        burla burlando van los tres delante.
    """.trimIndent()

    private val machado = """
        Todo pasa y todo queda,
        pero lo nuestro es pasar,
        pasar haciendo caminos,
        caminos sobre la mar.
    """.trimIndent()

    private fun textos(r: AnalisisPoema.Resultado, rangos: List<IntRange>) = rangos.map { r.texto.substring(it) }

    @Test fun endecasilabosConRimaAbrazada() {
        val r = AnalisisPoema.analizar(lope)
        assertEquals(11, r.metro)
        assertEquals(listOf(11, 11, 11, 11), r.lineas.map { it.silabas })
        assertTrue(r.lineas.all { it.encaja })
        assertEquals("ABBA", r.esquema)
    }

    @Test fun arteMenorEnMinusculas() {
        val r = AnalisisPoema.analizar(machado)
        assertEquals(8, r.metro)
        assertEquals("-a-a", r.esquema)
    }

    @Test fun versoQueNoEncaja() {
        val r = AnalisisPoema.analizar("$lope\nsoneto")
        assertEquals(11, r.metro)
        assertFalse(r.lineas.last().encaja)
        assertEquals(3, r.lineas.last().silabas)
    }

    @Test fun posicionesYEstrofas() {
        val r = AnalisisPoema.analizar("hola\n\nadiós")
        assertEquals(listOf(0..4, 5..5, 6..11), r.lineas.map { it.inicio..it.fin })
        assertNull(r.lineas[1].silabas)
        assertNull(r.lineas[1].rima)
        assertEquals("- -", r.esquema)
    }

    @Test fun textoVacio() {
        val r = AnalisisPoema.analizar("")
        assertNull(r.metro)
        assertEquals("", r.esquema)
        assertTrue(r.recursos.isEmpty())
    }

    @Test fun resaltaAnadiplosis() {
        val r = AnalisisPoema.analizar(machado)
        val recurso = r.recursos.first { it.tipo == ANADIPLOSIS }
        assertEquals(listOf("pasar", "pasar"), textos(r, AnalisisPoema.rangos(r, recurso)))
    }

    @Test fun resaltaAnaforaSoloAlInicio() {
        val r = AnalisisPoema.analizar("te busco y te llamo,\nte pienso y te sueño,\nte quiero y te nombro")
        val recurso = r.recursos.first { it.tipo == ANAFORA }
        val rangos = AnalisisPoema.rangos(r, recurso)
        assertEquals(listOf("te", "te", "te"), textos(r, rangos))
        assertEquals(listOf(0, 21, 43), rangos.map { it.first })
    }

    @Test fun resaltaVersoEnteroSinPalabras() {
        val r = AnalisisPoema.analizar("¡Ojos, boca, manos, pelo!")
        val recurso = r.recursos.first { it.tipo == ASINDETON }
        assertEquals(listOf("Ojos, boca, manos, pelo"), textos(r, AnalisisPoema.rangos(r, recurso)))
    }

    @Test fun resaltaGeminacion() {
        val r = AnalisisPoema.analizar("Verde que te quiero verde, verde, verde")
        val recurso = r.recursos.first { it.tipo == GEMINACION }
        assertEquals(listOf("verde", "verde", "verde"), textos(r, AnalisisPoema.rangos(r, recurso)))
    }
}
