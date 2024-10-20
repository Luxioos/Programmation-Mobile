package com.example.ontimego

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ontimego.ui.theme.OnTimeGoTheme

class MainActivity : ComponentActivity() {
    private var userName: String? = null
    private var transportMode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnTimeGoTheme {
                var currentScreen by remember { mutableStateOf(0) }

                when (currentScreen) {
                    0 -> WelcomeScreen { name ->
                        userName = name
                        currentScreen = 1
                    }
                    1 -> TransportModeScreen(userName ?: "") { mode ->
                        transportMode = mode
                        currentScreen = 2
                    }
                    2 -> LocationPermissionScreen {
                        // L'utilisateur peut passer à la page d'accueil ici
                        currentScreen = 3
                    }
                    3 -> MainScreen() // La page d'accueil
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            AppTopBar(title = "OnTimeGo")
        },
        bottomBar = {
            NavigationBar(selectedTab) { tab ->
                selectedTab = tab
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreenPage(modifier = Modifier.padding(innerPadding))
            1 -> AddTripScreenPage(modifier = Modifier.padding(innerPadding))
            2 -> ScheduleScreenPage(modifier = Modifier.padding(innerPadding))
            3 -> SettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun NavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavigationBarItem(
                icon = {
                    Icon(painterResource(id = R.drawable.home), contentDescription = "Accueil", modifier = Modifier.size(24.dp))
                },
                label = { Text("Accueil") },
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            NavigationBarItem(
                icon = {
                    Icon(painterResource(id = R.drawable.add), contentDescription = "Ajouter un trajet", modifier = Modifier.size(24.dp))
                },
                label = { Text("Ajouter un trajet") },
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            NavigationBarItem(
                icon = {
                    Icon(painterResource(id = R.drawable.calendar), contentDescription = "Emploi du temps", modifier = Modifier.size(24.dp))
                },
                label = { Text( "Agenda") },
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
            NavigationBarItem(
                icon = {
                    Icon(painterResource(id = R.drawable.equalizer), contentDescription = "Paramètres", modifier = Modifier.size(24.dp))
                },
                label = { Text("Paramètres") },
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )
                Text(text = title, modifier = Modifier.align(Alignment.Center))
            }
        }
    )
}



@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Text("Paramètres", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    OnTimeGoTheme {
        MainScreen()
    }
}