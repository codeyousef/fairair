package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyadeal.app.localization.AppStrings
import com.flyadeal.app.ui.screens.search.SearchField
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Natural language sentence builder for flight search.
 *
 * Displays a conversational sentence with tappable inline fields:
 * "I want to fly from [Origin] to [Destination] departing on [Date] with [Passengers]."
 *
 * @param originValue The selected origin city name, or null if not selected
 * @param destinationValue The selected destination city name, or null if not selected
 * @param dateValue The formatted departure date, or null if not selected
 * @param passengerValue The formatted passenger count string
 * @param activeField The currently active/focused field (for highlighting)
 * @param onFieldClick Callback when a field is tapped
 * @param strings Localized strings for the sentence parts
 * @param modifier Modifier to apply to the component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilder(
    originValue: String?,
    destinationValue: String?,
    dateValue: String?,
    passengerValue: String,
    activeField: SearchField?,
    onFieldClick: (SearchField) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Column(
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "I want to fly"
            SentenceText(
                text = strings.velocitySentencePrefix,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "from"
            SentenceText(
                text = strings.velocitySentenceFrom,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Origin] - tappable field
            MagicInputField(
                value = originValue,
                placeholder = strings.velocitySelectOrigin,
                onClick = { onFieldClick(SearchField.ORIGIN) },
                isActive = activeField == SearchField.ORIGIN,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "to"
            SentenceText(
                text = strings.velocitySentenceTo,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Destination] - tappable field
            MagicInputField(
                value = destinationValue,
                placeholder = strings.velocitySelectDestination,
                onClick = { onFieldClick(SearchField.DESTINATION) },
                isActive = activeField == SearchField.DESTINATION,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "departing on"
            SentenceText(
                text = strings.velocitySentenceDeparting,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Date] - tappable field
            MagicInputField(
                value = dateValue,
                placeholder = strings.velocitySelectDate,
                onClick = { onFieldClick(SearchField.DATE) },
                isActive = activeField == SearchField.DATE,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "with"
            SentenceText(
                text = strings.velocitySentenceWith,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Passengers] - tappable field
            MagicInputField(
                value = passengerValue,
                placeholder = "1 ${strings.velocityAdult}",
                onClick = { onFieldClick(SearchField.PASSENGERS) },
                isActive = activeField == SearchField.PASSENGERS
            )

            // Period
            SentenceText(text = ".")
        }
    }
}

/**
 * Helper composable for sentence text parts.
 */
@Composable
private fun SentenceText(
    text: String,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography
    Text(
        text = text,
        style = typography.sentenceBuilder,
        modifier = modifier
    )
}

/**
 * Arabic version of the sentence builder with RTL flow.
 *
 * Arabic sentence: "أبي أسافر من [الرياض] إلى [دبي] بتاريخ [1 ديسمبر] لعدد [1 بالغ]"
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilderArabic(
    originValue: String?,
    destinationValue: String?,
    dateValue: String?,
    passengerValue: String,
    activeField: SearchField?,
    onFieldClick: (SearchField) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Column(
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "أبي أسافر"
            Text(
                text = strings.velocitySentencePrefix,
                style = typography.sentenceBuilder,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "من"
            Text(
                text = strings.velocitySentenceFrom,
                style = typography.sentenceBuilder,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Origin]
            MagicInputField(
                value = originValue,
                placeholder = strings.velocitySelectOrigin,
                onClick = { onFieldClick(SearchField.ORIGIN) },
                isActive = activeField == SearchField.ORIGIN,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "إلى"
            Text(
                text = strings.velocitySentenceTo,
                style = typography.sentenceBuilder,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Destination]
            MagicInputField(
                value = destinationValue,
                placeholder = strings.velocitySelectDestination,
                onClick = { onFieldClick(SearchField.DESTINATION) },
                isActive = activeField == SearchField.DESTINATION,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "بتاريخ"
            Text(
                text = strings.velocitySentenceDeparting,
                style = typography.sentenceBuilder,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Date]
            MagicInputField(
                value = dateValue,
                placeholder = strings.velocitySelectDate,
                onClick = { onFieldClick(SearchField.DATE) },
                isActive = activeField == SearchField.DATE,
                modifier = Modifier.padding(end = 8.dp)
            )

            // "لعدد"
            Text(
                text = strings.velocitySentenceWith,
                style = typography.sentenceBuilder,
                modifier = Modifier.padding(end = 8.dp)
            )

            // [Passengers]
            MagicInputField(
                value = passengerValue,
                placeholder = "1 ${strings.velocityAdult}",
                onClick = { onFieldClick(SearchField.PASSENGERS) },
                isActive = activeField == SearchField.PASSENGERS
            )
        }
    }
}
