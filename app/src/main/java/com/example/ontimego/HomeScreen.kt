package com.example.ontimego

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreenPage(
    modifier: Modifier = Modifier,
    routes: List<Route>,
    sharedPreferences: SharedPreferences,
    selectedDate: String,
    onRemoveRoute: (Route) -> Unit,
    context: Context
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(routes, selectedDate) {
        scheduleNotificationForRoutes(routes, selectedDate, context)
    }
    Scaffold(
        topBar = {
            AppTopBar(title = "Accueil")
        },
        bottomBar = {
            NavigationBar(selectedTab = 0) { }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Box(modifier = modifier.padding(it)) {
            HomeContent(routes, onRemoveRoute, context, snackbarHostState, coroutineScope)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    routes: List<Route>,
    onRemoveRoute: (Route) -> Unit,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titre
            Text(
                text = "Accueil",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Prochain(s) trajet(s)",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (routes.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Vous n'avez aucun trajet de prévu",
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(routes) { route ->
                        RouteCard(
                            route = route,
                            modifier = Modifier.fillMaxWidth(),
                            onViewDetails = { },
                            onDelete = {
                                onRemoveRoute(route)
                                cancelNotification(route, context)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Suppression de l'itinéraire réussie")
                                } },
                            showDeleteIcon = true
                        )
                    }
                }
            }
        }
}

/**
 * Programmer une notification à chaque ajout d'itinéraire
 */
fun scheduleNotificationForRoutes(routes: List<Route>, selectedDate: String, context: android.content.Context) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()

    val sharedPreferences = context.getSharedPreferences("OnTimeGoPrefs", Context.MODE_PRIVATE)
    val plannedNotifications = sharedPreferences.getStringSet("PLANNED_NOTIFICATIONS", mutableSetOf()) ?: mutableSetOf()

    routes.forEach { route ->
        val appointmentTime = route.appointmentTime.trim()
        try {
            val completeAppointmentTime = "${selectedDate.trim()} ${appointmentTime.trim()}"

            val appointmentDate = dateFormat.parse(completeAppointmentTime)
            val appointmentTimeMillis = appointmentDate?.time ?: return@forEach

            val currentTimeMillis = System.currentTimeMillis()
            val delayMillis = appointmentTimeMillis - currentTimeMillis - TimeUnit.HOURS.toMillis(1)

            val notificationId = "${selectedDate}_${appointmentTime}_${route.endAddress}"

            if (!plannedNotifications.contains(notificationId) && delayMillis > 0){
                plannedNotifications.add(notificationId)
                sharedPreferences.edit()
                    .putStringSet("PLANNED_NOTIFICATIONS", plannedNotifications)
                    .apply()

                val workData = Data.Builder()
                    .putString("endAddress", route.endAddress)
                    .putString("appointmentTime", appointmentTime)
                    .build()

                val notificationRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .addTag(route.generateNotificationTag())
                    .setInputData(workData)
                    .build()

                WorkManager.getInstance(context).enqueue(notificationRequest)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Fonctions pour la supression de notifications programmées lors de la suppression d'un itinéraire
 */
fun Route.generateNotificationTag(): String {
    return "Notification_${this.startAddress}_${this.endAddress}_${this.userDepartureTime}"
}
fun cancelNotification(route: Route, context: Context) {
    val tag = route.generateNotificationTag()
    WorkManager.getInstance(context).cancelAllWorkByTag(tag)
}


