package com.example.ontimego

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransportModeScreen(userName: String ,onNext: (String) -> Unit) {
    var selectedMode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Salut $userName ! Comment te déplaces-tu le plus souvent ?")

        val transportModes = listOf("Voiture", "Bus", "Métro", "Pied", "Vélo")
        transportModes.forEach { mode ->
            Row {
                RadioButton(
                    selected = (selectedMode == mode),
                    onClick = { selectedMode = mode }
                )
                Text(text = mode)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onNext(selectedMode) },
            enabled = selectedMode.isNotBlank()
        ) {
            Text("Suivant")
        }
    }
}
