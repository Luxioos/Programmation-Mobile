package com.example.ontimego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import kotlin.reflect.KFunction3


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
                var isSetupComplete by remember { mutableStateOf(false) }

                fun updateUserSettings(
                    newUserName: String,
                    newTransportMode: String,
                    newAddress: String
                ) {
                    userName = newUserName
                    transportMode = newTransportMode
                    userAddress = newAddress
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
    onScreenChange: (Int) -> Unit,
    onUpdateSettings: KFunction3<String, String, String, Unit>
) {
    var selectedTab by remember { mutableStateOf(0) }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) } // trajets proposés
    var savedRoutes = remember { mutableStateListOf<Route>() } // trajets ajoutés
    var selectedRoute = remember { mutableStateOf<Route>(Route("","","","","","","","","","",0,"","")) }

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
            0 -> HomeScreenPage(modifier = Modifier.padding(innerPadding), routes = savedRoutes)
            1 -> AddTripScreenPage(modifier = Modifier.padding(innerPadding),
                locationManager = locationManager,
                onRoutesFetched = { fetchedRoutes ->
                    routes = fetchedRoutes
                    selectedTab = 4 },
                defaultAddress = userAddress
            )
            2 -> ScheduleScreenPage(routes = savedRoutes,modifier = Modifier.padding(innerPadding))
            3 -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
                userName = userName,
                transportMode = transportMode,
                address = userAddress,
                onSave = { name, mode, address  ->
                    onUpdateSettings(
                        name,
                        mode,
                        address
                    )
                }
            )
            4 -> ListItineraires(
                onViewDetails = { route ->
                    selectedRoute.value = route
                    selectedTab = 5
                },
                routes = routes,
                modifier = Modifier.padding(16.dp))
            5 -> ItineraireDetails(
                modifier = Modifier.padding(16.dp),
                selectedRoute = selectedRoute.value,
                onAddRoute = { route ->
                    savedRoutes.add(route)
                    selectedTab = 0
                }
            )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(id = R.drawable.home),
                            contentDescription = "Accueil",
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Accueil", style = MaterialTheme.typography.bodySmall)
                    }
                },
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            NavigationBarItem(
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painterResource(id = R.drawable.add),
                                contentDescription = "Ajouter un trajet",
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Ajouter un trajet", style = MaterialTheme.typography.bodySmall)
                        }
                    },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
            )
            NavigationBarItem(
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(id = R.drawable.calendar),
                            contentDescription = "Emploi du temps",
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Agenda", style = MaterialTheme.typography.bodySmall)
                    }
                },
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
            NavigationBarItem(
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(id = R.drawable.equalizer),
                            contentDescription = "Paramètres",
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Paramètres", style = MaterialTheme.typography.bodySmall)
                    }
                },
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