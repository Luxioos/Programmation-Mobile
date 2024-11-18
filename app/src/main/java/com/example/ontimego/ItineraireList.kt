package com.example.ontimego

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ListItineraires(routes: List<Route>, modifier: Modifier)
{
    Scaffold(
        topBar = {
            AppTopBar(title = "Itinéraires proposés")
        },
        bottomBar = {
            NavigationBar(selectedTab = 4) { } // Menu de navigation
        }
    ) {
        Box(modifier = modifier.padding(it)) {
            ItinerairesContent(routes = routes)
        }
    }
}

@Composable
fun ItinerairesContent(routes: List<Route>)
{
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Itinéraires proposés", style = MaterialTheme.typography.headlineMedium)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(routes) { route ->
                RouteCard(route)
            }
        }
    }
}

@Composable
fun RouteCard(route: Route) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            //Text(text = "Départ: ${route.startAddress}", style = MaterialTheme.typography.headlineSmall)
            //Text(text = "Arrivée: ${route.endAddress}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Distance: ${route.distance}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Durée: ${route.duration}", style = MaterialTheme.typography.bodySmall)
        }
    }
}