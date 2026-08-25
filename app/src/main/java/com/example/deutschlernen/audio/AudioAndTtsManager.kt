package com.example.deutschlernen.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class AudioAndTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.GERMAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.GERMANY)
            }
            isTtsReady = true
        }
    }

    fun speakGerman(text: String, speed: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isTtsReady || text.isBlank()) return
        try {
            tts?.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
            tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
            // Remove articles and punctuations if needed or pronounce complete phrase
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "german_tts_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playCorrect(enabled: Boolean, volume: Float, style: String = "chime") {
        if (!enabled || volume <= 0f) return
        scope.launch {
            when (style) {
                "piano" -> playSynthTones(listOf(523.25f, 659.25f, 783.99f), 120, volume * 0.7f) // C5-E5-G5
                "soft" -> playSynthTones(listOf(440.0f, 554.37f), 150, volume * 0.5f) // A4-C#5
                else -> playSynthTones(listOf(587.33f, 880.0f), 100, volume * 0.6f) // D5-A5 chime
            }
        }
    }

    fun playWrong(enabled: Boolean, volume: Float) {
        if (!enabled || volume <= 0f) return
        scope.launch {
            playSynthTones(listOf(220.0f, 185.0f), 140, volume * 0.5f)
        }
    }

    fun playCardFlip(enabled: Boolean, volume: Float) {
        if (!enabled || volume <= 0f) return
        scope.launch {
            playSynthTones(listOf(1200.0f), 30, volume * 0.3f)
        }
    }

    fun playMatch(enabled: Boolean, volume: Float) {
        if (!enabled || volume <= 0f) return
        scope.launch {
            playSynthTones(listOf(523.25f, 659.25f, 1046.5f), 140, volume * 0.7f)
        }
    }

    fun playVictory(enabled: Boolean, volume: Float) {
        if (!enabled || volume <= 0f) return
        scope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
            playSynthTones(notes, 180, volume * 0.8f)
        }
    }

    private fun playSynthTones(frequencies: List<Float>, durationMs: Int, volume: Float) {
        try {
            val sampleRate = 44100
            val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt() * frequencies.size
            val buffer = ShortArray(totalSamples)

            var offset = 0
            val samplesPerTone = (sampleRate * (durationMs / 1000.0)).toInt()

            for (freq in frequencies) {
                for (i in 0 until samplesPerTone) {
                    val time = i.toDouble() / sampleRate
                    // Apply envelope to avoid clicks
                    val envelope = if (i < 200) i / 200.0 else if (i > samplesPerTone - 400) (samplesPerTone - i) / 400.0 else 1.0
                    val sample = (sin(2.0 * Math.PI * freq * time) * Short.MAX_VALUE * volume * envelope).toInt()
                    buffer[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                offset += samplesPerTone
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            Thread.sleep(durationMs.toLong() * frequencies.size + 50)
            track.release()
        } catch (e: Exception) {
            // Ignore audio interruption
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
