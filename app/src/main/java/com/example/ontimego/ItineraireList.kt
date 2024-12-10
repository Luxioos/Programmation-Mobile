package com.example.ontimego

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Liste des itinéraires récupérés à afficher et sélectionnables
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItineraires(
    onViewDetails: (Route) -> Unit,
    onDelete: (Route) -> Unit,
    routes: List<Route>,
    frequency: String?,
    untilDate: String?,
    name: String?,
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
                    var cleanedArrivalTime = route.transportArrivalTime
                        .replace("[^\\x20-\\x7E]".toRegex(), "")
                        .replace(" ", "")
                        .replace("\u202F", "")
                        .replace("\u00A0", "")
                        .trim()
                    if (!cleanedArrivalTime.contains(" ")) {
                        cleanedArrivalTime = cleanedArrivalTime.replace("AM", " AM").replace("PM", " PM")
                    }
                    val timeFormatAMPM = SimpleDateFormat("hh:mm a", Locale.US)
                    val arrivalTime = timeFormatAMPM.parse(cleanedArrivalTime)?.time
                    val timeFormat24H = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val appointmentTime = timeFormat24H.parse(route.appointmentTime)?.time
                    if (arrivalTime != null && appointmentTime != null) {
                        val waitingTime = Math.abs(appointmentTime - arrivalTime)
                        waitingTime
                    } else {
                        Long.MAX_VALUE
                    }
                } catch (e: Exception) {
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
            NavigationBar(selectedTab = 4) { }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                text = "Sélectionnez un itinéraire",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            /**
             * DropDownMenu pour le tri des itinéraires
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val configuration = LocalConfiguration.current
                val screenWidthDp = configuration.screenWidthDp.dp
                Button(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF296A48) else Color(0xFFAEF2C6),
                        contentColor = if (isSystemInDarkTheme()) Color(0xFFAEF2C6) else Color(0xFF296A48))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sortOption,
                            maxLines = 1,
                            color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(id = android.R.drawable.arrow_down_float),
                            contentDescription = "Icône menu déroulant",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    sortOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isSystemInDarkTheme()) Color.White else Color.Black
                                )
                                   },
                            onClick = {
                                sortOption = option
                                expanded = false
                            },
                            modifier = Modifier
                                .background(
                                    if (isSystemInDarkTheme()) Color(0xFF296A48) else Color(0xFFAEF2C6)
                                )
                        )
                    }
                }
        }
            /**
             * Liste des itinéraires
             */
            LazyColumn {
                items(sortedRoutes) { route ->
                    RouteCard(
                        route = route.copy(name = name.orEmpty()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onViewDetails = { selectedRoute ->
                            onViewDetails(
                                selectedRoute.copy(
                                    name = name.orEmpty(),
                                    frequency = frequency,
                                    untilDate = untilDate
                                )
                            )
                        },
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

/**
 * Format et contenu d'une "carte" d'itinéraire
 */
@Composable
fun RouteCard(
    route: Route,
    modifier: Modifier,
    onViewDetails: (Route) -> Unit,
    onDelete: (Route) -> Unit,
    showDeleteIcon: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF1E1F25) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFE2E2E9) else Color.Black
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable {
                onViewDetails(route)
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            if (!route.name.isNullOrBlank() && showDeleteIcon) {
                Text(
                    text = "Nom : ${route.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "Itinéraire",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Transport information for homepage
            if(showDeleteIcon){
                RowInfo(label = "Arrivée :", value = "${route.endAddress}")
                Text(text = "${route.vehicleType} ${route.lineNumber}", style = MaterialTheme.typography.bodyMedium)

                if(route.vehicleType == "Bus") { // Champs nécessaires qu'aux trajets en bus
                    RowInfo(
                        label = "Départ arrêt :",
                        value = "${route.startStop} (${route.transportDepartTime})"
                    )
                }

                RowInfo(
                    label = "Rendez-vous à:",
                    value = "${route.appointmentTime} le ${route.selectedDate}"
                )
                RowInfo(label = "Heure de départ conseillée :", value = "${route.userDepartureTime}")

                Spacer(modifier = Modifier.height(8.dp))

                if(isExpanded){
                    RowInfo(label = "Départ :", value = "${route.startAddress}")
                    RowInfo(label = "Distance :", value = "${route.distance}")
                    RowInfo(label = "Durée :", value = "${route.duration}")
                    if(route.vehicleType == "Bus"){
                        RowInfo(
                            label = "Arrivée arrêt :",
                            value = "${route.endStop} (${route.transportArrivalTime})"
                        )
                        RowInfo(label = "Direction :", value = "${route.direction}")
                        RowInfo(label = "Nombre d'arrêts :", value = "${route.nbStop}")
                    }
                }
            }else{
                RowInfo(label = "Départ :", value = "${route.startAddress}")
                RowInfo(label = "Arrivée :", value = "${route.endAddress}")
                RowInfo(label = "Distance :", value = "${route.distance}")
                RowInfo(label = "Durée :", value = "${route.duration}")
                Text(text = "${route.vehicleType} ${route.lineNumber}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                if(route.vehicleType == "Bus") {
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (showDeleteIcon) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            painter = painterResource(
                                id = if (isExpanded) R.drawable.baseline_keyboard_arrow_up_24 else R.drawable.baseline_keyboard_arrow_down_24
                            ),
                            contentDescription = if (isExpanded) "Réduire" else "Agrandir",
                            tint = textColor
                        )
                    }
                }
            }

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