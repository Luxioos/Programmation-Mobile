package com.example.ontimego

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun ItineraireDetails(modifier: Modifier = Modifier, selectedRoute: Route, onAddRoute: (Route) -> Unit)
{
    Scaffold(
        topBar = {
            AppTopBar(title = "Itinéraires proposés")
        },
        bottomBar = {
            NavigationBar(selectedTab = 5) { } // Menu de navigation
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onAddRoute(selectedRoute)
                }
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
                    onViewDetails = {}
                )
            }
        }
    }
}