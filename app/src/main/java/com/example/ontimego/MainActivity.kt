package com.example.ontimego

import android.content.Context
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ontimego.ui.theme.OnTimeGoTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale


class MainActivity : ComponentActivity() {
    private var userName: String? = null
    private var transportMode: String? = null
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = LocationManager(this)

        setContent {
            OnTimeGoTheme {
                /**
                 * Déclaration des variables
                 */
                // Préferences pour sauvegarder les informations de l'utilisateur sur l'appli de SON appareil, elles sont encore là même si il quitte l'appli et revient
                val sharedPreferences: SharedPreferences =
                    getSharedPreferences("OnTimeGoPrefs", Context.MODE_PRIVATE)
                val storedUserName = sharedPreferences.getString("USER_NAME", null)
                val storedTransportMode = sharedPreferences.getString("TRANSPORT_MODE", null)
                val storedUserAddress = sharedPreferences.getString("userAddress", "")
                val isSetupCompleteInitially by remember { mutableStateOf(!storedUserName.isNullOrEmpty() && !storedTransportMode.isNullOrEmpty()) }
                var currentScreen by remember { mutableStateOf(0) }
                var userName by remember { mutableStateOf(storedUserName ?: "") }
                var transportMode by remember { mutableStateOf(storedTransportMode ?: "") }
                var userAddress by remember { mutableStateOf(storedUserAddress ?: "") }
                var isSetupComplete by remember { mutableStateOf(isSetupCompleteInitially) }

                /**
                 * Mise à jour des paramètres utilisateur, nom/mode de transport/adresse
                 */
                fun updateUserSettings(
                    newUserName: String,
                    newTransportMode: String,
                    newAddress: String
                ) {
                    sharedPreferences.edit()
                        .putString("userName", newUserName)
                        .putString("transportMode", newTransportMode)
                        .putString("userAddress", newAddress)
                        .apply()

                    userName = newUserName
                    transportMode = newTransportMode
                    userAddress = newAddress
                }

                if (!isSetupComplete) {
                    when (currentScreen) {
                        0 -> WelcomeScreen { name ->
                            if (name.isNotEmpty()) {
                                sharedPreferences.edit().putString("USER_NAME", name).apply()
                                userName = name
                                currentScreen = 1
                            }
                        }
                        1 -> TransportModeScreen(userName) { mode ->
                            if (mode.isNotEmpty()) {
                                sharedPreferences.edit().putString("TRANSPORT_MODE", mode).apply()
                                transportMode = mode
                                isSetupComplete = true
                            }
                        }
                    }
                } else {
                    MainScreen(
                        locationManager = locationManager,
                        userName = userName,
                        transportMode = transportMode,
                        userAddress = userAddress,
                        sharedPreferences = sharedPreferences,
                        context = this,
                        onScreenChange = { currentScreen = it },
                        onUpdateSettings = { name, mode, address ->
                            sharedPreferences.edit()
                                .putString("USER_NAME", name)
                                .putString("TRANSPORT_MODE", mode)
                                .apply()
                            userName = name
                            transportMode = mode
                        }
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

/**
 * Contenu de la page actuelle vue par l'utilisateur
 */
@Composable
fun MainScreen(
    locationManager: LocationManager,
    userName: String,
    transportMode: String,
    userAddress: String,
    sharedPreferences: SharedPreferences,
    context: Context,
    onScreenChange: (Int) -> Unit,
    onUpdateSettings: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf("") }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) } // trajets proposés
    var selectedRoute = remember { mutableStateOf<Route>(Route(0,0L,"","","","","","","","","","0",0,"","", appointmentTime = "", userDepartureTime = "", selectedDate="")) }

    /**
     * Récupérer les itinéraires stockés dans les sharedPreferences
     */
    fun loadRoutesFromPreferences(sharedPreferences: SharedPreferences): MutableList<Route> {
        val savedRoutesJson = sharedPreferences.getString("USER_ROUTES", "[]")
        val jsonArray = JSONArray(savedRoutesJson)
        val routes = mutableListOf<Route>()

        for (i in 0 until jsonArray.length()) {
            val routeJson = jsonArray.getJSONObject(i)
            routes.add(Route.fromJson(routeJson))
        }

        return routes
    }
    val savedRoutes = remember {
        mutableStateListOf<Route>().apply {
            addAll(loadRoutesFromPreferences(sharedPreferences))
        }
    }

    /**
     * Enregistrer les itinéraires dans les sharedPreferences
     */
    fun saveRoutesToPreferences(routes: List<Route>, sharedPreferences: SharedPreferences) {
        val jsonArray = JSONArray()
        routes.forEach { route ->
            jsonArray.put(route.toJson())
        }
        sharedPreferences.edit()
            .putString("USER_ROUTES", jsonArray.toString())
            .apply()
    }
    fun getRoutesFromPreferences(sharedPreferences: SharedPreferences): List<Route> {
        val gson = Gson()
        val jsonRoutes = sharedPreferences.getString("SAVED_ROUTES", null)
        return if (jsonRoutes != null) {
            val type = object : TypeToken<List<Route>>() {}.type
            gson.fromJson(jsonRoutes, type)
        } else {
            emptyList()
        }
    }

    /**
     * S'occupe de la suppression des itinéraires et de leurs notifications associées
     */
    fun removeRoute(route: Route, sharedPreferences: SharedPreferences, context: Context) {
        savedRoutes.remove(route)
        saveRoutesToPreferences(savedRoutes, sharedPreferences)
        cancelNotification(route, context)
    }

    /**
     * Suppression des itinéraires qui sont passés
     */
    fun removeExpiredRoutes() {
        val currentTimeMillis = System.currentTimeMillis()
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val expiredRoutes = savedRoutes.filter { route ->
            try {
                val routeDateTimeString = "${selectedDate} ${route.appointmentTime}"
                val routeDateTimeMillis = dateTimeFormat.parse(routeDateTimeString)?.time

                routeDateTimeMillis != null && routeDateTimeMillis < currentTimeMillis
            } catch (e: Exception) {
                false
            }
        }
        savedRoutes.removeAll(expiredRoutes)
        saveRoutesToPreferences(savedRoutes, sharedPreferences)
        expiredRoutes.forEach { route ->
            cancelNotification(route, context)
        }
    }

    /**
     * Trier les itinéraires selon leur date et non pas leur ordre d'ajout
     */
    fun sortRoutesByDateAndTime(routes: List<Route>): List<Route> {
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return routes.sortedBy { route ->
            try {
                dateTimeFormat.parse("${route.selectedDate} ${route.appointmentTime}")
            } catch (e: Exception) {
                null
            }
        }
    }

    LaunchedEffect(Unit) {
        val loadedRoutes = getRoutesFromPreferences(sharedPreferences)
        savedRoutes.addAll(loadedRoutes)
    }

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
            0 -> {
                removeExpiredRoutes()
                val sortedRoutes = sortRoutesByDateAndTime(savedRoutes)
                HomeScreenPage(
                modifier = Modifier.padding(innerPadding),
                routes = sortedRoutes,
                selectedDate = selectedDate,
                sharedPreferences = sharedPreferences,
                onRemoveRoute = {
                    route -> removeRoute(route, sharedPreferences, context) },
                context = context
            )}

            1 -> AddTripScreenPage(modifier = Modifier.padding(innerPadding),
                locationManager = locationManager,
                onRoutesFetched = { fetchedRoutes, date ->
                    routes = fetchedRoutes
                    selectedDate = date
                    selectedTab = 4 },
                defaultAddress = userAddress,
                favoriteTransportMode = transportMode,
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
                onDelete = { route ->
                    removeRoute(route, sharedPreferences, context)
                },
                routes = routes,
                modifier = Modifier.padding(16.dp))
            5 -> ItineraireDetails(
                modifier = Modifier.padding(16.dp),
                selectedRoute = selectedRoute.value,
                onAddRoute = { route ->
                    savedRoutes.add(route)
                    saveRoutesToPreferences(savedRoutes, sharedPreferences)
                    selectedTab = 0
                },
                sharedPreferences = sharedPreferences,
                onRemoveRoute = { route -> removeRoute(route, sharedPreferences, context) }
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
                            Text("Ajouter un trajet", style = MaterialTheme.typography.bodySmall,textAlign = TextAlign.Center)
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