package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme
import kotlinx.datetime.*

/**
 * Date selection sheet with a calendar-style picker.
 *
 * Features:
 * - Month navigation
 * - Day of week headers
 * - Selected date highlighting
 * - Past dates disabled
 * - Current date indicator
 *
 * @param title The title to display at the top
 * @param selectedDate The currently selected date, if any
 * @param onSelect Callback when a date is selected
 * @param onDismiss Callback when the sheet is dismissed
 */
@Composable
fun DateSelectionSheet(
    title: String,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val typography = VelocityTheme.typography
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    var displayedMonth by remember {
        mutableStateOf(selectedDate ?: today)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = VelocityColors.BackgroundDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = typography.timeBig
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = VelocityColors.TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        displayedMonth = displayedMonth.minus(1, DateTimeUnit.MONTH)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = VelocityColors.Accent
                    )
                }

                Text(
                    text = formatMonthYear(displayedMonth),
                    style = typography.body.copy(color = VelocityColors.TextMain)
                )

                IconButton(
                    onClick = {
                        displayedMonth = displayedMonth.plus(1, DateTimeUnit.MONTH)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = VelocityColors.Accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of week headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        style = typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val daysInMonth = getDaysInMonth(displayedMonth)
            val firstDayOfWeek = getFirstDayOfWeek(displayedMonth)

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Empty cells for days before the first of the month
                items(firstDayOfWeek) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }

                // Day cells
                items(daysInMonth) { dayIndex ->
                    val day = dayIndex + 1
                    val date = LocalDate(displayedMonth.year, displayedMonth.month, day)
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val isPast = date < today

                    DayCell(
                        day = day,
                        isSelected = isSelected,
                        isToday = isToday,
                        isPast = isPast,
                        onClick = {
                            if (!isPast) {
                                onSelect(date)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val typography = VelocityTheme.typography

    val backgroundColor = when {
        isSelected -> VelocityColors.Accent
        isToday -> VelocityColors.GlassBg
        else -> androidx.compose.ui.graphics.Color.Transparent
    }

    val textColor = when {
        isSelected -> VelocityColors.BackgroundDeep
        isPast -> VelocityColors.Disabled
        else -> VelocityColors.TextMain
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = !isPast, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = typography.body.copy(color = textColor),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatMonthYear(date: LocalDate): String {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    return "${monthNames[date.monthNumber - 1]} ${date.year}"
}

private fun getDaysInMonth(date: LocalDate): Int {
    val month = date.month
    val year = date.year
    return when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}

private fun getFirstDayOfWeek(date: LocalDate): Int {
    val firstOfMonth = LocalDate(date.year, date.month, 1)
    return firstOfMonth.dayOfWeek.ordinal // Monday = 0, Sunday = 6
        .let { (it + 1) % 7 } // Adjust to Sunday = 0
}

/**
 * Displays the date selection as a modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionBottomSheet(
    isVisible: Boolean,
    title: String,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = VelocityColors.BackgroundDeep,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = VelocityColors.GlassBorder
                ) {}
            }
        ) {
            DateSelectionSheet(
                title = title,
                selectedDate = selectedDate,
                onSelect = onSelect,
                onDismiss = onDismiss
            )
        }
    }
}
