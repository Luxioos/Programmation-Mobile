package com.example.ontimego

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ontimego.ui.theme.OnTimeGoTheme
import java.util.*

/**
 * Récupération de la date par l'utilisateur
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker() {
    var selectedDate by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(32.dp)) {
        Text(text = "Sélectionner une date")

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
                        tint = Color.Gray
                    )
                }
            }
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
    }
}

@Composable
fun CustomDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val LightButtonColor = Color(0xFF296A48)
    val DarkButtonColor = Color(0xFF92D5AB)
    val buttonColor = if (isSystemInDarkTheme()) DarkButtonColor else LightButtonColor

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Sélectionner une date")
        },
        text = {
            Column {
                AndroidView(
                    factory = { context ->
                        android.widget.DatePicker(context).apply {
                            init(year, month, day) { _, selectedYear, selectedMonth, selectedDay ->
                                selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                            }
                        }
                    },
                    update = { view ->
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedDate.isNotEmpty()) {
                    onDateSelected(selectedDate)
                }
                onDismissRequest()
            }) {
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
fun CalendarDialog(
    onDateSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    CustomDatePickerDialog(
        onDateSelected = onDateSelected,
        onDismissRequest = onDismissRequest
    )
}

