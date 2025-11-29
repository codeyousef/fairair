package com.flyadeal.app.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.flyadeal.app.localization.*
import com.flyadeal.app.navigation.AppScreen
import com.flyadeal.app.persistence.LocalStorage
import com.flyadeal.app.ui.components.velocity.GlassCard
import com.flyadeal.app.ui.components.velocity.glassmorphism
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme
import com.flyadeal.app.ui.theme.VelocityThemeWithBackground
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Velocity-styled settings screen with glassmorphic design.
 *
 * Features:
 * - Glassmorphic cards for settings sections
 * - Language toggle with instant direction switching
 * - Deep purple gradient background
 * - RTL-aware layout
 */
class VelocitySettingsScreen : Screen, AppScreen.Settings {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val localization = localization()
        val strings = strings()
        val localStorage = koinInject<LocalStorage>()
        val scope = rememberCoroutineScope()

        VelocityThemeWithBackground(isRtl = localization.isRtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header with back button
                VelocitySettingsHeader(
                    title = strings.settings,
                    onBack = { navigator.pop() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Language Section
                VelocityLanguageSection(
                    title = strings.language,
                    currentLanguage = localization.currentLanguage,
                    onLanguageSelected = { language ->
                        localization.setLanguage(language)
                        scope.launch {
                            localStorage.setCurrentLanguage(language.code)
                        }
                    },
                    strings = strings
                )

                // About Section
                VelocityAboutSection(
                    title = strings.appName,
                    strings = strings
                )

                Spacer(modifier = Modifier.weight(1f))

                // Version footer
                Text(
                    text = "Version 1.0.0",
                    style = VelocityTheme.typography.labelSmall,
                    color = VelocityColors.TextMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun VelocitySettingsHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VelocityColors.TextMain
            )
        }

        Text(
            text = title,
            style = VelocityTheme.typography.timeBig,
            color = VelocityColors.TextMain
        )

        // Spacer for balanced layout
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun VelocityLanguageSection(
    title: String,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    strings: AppStrings
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = VelocityTheme.typography.body,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(start = 4.dp)
        )

        GlassCard(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // English option
                VelocityLanguageOption(
                    language = AppLanguage.ENGLISH,
                    displayName = strings.english,
                    nativeName = "English",
                    isSelected = currentLanguage == AppLanguage.ENGLISH,
                    onClick = { onLanguageSelected(AppLanguage.ENGLISH) }
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                        .glassmorphism()
                )

                // Arabic option
                VelocityLanguageOption(
                    language = AppLanguage.ARABIC,
                    displayName = strings.arabic,
                    nativeName = "العربية",
                    isSelected = currentLanguage == AppLanguage.ARABIC,
                    onClick = { onLanguageSelected(AppLanguage.ARABIC) }
                )
            }
        }
    }
}

@Composable
private fun VelocityLanguageOption(
    language: AppLanguage,
    displayName: String,
    nativeName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder.copy(alpha = 0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = displayName,
                style = VelocityTheme.typography.body,
                color = if (isSelected) VelocityColors.Accent else VelocityColors.TextMain
            )
            Text(
                text = nativeName,
                style = VelocityTheme.typography.duration,
                color = VelocityColors.TextMuted
            )
        }

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

@Composable
private fun VelocityAboutSection(
    title: String,
    strings: AppStrings
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "About",
            style = VelocityTheme.typography.body,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(start = 4.dp)
        )

        GlassCard(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App name and branding
                Text(
                    text = title,
                    style = VelocityTheme.typography.timeBig,
                    color = VelocityColors.Accent
                )

                // Description
                Text(
                    text = "Book affordable flights across Saudi Arabia with flyadeal - the Kingdom's low-cost carrier.",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMuted
                )

                // Feature highlights
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureBadge("3 Fare Types")
                    FeatureBadge("30+ Routes")
                    FeatureBadge("RTL Support")
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(text: String) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = VelocityColors.GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted
        )
    }
}
