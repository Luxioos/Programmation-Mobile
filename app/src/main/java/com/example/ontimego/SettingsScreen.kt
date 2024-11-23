package com.example.ontimego

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
    onSave: (String, String, String) -> Unit
) {
    /**
     * Déclaration des variables
     */
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserSettings", Context.MODE_PRIVATE)
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
                fetchAddressSuggestions(query) { suggestions ->
                    addressSuggestions.clear()
                    addressSuggestions.addAll(suggestions)
                }
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

        Button(
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

/**
 * Récupérer les adresses selon ce qui est écrit par l'utilisateur avec l'API Google Places
 */
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