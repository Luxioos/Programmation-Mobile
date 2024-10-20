package com.example.ontimego

import android.app.DatePickerDialog
import android.widget.Toast
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
import com.example.ontimego.ui.theme.OnTimeGoTheme
import java.util.*

@Composable
fun ScheduleScreenPage(modifier: Modifier = Modifier) { // Renamed function to avoid conflict
    Scaffold(
        topBar = {
            AppTopBar(title = "Emploi du temps")
        },
        bottomBar = {
            NavigationBar(selectedTab = 2) { } // Menu de navigation
        }
    ) {
        Box(modifier = modifier.padding(it)) {
            DatePicker()
        }
    }
}

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
            CalendarDialog(onDateSelected = { date ->
                selectedDate = date
                showCalendar = false
            })
        }


        }
    }

@Composable
fun CalendarDialog(onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(
        LocalContext.current,
        { _, selectedYear, selectedMonth, selectedDay ->
            val date = "$selectedDay/${selectedMonth + 1}/$selectedYear" // Format de date
            onDateSelected(date)
        },
        year, month, day
    ).show()
}

@Preview(showBackground = true)
@Composable
fun ScheduleScreenPagePreview() {
    OnTimeGoTheme {
        ScheduleScreenPage()
    }
}
