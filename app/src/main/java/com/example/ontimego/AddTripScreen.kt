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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Calendar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime
import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreenPage(
    locationManager: LocationManager,
    onRoutesFetched: (List<Route>) -> Unit,
    onEventAdded : (Event) -> Unit,
    modifier: Modifier = Modifier
) { // Renamed function to avoid conflict
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

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
            LaunchedEffect(Unit) {
                if (!permissionRequested) {
                    permissionRequested = true
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        hasLocationPermission = true
                    }
                }
            }

            if (hasLocationPermission) {
                TripSetter(locationManager, onRoutesFetched, onEventAdded)
            } else {
                Text(
                    text = "Autorisation de localisation requise pour ajouter un trajet.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

data class Route(/*val polyline: String, */val distance: String, val duration: String)

fun getRoutes(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
    mode: String,
    departureTime: Long,
    onResult: (List<Route>) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {

        val client = OkHttpClient()
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=$originLat,$originLng&destination=$destLat,$destLng&mode=$mode&departure_time=$departureTime&key="

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

                            routeList.add(Route(/*overviewPolyline,*/distance, duration))
                        }
                        onResult(routeList)
                        /*val routeList = mutableListOf<Route>()
            routeList.add(Route("1 m", "1 s"))
            onResult(routeList)*/
                    } else {
                        onResult(emptyList())
                    }
                } else {
                    /*val routeList = mutableListOf<Route>()
            routeList.add(Route("0 m", "0 s"))
            onResult(routeList)*/
                    onResult(emptyList())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun geocodeAddress(address: String, onResult: (Double?, Double?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {

        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${
            address.replace(
                " ",
                "+"
            )
        }&key="

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSetter(locationManager: LocationManager, onRoutesFetched: (List<Route>) -> Unit,onEventAdded : (Event) -> Unit) {
    val context = LocalContext.current

    val frequence = arrayOf("Unique","Journalier","Jour de la semaine","Week-end","Hebdomadaire")
    var selectedFrequency by remember { mutableStateOf(frequence[0]) }
    var expandedFrequency by remember { mutableStateOf(false) }

    val transport = arrayOf("Voiture","Bus","Metro","Marche")
    var selectedTransport by remember {mutableStateOf(transport[0])}
    var expandedTransport by remember {mutableStateOf(false)}

    var selectedDate by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    var selectedAdresse by remember { mutableStateOf("") }
    var isAddressSelected by remember { mutableStateOf(false) }
    var hasInteractedWithAddress by remember { mutableStateOf(false) }
    val addressSuggestions = remember { mutableStateListOf<AddressSuggestion>() }


    var selectedTime by remember {mutableStateOf("")}
    var showTime by remember {mutableStateOf(false)}

    val scrollState = rememberScrollState()

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

    LazyColumn(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        item {
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
                }
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
                    modifier = Modifier.menuAnchor()
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
            Text("Adresse d'arrivée")
            OutlinedTextField(
                value = selectedAdresse,
                onValueChange = { query ->
                    selectedAdresse = query
                    isAddressSelected = false
                    hasInteractedWithAddress = true
                },
                label = { Text("Adresse") },
                modifier = Modifier.fillMaxWidth(),
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
                }
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
                    modifier = Modifier.menuAnchor()
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
                val newEvent = Event(
                    name = "Google I/O Keynote",
                    color = Color(0xFFAFBBF2),
                    start = LocalDateTime.parse("2021-05-18T09:00:00"),
                    end = LocalDateTime.parse("2021-05-18T11:00:00"),
                    description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
                )
                onEventAdded(newEvent)
                locationManager.getCurrentLocation { originLat, originLng ->
                    geocodeAddress(selectedAdresse) { destLat, destLng ->
                        if (destLat != null && destLng != null) {
                            var mode: String
                            if (selectedTransport == "Voiture") mode = "driving"
                            else if (selectedTransport == "Marche") mode = "walking"
                            else mode = "transit"
                            val departureTime = convertToTimestamp(selectedDate, selectedTime)
                            getRoutes(originLat, originLng, destLat, destLng, mode, departureTime) { routes ->
                                // envoyer les itinéraires à HomeScreen
                                onRoutesFetched(routes)
                                // rajoute le nouveau trajet aux évènements
                                val newEvent = Event(
                                    name = "Google I/O Keynote",
                                    color = Color(0xFFAFBBF2),
                                    start = LocalDateTime.parse("2021-05-18T09:00:00"),
                                    end = LocalDateTime.parse("2021-05-18T11:00:00"),
                                    description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
                                )
                                onEventAdded(newEvent)
                            }
                        }
                    }
                }
            }) {
                Text(text = "Ajouter un trajet")
            }
        }
    }

    }

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


