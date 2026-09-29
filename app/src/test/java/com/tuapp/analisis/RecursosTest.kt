package com.tuapp.analisis

import com.tuapp.analisis.Recursos.Tipo
import com.tuapp.analisis.Recursos.Tipo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecursosTest {

    private fun tipos(vararg lineas: String): List<Tipo> =
        Recursos.detectar(lineas.toList()).map { it.tipo }

    private fun primero(tipo: Tipo, vararg lineas: String) =
        Recursos.detectar(lineas.toList()).first { it.tipo == tipo }

    @Test fun anafora() {
        val r = primero(ANAFORA,
            "Temprano levantó la muerte el vuelo,",
            "temprano madrugó la madrugada,",
            "temprano estás rodando por el suelo.")
        assertEquals(listOf(0, 1, 2), r.lineas)
        assertEquals(listOf("temprano"), r.palabras)
    }

    @Test fun anaforaEpiforaYParalelismo() {
        val t = tipos("te busco en la noche,", "te pienso en la noche,", "te sueño en la noche")
        assertEquals(listOf(ANAFORA, EPIFORA, PARALELISMO), t)
        assertEquals(listOf("en", "la", "noche"),
            primero(EPIFORA, "te busco en la noche,", "te pienso en la noche,").palabras)
    }

    @Test fun anadiplosis() {
        val r = Recursos.detectar(listOf(
            "Todo pasa y todo queda,",
            "pero lo nuestro es pasar,",
            "pasar haciendo caminos,",
            "caminos sobre la mar."
        )).filter { it.tipo == ANADIPLOSIS }
        assertEquals(listOf(listOf(1, 2), listOf(2, 3)), r.map { it.lineas })
    }

    @Test fun epanadiplosis() {
        assertEquals(listOf(EPANADIPLOSIS), tipos("Verde que te quiero verde."))
    }

    @Test fun paralelismo() {
        assertEquals(listOf(PARALELISMO), tipos(
            "Los suspiros son aire y van al aire.",
            "Las lágrimas son agua y van al mar."))
    }

    @Test fun asindetonYPolisindeton() {
        assertEquals(listOf(ASINDETON), tipos("Acude, corre, vuela,"))
        assertEquals(listOf(POLISINDETON), tipos("y el santo y la seña y la voz y la luz"))
    }

    @Test fun geminacionNoEsAsindeton() {
        val t = tipos("Palabras, palabras, palabras")
        assertTrue(GEMINACION in t)
        assertTrue(ASINDETON !in t)
    }

    @Test fun estribillo() {
        val r = primero(ESTRIBILLO,
            "Quédate conmigo esta noche", "que el tiempo se nos va", "",
            "Quédate conmigo esta noche", "no me dejes nunca más")
        assertEquals(listOf(0, 3), r.lineas)
    }

    @Test fun aliteracion() {
        val r = Recursos.detectar(listOf("bajo el ala aleve del leve abanico"))
            .filter { it.tipo == ALITERACION }
        assertEquals(setOf("sonido «b/v» ×4", "sonido «l» ×3"), r.map { it.evidencia }.toSet())
        assertTrue(tipos("en el silencio sólo se escuchaba").contains(ALITERACION))
        assertTrue(tipos("mi mamá me mima mucho").contains(ALITERACION))
    }

    @Test fun aliteracionEntreVersos() {
        // Garcilaso: la "s" se reparte entre los dos versos
        val r = Recursos.detectar(listOf(
            "en el silencio sólo se escuchaba",
            "un susurro de abejas que sonaba"
        )).filter { it.tipo == ALITERACION }
        assertEquals(1, r.size)
        assertEquals(listOf(0, 1), r[0].lineas)
        assertEquals("sonido «s» ×6 en 2 versos", r[0].evidencia)
    }

    @Test fun versosSueltosSeFusionanEnUnaSola() {
        val r = Recursos.detectar(listOf(
            "mis manos buscan tu mirada muda",
            "mientras la madrugada muere mansa"
        )).filter { it.tipo == ALITERACION }
        assertEquals(1, r.size)                       // no tres (verso 1, verso 2, par)
        assertEquals(listOf(0, 1), r[0].lineas)
    }

    @Test fun intensidad() {
        val r = Recursos.detectar(listOf("bajo el ala aleve del leve abanico"))
            .filter { it.tipo == ALITERACION }.associateBy { it.evidencia }
        assertTrue(r["sonido «b/v» ×4"]!!.clara)      // 4,7× lo normal
        assertTrue(!r["sonido «l» ×3"]!!.clara)       // 3,6×: posible
    }

    @Test fun sinFalsosPositivosEntreVersos() {
        assertEquals(emptyList<Tipo>(), tipos(
            "Puedo escribir los versos más tristes esta noche",
            "Escribir, por ejemplo: la noche está estrellada"))
        assertEquals(emptyList<Tipo>(), tipos(
            "Escrito está en mi alma vuestro gesto",
            "y cuanto yo escribir de vos deseo"))
    }

    @Test fun sinFalsosPositivos() {
        assertEquals(emptyList<Tipo>(), tipos(
            "Volverán las oscuras golondrinas",
            "en tu balcón sus nidos a colgar"))
        assertEquals(emptyList<Tipo>(), tipos("Caminante, no hay camino"))
        assertEquals(emptyList<Tipo>(), tipos("Puedo escribir los versos más tristes esta noche"))
    }
}
