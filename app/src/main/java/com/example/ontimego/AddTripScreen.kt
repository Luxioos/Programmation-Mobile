package com.example.ontimego

import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Calendar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.Manifest
import android.content.Context
import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Page de l'ajout de trajet sans contenu, vérification de la présence de l'autorisation système d'accès à la localisation
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreenPage(
    locationManager: LocationManager,
    onRoutesFetched: (List<Route>, String) -> Unit,
    defaultAddress: String,
    favoriteTransportMode: String,
    modifier: Modifier = Modifier,
    selectedFrequency: MutableState<String?>,
    name: MutableState<String?>,
    untilDate: MutableState<String?>
    ) { // Renamed function to avoid conflict

    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var isPermissionChecked by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Accès à la localisation autorisé", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            hasLocationPermission = true
        }
        isPermissionChecked = true
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Ajouter un trajet")
        },
        bottomBar = {
            NavigationBar(selectedTab = 1) { }
        }
    ) { paddingValues ->
        Box(modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (!isPermissionChecked) {
                Text(
                    text = "Chargement...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (hasLocationPermission) {
                TripSetter(
                    locationManager,
                    onRoutesFetched = { fetchedRoutes, date ->
                        println("Nom actuel transmis : ${name.value}") // Debug
                        onRoutesFetched(fetchedRoutes, date)
                    },
                    defaultAddress = defaultAddress,
                    defaultTransportMode = favoriteTransportMode,
                    onFrequencySelected = { frequency -> selectedFrequency.value = frequency },
                    onUntilDateSelected = { date -> untilDate.value = date },
                    onNameSelected = { name.value = it }
                )
            }
        }
    }
}

/**
 * Data class Route pour récupérer les infos de l'API et les transmettre aux RouteCards
 */

