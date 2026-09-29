package com.tuapp

import android.app.Application
import com.tuapp.data.NotasRepositorio
import com.tuapp.data.Preferencias
import com.tuapp.data.VersoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Punto de entrada de la app. Hace de contenedor de dependencias sencillo
 * (sin Hilt, para mantener el proyecto ligero mientras crece).
 */
class VersoApp : Application() {

    /** Ámbito que sobrevive a las pantallas: sirve para guardar al salir del editor. */
    val scopeApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val baseDeDatos by lazy { VersoDatabase.crear(this) }
    val repositorio by lazy { NotasRepositorio(baseDeDatos.notaDao()) }
    val preferencias by lazy { Preferencias(this) }
}
