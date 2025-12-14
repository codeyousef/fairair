package com.fairair.contract.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to start a voice session.
 */
@Serializable
data class VoiceStartRequest(
    val userId: String,
    val locale: String
)

/**
 * Events streamed from the voice server to the client.
 */
@Serializable
sealed class VoiceEvent {
    /** Real-time transcription of user speech */
    @Serializable
    @SerialName("transcription")
    data class Transcription(val text: String) : VoiceEvent()

    /** Signal that user interrupted the bot */
    @Serializable
    @SerialName("interruption")
    object Interruption : VoiceEvent()

    /** Audio chunk for playback (PCM or similar format) */
    @Serializable
    @SerialName("audio")
    data class Audio(
        val pcmData: ByteArray
    ) : VoiceEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Audio

            return pcmData.contentEquals(other.pcmData)
        }

        override fun hashCode(): Int {
            return pcmData.contentHashCode()
        }
    }
}
