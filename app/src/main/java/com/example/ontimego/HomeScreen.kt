package com.example.ontimego

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreenPage(locationManager: LocationManager, routes: List<Route>, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            AppTopBar(title = "Accueil")
        },
        bottomBar = {
            NavigationBar(selectedTab = 0) { } // Menu de navigation
        }
    ) {
        Box(modifier = modifier.padding(it)) {
            HomeContent(locationManager, routes)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(locationManager: LocationManager, routes: List<Route>) {

    /* //Obtenir la latitude et la longitude
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(Unit) {
        locationManager.getCurrentLocation { lat, lon ->
            latitude = lat
            longitude = lon
        }
    }*/
    if (routes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titre
            Text(
                text = "Accueil",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Texte sous le titre
            Text(
                text = "Prochain trajet",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Encadré
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Vous n'avez aucun trajet de prévu",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
    else {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Liste des itinéraires", style = MaterialTheme.typography.headlineMedium)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(routes) { route ->
                    RouteCard(route)
                }
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