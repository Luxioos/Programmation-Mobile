package com.example.ontimego

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    userName: String,
    transportMode: String,
    address: String,
    onSave: (String, String, String) -> Unit
) {
    /**
     * Déclaration des variables
     */
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("OnTimeGoPrefs", Context.MODE_PRIVATE)

    var editableUserName by remember { mutableStateOf(userName) }
    var editableTransportMode by remember { mutableStateOf(transportMode) }
    var editableAddress by remember { mutableStateOf(address) }
    var expandedTransport by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val addressSuggestions = remember { mutableStateListOf<AddressSuggestion>() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var isAddressSelected by remember { mutableStateOf(false) }
    var hasInteractedWithAddress by remember { mutableStateOf(false) }
    var addressQueryDelay by remember { mutableStateOf(0L) }

    val locationProvider = LocationServices.getFusedLocationProviderClient(context)
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var isPermissionChecked by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Accès à la localisation autorisé", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Quand une adresse est cliquée/sélectionnée dans la liste des suggestions
     */
    fun onAddressSelected(suggestion: AddressSuggestion) {
        editableAddress = suggestion.description
        addressSuggestions.clear()
        isAddressSelected = true
        hasInteractedWithAddress = false
    }

    /**
     * Sauvegarder les infos entrées par l'utilisateur
     */
    fun saveUserSettings() {
        val editor = sharedPreferences.edit()
        editor.putString("userName", editableUserName)
        editor.putString("transportMode", editableTransportMode)
        editor.putString("userAddress", editableAddress)
        editor.apply()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            hasLocationPermission = true
        }
        isPermissionChecked = true
    }
    if(hasLocationPermission){
        LaunchedEffect(editableAddress, hasInteractedWithAddress, currentLatitude, currentLongitude) {
            if (System.currentTimeMillis() - addressQueryDelay > 300) {
                addressQueryDelay = System.currentTimeMillis()
                if (editableAddress.isNotBlank() && !isAddressSelected && hasInteractedWithAddress) {
                    fetchAddressSuggestions(
                        query = editableAddress,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        radius = 100000
                    ) { suggestions ->
                        addressSuggestions.clear()
                        addressSuggestions.addAll(suggestions)
                    }
                } else {
                    addressSuggestions.clear()
                }
            }
        }
    }

    /**
     * Rediriger vers les paramètres systèmes de l'application
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Contenu de la page
     */
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Paramètres du profil", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = editableUserName,
            onValueChange = { editableUserName = it },
            label = { Text("Nom") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                    .menuAnchor(),
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

        Spacer(modifier = Modifier.height(32.dp))

        Text("Lieu principal de travail ou d'études", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = editableAddress,
            onValueChange = { query ->
                editableAddress = query
                isAddressSelected = false
                hasInteractedWithAddress = true
                if (currentLatitude != null && currentLongitude != null) {
                    fetchAddressSuggestions(
                        query = query,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        radius = 100000
                    ) { suggestions ->
                        addressSuggestions.clear()
                        addressSuggestions.addAll(suggestions)
                    }
                }else {
                    // Appel sans localisation si non disponible
                    fetchAddressSuggestions(
                        query = query,
                        latitude = null,
                        longitude = null,
                        radius = 100000
                    ) { suggestions ->
                        addressSuggestions.clear()
                        addressSuggestions.addAll(suggestions)
                    }
                }
            },
            label = { Text("Adresse") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (editableAddress.isNotEmpty()) {
                    IconButton(onClick = {
                        editableAddress = ""
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

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Autorisations",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                openAppSettings(context)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.fleche_grise),
                    contentDescription = "Ouvrir les paramètres de l'application"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        CustomButtonStyle(
            onClick = {
                onSave(editableUserName, editableTransportMode, editableAddress)
                saveUserSettings()
                coroutineScope.launch {
                    val snackbarJob = launch {
                        snackbarHostState.showSnackbar("Sauvegarde réussie !")
                    }
                    delay(1000)
                    snackbarJob.cancel()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            text = "Sauvegarder")

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        )
    }
}

data class AddressSuggestion(val description: String, val placeId: String)

/**
 * Récupérer les adresses selon ce qui est écrit par l'utilisateur avec l'API Google Places
 */
fun fetchAddressSuggestions(
    query: String,
    latitude: Double?,
    longitude: Double?,
    radius: Int = 100000, // en metres
    onSuggestionsFetched: (List<AddressSuggestion>) -> Unit
) {
    val client = OkHttpClient()
    val apiKey = "CLE_API"
    val encodedQuery = query.replace(" ", "%20")
    val locationParam = if (latitude != null && longitude != null) "&location=$latitude,$longitude" else ""
    val radiusParam = if (latitude != null && longitude != null) "&radius=$radius" else ""
    val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=$encodedQuery$locationParam$radiusParam&key=$apiKey"

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

                for (i in 0 until minOf(predictions.length(), 5)) {
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
