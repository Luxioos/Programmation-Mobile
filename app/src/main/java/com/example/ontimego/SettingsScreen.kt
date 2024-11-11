package com.example.ontimego

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    userName: String,
    transportMode: String,
    onSave: (String, String) -> Unit
) {
    var editableUserName by remember { mutableStateOf(userName) }
    var editableTransportMode by remember { mutableStateOf(transportMode) }
    var expandedTransport by remember { mutableStateOf(false) }
    var showSaveMessage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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

        Button(
            onClick = {
                onSave(editableUserName, editableTransportMode)
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