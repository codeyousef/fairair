package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyadeal.app.localization.AppStrings
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Passenger count selection sheet.
 *
 * Allows selection of 1-9 adult passengers.
 * Uses increment/decrement controls with visual feedback.
 *
 * @param title The title to display at the top
 * @param currentCount The currently selected passenger count
 * @param strings Localized strings for labels
 * @param onSelect Callback when a count is selected
 * @param onDismiss Callback when the sheet is dismissed
 */
@Composable
fun PassengerSelectionSheet(
    title: String,
    currentCount: Int,
    strings: AppStrings,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val typography = VelocityTheme.typography
    var tempCount by remember(currentCount) { mutableStateOf(currentCount) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = VelocityColors.BackgroundDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(32.dp))

            // Passenger counter
            PassengerCounter(
                label = strings.adults,
                description = "12+ years",
                count = tempCount,
                minValue = 1,
                maxValue = 9,
                onIncrement = { tempCount = (tempCount + 1).coerceAtMost(9) },
                onDecrement = { tempCount = (tempCount - 1).coerceAtLeast(1) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Confirm button
            Button(
                onClick = {
                    onSelect(tempCount)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VelocityColors.Accent,
                    contentColor = VelocityColors.BackgroundDeep
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = strings.confirm,
                    style = typography.button
                )
            }
        }
    }
}

@Composable
private fun PassengerCounter(
    label: String,
    description: String,
    count: Int,
    minValue: Int,
    maxValue: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val typography = VelocityTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = typography.body
            )
            Text(
                text = description,
                style = typography.duration
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Decrement button
            CounterButton(
                enabled = count > minValue,
                onClick = onDecrement,
                isIncrement = false
            )

            // Count display
            Text(
                text = count.toString(),
                style = typography.timeBig,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Increment button
            CounterButton(
                enabled = count < maxValue,
                onClick = onIncrement,
                isIncrement = true
            )
        }
    }
}

@Composable
private fun CounterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    isIncrement: Boolean
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) VelocityColors.GlassBg else VelocityColors.GlassBg.copy(alpha = 0.5f),
        onClick = onClick,
        enabled = enabled
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isIncrement) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (enabled) VelocityColors.Accent else VelocityColors.Disabled,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // Minus icon (using a simple line since there's no built-in minus)
                Surface(
                    modifier = Modifier.size(16.dp, 2.dp),
                    color = if (enabled) VelocityColors.Accent else VelocityColors.Disabled,
                    shape = RoundedCornerShape(1.dp)
                ) {}
            }
        }
    }
}

/**
 * Displays the passenger selection as a modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerSelectionBottomSheet(
    isVisible: Boolean,
    title: String,
    currentCount: Int,
    strings: AppStrings,
    onSelect: (Int) -> Unit,
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
            PassengerSelectionSheet(
                title = title,
                currentCount = currentCount,
                strings = strings,
                onSelect = onSelect,
                onDismiss = onDismiss
            )
        }
    }
}
