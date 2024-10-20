package com.example.ontimego

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreenPage(modifier: Modifier = Modifier) { // Renamed function to avoid conflict
    Scaffold(
        topBar = {
            AppTopBar(title = "Accueil")
        },
        bottomBar = {
            NavigationBar(selectedTab = 0) { } // Menu de navigation
        }
    ) {
        Box(modifier = modifier.padding(it)) {
            HomeContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent() {
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
                    text = "Vous n'avez aucun trajet de prévu.",
                    fontSize = 14.sp
                )
            }
        }

    }
}