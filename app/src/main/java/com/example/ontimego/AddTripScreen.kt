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
import androidx.compose.foundation.rememberScrollState
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
import java.time.LocalDateTime
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Page de l'ajout de trajet sans contenu, vérification de la présence de l'autorisation système d'accès à la localisation
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreenPage(
    locationManager: LocationManager,
    onRoutesFetched: (List<Route>) -> Unit,
    defaultAddress: String,
    modifier: Modifier = Modifier) { // Renamed function to avoid conflict
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
        } else {
            Toast.makeText(context, "Accès à la localisation refusé", Toast.LENGTH_SHORT).show()
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
                TripSetter(locationManager, onRoutesFetched, defaultAddress = defaultAddress)
            }
        }
    }
}

/**
 * Data class Route pour récupérer les infos de l'API et les transmettre aux RouteCards
 */

data class Route(
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
    val userDepartureTime: String)

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
    onResult: (List<Route>) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {

        val client = OkHttpClient()
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=$originLat,$originLng&destination=$destLat,$destLng&mode=$mode&arrival_time=$arrivalTime&alternatives=true&key=CLE_API"

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
                                var nbStop = 0
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
                                        nbStop = transitDetails.getInt("num_stops")
                                        val line = transitDetails.getJSONObject("line")
                                        lineNumber = line.optString("short_name", "N/A")
                                        vehicleType = line.getJSONObject("vehicle").getString("name")
                                    }
                                }

                                routeList.add(
                                    Route(
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
                                        lineNumber = lineNumber,
                                        vehicleType = vehicleType,
                                        appointmentTime = selectedTime,
                                        userDepartureTime = calculateDepartureTime(
                                            selectedTime,
                                            parseDurationToSeconds(duration),
                                            isTransit = true,
                                            arrivalTime = arrivalTransportTime
                                        )
                                    )
                                )
                            } else {
                                // Pour les modes non transit : driving, walking, bicycling
                                routeList.add(
                                    Route(
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
                                        )
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

        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${
            address.replace(
                " ",
                "+"
            )
        }&key=CLE_API"

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
    onRoutesFetched: (List<Route>) -> Unit,
    defaultAddress: String) {
    /**
     * Déclaration des variables
     */
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val frequence = arrayOf("Unique","Journalier","Jour de la semaine","Week-end","Hebdomadaire")
    var selectedFrequency by remember { mutableStateOf(frequence[0]) }
    var expandedFrequency by remember { mutableStateOf(false) }

    val transport = arrayOf("Voiture","Transport en commun","Marche", "Vélo")
    var selectedTransport by remember {mutableStateOf(transport[0])}
    var expandedTransport by remember {mutableStateOf(false)}

    var selectedDate by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    var selectedAdresse by remember { mutableStateOf(defaultAddress) }
    var isAddressSelected by remember { mutableStateOf(false) }
    var hasInteractedWithAddress by remember { mutableStateOf(false) }
    val addressSuggestions = remember { mutableStateListOf<AddressSuggestion>() }

    var selectedTime by remember {mutableStateOf("")}
    var showTime by remember {mutableStateOf(false)}

    /**
     * Autorisations système d'accès aux notifications
     */

    var hasNotificationPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permission de notifications refusée.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Demande d'accès aux notifications
     */

    fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
        }
    }

    /**
     * Mise à jour des adresses proposées par l'API Google Places
     */

    LaunchedEffect(selectedAdresse, hasInteractedWithAddress) {
        if (selectedAdresse.isNotBlank() && !isAddressSelected && hasInteractedWithAddress) {
            fetchAddressSuggestions(selectedAdresse) { suggestions ->
                addressSuggestions.clear()
                addressSuggestions.addAll(suggestions)
            }
        } else if (isAddressSelected) {
            addressSuggestions.clear()
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
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            Text(text = "Sélectionner une date")
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                label = { Text("Date") },
                modifier = Modifier,
                trailingIcon = {
                    IconButton(onClick = { showCalendar = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar),
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
                CalendarDialog(onDateSelected = { date ->
                    selectedDate = date
                    showCalendar = false
                })
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Fréquence du trajet")
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedFrequency,
                onExpandedChange = {
                    expandedFrequency = !expandedFrequency
                },
            ) {
                TextField(
                    value = selectedFrequency,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                    modifier = Modifier.menuAnchor(),
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
                            }
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
                modifier = Modifier,
                trailingIcon = {
                    IconButton(onClick = { showTime = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.logo),
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

            Spacer(modifier = Modifier.height(16.dp))
            if (showTime) {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                        showTime = false
                    },
                    hour,
                    minute,
                    true  // format 24h
                ).show()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Transports ")

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedTransport,
                onExpandedChange = {
                    expandedTransport = !expandedTransport
                },
            ) {
                TextField(
                    value = selectedTransport,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTransport) },
                    modifier = Modifier.menuAnchor(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )

                ExposedDropdownMenu(
                    expanded = expandedTransport,
                    onDismissRequest = { expandedTransport = false }
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

            Button(onClick = {
                // Trouver les champs qui n'ont pas été remplis
                val missingFields = mutableListOf<String>()
                if (selectedDate.isBlank()) missingFields.add("Date")
                if (selectedAdresse.isBlank()) missingFields.add("Adresse")
                if (selectedTime.isBlank()) missingFields.add("Heure")
                if (missingFields.isNotEmpty()) {
                    errorMessage = "Veuillez remplir le(s) champ(s) : ${missingFields.joinToString(", ")}"
                } else {
                    errorMessage = ""
                    isLoading = true
                    requestNotificationPermission()
                    if (hasNotificationPermission) {
                    locationManager.getCurrentLocation { originLat, originLng ->
                        geocodeAddress(selectedAdresse) { destLat, destLng ->
                            if (destLat != null && destLng != null) {
                                var mode: String
                                if (selectedTransport == "Voiture") mode = "driving"
                                else if (selectedTransport == "Marche") mode = "walking"
                                else if (selectedTransport == "Vélo") mode = "bicycling"
                                else mode = "transit"
                                val arrivalTime = convertToTimestamp(selectedDate, selectedTime)
                                //val departureTime = convertToTimestamp(selectedDate, selectedTime)
                                getRoutes(
                                    originLat,
                                    originLng,
                                    destLat,
                                    destLng,
                                    mode,
                                    arrivalTime,
                                    selectedTime,
                                    arrivalTime
                                ) { routes ->
                                    val uniqueRoutes = removeDuplicateRoutes(routes)
                                    val enrichedRoutes = uniqueRoutes.map { route ->
                                        route.copy(
                                            appointmentTime = selectedTime,
                                            userDepartureTime = calculateDepartureTime(
                                                appointmentTime = selectedTime,
                                                durationInSeconds = parseDurationToSeconds(route.duration),
                                                isTransit = route.vehicleType == "Bus",
                                                arrivalTime = if (route.vehicleType == "Bus") route.transportArrivalTime else null
                                            )
                                        )
                                    }
                                    onRoutesFetched(enrichedRoutes)
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Veuillez autoriser les notifications pour continuer.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            }) {
                Text(text = "Voir les trajets")
            }
        }
    }
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {}
        ) {
            CircularProgressIndicator( // Roue de chargement lors de la récupération des itinéraires
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
