package com.example.ontimego

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    userName: String,
    transportMode: String,
    address: String,
    city: String,
    postalCode: String,
    country: String,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var editableUserName by remember { mutableStateOf(userName) }
    var editableTransportMode by remember { mutableStateOf(transportMode) }
    var editableAddress by remember { mutableStateOf(address) }
    var editableCity by remember { mutableStateOf(city) }
    var editablePostalCode by remember { mutableStateOf(postalCode) }
    var editableCountry by remember { mutableStateOf(country) }
    var expandedTransport by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val addressSuggestions = remember { mutableStateListOf<AddressSuggestion>() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isAddressSelected by remember { mutableStateOf(false) }
    var hasInteractedWithAddress by remember { mutableStateOf(false) }

    fun onAddressSelected(suggestion: AddressSuggestion) {
        fetchPlaceDetails(suggestion.placeId) { street, city, postal, country ->
            editableAddress = street
            editableCity = city
            editablePostalCode = postal
            editableCountry = country
        }
        addressSuggestions.clear()
        isAddressSelected = true
        hasInteractedWithAddress = false
    }

    LaunchedEffect(editableAddress, hasInteractedWithAddress) {
        if (editableAddress.isNotBlank() && !isAddressSelected && hasInteractedWithAddress) {
            fetchAddressSuggestions(editableAddress) { suggestions ->
                addressSuggestions.clear()
                addressSuggestions.addAll(suggestions)
            }
        } else {
            addressSuggestions.clear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Paramètres du profil", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editableUserName,
            onValueChange = { editableUserName = it },
            label = { Text("Nom") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Mode de transport", style = MaterialTheme.typography.bodyLarge)
        ExposedDropdownMenuBox(
            expanded = expandedTransport,
            onExpandedChange = { expandedTransport = !expandedTransport }
        ) {
            OutlinedTextField(
                value = editableTransportMode,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTransport) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedTransport,
                onDismissRequest = { expandedTransport = false }
            ) {
                listOf("Voiture", "Bus", "Métro", "Pied", "Vélo").forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(text = mode) },
                        onClick = {
                            editableTransportMode = mode
                            expandedTransport = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = editableAddress,
            onValueChange = { query ->
                editableAddress = query
                isAddressSelected = false
                hasInteractedWithAddress = true
                fetchAddressSuggestions(query) { suggestions ->
                    addressSuggestions.clear()
                    addressSuggestions.addAll(suggestions)
                }
            },
            label = { Text("Adresse") },
            modifier = Modifier.fillMaxWidth()
        )

        if (addressSuggestions.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(addressSuggestions) { suggestion ->
                    Text(
                        text = suggestion.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddressSelected(suggestion)
                            }
                            .padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editableCity,
            onValueChange = { editableCity = it },
            label = { Text("Ville") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editablePostalCode,
            onValueChange = { editablePostalCode = it },
            label = { Text("Code Postal") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editableCountry,
            onValueChange = { editableCountry = it },
            label = { Text("Pays") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(
                    editableUserName,
                    editableTransportMode,
                    editableAddress,
                    editableCity,
                    editablePostalCode,
                    editableCountry
                )
                coroutineScope.launch {
                    val snackbarJob = launch {
                        snackbarHostState.showSnackbar("Sauvegarde réussie !")
                    }
                    delay(1000)
                    snackbarJob.cancel()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sauvegarder")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        )
    }
}

data class AddressSuggestion(val description: String, val placeId: String)

fun fetchAddressSuggestions(query: String, onSuggestionsFetched: (List<AddressSuggestion>) -> Unit) {
    val client = OkHttpClient()
    val apiKey = "CLE_API"
    val encodedQuery = query.replace(" ", "%20")
    val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=$encodedQuery&key=$apiKey"
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onSuggestionsFetched(emptyList())
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val predictions = json.getJSONArray("predictions")
                val suggestions = mutableListOf<AddressSuggestion>()

                for (i in 0 until minOf(predictions.length(), 10)) {
                    val prediction = predictions.getJSONObject(i)
                    val description = prediction.getString("description")
                    val placeId = prediction.getString("place_id")
                    suggestions.add(AddressSuggestion(description, placeId))
                }
                onSuggestionsFetched(suggestions)
            } else {
                onSuggestionsFetched(emptyList())
            }
        }
    })
}

fun fetchPlaceDetails(placeId: String, onDetailsFetched: (String, String, String, String) -> Unit) {
    val client = OkHttpClient()
    val apiKey = "CLE_API"
    val url = "https://maps.googleapis.com/maps/api/place/details/json?place_id=$placeId&key=$apiKey"

    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onDetailsFetched("", "", "", "")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val addressComponents = json.getJSONObject("result").getJSONArray("address_components")

                var streetAddress = ""
                var city = ""
                var postalCode = ""
                var country = ""

                for (i in 0 until addressComponents.length()) {
                    val component = addressComponents.getJSONObject(i)
                    val types = component.getJSONArray("types")

                    when {
                        "street_number" in types.toString() -> {
                            streetAddress = component.getString("long_name") + " $streetAddress"
                        }
                        "route" in types.toString() -> {
                            streetAddress += component.getString("long_name")
                        }
                        "locality" in types.toString() -> {
                            city = component.getString("long_name")
                        }
                        "postal_code" in types.toString() -> {
                            postalCode = component.getString("long_name")
                        }
                        "country" in types.toString() -> {
                            country = component.getString("long_name")
                        }
                    }
                }
                onDetailsFetched(streetAddress, city, postalCode, country)
            } else {
                onDetailsFetched("", "", "", "")
            }
        }
    })
}

