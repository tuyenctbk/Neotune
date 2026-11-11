package com.easeaudio.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

sealed interface VoiceAction {
    data class Play(val query: String) : VoiceAction
    data class Search(val query: String) : VoiceAction
    data object NextStation : VoiceAction
    data object PreviousStation : VoiceAction
    data object TogglePlayPause : VoiceAction
    data object ToggleFavorite : VoiceAction
    data class SetSleepTimer(val minutes: Int) : VoiceAction
    data class Unrecognized(val rawText: String) : VoiceAction
}

object VoiceTuningHelper {

    fun createSpeechIntent(context: Context, prompt: String = "Speak station name or command..."): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    fun parseVoiceInput(text: String): VoiceAction {
        val trimmed = text.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        return when {
            // Transport controls
            lower == "pause" || lower == "stop" || lower == "halt" || lower == "play" || lower == "resume" -> {
                VoiceAction.TogglePlayPause
            }
            lower == "next" || lower == "next station" || lower == "skip" || lower == "forward" -> {
                VoiceAction.NextStation
            }
            lower == "previous" || lower == "prev" || lower == "back" || lower == "last station" -> {
                VoiceAction.PreviousStation
            }
            lower == "favorite" || lower == "like" || lower == "love" || lower == "star" -> {
                VoiceAction.ToggleFavorite
            }

            // Sleep timer commands: "sleep timer 30 minutes", "sleep 15 min", "timer 45"
            lower.contains("sleep") || lower.contains("timer") -> {
                val digits = lower.filter { it.isDigit() }
                val minutes = digits.toIntOrNull() ?: 30
                VoiceAction.SetSleepTimer(minutes)
            }

            // Explicit play commands: "play bbc", "tune to jazz", "listen to lo-fi"
            lower.startsWith("play ") -> {
                VoiceAction.Play(trimmed.substring(5).trim())
            }
            lower.startsWith("tune to ") -> {
                VoiceAction.Play(trimmed.substring(8).trim())
            }
            lower.startsWith("listen to ") -> {
                VoiceAction.Play(trimmed.substring(10).trim())
            }

            // Explicit search commands: "search rock", "find ambient"
            lower.startsWith("search ") -> {
                VoiceAction.Search(trimmed.substring(7).trim())
            }
            lower.startsWith("find ") -> {
                VoiceAction.Search(trimmed.substring(5).trim())
            }

            // Default fallback: If user just says a station/genre name (e.g. "Chillout", "Classic Rock", "NPR")
            trimmed.isNotBlank() -> {
                VoiceAction.Play(trimmed)
            }

            else -> VoiceAction.Unrecognized(text)
        }
    }
}
