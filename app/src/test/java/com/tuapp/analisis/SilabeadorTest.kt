package com.tuapp.analisis

import com.tuapp.analisis.Silabeador.TipoAcentual.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SilabeadorTest {

    private fun check(palabra: String, esperado: String) =
        assertEquals(palabra, esperado, Silabeador.silabear(palabra).joinToString("-"))

    @Test fun basicas() {
        check("casa", "ca-sa"); check("sol", "sol"); check("examen", "e-xa-men")
    }

    @Test fun digrafos() {
        check("perro", "pe-rro"); check("chocolate", "cho-co-la-te"); check("guerra", "gue-rra")
    }

    @Test fun uMudaYDieresis() {
        check("queso", "que-so"); check("guitarra", "gui-ta-rra")
        check("pingüino", "pin-güi-no"); check("averigüéis", "a-ve-ri-güéis")
    }

    @Test fun diptongosYTriptongos() {
        check("ciudad", "ciu-dad"); check("cuidado", "cui-da-do"); check("aire", "ai-re")
        check("Europa", "Eu-ro-pa"); check("huevo", "hue-vo"); check("construí", "cons-truí")
        check("buey", "buey"); check("Uruguay", "U-ru-guay"); check("muy", "muy")
    }

    @Test fun hiatos() {
        check("río", "rí-o"); check("maíz", "ma-íz"); check("poeta", "po-e-ta")
        check("leer", "le-er"); check("aéreo", "a-é-re-o"); check("oído", "o-í-do")
        check("Raúl", "Ra-úl"); check("chiita", "chi-i-ta"); check("búho", "bú-ho")
    }

    @Test fun yGriega() {
        check("reyes", "re-yes"); check("hoy", "hoy"); check("y", "y")
    }

    @Test fun gruposConsonanticos() {
        check("abrazo", "a-bra-zo"); check("acción", "ac-ción"); check("inflamar", "in-fla-mar")
        check("obstáculo", "obs-tá-cu-lo"); check("instrumento", "ins-tru-men-to")
        check("transplante", "trans-plan-te"); check("atleta", "at-le-ta")
        check("ahumar", "a-hu-mar")
    }

    @Test fun tipoAcentual() {
        assertEquals(AGUDA, Silabeador.analizar("corazón").tipo)
        assertEquals(AGUDA, Silabeador.analizar("reloj").tipo)
        assertEquals(AGUDA, Silabeador.analizar("sol").tipo)
        assertEquals(LLANA, Silabeador.analizar("casa").tipo)
        assertEquals(LLANA, Silabeador.analizar("árbol").tipo)
        assertEquals(LLANA, Silabeador.analizar("examen").tipo)
        assertEquals(ESDRUJULA, Silabeador.analizar("murciélago").tipo)
        assertEquals(SOBRESDRUJULA, Silabeador.analizar("dígamelo").tipo)
    }

    @Test fun palabrasDeVerso() {
        assertEquals(
            listOf("Verde", "que", "te", "quiero", "verde"),
            Silabeador.palabrasDe("Verde que te quiero verde.")
        )
    }
}
