package com.fairair.app.ui.screens.passengers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.navigation.AppScreen
import com.fairair.app.ui.screens.ancillaries.AncillariesScreen
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Passenger information entry screen with Velocity design system.
 */
class PassengerInfoScreen : Screen, AppScreen.PassengerInfo {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<PassengerInfoScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        VelocityTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                VelocityColors.GradientStart,
                                VelocityColors.GradientEnd
                            )
                        )
                    )
            ) {
                PassengerInfoContent(
                    uiState = uiState,
                    onFieldChange = screenModel::updatePassengerField,
                    onNextPassenger = screenModel::nextPassenger,
                    onPreviousPassenger = screenModel::previousPassenger,
                    onGoToPassenger = screenModel::goToPassenger,
                    onBack = { navigator.pop() },
                    onContinue = {
                        screenModel.validateAndProceed {
                            navigator.push(AncillariesScreen())
                        }
                    },
                    onClearError = screenModel::clearError
                )
            }
        }
    }
}

@Composable
private fun PassengerInfoContent(
    uiState: PassengerInfoUiState,
    onFieldChange: (String, PassengerField, String) -> Unit,
    onNextPassenger: () -> Unit,
    onPreviousPassenger: () -> Unit,
    onGoToPassenger: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPassenger = uiState.currentPassenger
    val typography = VelocityTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VelocityColors.TextMain
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Passengers",
                    style = typography.timeBig,
                    color = VelocityColors.TextMain
                )
                if (uiState.passengers.isNotEmpty()) {
                    Text(
                        text = "${uiState.currentPassengerIndex + 1} of ${uiState.passengers.size}",
                        style = typography.duration,
                        color = VelocityColors.TextMuted
                    )
                }
            }
        }

        // Progress dots
        if (uiState.passengers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.passengers.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == uiState.currentPassengerIndex) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= uiState.currentPassengerIndex) VelocityColors.Accent
                                else VelocityColors.GlassBorder
                            )
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onGoToPassenger(index) }
                    )
                    if (index < uiState.passengers.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    if (index < uiState.currentPassengerIndex) VelocityColors.Accent
                                    else VelocityColors.GlassBorder
                                )
                        )
                    }
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VelocityColors.Accent)
                }
            }
            currentPassenger == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "No passenger data",
                        style = typography.body,
                        color = VelocityColors.Error
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Passenger type badge
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = VelocityColors.Accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (currentPassenger.type) {
                                            PassengerType.ADULT -> Icons.Default.Person
                                            PassengerType.CHILD -> Icons.Default.Face
                                            PassengerType.INFANT -> Icons.Default.Favorite
                                        },
                                        contentDescription = null,
                                        tint = VelocityColors.Accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = currentPassenger.label,
                                        style = typography.button,
                                        color = VelocityColors.Accent
                                    )
                                }
                            }
                        }
                    }

                    // Personal Details Card
                    item {
                        VelocityFormCard(title = "Personal Details") {
                            // Title selector
                            VelocityChipSelector(
                                label = "Title",
                                options = when (currentPassenger.type) {
                                    PassengerType.ADULT -> listOf("Mr", "Mrs", "Ms", "Dr")
                                    PassengerType.CHILD -> listOf("Master", "Miss")
                                    PassengerType.INFANT -> listOf("Infant")
                                },
                                selectedOption = currentPassenger.title,
                                onSelect = { onFieldChange(currentPassenger.id, PassengerField.TITLE, it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Name fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.firstName,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.FIRST_NAME, it) },
                                    label = "First Name",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.lastName,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.LAST_NAME, it) },
                                    label = "Last Name",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Date of birth and nationality
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.dateOfBirth,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.DATE_OF_BIRTH, it) },
                                    label = "Date of Birth",
                                    placeholder = "YYYY-MM-DD",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.nationality,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.NATIONALITY, it) },
                                    label = "Nationality",
                                    placeholder = "SA",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Travel Document Card
                    item {
                        VelocityFormCard(title = "Travel Document") {
                            // Document type chips
                            VelocityChipSelector(
                                label = "Document Type",
                                options = listOf("Passport", "National ID", "Iqama"),
                                selectedOption = when (currentPassenger.documentType) {
                                    "PASSPORT" -> "Passport"
                                    "NATIONAL_ID" -> "National ID"
                                    "IQAMA" -> "Iqama"
                                    else -> "Passport"
                                },
                                onSelect = { selected ->
                                    val code = when (selected) {
                                        "Passport" -> "PASSPORT"
                                        "National ID" -> "NATIONAL_ID"
                                        "Iqama" -> "IQAMA"
                                        else -> "PASSPORT"
                                    }
                                    onFieldChange(currentPassenger.id, PassengerField.DOCUMENT_TYPE, code)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Document number and expiry
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.documentNumber,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.DOCUMENT_NUMBER, it) },
                                    label = "Document Number",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.documentExpiry,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.DOCUMENT_EXPIRY, it) },
                                    label = "Expiry Date",
                                    placeholder = "YYYY-MM-DD",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Contact Info Card (only for primary adult)
                    if (currentPassenger.type == PassengerType.ADULT && currentPassenger.id == "adult_0") {
                        item {
                            VelocityFormCard(title = "Contact Information") {
                                VelocityGlassTextField(
                                    value = currentPassenger.email,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.EMAIL, it) },
                                    label = "Email Address",
                                    placeholder = "your@email.com",
                                    keyboardType = KeyboardType.Email
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                VelocityGlassTextField(
                                    value = currentPassenger.phone,
                                    onValueChange = { onFieldChange(currentPassenger.id, PassengerField.PHONE, it) },
                                    label = "Phone Number",
                                    placeholder = "+966 5XX XXX XXXX",
                                    keyboardType = KeyboardType.Phone
                                )
                            }
                        }
                    }

                    // Error message
                    if (uiState.error != null) {
                        item {
                            Surface(
                                color = VelocityColors.Error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.error,
                                        style = typography.body,
                                        color = VelocityColors.Error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onClearError) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = VelocityColors.Error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                // Bottom navigation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    VelocityColors.BackgroundDeep.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!uiState.isFirstPassenger) {
                            Surface(
                                onClick = onPreviousPassenger,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = VelocityColors.GlassBg,
                                border = BorderStroke(1.dp, VelocityColors.GlassBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Previous",
                                        style = typography.button,
                                        color = VelocityColors.TextMain
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = if (uiState.isLastPassenger) onContinue else onNextPassenger,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = VelocityColors.Accent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (uiState.isLastPassenger) "Continue" else "Next Passenger",
                                    style = typography.button,
                                    color = VelocityColors.BackgroundDeep
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Glassmorphic form card container.
 */
@Composable
private fun VelocityFormCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val typography = VelocityTheme.typography

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VelocityColors.GlassBg,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = typography.body,
                color = VelocityColors.TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * Glassmorphic text field.
 */
@Composable
private fun VelocityGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val typography = VelocityTheme.typography

    Column(modifier = modifier) {
        Text(
            text = label,
            style = typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            textStyle = typography.body.copy(color = VelocityColors.TextMain),
            cursorBrush = SolidColor(VelocityColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = typography.body,
                            color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Chip selector for options.
 */
@Composable
private fun VelocityChipSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Column(modifier = modifier) {
        Text(
            text = label,
            style = typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) VelocityColors.Accent else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder
                    )
                ) {
                    Text(
                        text = option,
                        style = typography.button,
                        color = if (isSelected) VelocityColors.BackgroundDeep else VelocityColors.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
