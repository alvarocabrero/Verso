package com.tuapp.analisis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricaTest {

    private fun check(verso: String, min: Int, max: Int, metro: Int, silabas: String) {
        val v = Metrica.Verso(verso)
        assertEquals("$verso (mín)", min, v.minimo)
        assertEquals("$verso (máx)", max, v.maximo)
        assertEquals(verso, silabas, v.silabasPara(metro)!!.joinToString("-"))
    }

    @Test fun sinSinalefas() {
        check("Verde que te quiero verde", 8, 8, 8, "Ver-de-que-te-quie-ro-ver-de")
        check("Volverán las oscuras golondrinas", 11, 11, 11,
            "Vol-ve-rán-las-os-cu-ras-go-lon-dri-nas")
    }

    @Test fun sinalefaConH() {
        check("Caminante, no hay camino", 8, 9, 8, "Ca-mi-nan-te-no‿hay-ca-mi-no")
        check("la hierba verde", 5, 5, 5, "la-hier-ba-ver-de")   // hie- no hace sinalefa
    }

    @Test fun leyDelAcentoFinal() {
        check("se hace camino al andar", 8, 10, 8, "se‿ha-ce-ca-mi-no‿al-an-dar")      // aguda +1
        check("quiero yo mi suerte echar", 8, 9, 8, "quie-ro-yo-mi-suer-te‿e-char")    // aguda +1
        check("dame la mano, murciélago", 8, 8, 8, "da-me-la-ma-no-mur-cié-la-go")     // esdrújula −1
    }

    @Test fun dialefaEnVocalTonica() {
        // Garcilaso: se rompe "está‿en" (vocal final tónica) y se mantiene "mi‿alma"
        check("Escrito está en mi alma vuestro gesto", 10, 13, 11,
            "Es-cri-to‿es-tá-en-mi‿al-ma-vues-tro-ges-to")
        check("En tanto que de rosa y azucena", 10, 12, 11,
            "En-tan-to-que-de-ro-sa‿y-a-zu-ce-na")
    }

    @Test fun alejandrino() {
        check("Puedo escribir los versos más tristes esta noche", 14, 15, 14,
            "Pue-do‿es-cri-bir-los-ver-sos-más-tris-tes-es-ta-no-che")
    }

    @Test fun yFinalNoHaceSinalefa() {
        check("Yo soy un hombre sincero", 8, 8, 8, "Yo-soy-un-hom-bre-sin-ce-ro")
    }

    @Test fun metroNoAdmitido() {
        assertNull(Metrica.Verso("Verde que te quiero verde").silabasPara(11))
    }

    @Test fun metroDominanteSoneto() {
        val versos = Metrica.analizarTexto(
            """
            Escrito está en mi alma vuestro gesto
            y cuanto yo escribir de vos deseo;
            vos sola lo escribistes, yo lo leo
            tan solo, que aun de vos me guardo en esto.
            """.trimIndent()
        )
        val metro = Metrica.metroDominante(versos)
        assertEquals(11, metro)
        assertEquals("endecasílabo", Metrica.nombreMetro(metro!!))
    }
}
