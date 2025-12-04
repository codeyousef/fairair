package com.fairair.app.ui.components.velocity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.api.LowFareDateDto
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme
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
 * - Optional price display for each date
 * - Optional minimum date constraint
 *
 * @param title The title to display at the top
 * @param selectedDate The currently selected date, if any
 * @param minDate Optional minimum selectable date (dates before this are disabled)
 * @param lowFares Optional map of date to low fare data for price display
 * @param isLoadingPrices Whether prices are currently being loaded
 * @param onSelect Callback when a date is selected
 * @param onDismiss Callback when the sheet is dismissed
 * @param onMonthChange Callback when the displayed month changes (for fetching prices)
 */
@Composable
fun DateSelectionSheet(
    title: String,
    selectedDate: LocalDate?,
    minDate: LocalDate? = null,
    lowFares: Map<LocalDate, LowFareDateDto> = emptyMap(),
    isLoadingPrices: Boolean = false,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onMonthChange: ((year: Int, month: Int) -> Unit)? = null
) {
    val typography = VelocityTheme.typography
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val effectiveMinDate = minDate ?: today

    var displayedMonth by remember {
        mutableStateOf(selectedDate ?: effectiveMinDate)
    }
    
    // Notify when month changes
    LaunchedEffect(displayedMonth) {
        onMonthChange?.invoke(displayedMonth.year, displayedMonth.monthNumber)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f),
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMonthYear(displayedMonth),
                        style = typography.body.copy(color = VelocityColors.TextMain)
                    )
                    if (isLoadingPrices) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.width(100.dp).height(2.dp),
                            color = VelocityColors.Accent,
                            trackColor = VelocityColors.GlassBg
                        )
                    }
                }

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
                    .padding(horizontal = 8.dp),
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
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Empty cells for days before the first of the month
                items(firstDayOfWeek) {
                    Box(modifier = Modifier.aspectRatio(0.7f))
                }

                // Day cells
                items(daysInMonth) { dayIndex ->
                    val day = dayIndex + 1
                    val date = LocalDate(displayedMonth.year, displayedMonth.month, day)
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val isDisabled = date < effectiveMinDate
                    val lowFare = lowFares[date]

                    DayCellWithPrice(
                        day = day,
                        isSelected = isSelected,
                        isToday = isToday,
                        isPast = isDisabled,
                        lowFare = lowFare,
                        showPrices = lowFares.isNotEmpty(),
                        onClick = {
                            if (!isDisabled) {
                                onSelect(date)
                                onDismiss()
                            }
                        }
                    )
                }
            }
            
            // Legend if prices are shown
            if (lowFares.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = VelocityColors.PriceLow, label = "Low")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = VelocityColors.PriceMedium, label = "Medium")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = VelocityColors.PriceHigh, label = "High")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val typography = VelocityTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = typography.labelSmall.copy(fontSize = 10.sp)
        )
    }
}

@Composable
private fun DayCellWithPrice(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isPast: Boolean,
    lowFare: LowFareDateDto?,
    showPrices: Boolean,
    onClick: () -> Unit
) {
    val typography = VelocityTheme.typography

    val backgroundColor = when {
        isSelected -> VelocityColors.Accent
        isToday -> VelocityColors.GlassBg
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> VelocityColors.BackgroundDeep
        isPast -> VelocityColors.Disabled
        lowFare?.available == false -> VelocityColors.Disabled
        else -> VelocityColors.TextMain
    }
    
    // Determine price color based on relative price
    val priceColor = when {
        isPast -> VelocityColors.Disabled
        lowFare == null -> VelocityColors.TextMuted
        !lowFare.available -> VelocityColors.Disabled
        isSelected -> VelocityColors.BackgroundDeep
        else -> getPriceColor(lowFare.priceMinor)
    }

    Column(
        modifier = Modifier
            .aspectRatio(0.7f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .pointerHoverIcon(if (!isPast && lowFare?.available != false) PointerIcon.Hand else PointerIcon.Default)
            .clickable(enabled = !isPast && lowFare?.available != false, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = typography.body.copy(color = textColor),
            textAlign = TextAlign.Center
        )
        
        if (showPrices) {
            val priceText = when {
                isPast -> ""
                lowFare == null -> ""
                !lowFare.available -> "—"
                lowFare.priceFormatted != null -> {
                    // Extract just the number part (e.g., "350" from "350.00 SAR")
                    lowFare.priceFormatted.split(" ").firstOrNull()?.split(".")?.firstOrNull() ?: ""
                }
                else -> ""
            }
            
            if (priceText.isNotEmpty()) {
                Text(
                    text = priceText,
                    style = typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = priceColor
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Returns a color based on the price level.
 * Uses a simple heuristic based on typical fare ranges.
 */
private fun getPriceColor(priceMinor: Long?): Color {
    if (priceMinor == null) return VelocityColors.TextMuted
    
    // Price in major units (e.g., SAR)
    val price = priceMinor / 100
    
    return when {
        price < 400 -> VelocityColors.PriceLow      // Low/cheap
        price < 600 -> VelocityColors.PriceMedium   // Medium
        else -> VelocityColors.PriceHigh            // High/expensive
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
        else -> Color.Transparent
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
            .pointerHoverIcon(if (!isPast) PointerIcon.Hand else PointerIcon.Default)
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
 * 
 * @param isVisible Whether the sheet is visible
 * @param title The title to display
 * @param selectedDate The currently selected date
 * @param minDate Optional minimum selectable date (dates before this are disabled)
 * @param lowFares Map of date to low fare data for price display
 * @param isLoadingPrices Whether prices are currently being loaded
 * @param onSelect Callback when a date is selected
 * @param onDismiss Callback when the sheet is dismissed
 * @param onMonthChange Callback when the displayed month changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionBottomSheet(
    isVisible: Boolean,
    title: String,
    selectedDate: LocalDate?,
    minDate: LocalDate? = null,
    lowFares: Map<LocalDate, LowFareDateDto> = emptyMap(),
    isLoadingPrices: Boolean = false,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onMonthChange: ((year: Int, month: Int) -> Unit)? = null
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
                minDate = minDate,
                lowFares = lowFares,
                isLoadingPrices = isLoadingPrices,
                onSelect = onSelect,
                onDismiss = onDismiss,
                onMonthChange = onMonthChange
            )
        }
    }
}
