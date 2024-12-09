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
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt


val EventTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Composable représentant un évènement de l'emploi du temps
 */
@Composable
fun BasicEvent(
    event: Event,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(end = 2.dp, bottom = 2.dp)
            .background(event.color, shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Text(
            text = "${event.start.format(EventTimeFormatter)} - ${event.end.format(EventTimeFormatter)}",
        )

        Text(
            text = event.name,
            fontWeight = FontWeight.Bold,
        )

        if (event.description != null) {
            Text(
                text = event.description,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private class EventDataModifier(
    val event: Event,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = event
}

private fun Modifier.eventData(event: Event) = this.then(EventDataModifier(event))

private val DayFormatter = DateTimeFormatter.ofPattern("EE, MMM d")

@Composable
fun BasicDayHeader(
    day: LocalDate,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isHighlighted) MaterialTheme.colorScheme.primary else {
        if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color.Black
    }
    Text(
        text = day.format(DayFormatter),
        textAlign = TextAlign.Center,
        color = if (isHighlighted) MaterialTheme.colorScheme.primary else textColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
    )
}

@Composable
fun ScheduleHeader(
    currentWeekStart: LocalDate,
    minDate: LocalDate,
    maxDate: LocalDate,
    dayWidth: Dp,
    highlightedDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    dayHeader: @Composable (LocalDate, Boolean) -> Unit = { day, isHighlighted ->
        BasicDayHeader(day = day, isHighlighted = isHighlighted)
    },
) {
    Row(modifier = modifier) {
        (0..6).forEach { offset ->
            val currentDate = currentWeekStart.plusDays(offset.toLong())
            Box(modifier = Modifier.width(dayWidth)) {
                dayHeader(currentDate, highlightedDates.contains(currentDate))
            }
        }
    }
}
val HourFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

@Composable
fun BasicSidebarLabel(
    time: LocalTime,
    modifier: Modifier = Modifier,
) {
    Text(
        text = time.format(HourFormatter),
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
    )
}

@Composable
fun ScheduleSidebar(
    hourHeight: Dp,
    modifier: Modifier = Modifier,
    label: @Composable (time: LocalTime) -> Unit = { BasicSidebarLabel(time = it) },
) {
    Column(modifier = modifier) {
        val startTime = LocalTime.MIN
        repeat(24) { i ->
            Box(modifier = Modifier.height(hourHeight)) {
                label(startTime.plusHours(i.toLong()))
            }
        }
    }
}

/**
 * Emploi du temps complet
 */
@Composable
fun Schedule(
    routes: List<Route>,
    modifier: Modifier = Modifier,
    eventContent: @Composable (event: Event) -> Unit = { BasicEvent(event = it) }
) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val today = LocalDate.now()
    var currentWeekStart by remember { mutableStateOf(today.minusDays((today.dayOfWeek.value - 1).toLong())) }

    val events = routes.map { route ->
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = route.selectedDate
        val startTime = parseDateTime(route.selectedDate, route.userDepartureTime)
        val endTime = parseDateTime(route.selectedDate, route.appointmentTime)

        if (startTime == null || endTime == null) {
            throw IllegalArgumentException("Impossible de parser la date ou l'heure pour le trajet : $route")
        }

        Event(
            name = route.endAddress,
            nameR = route.name,
            color = Color(0xFFAFBBF2),
            start = startTime,
            end = endTime,
            depart = route.startAddress,
            description = "Moyen de transport : ${route.vehicleType} ${route.lineNumber}"
        )
    }

    val dayWidth = 256.dp
    val hourHeight = 96.dp
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var sidebarWidth by remember { mutableStateOf(0) }
    val eventDates = events.map { it.start.toLocalDate() }.toSet()

    Column(modifier = modifier) {
        WeekNavigation(
            currentWeekStart = currentWeekStart,
            onNavigatePrevious = { currentWeekStart = currentWeekStart.minusWeeks(1) },
            onNavigateNext = { currentWeekStart = currentWeekStart.plusWeeks(1) }
        )
        ScheduleHeader(
            currentWeekStart = currentWeekStart,
            minDate = currentWeekStart,
            maxDate = currentWeekStart.plusDays(6),
            dayWidth = dayWidth,
            dayHeader = { day: LocalDate, isHighlighted: Boolean ->
                BasicDayHeader(day = day, isHighlighted = isHighlighted)
            },
            highlightedDates = eventDates,
            modifier = Modifier
                .padding(start = with(LocalDensity.current) { sidebarWidth.toDp() })
                .horizontalScroll(horizontalScrollState)
        )
        Row(modifier = Modifier.weight(1f)) {
            ScheduleSidebar(
                hourHeight = hourHeight,
                modifier = Modifier
                    .verticalScroll(verticalScrollState)
                    .onGloballyPositioned { sidebarWidth = it.size.width }
            )
            BasicSchedule(
                events = events,
                currentWeekStart = currentWeekStart,
                eventContent = { event ->
                    BasicEvent(
                        event = event,
                        modifier = Modifier.clickable { selectedEvent = event }
                    )
                },
                dayWidth = dayWidth,
                hourHeight = hourHeight,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
            )
        }
    }
    selectedEvent?.let { event ->
        EventDetailsModal(event = event, onDismiss = { selectedEvent = null })
    }
}

@Composable
fun EventDetailsModal(event: Event, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row{
                    Text(
                        text = "Détails du trajet ",
                        color = Color.Black,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("${event.nameR}", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Départ : ${event.depart}", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                Text("Destination : ${event.name}", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                Text("Heure de départ conseillée: ${event.start.format(EventTimeFormatter)}",color = Color.Black)
                Text("Heure du rendez-vous : ${event.end.format(EventTimeFormatter)}",color = Color.Black)
                event.description?.let {
                    Text(it, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF296A48) else Color(0xFFAEF2C6),
                        contentColor = if (isSystemInDarkTheme()) Color(0xFFAEF2C6) else Color(0xFF296A48)
                    )
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}

@Composable
fun BasicSchedule(
    events: List<Event>,
    currentWeekStart: LocalDate,
    modifier: Modifier = Modifier,
    eventContent: @Composable (event: Event) -> Unit = { BasicEvent(event = it) },
    minDate: LocalDate = events.minByOrNull(Event::start)!!.start.toLocalDate(),
    maxDate: LocalDate = events.maxByOrNull(Event::end)!!.end.toLocalDate(),
    dayWidth: Dp,
    hourHeight: Dp,
) {
    val daysInWeek = (0..6).toList()
    val numDays = ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1
    val dividerColor = Color.LightGray
    Layout(
        content = {
            daysInWeek.forEach { offset ->
                if (offset >= 7) return@forEach
                val dayDate = currentWeekStart.plusDays(offset.toLong())
                val eventsForDay = events.filter { it.start.toLocalDate() == dayDate }

                eventsForDay.forEach { event ->
                    Box(modifier = Modifier.eventData(event)) {
                        eventContent(event)
                    }
                }
            }
        },
        modifier = modifier
            .drawBehind {
                repeat(23) {
                    drawLine(
                        dividerColor,
                        start = Offset(0f, (it + 1) * hourHeight.toPx()),
                        end = Offset(size.width, (it + 1) * hourHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                repeat(numDays - 1) {
                    drawLine(
                        dividerColor,
                        start = Offset((it + 1) * dayWidth.toPx(), 0f),
                        end = Offset((it + 1) * dayWidth.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
    ) { measureables, constraints ->
        val height = hourHeight.roundToPx() * 24
        val width = dayWidth.roundToPx() * 7
        val placeablesWithEvents = measureables.map { measurable ->
            val event = measurable.parentData as Event
            val eventDurationMinutes = ChronoUnit.MINUTES.between(event.start, event.end)
            val eventHeight = ((eventDurationMinutes / 60f) * hourHeight.toPx()).roundToInt()
            val placeable = measurable.measure(constraints.copy(minWidth = dayWidth.roundToPx(), maxWidth = dayWidth.roundToPx(), minHeight = eventHeight, maxHeight = eventHeight))
            Pair(placeable, event)
        }
        layout(width, height) {
            placeablesWithEvents.forEach { (placeable, event) ->
                val eventOffsetMinutes = ChronoUnit.MINUTES.between(LocalTime.MIN, event.start.toLocalTime())
                val eventY = ((eventOffsetMinutes / 60f) * hourHeight.toPx()).roundToInt()
                val eventOffsetDays = ChronoUnit.DAYS.between(currentWeekStart, event.start.toLocalDate()).toInt()
                Log.d("Schedule", "Event Offset Days: $eventOffsetDays for Event: ${event.start}")
                Log.d("Schedule", "Date minimum: $minDate")
                val eventX = eventOffsetDays * dayWidth.roundToPx()
                placeable.place(eventX, eventY)
            }
        }
    }
}

@Composable
fun WeekNavigation(
    currentWeekStart: LocalDate,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigatePrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Semaine précédente")
        }
        Text(
            text = "${currentWeekStart.format(DateTimeFormatter.ofPattern("MMM d"))} - ${currentWeekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}",
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = onNavigateNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Semaine suivante")
        }
    }
}

/**
 * Classe des éléments qui composent l'emploi du temps
 */

data class Event(
    val name: String,
    val depart: String,
    val nameR: String?,
    val color: Color,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val description: String? = null,
)

/**
 * Fonctions utilitaires pour conversions d'heure
 */

fun convertTimestampToISO8601(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val zoneId = ZoneId.systemDefault()

    val localDateTime = instant.atZone(zoneId).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    return localDateTime.format(formatter)
}

fun convertDurationStringToTimestamp(duration: String): Long {
    var totalMillis: Long = 0

    val daysRegex = """(\d+)\s*jours""".toRegex()
    val hoursRegex = """(\d+)\s*hours?""".toRegex()
    val minsRegex = """(\d+)\s*mins?""".toRegex()

    val daysMatch = daysRegex.find(duration)
    if (daysMatch != null) {
        val days = daysMatch.groupValues[1].toLong()
        totalMillis += days * 24 * 60 * 60 * 1000
    }

    val hoursMatch = hoursRegex.find(duration)
    if (hoursMatch != null) {
        val hours = hoursMatch.groupValues[1].toLong()
        totalMillis += hours * 60 * 60 * 1000
    }

    val minsMatch = minsRegex.find(duration)
    if (minsMatch != null) {
        val mins = minsMatch.groupValues[1].toLong()
        totalMillis += mins * 60 * 1000
    }

    return totalMillis
}
fun parseDateTime(date: String, time: String): LocalDateTime? {
    return try {
        val normalizedDate = date.split("/").joinToString("/") { it.padStart(2, '0') }
        val normalizedTime = time.split(":").joinToString(":") { it.padStart(2, '0') }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
        LocalDateTime.parse("$normalizedDate $normalizedTime", formatter)
    } catch (e: Exception) {
        println("Erreur de parsing pour la date : $date et l'heure : $time -> ${e.message}")
        null
    }
}








