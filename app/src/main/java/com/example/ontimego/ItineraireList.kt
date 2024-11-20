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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ListItineraires(
    onViewDetails: (Route) -> Unit,
    routes: List<Route>,
    modifier: Modifier = Modifier)
{
    var selectedRoute = remember { mutableStateOf<Route?>(null) }
    Scaffold(
        topBar = {
            AppTopBar(title = "Itinéraires proposés")
        },
        bottomBar = {
            NavigationBar(selectedTab = 4) { } // Menu de navigation
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            Text(text = "Sélectionnez un itinéraire", style = MaterialTheme.typography.headlineMedium)
            LazyColumn {
                items(routes) { route ->
                    RouteCard(
                        route = route,
                        modifier = Modifier
                            .fillMaxWidth(),
                        onViewDetails
                    )
                }
            }
        }
    }
}

@Composable
fun RouteCard(route: Route, modifier: Modifier, onViewDetails: (Route) -> Unit) {
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "${route.vehicleType} ${route.lineNumber}", style = MaterialTheme.typography.bodyMedium)
            RowInfo(label = "Départ arrêt :", value = "${route.startStop} (${route.transportDepartTime})")
            RowInfo(label = "Arrivée arrêt :", value = "${route.endStop} (${route.transportArrivalTime})")
            RowInfo(label = "Direction :", value = "${route.direction}")
            RowInfo(label = "Nombre d'arrêts :", value = "${route.nbStop}")

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