package com.example.ontimego

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Page au lancement quand l'utilisateur doit entrer son mode de transport favori
 */
@Composable
fun TransportModeScreen(userName: String ,onNext: (String) -> Unit) {
    var selectedMode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Salut $userName ! Comment te déplaces-tu le plus souvent ?", color = textColor)

        val transportModes = listOf("Voiture", "Bus", "Métro", "Pied", "Vélo")
        transportModes.forEach { mode ->
            Row {
                RadioButton(
                    selected = (selectedMode == mode),
                    onClick = { selectedMode = mode },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color.Blue,
                        unselectedColor = if (isSystemInDarkTheme()) Color.White else Color.DarkGray
                    )
                )
                Text(
                    text = mode,
                    color = textColor,
                    modifier = Modifier.align(Alignment.CenterVertically))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedMode.isNotEmpty()) {
                    val sharedPreferences = context.getSharedPreferences("OnTimeGoPrefs", MODE_PRIVATE)
                    sharedPreferences.edit()
                        .putString("TRANSPORT_MODE", selectedMode)
                        .apply()
                    onNext(selectedMode)
                }
            },
            enabled = selectedMode.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                contentColor = if (isSystemInDarkTheme()) Color.Black else Color.White
            )
        ) {
            Text("Suivant")
        }
    }
    println("SplashActivity - Retrieved TRANSPORT_MODE: $selectedMode")
}
