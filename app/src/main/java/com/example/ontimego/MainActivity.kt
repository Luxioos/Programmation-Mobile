package com.example.ontimego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
            0 -> AddTripScreenPage(modifier = Modifier.padding(innerPadding))
            1 -> ScheduleScreenPage(modifier = Modifier.padding(innerPadding))
            2 -> SettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun NavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(painterResource(id = R.drawable.add), contentDescription = "Ajouter un trajet", modifier = Modifier.size(24.dp))
            },
            label = { Text("Ajouter un trajet") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = {
                Icon(painterResource(id = R.drawable.calendar), contentDescription = "Emploi du temps", modifier = Modifier.size(24.dp))
            },
            label = { Text("Emploi du temps") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = {
                Icon(painterResource(id = R.drawable.equalizer), contentDescription = "Paramètres", modifier = Modifier.size(24.dp))
            },
            label = { Text("Paramètres") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String) {
    TopAppBar(
        title = {
            Text(text = title)
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