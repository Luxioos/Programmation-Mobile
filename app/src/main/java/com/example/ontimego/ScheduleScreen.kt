package com.example.ontimego

import androidx.compose.material3.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.padding

import androidx.compose.runtime.Composable

import java.time.LocalDateTime

/**
 * Page de l'emploi du temps
 */
@Composable
fun ScheduleScreenPage(routes : List<Route>,modifier: Modifier = Modifier) { // Renamed function to avoid conflict
    Scaffold(
        topBar = {
            AppTopBar(title = "Emploi du temps")
        },
        bottomBar = {
            NavigationBar(selectedTab = 2) { } // Menu de navigation
        }
    ) {

        Box(modifier = modifier.padding(it)) {
            Surface() {
                if(routes.size != 0) {
                    Schedule(routes = routes)
                } else {
                    Text("Aucun trajet enregistré")
                }
            }
        }
    }
}








