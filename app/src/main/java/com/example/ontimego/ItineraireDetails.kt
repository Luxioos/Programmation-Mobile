package com.example.ontimego

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext

/**
 *  Fonction pour voir les détails sur un itinéraire sélectionné
 */
@Composable
fun ItineraireDetails(
    modifier: Modifier = Modifier,
    selectedRoute: Route,
    onAddRoute: (Route) -> Unit,
    onRemoveRoute: (Route) -> Unit,
    frequency: String?,
    untilDate: String?,
    onScreenChange: (Int) -> Unit,
    sharedPreferences: SharedPreferences,
    locationManager: LocationManager
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            AppTopBar(title = "Itinéraires proposés")
        },
        bottomBar = {
            NavigationBar(selectedTab = 5) { }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val departureTime = formatter.parse(selectedRoute.userDepartureTime)
                    val appointmentTime = formatter.parse(selectedRoute.appointmentTime)
                    if (departureTime != null && appointmentTime != null && departureTime.after(appointmentTime)) {
                        // Affiche un message d'erreur si l'heure de départ est après l'heure de rendez-vous, bus pas disponibles
                        Toast.makeText(
                            context,
                            "Bus pas disponibles à cette heure-ci",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@FloatingActionButton
                    }

                    if (frequency == "Unique") {
                        onAddRoute(selectedRoute)
                        onScreenChange(0)
                    } else if (!untilDate.isNullOrEmpty()) {
                        println("Ajout d'un trajet répété")
                        generateAdditionalRoutes(
                            selectedRoute = selectedRoute,
                            frequency = frequency!!,
                            untilDate = untilDate
                        ) { additionalRoutes ->
                            additionalRoutes.forEach { route ->
                                onAddRoute(route)
                            }
                            onScreenChange(0)
                        }
                    }
                },
                containerColor = if (isSystemInDarkTheme()) Color(0xFF296A48) else Color(0xFFAEF2C6),
                contentColor = if (isSystemInDarkTheme()) Color(0xFFAEF2C6) else Color(0xFF296A48)
            ) {
                Text("Ajouter ce trajet")
            }
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            Text(text = "Détails de l'itinéraire", style = MaterialTheme.typography.headlineMedium)
            Column {
                RouteCard(
                    route = selectedRoute,
                    modifier = Modifier
                        .fillMaxSize(),
                    onViewDetails = {},
                    onDelete = { route ->
                        onRemoveRoute(route)
                    }
                )
            }
        }
    }
}

/**
 *  Générer les autres routes selon la fréquence entrée
 */
fun generateAdditionalRoutes(
    selectedRoute: Route,
    frequency: String,
    untilDate: String,
    onRoutesGenerated: (List<Route>) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val routeList = mutableListOf<Route>()
    if (frequency == "Unique") {
        onRoutesGenerated(listOf(selectedRoute))
        return
    }

    val startDate = dateFormat.parse(selectedRoute.selectedDate)
    val endDate = dateFormat.parse(untilDate)
    if (startDate == null || endDate == null) {
        onRoutesGenerated(emptyList())
        return
    }
    calendar.time = startDate
    while (!calendar.time.after(endDate)) {
        val currentDay = dateFormat.format(calendar.time)
        val newRoute = selectedRoute.copy(selectedDate = currentDay, name = selectedRoute.name)
        println("Route Name : ${selectedRoute.name ?: "null"}")
        routeList.add(newRoute)
        when (frequency) {
            "Journalier" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            "Hebdomadaire" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "Mensuel" -> calendar.add(Calendar.MONTH, 1)
            else -> throw IllegalArgumentException("Fréquence non supportée : $frequency")
        }
    }
    onRoutesGenerated(routeList)
}