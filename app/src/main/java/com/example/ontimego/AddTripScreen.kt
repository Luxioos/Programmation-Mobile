package com.example.ontimego

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreenPage(modifier: Modifier = Modifier) { // Renamed function to avoid conflict
    Scaffold(
        topBar = {
            AppTopBar(title = "Ajouter un trajet")
        },
        bottomBar = {
            NavigationBar(selectedTab = 1) { } // Menu de navigation
        }
    ) {
        Box(modifier = modifier.padding(it)) {
        }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {

                TripSetter()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSetter(){
    val context = LocalContext.current

    val frequence = arrayOf("Unique","Journalier","Jour de la semaine","Week-end","Hebdomadaire")
    var selectedFrequency by remember { mutableStateOf(frequence[0]) }
    var expandedFrequency by remember { mutableStateOf(false) }

    val transport = arrayOf("Voiture","Bus","Metro","Marche")
    var selectedTransport by remember {mutableStateOf(transport[0])}
    var expandedTransport by remember {mutableStateOf(false)}

    var selectedDate by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    var selectedAdresse by remember {mutableStateOf("")}

    var selectedTime by remember {mutableStateOf("")}
    var showTime by remember {mutableStateOf(false)}

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Sélectionner une date")
    Column(modifier = Modifier.padding(16.dp)) {

        OutlinedTextField(
            value = selectedDate,
            onValueChange = { selectedDate = it },
            label = { Text("Date") },
            modifier = Modifier.padding(top = 16.dp),
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
            }
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
        Spacer(modifier = Modifier.height(8.dp))
        Text("Adresse")
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = selectedAdresse,
            onValueChange = { selectedAdresse = it },
            label = { Text("Adresse") },
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Heure")
        Spacer(modifier = Modifier.height(8.dp))


        OutlinedTextField(
            value = selectedTime,
            onValueChange = { selectedTime = it },
            label = { Text("Heure") },
            modifier = Modifier.padding(top = 16.dp),
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
                true  // formay 24h
            ).show()
        }



        Spacer(modifier = Modifier.height(8.dp))
        Text("Transports ")
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expandedTransport,
            onExpandedChange = {
                expandedTransport = !expandedTransport
            }
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

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {}) {
            Text(text = "Ajouter un trajet")
        }
    }
}


