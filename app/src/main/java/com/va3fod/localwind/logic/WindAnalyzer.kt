package com.va3fod.localwind.logic

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

class WindAnalyzer {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    suspend fun measureWindSpeed(): Double = withContext(Dispatchers.IO) {
        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            ).apply {
                if (state != AudioRecord.STATE_INITIALIZED) return@withContext 0.0
            }
        } catch (e: Exception) {
            return@withContext 0.0
        }

        try {
            // Attempt to disable AGC and Noise Suppression for raw wind noise
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl.create(audioRecord.audioSessionId)?.enabled = false
            }
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(audioRecord.audioSessionId)?.enabled = false
            }

            audioRecord.startRecording()
            val buffer = ShortArray(bufferSize)
            var totalRms = 0.0
            val numSamples = 50 // Roughly 1-2 seconds

            repeat(numSamples) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += buffer[i].toDouble() * buffer[i].toDouble()
                    }
                    totalRms += sqrt(sum / read)
                }
            }
            
            val avgRms = totalRms / numSamples
            if (avgRms <= 0) return@withContext 0.0
            
            // Refined Acoustic Mapping (empirical log curve)
            val speed = 20.0 * log10(avgRms / 10.0) 
            speed.coerceAtLeast(0.0)
        } catch (e: Exception) {
            0.0
        } finally {
            try {
                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) { /* Ignore */ }
        }
    }
}
