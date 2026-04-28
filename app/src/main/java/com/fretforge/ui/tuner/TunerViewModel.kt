package com.fretforge.ui.tuner

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log2

data class GuitarString(val label: String, val note: String, val targetHz: Float, val pegIndex: Int)

enum class TuneStatus { SHARP, FLAT, IN_TUNE, LISTENING }

data class TunerState(
    val detectedHz: Float     = 0f,
    val centsOff: Float       = 0f,
    val activeString: GuitarString? = null,
    val status: TuneStatus    = TuneStatus.LISTENING
)

class TunerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val STRINGS = listOf(
            GuitarString("E2", "6th", 82.41f,  5),
            GuitarString("A2", "5th", 110.00f, 4),
            GuitarString("D3", "4th", 146.83f, 3),
            GuitarString("G3", "3rd", 196.00f, 2),
            GuitarString("B3", "2nd", 246.94f, 1),
            GuitarString("E4", "1st", 329.63f, 0)
        )
        private const val SAMPLE_RATE        = 44100
        private const val BUFFER_FRAMES      = 4096
        private const val IN_TUNE_CENTS      = 8f
        // Raised threshold — voice/ambient is usually below ~1500
        private const val MIN_AMPLITUDE      = 1800.0
        // Rolling average window for Hz smoothing
        private const val SMOOTH_WINDOW      = 5
        // Consecutive valid reads before emitting a result
        private const val STABILITY_REQUIRED = 3
    }

    private val _state = MutableStateFlow(TunerState())
    val state = _state.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var isRunning = false

    // Stability / smoothing state
    private val hzWindow       = ArrayDeque<Float>(SMOOTH_WINDOW)
    private var stableCount    = 0
    private var lastStringIdx  = -1

    fun startListening() {
        if (isRunning) return
        isRunning = true
        viewModelScope.launch(Dispatchers.IO) {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = maxOf(minBuf, BUFFER_FRAMES * 2)
            val ar = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
            )
            audioRecord = ar
            ar.startRecording()
            val buffer = ShortArray(BUFFER_FRAMES)
            while (isRunning) {
                val read = ar.read(buffer, 0, BUFFER_FRAMES)
                if (read > 0) processBuffer(buffer.copyOf(read))
            }
            ar.stop()
            ar.release()
        }
    }

    fun stopListening() {
        isRunning = false
        audioRecord = null
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    private fun processBuffer(buffer: ShortArray) {
        val amplitude = buffer.maxOrNull()?.let { abs(it.toDouble()) } ?: 0.0
        if (amplitude < MIN_AMPLITUDE) {
            // Reset smoothing on silence
            hzWindow.clear()
            stableCount   = 0
            lastStringIdx = -1
            _state.value  = TunerState()
            return
        }
        val hz = detectPitch(buffer) ?: run {
            hzWindow.clear(); stableCount = 0; return
        }

        // Find closest string (within a 4-semitone window ≈ 26%)
        val closest = STRINGS.minByOrNull { abs(it.targetHz - hz) } ?: return
        if (abs(closest.targetHz - hz) / closest.targetHz > 0.26f) {
            hzWindow.clear(); stableCount = 0; return
        }

        // Reset window if we switched to a different string
        if (closest.pegIndex != lastStringIdx) {
            hzWindow.clear()
            stableCount   = 0
            lastStringIdx = closest.pegIndex
        }

        // Rolling Hz average
        if (hzWindow.size >= SMOOTH_WINDOW) hzWindow.removeFirst()
        hzWindow.addLast(hz)
        stableCount++

        // Don't emit until we have enough stable readings
        if (stableCount < STABILITY_REQUIRED) return

        val smoothHz = hzWindow.average().toFloat()
        val cents    = 1200f * log2(smoothHz / closest.targetHz)
        val status   = when {
            cents > IN_TUNE_CENTS  -> TuneStatus.SHARP
            cents < -IN_TUNE_CENTS -> TuneStatus.FLAT
            else                   -> TuneStatus.IN_TUNE
        }
        _state.value = TunerState(smoothHz, cents, closest, status)
    }

    /** YIN-based fundamental frequency detector */
    private fun detectPitch(buffer: ShortArray): Float? {
        val n       = buffer.size
        val half    = n / 2
        val signal  = FloatArray(n) { buffer[it] / 32768f }

        // Difference function
        val diff = FloatArray(half)
        for (tau in 1 until half) {
            var sum = 0f
            for (i in 0 until half) {
                val d = signal[i] - signal[i + tau]
                sum  += d * d
            }
            diff[tau] = sum
        }

        // Cumulative mean normalised difference
        val cmndf    = FloatArray(half)
        cmndf[0]     = 1f
        var running  = 0f
        for (tau in 1 until half) {
            running    += diff[tau]
            cmndf[tau]  = if (running == 0f) 0f else diff[tau] * tau / running
        }

        // First dip below threshold
        val threshold = 0.15f
        var tau = 2
        while (tau < half - 1) {
            if (cmndf[tau] < threshold) {
                while (tau + 1 < half && cmndf[tau + 1] < cmndf[tau]) tau++
                break
            }
            tau++
        }
        if (tau >= half - 1) return null

        // Parabolic interpolation
        val betterTau = if (tau in 1 until half - 1) {
            val s0 = cmndf[tau - 1]; val s1 = cmndf[tau]; val s2 = cmndf[tau + 1]
            tau + (s2 - s0) / (2f * (2f * s1 - s2 - s0))
        } else tau.toFloat()

        val hz = SAMPLE_RATE / betterTau
        return if (hz in 60f..400f) hz else null
    }
}
