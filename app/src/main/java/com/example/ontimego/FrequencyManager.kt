package com.example.ontimego

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestion des fréquences d'itinéraires et de la répétition jusqu'à une date donnée
 */
object FrequencyManager {

    /**
     * Génère les itinéraires hebdomadaires et mensuels
     */
    fun generateWeeklyOrMonthlyRoutes(
        locationManager: LocationManager,
        destLat: Double,
        destLng: Double,
        mode: String,
        startDate: String,
        endDate: String,
        appointmentTime: String,
        frequency: String,
        onRoutesGenerated: (List<Route>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val routeList = mutableListOf<Route>()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)
            if (start == null || end == null) {
                onRoutesGenerated(emptyList())
                return@launch
            }

            calendar.time = start
            val (originLat, originLng) = getCurrentLocationSuspended(locationManager)
            while (!calendar.time.after(end)) {
                val currentDay = dateFormat.format(calendar.time)
                val arrivalTime = convertToTimestamp(currentDay, appointmentTime)
                try {
                    val dailyRoutes = getRoutesForDate(
                        originLat = originLat,
                        originLng = originLng,
                        destLat = destLat,
                        destLng = destLng,
                        mode = mode,
                        appointmentTime = appointmentTime,
                        arrivalTime = arrivalTime,
                        selectedDate = currentDay
                    )
                    if (dailyRoutes.isNotEmpty()) {
                        routeList.addAll(dailyRoutes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                when (frequency) {
                    "Hebdomadaire" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    "Mensuel" -> calendar.add(Calendar.MONTH, 1)
                    else -> throw IllegalArgumentException("Fréquence non supportée : $frequency")
                }
            }
            onRoutesGenerated(routeList)
        }
    }

    /**
     * Appelle l'API pour récupérer les itinéraires pour une date donnée
     */
    private suspend fun getRoutesForDate(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        mode: String,
        appointmentTime: String,
        arrivalTime: Long,
        selectedDate: String
    ): List<Route> = suspendCancellableCoroutine { continuation ->
        getRoutes(
            originLat = originLat,
            originLng = originLng,
            destLat = destLat,
            destLng = destLng,
            mode = mode,
            departureTime = arrivalTime,
            selectedTime = appointmentTime,
            arrivalTime = arrivalTime,
            selectedDate = selectedDate
        ) { routes ->
            continuation.resume(routes)
        }

        continuation.invokeOnCancellation {
            println("Coroutine annulée pour la date : $selectedDate")
        }
    }

    /**
     * Récupère la localisation actuelle de manière suspendue
     */
    private suspend fun getCurrentLocationSuspended(locationManager: LocationManager): Pair<Double, Double> =
        suspendCancellableCoroutine { continuation ->
            locationManager.getCurrentLocation { lat, lng ->
                continuation.resume(Pair(lat, lng))
            }
        }

    /**
     * Convertit une date et une heure en timestamp UNIX
     */
    private fun convertToTimestamp(date: String, time: String): Long {
        val dateTimeString = "$date $time"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        val dateTime = dateFormat.parse(dateTimeString)
        return dateTime?.time?.div(1000) ?: throw IllegalArgumentException("Date ou heure invalide")
    }

    /**
     * Appelle l'API Google Maps pour récupérer les itinéraires
     */
    private fun getRoutes(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        mode: String,
        departureTime: Long,
        selectedTime: String,
        arrivalTime: Long,
        selectedDate: String,
        onResult: (List<Route>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {

            val client = OkHttpClient()
            val apiKey = "CLE_API"
            val url =
                "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=$originLat,$originLng" +
                        "&destination=$destLat,$destLng" +
                        "&mode=$mode" +
                        "&arrival_time=$arrivalTime" +
                        "&alternatives=true" +
                        "&key=$apiKey"

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

                                    if (nbStop != null && nbStop > 0) {
                                        routeList.add(
                                            Route(
                                                id = 0,
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
                                                lineNumber = lineNumber,
                                                vehicleType = vehicleType,
                                                appointmentTime = selectedTime,
                                                userDepartureTime = calculateDepartureTime(
                                                    appointmentTime = selectedTime,
                                                    durationInSeconds = parseDurationToSeconds(duration),
                                                    isTransit = true,
                                                    arrivalTime = arrivalTransportTime
                                                ),
                                                selectedDate = selectedDate
                                            )
                                        )
                                    }
                                } else {
                                    // Gestion pour les modes "driving", "walking", "bicycling"
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
                                            selectedDate = selectedDate
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
                onResult(emptyList())
            }
        }
    }}

fun getRoutesDaily(
    selectedRoute: Route,
    untilDate: String,
    onRoutesGenerated: (List<Route>) -> Unit
) {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val startDate = formatter.parse(selectedRoute.selectedDate)
    val endDate = formatter.parse(untilDate)

    if (startDate != null && endDate != null && !startDate.after(endDate)) {
        val routes = mutableListOf<Route>()
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        while (!calendar.time.after(endDate)) {
            val newRoute = selectedRoute.copy(
                selectedDate = formatter.format(calendar.time)
            )
            routes.add(newRoute)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        onRoutesGenerated(routes)
    } else {
        onRoutesGenerated(emptyList())
    }
}
