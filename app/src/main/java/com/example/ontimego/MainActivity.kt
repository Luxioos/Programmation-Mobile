package com.example.ontimego

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
import androidx.compose.ui.unit.dp
import com.example.ontimego.ui.theme.OnTimeGoTheme


class MainActivity : ComponentActivity() {
    private var userName: String? = null
    private var transportMode: String? = null
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = LocationManager(this)

        setContent {
            OnTimeGoTheme {
                var currentScreen by remember { mutableStateOf(0) }
                var userName by remember { mutableStateOf("") }
                var transportMode by remember { mutableStateOf("") }
                var userAddress by remember { mutableStateOf("") }
                var userCity by remember { mutableStateOf("") }
                var userPostalCode by remember { mutableStateOf("") }
                var userCountry by remember { mutableStateOf("Canada") }
                var isSetupComplete by remember { mutableStateOf(false) }

                fun updateUserSettings(
                    newUserName: String,
                    newTransportMode: String,
                    newAddress: String,
                    newCity: String,
                    newPostalCode: String,
                    newCountry: String)
                {
                    userName = newUserName
                    transportMode = newTransportMode
                    userAddress = newAddress
                    userCity = newCity
                    userPostalCode = newPostalCode
                    userCountry = newCountry
                }

                if (!isSetupComplete) {
                    when (currentScreen) {
                        0 -> WelcomeScreen { name ->
                            userName = name
                            currentScreen = 1
                        }
                        1 -> TransportModeScreen(userName) { mode ->
                            transportMode = mode
                            isSetupComplete = true
                        }
                    }
                } else {
                    MainScreen(
                        locationManager = locationManager,
                        userName = userName,
                        transportMode = transportMode,
                        userAddress = userAddress,
                        userCity = userCity,
                        userPostalCode = userPostalCode,
                        userCountry = userCountry,
                        onScreenChange = { currentScreen = it },
                        onUpdateSettings = ::updateUserSettings
                    )
                }
            }
        }

    }
    override fun onStop() {
        super.onStop()
        locationManager.stopLocationUpdates()
    }
}

@Composable
fun MainScreen(
    locationManager: LocationManager,
    userName: String,
    transportMode: String,
    userAddress: String,
    userCity: String,
    userPostalCode: String,
    userCountry: String,
    onScreenChange: (Int) -> Unit,
    onUpdateSettings: (String, String, String, String, String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var events by remember { mutableStateOf(mutableListOf<Event>()) }

    Scaffold(
        topBar = {
            AppTopBar(title = "OnTimeGo")
        },
        bottomBar = {
            NavigationBar(selectedTab) { tab ->
                selectedTab = tab
                onScreenChange(tab)
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreenPage(modifier = Modifier.padding(innerPadding))
            1 -> AddTripScreenPage(modifier = Modifier.padding(innerPadding),
                locationManager = locationManager,
                onRoutesFetched = { fetchedRoutes ->
                    routes = fetchedRoutes
                    selectedTab = 4
                },
                onEventAdded = { newEvent ->
                    events.add(newEvent)
                })
            2 -> ScheduleScreenPage(eventList = events,modifier = Modifier.padding(innerPadding))
            3 -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
                userName = userName,
                transportMode = transportMode,
                address = userAddress,
                city = userCity,
                postalCode = userPostalCode,
                country = userCountry,
                onSave = { name, mode, address, city, postalCode, country ->
                    onUpdateSettings(
                        name,
                        mode,
                        address,
                        city,
                        postalCode,
                        country
                    )
                }
            )
            4 -> ListItineraires(routes = routes, modifier = Modifier.padding(16.dp))
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