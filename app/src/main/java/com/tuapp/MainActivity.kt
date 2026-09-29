package com.tuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuapp.ui.editor.EditorScreen
import com.tuapp.ui.notas.NotasScreen
import com.tuapp.ui.theme.VersoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VersoTheme { Navegacion() }
        }
    }
}

@Composable
private fun Navegacion() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "notas") {
        composable("notas") {
            NotasScreen(abrirNota = { id -> nav.navigate("editor/$id") })
        }
        composable(
            route = "editor/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            EditorScreen(alVolver = { nav.navigateUp() })
        }
    }
}
