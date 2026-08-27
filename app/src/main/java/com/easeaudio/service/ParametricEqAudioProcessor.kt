package com.easeaudio.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance real-time Parametric Equalizer AudioProcessor for Media3 / ExoPlayer pipeline.
 * Utilizes 5-band cascaded Transposed Direct Form II Biquad Filter Nodes (Audio EQ Cookbook / Web Audio BiquadFilterNode equivalents)
 * with real-time preset switching (Rock, Jazz, Acoustic, Bass Boost, Chill Lounge, Vocal Focus, Balanced).
 */
@OptIn(UnstableApi::class)
class ParametricEqAudioProcessor : BaseAudioProcessor() {

    enum class FilterType {
        LOW_SHELF,
        PEAKING,
        HIGH_SHELF
    }

    private class BiquadFilter(
        var type: FilterType,
        var frequency: Double,
        var q: Double,
        var gainDb: Double
    ) {
        // Normalized filter coefficients
        var b0: Double = 1.0
        var b1: Double = 0.0
        var b2: Double = 0.0
        var a1: Double = 0.0
        var a2: Double = 0.0

        // Transposed Direct Form II state per channel (supports up to 8 channels)
        val s1 = DoubleArray(8)
        val s2 = DoubleArray(8)

        fun reset() {
            s1.fill(0.0)
            s2.fill(0.0)
        }

        fun computeCoefficients(sampleRate: Int) {
            if (sampleRate <= 0) return
            val nyquist = sampleRate / 2.0
            val f0 = frequency.coerceIn(20.0, nyquist - 50.0)
            val w0 = 2.0 * Math.PI * (f0 / sampleRate)
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val a = 10.0.pow(gainDb / 40.0)
            val alpha = sinW0 / (2.0 * q.coerceAtLeast(0.1))

            var a0 = 1.0

            when (type) {
                FilterType.PEAKING -> {
                    b0 = 1.0 + alpha * a
                    b1 = -2.0 * cosW0
                    b2 = 1.0 - alpha * a
                    a0 = 1.0 + alpha / a
                    a1 = -2.0 * cosW0
                    a2 = 1.0 - alpha / a
                }
                FilterType.LOW_SHELF -> {
                    val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
                    b0 = a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)
                    b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
                    b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)
                    a0 = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
                    a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
                    a2 = (a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha
                }
                FilterType.HIGH_SHELF -> {
                    val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
                    b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)
                    b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
                    b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)
                    a0 = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
                    a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0)
                    a2 = (a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha
                }
            }

            // Normalize coefficients by a0
            if (a0 != 0.0 && a0 != 1.0) {
                b0 /= a0
                b1 /= a0
                b2 /= a0
                a1 /= a0
                a2 /= a0
            }
        }

        fun process(channel: Int, x: Double): Double {
            // Transposed Direct Form II step:
            // y = b0 * x + s1
            // s1 = b1 * x - a1 * y + s2
            // s2 = b2 * x - a2 * y
            val ch = channel.coerceIn(0, 7)
            val y = b0 * x + s1[ch]
            s1[ch] = b1 * x - a1 * y + s2[ch]
            s2[ch] = b2 * x - a2 * y
            return y
        }
    }

    private val filters = arrayOf(
        BiquadFilter(FilterType.LOW_SHELF, 80.0, 0.707, 0.0),    // Band 0: Sub & Low Bass
        BiquadFilter(FilterType.PEAKING, 350.0, 1.0, 0.0),       // Band 1: Low-Mid / Warmth
        BiquadFilter(FilterType.PEAKING, 1200.0, 1.0, 0.0),      // Band 2: Mid-Range / Presence
        BiquadFilter(FilterType.PEAKING, 3800.0, 1.0, 0.0),      // Band 3: High-Mid / Crispness
        BiquadFilter(FilterType.HIGH_SHELF, 9000.0, 0.707, 0.0)  // Band 4: Air & Treble
    )

    private var currentPreset: String = "Balanced"
    private var isEnabled: Boolean = true

    @Synchronized
    fun setPreset(presetName: String) {
        currentPreset = presetName
        when (presetName) {
            "Rock" -> {
                filters[0].gainDb = 4.5
                filters[1].gainDb = -2.5
                filters[2].gainDb = 1.0
                filters[3].gainDb = 3.5
                filters[4].gainDb = 5.0
            }
            "Jazz" -> {
                filters[0].gainDb = 3.5
                filters[1].gainDb = 1.5
                filters[2].gainDb = 2.0
                filters[3].gainDb = 1.5
                filters[4].gainDb = 2.5
            }
            "Acoustic" -> {
                filters[0].gainDb = 1.5
                filters[1].gainDb = -1.0
                filters[2].gainDb = 2.5
                filters[3].gainDb = 4.0
                filters[4].gainDb = 4.5
            }
            "Bass Boost" -> {
                filters[0].gainDb = 6.5
                filters[1].gainDb = 3.0
                filters[2].gainDb = 0.0
                filters[3].gainDb = 0.0
                filters[4].gainDb = -1.0
            }
            "Chill Lounge" -> {
                filters[0].gainDb = 3.0
                filters[1].gainDb = -1.5
                filters[2].gainDb = 0.0
                filters[3].gainDb = 2.0
                filters[4].gainDb = 3.0
            }
            "Vocal Focus", "Speech" -> {
                filters[0].gainDb = -3.0
                filters[1].gainDb = 1.0
                filters[2].gainDb = 4.0
                filters[3].gainDb = 3.5
                filters[4].gainDb = 1.0
            }
            else -> { // Balanced / Flat
                filters[0].gainDb = 0.0
                filters[1].gainDb = 0.0
                filters[2].gainDb = 0.0
                filters[3].gainDb = 0.0
                filters[4].gainDb = 0.0
            }
        }
        recomputeCoefficients()
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        if (!enabled) {
            resetFilterStates()
        }
    }

    private fun recomputeCoefficients() {
        val sampleRate = inputAudioFormat.sampleRate
        if (sampleRate > 0) {
            for (filter in filters) {
                filter.computeCoefficients(sampleRate)
            }
        }
    }

    private fun resetFilterStates() {
        for (filter in filters) {
            filter.reset()
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        recomputeCoefficients()
        resetFilterStates()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val buffer = replaceOutputBuffer(remaining)
        val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)

        // If EQ is disabled or balanced (0 dB on all bands), fast copy
        val isAllZero = filters.all { it.gainDb == 0.0 }
        if (!isEnabled || isAllZero) {
            buffer.put(inputBuffer)
            buffer.flip()
            return
        }

        // Process 16-bit PCM interleaved stream through cascaded biquad stages
        val byteOrder = inputBuffer.order()
        buffer.order(byteOrder)

        val shortCount = remaining / 2
        var sampleIndex = 0

        while (inputBuffer.hasRemaining()) {
            val rawSample = inputBuffer.short
            val channel = sampleIndex % channelCount
            var sampleVal = rawSample.toDouble()

            // Cascade 5 biquad filter nodes
            for (i in 0 until 5) {
                sampleVal = filters[i].process(channel, sampleVal)
            }

            // Soft-clip limiter: tanh-based saturation rounds peaks smoothly instead
            // of hard-clipping them. The final coerceIn is a safety guard only.
            val normalized = sampleVal / 32768.0
            val softClipped = kotlin.math.tanh(normalized) * 32768.0
            val clipped = softClipped.coerceIn(-32768.0, 32767.0).toInt().toShort()
            buffer.putShort(clipped)
            sampleIndex++
        }

        buffer.flip()
    }

    override fun onFlush() {
        super.onFlush()
        resetFilterStates()
    }

    override fun onReset() {
        super.onReset()
        resetFilterStates()
    }
}
