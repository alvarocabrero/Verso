package com.tuapp.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tuapp.VersoApp
import com.tuapp.ui.audios.AudiosScreen
import com.tuapp.ui.notes.NotesScreen

/** Home: two sections, notes and audios, switched from the bottom bar. The last one is remembered. */
@Composable
fun HomeScreen(openNote: (Long) -> Unit) {
    val preferences = (LocalContext.current.applicationContext as VersoApp).preferences
    val tab = preferences.homeTab

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { preferences.updateHomeTab(0) },
                    icon = { Icon(if (tab == 0) Icons.AutoMirrored.Filled.Notes else Icons.AutoMirrored.Outlined.Notes, null) },
                    label = { Text("Notas") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { preferences.updateHomeTab(1) },
                    icon = { Icon(if (tab == 1) Icons.Filled.Headphones else Icons.Outlined.Headphones, null) },
                    label = { Text("Audios") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (tab == 1) AudiosScreen(openNote = openNote) else NotesScreen(openNote = openNote)
        }
    }
}
