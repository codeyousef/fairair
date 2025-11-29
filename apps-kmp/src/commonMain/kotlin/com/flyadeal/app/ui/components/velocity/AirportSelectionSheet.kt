package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flyadeal.app.api.StationDto
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Bottom sheet style airport selection dialog for origin/destination.
 *
 * Displays a list of available airports with code, city, and name.
 * Shows a checkmark next to the currently selected airport.
 *
 * @param title The title to display at the top of the sheet
 * @param stations List of available airports to choose from
 * @param selectedCode The currently selected airport code, if any
 * @param onSelect Callback when an airport is selected
 * @param onDismiss Callback when the sheet is dismissed
 */
@Composable
fun AirportSelectionSheet(
    title: String,
    stations: List<StationDto>,
    selectedCode: String?,
    onSelect: (StationDto) -> Unit,
    onDismiss: () -> Unit
) {
    val typography = VelocityTheme.typography

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
            // Header with title and close button
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

            // Divider
            HorizontalDivider(
                color = VelocityColors.GlassBorder,
                thickness = 1.dp
            )

            // Airport list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(stations) { station ->
                    AirportRow(
                        station = station,
                        isSelected = station.code == selectedCode,
                        onClick = {
                            onSelect(station)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AirportRow(
    station: StationDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typography = VelocityTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Airport code badge
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp)),
                color = if (isSelected) VelocityColors.Accent else VelocityColors.GlassBg
            ) {
                Text(
                    text = station.code,
                    style = typography.timeBig.copy(
                        color = if (isSelected) VelocityColors.BackgroundDeep else VelocityColors.TextMain
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // City and airport name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.city,
                    style = typography.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.name,
                    style = typography.duration,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Selection indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = VelocityColors.Accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Displays the airport selection as a modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirportSelectionBottomSheet(
    isVisible: Boolean,
    title: String,
    stations: List<StationDto>,
    selectedCode: String?,
    onSelect: (StationDto) -> Unit,
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
            AirportSelectionSheet(
                title = title,
                stations = stations,
                selectedCode = selectedCode,
                onSelect = onSelect,
                onDismiss = onDismiss
            )
        }
    }
}
