package com.example.ontimego

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItineraires(
    onViewDetails: (Route) -> Unit,
    onDelete: (Route) -> Unit,
    routes: List<Route>,
    modifier: Modifier = Modifier)
{
    var selectedRoute = remember { mutableStateOf<Route?>(null) }
    var sortOption by remember { mutableStateOf("Durée") }
    val sortOptions = listOf("Distance", "Durée", "Heure d'arrivée")
    var sortedRoutes by remember { mutableStateOf(routes) }
    var expanded by remember { mutableStateOf(false) }

    /**
     * Trier les itinéraires proposés
     */
    fun sortRoutes(routes: List<Route>, option: String): List<Route> {
        return when (option) {
            "Distance" -> routes.sortedBy { route ->
                route.distance.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: Double.MAX_VALUE
            }
            "Durée" -> routes.sortedBy { route ->
                parseDurationToSeconds(route.duration)
            }
            "Heure d'arrivée" -> routes.sortedBy { route ->
                try {
                    val arrivalTimeMatch = Regex("""\d{1,2}:\d{2}\s[AP]M""").find(route.transportArrivalTime)
                    val rawArrivalTime = arrivalTimeMatch?.value
                    if (rawArrivalTime != null) {
                        val cleanedArrivalTime = rawArrivalTime.replace(" ", "").replace("\u202F", "").trim()
                        val timeFormatAMPM = SimpleDateFormat("hh:mm a", Locale.US) // Forcer Locale.US
                        val timeFormat24H = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val arrivalTime = timeFormatAMPM.parse(cleanedArrivalTime)?.time
                        val appointmentTime = timeFormat24H.parse(route.appointmentTime)?.time
                        if (arrivalTime != null && appointmentTime != null) {
                            val waitingTime = Math.abs(appointmentTime - arrivalTime)
                            println("Waiting Time (ms): $waitingTime")
                            waitingTime
                        } else {
                            Long.MAX_VALUE
                        }
                    } else {
                        Long.MAX_VALUE
                    }
                } catch (e: Exception) {
                    println("Error parsing times for route: ${route.transportArrivalTime}")
                    Long.MAX_VALUE
                }
            }
            else -> routes
        }
    }
    LaunchedEffect(sortOption) {
        sortedRoutes = sortRoutes(routes, sortOption)
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Itinéraires proposés")
        },
        bottomBar = {
            NavigationBar(selectedTab = 4) { } // Menu de navigation
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sélectionnez un itinéraire",
                    style = MaterialTheme.typography.headlineMedium
                )

                /**
                 * DropDownMenu pour le tri des itinéraires
                 */
                Box {
                    Button(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.width(150.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = sortOption)
                            Icon(
                                painter = painterResource(id = android.R.drawable.arrow_down_float),
                                contentDescription = "Icône menu déroulant"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    sortOption = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn {
                items(sortedRoutes) { route ->
                    RouteCard(
                        route = route,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onViewDetails = onViewDetails,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun RouteCard(
    route: Route,
    modifier: Modifier,
    onViewDetails: (Route) -> Unit,
    onDelete: (Route) -> Unit,
    showDeleteIcon: Boolean = false
) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable {
                onViewDetails(route)
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Itinéraire",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Transport information
            RowInfo(label = "Départ :", value = "${route.startAddress}")
            RowInfo(label = "Arrivée :", value = "${route.endAddress}")
            RowInfo(label = "Distance :", value = "${route.distance}")
            RowInfo(label = "Durée :", value = "${route.duration}")
            Text(text = "${route.vehicleType} ${route.lineNumber}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            if(route.vehicleType == "Bus") { // Champs nécessaires qu'aux trajets en bus
                RowInfo(
                    label = "Départ arrêt :",
                    value = "${route.startStop} (${route.transportDepartTime})"
                )
                RowInfo(
                    label = "Arrivée arrêt :",
                    value = "${route.endStop} (${route.transportArrivalTime})"
                )
                RowInfo(label = "Direction :", value = "${route.direction}")
                RowInfo(label = "Nombre d'arrêts :", value = "${route.nbStop}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            RowInfo(
                label = "Rendez-vous à:",
                value = "${route.appointmentTime} le ${route.selectedDate}"
            )
            RowInfo(label = "Heure de départ conseillée :", value = "${route.userDepartureTime}")

            // Afficher l'icone pour effacer l'itineraire seulement si il a été  ajouté
            if (showDeleteIcon) {
                IconButton(onClick = { onDelete(route) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_delete_24),
                        contentDescription = "Supprimer",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun RowInfo(label: String, value: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
    }
}