data class Route(
    val id: Int,
    val longArrivalTime: Long,
    val departureTime: String,
    val distance: String,
    val duration: String,
    val transportDepartTime: String,
    val transportArrivalTime: String,
    val startAddress: String,
    val endAddress: String,
    val startStop: String,
    val endStop: String,
    val direction: String,
    val nbStop: Int,
    val lineNumber: String,
    val vehicleType: String,
    val appointmentTime: String,
    val userDepartureTime: String,
    val frequency: String? = null,
    val untilDate: String? = null,
    val endAddressLat: String? = null,
    val endAddressLng: String? = null,
    val selectedDate: String,
    val name: String? = ""){
    /**
     * Utilisé pour les itinéraires à stocker dans les sharedPreferences
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("departureTime", departureTime)
            put("startAddress", startAddress)
            put("endAddress", endAddress)
            put("direction", direction)
            put("distance", distance)
            put("duration", duration)
            put("endStop", endStop)
            put("lineNumber", lineNumber)
            put("longArrivalTime", longArrivalTime)
            put("nbStop", nbStop)
            put("startStop", startStop)
            put("transportArrivalTime", transportArrivalTime)
            put("transportDepartTime", transportDepartTime)
            put("userDepartureTime", userDepartureTime)
            put("vehicleType", vehicleType)
            put("appointmentTime", appointmentTime)
            put("selectedDate", selectedDate)
            put("name", name)
        }
    }
    companion object {
        fun fromJson(json: JSONObject): Route {
            return Route(
                id = json.optInt("id", 0),
                departureTime = json.optString("departureTime", ""),
                startAddress = json.optString("startAddress", ""),
                endAddress = json.optString("endAddress", ""),
                direction = json.optString("direction", ""),
                distance = json.optString("distance", ""),
                duration = json.optString("duration", ""),
                endStop = json.optString("endStop", ""),
                lineNumber = json.optString("lineNumber", ""),
                longArrivalTime = json.optLong("longArrivalTime", 0L),
                nbStop = json.optInt("nbStop", 0),
                startStop = json.optString("startStop", ""),
                transportArrivalTime = json.optString("transportArrivalTime", ""),
                transportDepartTime = json.optString("transportDepartTime", ""),
                userDepartureTime = json.optString("userDepartureTime", ""),
                vehicleType = json.optString("vehicleType", ""),
                appointmentTime = json.optString("appointmentTime", ""),
                selectedDate = json.optString("selectedDate", ""),
                name = json.optString("name", "")
            )
        }
    }
}

/**
 * Récupération des infos de l'API Google Direction
 */

fun getRoutes(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
    mode: String,
    departureTime: Long,
    selectedTime: String,
    arrivalTime: Long,
    selectedDate: String,
    name: String?,
    onResult: (List<Route>) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {

        val client = OkHttpClient()
        val cle_api ="CLE_API"
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=$originLat,$originLng&destination=$destLat,$destLng&mode=$mode&arrival_time=$arrivalTime&alternatives=true&key=$cle_api"

        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val routes = json.getJSONArray("routes")
                    val routeList = mutableListOf<Route>()

                    if (routes.length() > 0) {
                        for (i in 0 until routes.length()) {
                            val routeJson = routes.getJSONObject(i)
                            //val overviewPolyline = routeJson.getJSONObject("overview_polyline").getString("points")
                            val legs = routeJson.getJSONArray("legs").getJSONObject(0)
                            val distance = legs.getJSONObject("distance").getString("text")


                            val duration = legs.getJSONObject("duration").getString("text")
                            val startAddress = legs.getString("start_address")
                            val endAddress = legs.getString("end_address")

                            var vehicleType = when (mode) {
                                "driving" -> "Voiture"
                                "walking" -> "Marche"
                                "bicycling" -> "Vélo"
                                "transit" -> ""
                                else -> "Inconnu"
                            }

                            if (mode == "transit") {
                                val steps = legs.getJSONArray("steps")
                                var arrivalStop = ""
                                var departStop = ""
                                var arrivalTransportTime = ""
                                var departureTransportTime = ""
                                var direction = ""
                                var nbStop: Int? = null
                                var lineNumber = ""

                                for (j in 0 until steps.length()) {
                                    val step = steps.getJSONObject(j)

                                    if (step.getString("travel_mode") == "TRANSIT") {
                                        val transitDetails = step.getJSONObject("transit_details")
                                        arrivalStop = transitDetails.getJSONObject("arrival_stop").getString("name")
                                        arrivalTransportTime = transitDetails.getJSONObject("arrival_time").getString("text")
                                        departStop = transitDetails.getJSONObject("departure_stop").getString("name")
                                        departureTransportTime = transitDetails.getJSONObject("departure_time").getString("text")
                                        direction = transitDetails.getString("headsign")
                                        nbStop = transitDetails.optInt("num_stops", -1)
                                        val line = transitDetails.getJSONObject("line")
                                        lineNumber = line.optString("short_name", "N/A")
                                        vehicleType = line.getJSONObject("vehicle").getString("name")
                                    }
                                }
                                // Eviter d'afficher les trajets inadaptés proposés par Maps
                                if (nbStop != null && nbStop > 0){
                                    routeList.add(
                                        Route(
                                            id=0,
                                            longArrivalTime = arrivalTime,
                                            departureTime = departureTransportTime,
                                            distance = distance,
                                            duration = duration,
                                            transportDepartTime = departureTransportTime,
                                            transportArrivalTime = arrivalTransportTime,
                                            startAddress = startAddress,
                                            endAddress = endAddress,
                                            startStop = departStop,
                                            endStop = arrivalStop,
                                            direction = direction,
                                            nbStop = nbStop,
                                            lineNumber = lineNumber.toString(),
                                            vehicleType = vehicleType.toString(),
                                            appointmentTime = selectedTime,
                                            userDepartureTime = calculateDepartureTime(
                                                selectedTime,
                                                parseDurationToSeconds(duration),
                                                isTransit = true,
                                                arrivalTime = arrivalTransportTime
                                            ),
                                            selectedDate = selectedDate,
                                            name = name ?: "")
                                    )
                                }
                            } else {
                                // Pour les modes non transit : driving, walking, bicycling
                                routeList.add(
                                    Route(
                                        id = 0,
                                        longArrivalTime = arrivalTime,
                                        departureTime = "",
                                        distance = distance,
                                        duration = duration,
                                        transportDepartTime = "",
                                        transportArrivalTime = "",
                                        startAddress = startAddress,
                                        endAddress = endAddress,
                                        startStop = "",
                                        endStop = "",
                                        direction = "",
                                        nbStop = 0,
                                        lineNumber = "",
                                        vehicleType = vehicleType,
                                        appointmentTime = selectedTime,
                                        userDepartureTime = calculateDepartureTime(
                                            appointmentTime = selectedTime,
                                            durationInSeconds = parseDurationToSeconds(duration),
                                            isTransit = false
                                        ),
                                        selectedDate = selectedDate,
                                        name = name ?: ""
                                    )
                                )
                            }
                        }
                        onResult(routeList)
                    } else {
                        onResult(emptyList())
                    }
                } else {

                    onResult(emptyList())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Récupération de la localisation de l'appareil avec l'API Google Geocode
 */

fun geocodeAddress(address: String, onResult: (Double?, Double?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val cle_api = "CLE_API"
        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${
            address.replace(
                " ",
                "+"
            )
        }&key=$cle_api"

        val request = Request.Builder().url(url).build()
        try {

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val location = json.getJSONArray("results")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONObject("location")
                    val lat = location.getDouble("lat")
                    val lng = location.getDouble("lng")
                    onResult(lat, lng)
                    //onResult(0.1, 0.1)
                } else {
                    onResult(null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Fonction de définition du contenu de la page d'ajout de trajets
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSetter(
    locationManager: LocationManager,
    onRoutesFetched: (List<Route>, String) -> Unit,
    defaultAddress: String,
    defaultTransportMode: String,
    onFrequencySelected: (String) -> Unit,
    onUntilDateSelected: (String) -> Unit,
    onNameSelected: (String) -> Unit) {
    /**
     * Déclaration des variables
     */
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var debounceJob: Job? = null
    var routeName by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val frequence = arrayOf("Unique","Journalier","Hebdomadaire", "Mensuel")
    var selectedFrequency by remember { mutableStateOf(frequence[0]) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var untilDate by remember { mutableStateOf("") }
    var showUntilDatePicker by remember { mutableStateOf(false) }
    val isUntilDateEnabled = selectedFrequency != "Unique"

    val transport = arrayOf("Voiture","Bus","Marche", "Vélo")
    var selectedTransport by remember { mutableStateOf(defaultTransportMode) }
    var expandedTransport by remember {mutableStateOf(false)}

    var selectedDate by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    var selectedAdresse by remember { mutableStateOf(defaultAddress) }
    var isAddressSelected by remember { mutableStateOf(false) }
    var hasInteractedWithAddress by remember { mutableStateOf(false) }
    val addressSuggestions = remember { mutableStateListOf<AddressSuggestion>() }

    var departureAddress by remember { mutableStateOf("") }
    var isDepartureAddressSelected by remember { mutableStateOf(false) }
    var hasInteractedWithDepartureAddress by remember { mutableStateOf(false) }
    val departureAddressSuggestions = remember { mutableStateListOf<AddressSuggestion>() }

    var selectedTime by remember {mutableStateOf("")}
    var showTime by remember {mutableStateOf(false)}

    /**
     * Autorisations système d'accès aux notifications
     */

    var hasNotificationPermission by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasNotificationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Permission de notifications accordée.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Demande d'accès aux notifications
     */

    fun requestNotificationPermission() {
        if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Mise à jour des adresses proposées par l'API Google Places
     */

    LaunchedEffect(selectedAdresse, hasInteractedWithAddress) {
        debounceJob?.cancel()
        debounceJob = launch {
            delay(300)
            if (selectedAdresse.isNotBlank() && !isAddressSelected && hasInteractedWithAddress) {
                locationManager.getCurrentLocation { latitude, longitude ->
                    fetchAddressSuggestions(selectedAdresse, latitude, longitude) { suggestions ->
                        addressSuggestions.clear()
                        addressSuggestions.addAll(suggestions)
                    }
                }
            } else if (isAddressSelected) {
                addressSuggestions.clear()
            }
        }
    }

    LaunchedEffect(departureAddress, hasInteractedWithDepartureAddress) {
        debounceJob?.cancel()
        debounceJob = launch {
            delay(300)
            if (departureAddress.isNotBlank() && !isDepartureAddressSelected && hasInteractedWithDepartureAddress) {
                locationManager.getCurrentLocation { latitude, longitude ->
                    fetchAddressSuggestions(departureAddress, latitude, longitude) { suggestions ->
                        departureAddressSuggestions.clear()
                        departureAddressSuggestions.addAll(suggestions)
                    }
                }
            } else if (isDepartureAddressSelected) {
                departureAddressSuggestions.clear()
            }
        }
    }

    /**
     * UI de la page d'ajout de trajet
     */

    LazyColumn(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        item {
            // Indique à l'utilisateur les champs qui n'ont pas été remplis
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .fillMaxWidth()
                )
            }
            Text("Nom de l'itinéraire")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Entrez un nom") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Sélectionner une date", modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showCalendar = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_calendar_month_24),
                            contentDescription = "Ouvrir le calendrier",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (showCalendar) {
                CalendarDialog(
                    onDateSelected = { date ->
                        selectedDate = date
                        showCalendar = false
                    },
                    onDismissRequest = {
                        showCalendar = false
                    }
                )
            }


            Spacer(modifier = Modifier.height(8.dp))

            /**
             * Entrée de la fréquence de l'itinéraire recherché
             */
            Text("Fréquence du trajet")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedFrequency,
                    onExpandedChange = {
                        expandedFrequency = !expandedFrequency
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedFrequency,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedFrequency,
                        onDismissRequest = { expandedFrequency = false }
                    ) {
                        frequence.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item) },
                                onClick = {
                                    selectedFrequency = item
                                    expandedFrequency = false
                                    onFrequencySelected(item)
                                    if (item == "Unique") {
                                        untilDate = ""
                                        onUntilDateSelected(null.toString())
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = untilDate,
                    onValueChange = { newValue ->
                        untilDate = newValue
                        onUntilDateSelected(newValue)
                    },
                    enabled = isUntilDateEnabled,
                    label = { Text("Jusqu'à") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable (enabled = isUntilDateEnabled) { showUntilDatePicker = true },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (isUntilDateEnabled) showUntilDatePicker = true
                            },
                            enabled = isUntilDateEnabled
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_calendar_month_24),
                                contentDescription = "Sélectionner une date"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )
                if (showUntilDatePicker) {
                    CalendarDialog(
                        onDateSelected = { date ->
                            untilDate = date
                            showUntilDatePicker = false
                            onUntilDateSelected(date)
                        },
                        onDismissRequest = {
                            showUntilDatePicker = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Adresse de départ")
            OutlinedTextField(
                value = departureAddress,
                onValueChange = { query ->
                    departureAddress = query
                    isDepartureAddressSelected = false
                    hasInteractedWithDepartureAddress = true
                },
                label = { Text("Si laissé vide, localisation utilisée") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (departureAddress.isNotBlank()) {
                        IconButton(onClick = {
                            departureAddress = ""
                            departureAddressSuggestions.clear()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_clear_24),
                                contentDescription = "Effacer l'adresse"
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (departureAddressSuggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(departureAddressSuggestions) { suggestion ->
                        Text(
                            text = suggestion.description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    departureAddress = suggestion.description
                                    isDepartureAddressSelected = true
                                    hasInteractedWithDepartureAddress = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Adresse de destination")
            OutlinedTextField(
                value = selectedAdresse,
                onValueChange = { query ->
                    selectedAdresse = query
                    isAddressSelected = false
                    hasInteractedWithAddress = true
                },
                label = { Text("Adresse") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (selectedAdresse.isNotBlank()) {
                        IconButton(onClick = {
                            selectedAdresse = ""
                            addressSuggestions.clear()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_clear_24),
                                contentDescription = "Effacer l'adresse"
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )

            if (addressSuggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(addressSuggestions) { suggestion ->
                        Text(
                            text = suggestion.description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAdresse = suggestion.description
                                    isAddressSelected = true
                                    hasInteractedWithAddress = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Heure")
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Heure") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showTime = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_access_time_24),
                                contentDescription = "Ouvrir la sélection d'heure",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )
                if (showTime) {
                    CustomTimePickerDialog(
                        onTimeSelected = { time ->
                            selectedTime = time
                            showTime = false
                        },
                        onDismissRequest = { showTime = false }
                    )
                }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Transports ")

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedTransport,
                onExpandedChange = {
                    expandedTransport = !expandedTransport
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = selectedTransport,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTransport) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )

                ExposedDropdownMenu(
                    expanded = expandedTransport,
                    onDismissRequest = { expandedTransport = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    transport.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(text = item) },
                            onClick = {
                                selectedTransport = item
                                expandedTransport = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomButtonStyle(
                modifier = Modifier.padding(16.dp),
                onClick = {
                if (name.isNotBlank()) {
                    onNameSelected(name)
                }
                val missingFields = mutableListOf<String>()
                if (selectedDate.isBlank()) missingFields.add("Date")
                if (selectedAdresse.isBlank()) missingFields.add("Adresse de destination")
                if (selectedTime.isBlank()) missingFields.add("Heure")
                if (name.isBlank()) missingFields.add("Nom")
                if (missingFields.isNotEmpty()) {
                    errorMessage = "Veuillez remplir le(s) champ(s) : ${missingFields.joinToString(", ")}"
                } else if (!isDateTimeValid(selectedDate, selectedTime)) {
                    errorMessage = "La date de la recherche demandée est dépassée."
                } else {
                    errorMessage = ""
                    if (!hasNotificationPermission) {
                        requestNotificationPermission()
                    } else {
                        isLoading = true
                        if (departureAddress.isNotBlank() && isDepartureAddressSelected) {
                            // Si une adresse de départ est renseignée et validée
                            geocodeAddress(departureAddress) { originLat, originLng ->
                                if (originLat != null && originLng != null) {
                                    geocodeAddress(selectedAdresse) { destLat, destLng ->
                                        if (destLat != null && destLng != null) {
                                            var mode: String
                                            mode = when (selectedTransport) {
                                                "Voiture" -> "driving"
                                                "Marche" -> "walking"
                                                "Vélo" -> "bicycling"
                                                else -> "transit"
                                            }
                                            val arrivalTime = convertToTimestamp(selectedDate, selectedTime)
                                            getRoutes(
                                                originLat,
                                                originLng,
                                                destLat,
                                                destLng,
                                                mode,
                                                arrivalTime,
                                                selectedTime,
                                                arrivalTime,
                                                selectedDate,
                                                name
                                            ) { routes ->
                                                val uniqueRoutes = removeDuplicateRoutes(routes)
                                                val enrichedRoutes = uniqueRoutes.map { route ->
                                                    route.copy(
                                                        appointmentTime = selectedTime,
                                                        name = name,
                                                        userDepartureTime = calculateDepartureTime(
                                                            appointmentTime = selectedTime,
                                                            durationInSeconds = parseDurationToSeconds(route.duration),
                                                            isTransit = route.vehicleType == "Bus",
                                                            arrivalTime = if (route.vehicleType == "Bus") route.transportArrivalTime else null
                                                        )
                                                    )
                                                }
                                                enrichedRoutes.forEach { route ->
                                                    scheduleNotification(
                                                        routeName = "${route.startAddress} → ${route.endAddress}",
                                                        departureTimeMillis = convertToTimestamp(selectedDate, route.userDepartureTime),
                                                        location = route.startAddress,
                                                        context = context
                                                    )
                                                }
                                                onRoutesFetched(enrichedRoutes, selectedDate)
                                                isLoading = false
                                            }
                                        } else {
                                            isLoading = false
                                            errorMessage = "Adresse de destination introuvable."
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = "Adresse de départ introuvable."
                                }
                            }
                        } else {
                            // Si pas d'adresse de départ entrée
                            locationManager.getCurrentLocation { originLat, originLng ->
                                geocodeAddress(selectedAdresse) { destLat, destLng ->
                                    if (destLat != null && destLng != null) {
                                        var mode: String
                                        if (selectedTransport == "Voiture") mode = "driving"
                                        else if (selectedTransport == "Marche") mode = "walking"
                                        else if (selectedTransport == "Vélo") mode = "bicycling"
                                        else mode = "transit"
                                        val arrivalTime =
                                            convertToTimestamp(selectedDate, selectedTime)
                                        getRoutes(
                                            originLat,
                                            originLng,
                                            destLat,
                                            destLng,
                                            mode,
                                            arrivalTime,
                                            selectedTime,
                                            arrivalTime,
                                            selectedDate,
                                            name = name
                                        ) { routes ->
                                            val uniqueRoutes = removeDuplicateRoutes(routes)
                                            val enrichedRoutes = routes.map { route ->
                                                route.copy(
                                                    appointmentTime = selectedTime,
                                                    name = name,
                                                    userDepartureTime = calculateDepartureTime(
                                                        appointmentTime = selectedTime,
                                                        durationInSeconds = parseDurationToSeconds(
                                                            route.duration
                                                        ),
                                                        isTransit = route.vehicleType == "Bus",
                                                        arrivalTime = if (route.vehicleType == "Bus") route.transportArrivalTime else null
                                                    )
                                                )
                                            }
                                            enrichedRoutes.forEach { route ->
                                                //val departureTimeMillis = convertToTimestamp(selectedDate, route.userDepartureTime)
                                                scheduleNotification(
                                                    routeName = "${route.startAddress} → ${route.endAddress}",
                                                    departureTimeMillis = convertToTimestamp(
                                                        selectedDate,
                                                        route.userDepartureTime
                                                    ),
                                                    location = route.startAddress,
                                                    context = context
                                                )
                                            }
                                            onRoutesFetched(enrichedRoutes, selectedDate)
                                            isLoading = false
                                        }
                                    } else {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }
                }
            },  text = "Voir les trajets")
        }
    }
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {}
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}

/**
 * Convertir une durée en secondes, utilisé pour trouver l'heure de départ conseillée
 */
fun parseDurationToSeconds(duration: String): Long {
    val regex = Regex("(\\d+)\\s*(hour|hours|hr|hrs)?\\s*(\\d+)?\\s*(minute|minutes|min|mins)?")
    val match = regex.find(duration)

    if (match != null) {
        val hours = if (match.groups[2]?.value != null) match.groups[1]?.value?.toLongOrNull() ?: 0L else 0L
        val minutes = if (match.groups[4]?.value != null) match.groups[3]?.value?.toLongOrNull() ?: match.groups[1]?.value?.toLongOrNull() ?: 0L else 0L

        println("Durée analysée : $duration, heures : $hours, minutes : $minutes") // Debug
        return hours * 3600 + minutes * 60
    }
    println("Durée non reconnue : $duration") // Debug
    return 0L
}

/**
 * Utilisé pour convertir le PM AM en format 24h
 */

fun convertTo24HourFormat(time: String): String? {
    return try {
        val cleanedTime = time
            .replace(" ", "")
            .replace("\u202F", "")
            .replace(" ", "")
            .trim()

        val formatter12 = SimpleDateFormat("hh:mma", Locale.getDefault()) // Format 12h sans espace
        val formatter24 = SimpleDateFormat("HH:mm", Locale.getDefault()) // Format 24h

        val parsedTime = formatter12.parse(cleanedTime)
        if (parsedTime != null) {
            formatter24.format(parsedTime)
        } else {
            println("Error parsing time after cleaning: $cleanedTime")
            null
        }
    } catch (e: Exception) {
        println("Exception parsing normalized arrival time: $time")
        null
    }
}

/**
 * Calcul de l'heure de départ conseillée
 */

fun calculateDepartureTime(
    appointmentTime: String,
    durationInSeconds: Long,
    isTransit: Boolean,
    arrivalTime: String? = null
): String {
    val formatter24 = SimpleDateFormat("HH:mm", Locale.getDefault()) // Format 24h
    val calendar = Calendar.getInstance()

    return if (isTransit && arrivalTime != null) {
        val normalizedArrivalTime = convertTo24HourFormat(arrivalTime)
        if (normalizedArrivalTime != null) {
            val arrivalDate = formatter24.parse(normalizedArrivalTime)
            if (arrivalDate != null) {
                calendar.time = arrivalDate
                calendar.add(Calendar.SECOND, -(durationInSeconds.toInt())) // Enlever la durée trajet
                println("Transit departure time calculated: ${formatter24.format(calendar.time)}")
                formatter24.format(calendar.time)
            } else {
                println("Error parsing normalized arrival time: $normalizedArrivalTime")
                "Invalid Arrival Time"
            }
        } else {
            println("Error normalizing arrival time: $arrivalTime")
            "Invalid Arrival Time"
        }
    } else {
        try {
            val appointmentDate = formatter24.parse(appointmentTime)
            if (appointmentDate != null) {
                calendar.time = appointmentDate
                calendar.add(Calendar.SECOND, -(durationInSeconds + 10 * 60).toInt()) // - Durée trajet - 10 min
                println("Standard departure time calculated: ${formatter24.format(calendar.time)}")
                formatter24.format(calendar.time)
            } else {
                println("Error parsing appointment time: $appointmentTime")
                "Invalid Appointment Time"
            }
        } catch (e: Exception) {
            println("Exception parsing appointment time: $appointmentTime")
            "Invalid Appointment Time"
        }
    }
}

/**
 * Fonction pour n'afficher aucun itinéraire en doublon
 */

fun removeDuplicateRoutes(routes: List<Route>): List<Route> {
    return routes.distinctBy { route ->
        "${route.lineNumber}-${route.transportDepartTime}-${route.transportArrivalTime}-${route.startStop}"
    }
}

/**
 * Fonction pour convertir une date String en Date
 */

fun convertToTimestamp(date: String, time: String): Long {
    // Combiner date et heure en une seule chaîne
    val dateTimeString = "$date $time"

    // Adapter le format au type d'entrée
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Définir le fuseau horaire local à l'utilisateur pour le timestamp
    dateFormat.timeZone = TimeZone.getDefault()

    // Convertir la chaîne en objet Date
    val dateTime = dateFormat.parse(dateTimeString)

    // Retourner le timestamp UNIX en secondes
    return dateTime?.time?.div(1000) ?: throw IllegalArgumentException("Date ou heure invalide")
}

/**
 * Pour programmer une notification pour les itinéraires
 */
fun scheduleNotification(routeName: String, departureTimeMillis: Long, location: String, context: Context) {
    val currentTimeMillis = System.currentTimeMillis()
    val delayMillis = departureTimeMillis - currentTimeMillis - TimeUnit.HOURS.toMillis(1)

    if (delayMillis > 0) {
        val workData = Data.Builder()
            .putString("routeName", routeName)
            .putString("departureTime", departureTimeMillis.toString())
            .putString("location", location)
            .build()

        val notificationRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workData)
            .build()

        WorkManager.getInstance(context).enqueue(notificationRequest)
    }
}

/**
 * Vérifier si la date et l'heure entrées par l'utilisateur ne sont pas antérieures aux date et heure actuelles
 */
fun isDateTimeValid(selectedDate: String, selectedTime: String): Boolean {
    val currentDateTime = Calendar.getInstance()
    val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    return try {
        val selectedDateTime = dateTimeFormat.parse("$selectedDate $selectedTime")
        val selectedDateOnly = dateFormat.parse(selectedDate)

        if (selectedDateTime != null) {
            if (selectedDateOnly != null && selectedDateOnly == dateFormat.parse(dateFormat.format(currentDateTime.time))) {
                !selectedDateTime.before(currentDateTime.time)
            } else {
                !selectedDateTime.before(currentDateTime.time)
            }
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

@Composable
fun CustomTimePickerDialog(
    onTimeSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    val buttonColor = if (isSystemInDarkTheme()) Color(0xFF92D5AB) else Color(0xFF296A48)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Sélectionner une heure")
        },
        text = {
            AndroidView(
                factory = { context ->
                    TimePicker(context).apply {
                        setIs24HourView(true)
                        hour = selectedHour
                        minute = selectedMinute
                        setOnTimeChangedListener { _, hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                        }
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    onTimeSelected(formattedTime)
                    onDismissRequest()
                }
            ) {
                Text(text = "OK", color = buttonColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Annuler", color = buttonColor)
            }
        }
    )
}

@Composable
fun CustomButtonStyle(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String
) {
    val isDarkTheme = isSystemInDarkTheme()
    val containerColor = if (isDarkTheme) Color(0xFF003920) else Color(0xFFAEF2C6)
    val contentColor = if (isDarkTheme) Color(0xFF92D5AB) else Color(0xFF002111)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier
    ) {
        Text(text = text)
    }
}


