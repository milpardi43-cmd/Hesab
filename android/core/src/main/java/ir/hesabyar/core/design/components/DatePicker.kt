package ir.hesabyar.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText

@Composable
fun JalaliDateField(
    label: String,
    millis: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = HesabyarTheme.colors.onSurfaceSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Surface(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = millis?.let { JalaliDate.formatLongDate(it) } ?: "انتخاب تاریخ",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (millis == null) HesabyarTheme.colors.onSurfaceSecondary else MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
    if (showPicker) {
        JalaliDatePickerDialog(
            initial = millis?.let { JalaliDate.fromEpochMillis(it) } ?: JalaliDate.today(),
            onDismiss = { showPicker = false },
            onConfirm = { date ->
                onSelect(date.toEpochMillis())
                showPicker = false
            }
        )
    }
}

private val SHORT_WEEKDAYS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

@Composable
fun JalaliDatePickerDialog(
    initial: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (JalaliDate) -> Unit
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.month) }
    var day by remember { mutableIntStateOf(initial.day) }
    val today = JalaliDate.today()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("انتخاب تاریخ شمسی", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month -= 1; if (month < 1) { month = 12; year -= 1 } }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "ماه قبل")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = PersianText.toPersianDigits(JalaliDate.monthName(month)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "◂",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.clickable { year -= 1 }.padding(horizontal = 6.dp)
                            )
                            Text(
                                text = PersianText.toPersianDigits(year.toString()),
                                style = MaterialTheme.typography.labelMedium,
                                color = HesabyarTheme.colors.onSurfaceSecondary
                            )
                            Text(
                                text = "▸",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.clickable { year += 1 }.padding(horizontal = 6.dp)
                            )
                        }
                    }
                    IconButton(onClick = { month += 1; if (month > 12) { month = 1; year += 1 } }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "ماه بعد")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    SHORT_WEEKDAYS.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
                val offset = JalaliDate(year, month, 1).weekDayIndex()
                val daysInMonth = JalaliDate.daysInMonth(year, month)
                val totalCells = offset + daysInMonth
                val rows = (totalCells + 6) / 7
                Column {
                    repeat(rows) { rowIndex ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { columnIndex ->
                                val cellIndex = rowIndex * 7 + columnIndex
                                val dayNumber = cellIndex - offset + 1
                                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                    if (dayNumber in 1..daysInMonth) {
                                        val date = JalaliDate(year, month, dayNumber)
                                        val isSelected = dayNumber == day
                                        val isToday = date == today
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { day = dayNumber },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = PersianText.toPersianDigits(dayNumber.toString()),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeDay = day.coerceAtMost(JalaliDate.daysInMonth(year, month))
                    onConfirm(JalaliDate(year, month, safeDay))
                }
            ) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
