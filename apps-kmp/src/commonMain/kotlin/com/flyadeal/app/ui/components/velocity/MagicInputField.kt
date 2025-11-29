package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * A tappable inline text field for the sentence builder UI.
 *
 * Displays either a placeholder or selected value with accent styling,
 * and handles tap interactions for showing selection sheets.
 *
 * @param value The currently selected value to display, or null for placeholder
 * @param placeholder The placeholder text to show when no value is selected
 * @param onClick Callback when the field is tapped
 * @param modifier Modifier to apply to the component
 * @param isActive Whether this field is currently active/focused
 */
@Composable
fun MagicInputField(
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val typography = VelocityTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    val displayText = value ?: placeholder
    val hasValue = value != null

    val textStyle = if (hasValue) {
        typography.magicInput
    } else {
        typography.magicInput.copy(
            color = VelocityColors.Accent.copy(alpha = 0.7f)
        )
    }

    val backgroundColor = if (isActive) {
        VelocityColors.Accent.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayText,
            style = textStyle.copy(
                textDecoration = if (!hasValue) TextDecoration.Underline else TextDecoration.None
            )
        )
    }
}

/**
 * A smaller variant of MagicInputField for compact spaces.
 */
@Composable
fun MagicInputFieldSmall(
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val typography = VelocityTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    val displayText = value ?: placeholder
    val hasValue = value != null

    val textStyle = if (hasValue) {
        typography.timeBig.copy(color = VelocityColors.Accent)
    } else {
        typography.timeBig.copy(
            color = VelocityColors.Accent.copy(alpha = 0.7f)
        )
    }

    val backgroundColor = if (isActive) {
        VelocityColors.Accent.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = displayText,
            style = textStyle.copy(
                textDecoration = if (!hasValue) TextDecoration.Underline else TextDecoration.None
            )
        )
    }
}
