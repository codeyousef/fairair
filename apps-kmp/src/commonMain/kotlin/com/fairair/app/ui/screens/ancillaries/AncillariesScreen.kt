package com.fairair.app.ui.screens.ancillaries

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.navigation.AppScreen
import com.fairair.app.ui.screens.payment.PaymentScreen
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Ancillaries screen for selecting extras like baggage and meals.
 * Uses Velocity design system with glassmorphic cards.
 */
class AncillariesScreen : Screen, AppScreen.Ancillaries {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<AncillariesScreenModel>()
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    // Header
                    VelocityExtrasHeader(
                        onBack = { navigator.pop() }
                    )

                    // Content
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = VelocityColors.Accent)
                                }
                            }
                            uiState.error != null && uiState.baggageOptions.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = uiState.error ?: "Error loading extras",
                                        style = VelocityTheme.typography.body,
                                        color = VelocityColors.Error
                                    )
                                }
                            }
                            else -> {
                                ExtrasContent(
                                    uiState = uiState,
                                    onSelectBaggage = screenModel::selectBaggage,
                                    onSelectMeal = screenModel::selectMeal,
                                    onTogglePriority = screenModel::togglePriorityBoarding,
                                    onClearError = screenModel::clearError
                                )
                            }
                        }
                    }

                    // Bottom bar with price and continue button
                    VelocityExtrasBottomBar(
                        extrasTotal = uiState.ancillariesTotal,
                        grandTotal = uiState.grandTotal,
                        onContinue = {
                            screenModel.confirmAndProceed {
                                navigator.push(PaymentScreen())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VelocityExtrasHeader(
    onBack: () -> Unit
) {
    val typography = VelocityTheme.typography

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
                text = "Extras",
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
            Text(
                text = "Customize your journey",
                style = typography.duration,
                color = VelocityColors.TextMuted
            )
        }
    }
}

@Composable
private fun ExtrasContent(
    uiState: AncillariesUiState,
    onSelectBaggage: (String, Int) -> Unit,
    onSelectMeal: (String, String) -> Unit,
    onTogglePriority: () -> Unit,
    onClearError: () -> Unit
) {
    val typography = VelocityTheme.typography

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Skip extras option
        item {
            VelocityGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Skip extras?",
                            style = typography.body,
                            color = VelocityColors.TextMain
                        )
                        Text(
                            text = "You can add these later from Manage Booking",
                            style = typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = VelocityColors.TextMuted
                    )
                }
            }
        }

        // Baggage Section
        item {
            VelocityExtrasSection(
                title = "Checked Baggage",
                subtitle = "7 kg cabin baggage included",
                icon = Icons.Default.ShoppingCart
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.baggageSelections.forEach { (passengerId, selection) ->
                        VelocityBaggageCard(
                            passengerName = selection.passengerName,
                            selectedWeight = selection.checkedBagWeight,
                            options = uiState.baggageOptions,
                            onSelect = { weight -> onSelectBaggage(passengerId, weight) }
                        )
                    }
                }
            }
        }

        // Meals Section
        item {
            VelocityExtrasSection(
                title = "In-flight Meals",
                subtitle = "Pre-order for better selection",
                icon = Icons.Default.Star
            ) {
                VelocityMealsCard(
                    mealOptions = uiState.mealOptions,
                    mealSelections = uiState.mealSelections,
                    passengerIds = uiState.baggageSelections.keys.toList(),
                    onSelectMeal = onSelectMeal
                )
            }
        }

        // Priority Boarding
        item {
            VelocityExtrasSection(
                title = "Priority Boarding",
                subtitle = "Board first, secure overhead space",
                icon = Icons.Default.Star
            ) {
                VelocityPriorityCard(
                    isSelected = uiState.priorityBoarding,
                    onToggle = onTogglePriority
                )
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

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun VelocityGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VelocityColors.GlassBg,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun VelocityExtrasSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val typography = VelocityTheme.typography

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VelocityColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = typography.body,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = subtitle,
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }
        }
        content()
    }
}

@Composable
private fun VelocityBaggageCard(
    passengerName: String,
    selectedWeight: Int,
    options: List<BaggageOption>,
    onSelect: (Int) -> Unit
) {
    val typography = VelocityTheme.typography

    VelocityGlassCard {
        Text(
            text = passengerName,
            style = typography.button,
            color = VelocityColors.TextMain,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                VelocityBaggageChip(
                    option = option,
                    isSelected = selectedWeight == option.weight,
                    onClick = { onSelect(option.weight) }
                )
            }
        }
    }
}

@Composable
private fun VelocityBaggageChip(
    option: BaggageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typography = VelocityTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) VelocityColors.Accent else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = option.label,
                style = typography.button,
                color = if (isSelected) VelocityColors.BackgroundDeep else VelocityColors.TextMain
            )
            if (option.weight > 0) {
                Text(
                    text = "+SAR ${option.price}",
                    style = typography.labelSmall,
                    color = if (isSelected) VelocityColors.BackgroundDeep.copy(alpha = 0.8f) else VelocityColors.Accent
                )
            }
        }
    }
}

@Composable
private fun VelocityMealsCard(
    mealOptions: List<MealOption>,
    mealSelections: Map<String, String>,
    passengerIds: List<String>,
    onSelectMeal: (String, String) -> Unit
) {
    val typography = VelocityTheme.typography

    VelocityGlassCard {
        mealOptions.forEachIndexed { index, meal ->
            val isSelected = mealSelections.values.contains(meal.code)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable {
                        passengerIds.firstOrNull()?.let { passengerId ->
                            onSelectMeal(passengerId, meal.code)
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) VelocityColors.Accent
                                else Color.Transparent
                            )
                            .then(
                                if (!isSelected) Modifier.background(
                                    Color.Transparent,
                                    CircleShape
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) VelocityColors.Accent
                                    else VelocityColors.GlassBorder.copy(alpha = 0.5f)
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = VelocityColors.BackgroundDeep,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = meal.name,
                        style = typography.body,
                        color = VelocityColors.TextMain
                    )
                }
                if (meal.code != "NONE") {
                    Text(
                        text = "+SAR ${meal.price}",
                        style = typography.button,
                        color = VelocityColors.Accent
                    )
                }
            }

            if (index < mealOptions.size - 1) {
                HorizontalDivider(
                    color = VelocityColors.GlassBorder.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VelocityPriorityCard(
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val typography = VelocityTheme.typography

    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VelocityColors.GlassBg,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Board First",
                    style = typography.body,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = "Be among the first to board and secure your preferred overhead bin space",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SAR 35",
                    style = typography.button,
                    color = VelocityColors.Accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) VelocityColors.Accent
                            else VelocityColors.GlassBorder.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = VelocityColors.BackgroundDeep,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VelocityExtrasBottomBar(
    extrasTotal: String,
    grandTotal: String,
    onContinue: () -> Unit
) {
    val typography = VelocityTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        VelocityColors.BackgroundDeep.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            // Price summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Extras",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = "SAR $extrasTotal",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = typography.body,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = "SAR $grandTotal",
                    style = typography.timeBig,
                    color = VelocityColors.Accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue button
            Surface(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = VelocityColors.Accent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Continue to Payment",
                        style = typography.button,
                        color = VelocityColors.BackgroundDeep
                    )
                }
            }
        }
    }
}
