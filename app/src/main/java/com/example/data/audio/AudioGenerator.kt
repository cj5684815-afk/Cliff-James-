package com.example.data.audio

import android.content.Context
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioGenerator {

    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16

    data class PresetConfig(
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val durationSeconds: Int,
        val colorHex: String,
        val style: MusicStyle
    )

    enum class MusicStyle {
        SYNTHWAVE,
        LOFI_CHILL,
        AMBIENT_SPACE,
        ACOUSTIC_CHORD,
        CYBER_PULSE
    }

    suspend fun ensureDefaultOfflineTracks(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val musicDir = File(context.filesDir, "offline_music").apply { mkdirs() }
        val presets = listOf(
            PresetConfig(
                title = "Neon Horizon",
                artist = "Pulse Sound Lab",
                album = "Cyber Sessions",
                genre = "Synthwave",
                durationSeconds = 30,
                colorHex = "#FF007F",
                style = MusicStyle.SYNTHWAVE
            ),
            PresetConfig(
                title = "Midnight Chill",
                artist = "Lo-Fi Orbit",
                album = "Coffee & Clouds",
                genre = "Lo-Fi Beats",
                durationSeconds = 28,
                colorHex = "#7C4DFF",
                style = MusicStyle.LOFI_CHILL
            ),
            PresetConfig(
                title = "Ethereal Echoes",
                artist = "Zen Sanctuary",
                album = "Cosmic Mind",
                genre = "Ambient",
                durationSeconds = 32,
                colorHex = "#00E5FF",
                style = MusicStyle.AMBIENT_SPACE
            ),
            PresetConfig(
                title = "Acoustic Dawn",
                artist = "Morning Solitude",
                album = "Wooden Strings",
                genre = "Acoustic",
                durationSeconds = 25,
                colorHex = "#FF9100",
                style = MusicStyle.ACOUSTIC_CHORD
            ),
            PresetConfig(
                title = "Cyberpulse 2088",
                artist = "Volt Driver",
                album = "Hyperdrive Overload",
                genre = "Electro EDM",
                durationSeconds = 26,
                colorHex = "#00E676",
                style = MusicStyle.CYBER_PULSE
            )
        )

        val createdTracks = mutableListOf<Track>()

        for ((index, preset) in presets.withIndex()) {
            val fileName = "preset_${preset.style.name.lowercase()}.wav"
            val file = File(musicDir, fileName)

            if (!file.exists() || file.length() < 44L) {
                generateMusicWav(file, preset.durationSeconds, preset.style)
            }

            val sizeKb = file.length() / 1024
            val sizeFormatted = if (sizeKb >= 1024) String.format("%.1f MB", sizeKb / 1024.0) else "$sizeKb KB"

            createdTracks.add(
                Track(
                    id = 0,
                    title = preset.title,
                    artist = preset.artist,
                    album = preset.album,
                    durationMs = (preset.durationSeconds * 1000).toLong(),
                    filePath = file.absolutePath,
                    isFavorite = index == 0,
                    genre = preset.genre,
                    colorHex = preset.colorHex,
                    fileSizeFormatted = sizeFormatted,
                    isPreset = true
                )
            )
        }

        createdTracks
    }

    suspend fun createCustomTrack(
        context: Context,
        title: String,
        genre: String,
        bpm: Int,
        style: MusicStyle,
        durationSeconds: Int,
        colorHex: String
    ): Track = withContext(Dispatchers.IO) {
        val musicDir = File(context.filesDir, "offline_music").apply { mkdirs() }
        val fileName = "custom_${System.currentTimeMillis()}.wav"
        val file = File(musicDir, fileName)

        generateMusicWav(file, durationSeconds, style, bpm)

        val sizeKb = file.length() / 1024
        val sizeFormatted = if (sizeKb >= 1024) String.format("%.1f MB", sizeKb / 1024.0) else "$sizeKb KB"

        Track(
            id = 0,
            title = title.ifBlank { "Offline Synth Loop" },
            artist = "Synthesizer Studio",
            album = "My Custom Creations",
            durationMs = (durationSeconds * 1000).toLong(),
            filePath = file.absolutePath,
            genre = genre,
            colorHex = colorHex,
            fileSizeFormatted = sizeFormatted,
            isPreset = false
        )
    }

    private fun generateMusicWav(
        file: File,
        durationSeconds: Int,
        style: MusicStyle,
        customBpm: Int = 120
    ) {
        val totalSamples = SAMPLE_RATE * durationSeconds
        val fos = FileOutputStream(file)

        // Write placeholder header
        val header = ByteArray(44)
        fos.write(header)

        // Scale factors
        val chordFrequencies = when (style) {
            MusicStyle.SYNTHWAVE -> listOf(
                doubleArrayOf(220.0, 261.63, 329.63), // Am
                doubleArrayOf(174.61, 220.0, 261.63), // F
                doubleArrayOf(261.63, 329.63, 392.0), // C
                doubleArrayOf(196.0, 246.94, 293.66)  // G
            )
            MusicStyle.LOFI_CHILL -> listOf(
                doubleArrayOf(146.83, 220.0, 261.63, 329.63), // Dm7
                doubleArrayOf(196.0, 246.94, 293.66, 349.23), // G7
                doubleArrayOf(130.81, 196.0, 246.94, 329.63), // Cmaj7
                doubleArrayOf(110.0, 164.81, 220.0, 261.63)   // Am7
            )
            MusicStyle.AMBIENT_SPACE -> listOf(
                doubleArrayOf(130.81, 196.0, 293.66, 392.0),
                doubleArrayOf(146.83, 220.0, 329.63, 440.0),
                doubleArrayOf(164.81, 246.94, 329.63, 493.88)
            )
            MusicStyle.ACOUSTIC_CHORD -> listOf(
                doubleArrayOf(196.0, 246.94, 293.66, 392.0), // G
                doubleArrayOf(164.81, 246.94, 329.63),        // Em
                doubleArrayOf(130.81, 164.81, 196.0, 261.63), // C
                doubleArrayOf(146.83, 220.0, 293.66)         // D
            )
            MusicStyle.CYBER_PULSE -> listOf(
                doubleArrayOf(110.0, 220.0, 440.0), // A
                doubleArrayOf(123.47, 246.94, 493.88), // B
                doubleArrayOf(130.81, 261.63, 523.25), // C
                doubleArrayOf(146.83, 293.66, 587.33)  // D
            )
        }

        val buffer = ByteBuffer.allocate(4096 * 4).order(ByteOrder.LITTLE_ENDIAN)
        val chordCount = chordFrequencies.size
        val beatsPerChord = 4
        val bpm = if (customBpm > 0) customBpm else when (style) {
            MusicStyle.LOFI_CHILL -> 82
            MusicStyle.AMBIENT_SPACE -> 60
            MusicStyle.SYNTHWAVE -> 118
            MusicStyle.CYBER_PULSE -> 132
            MusicStyle.ACOUSTIC_CHORD -> 96
        }

        val secondsPerBeat = 60.0 / bpm
        val samplesPerBeat = (secondsPerBeat * SAMPLE_RATE).toInt()
        val samplesPerChord = samplesPerBeat * beatsPerChord

        var chordIndex = 0
        var currentChordSamples = 0
        var beatSampleCounter = 0
        var currentBeat = 0

        for (i in 0 until totalSamples) {
            val time = i.toDouble() / SAMPLE_RATE

            if (currentChordSamples >= samplesPerChord) {
                currentChordSamples = 0
                chordIndex = (chordIndex + 1) % chordCount
            }
            currentChordSamples++

            if (beatSampleCounter >= samplesPerBeat) {
                beatSampleCounter = 0
                currentBeat = (currentBeat + 1) % 4
            }
            beatSampleCounter++

            val chord = chordFrequencies[chordIndex]
            val chordProgress = currentChordSamples.toDouble() / samplesPerChord
            val beatProgress = beatSampleCounter.toDouble() / samplesPerBeat

            var leftSample = 0.0
            var rightSample = 0.0

            when (style) {
                MusicStyle.SYNTHWAVE -> {
                    // Bass arp
                    val arpStep = (beatSampleCounter * 4 / samplesPerBeat) % chord.size
                    val bassFreq = chord[arpStep] * 0.5
                    val bassEnv = exp(-beatProgress * 3.0)
                    val bass = (sin(2 * PI * bassFreq * time) + 0.4 * sin(4 * PI * bassFreq * time)) * bassEnv * 0.35

                    // Lead melody
                    val leadFreq = chord[chord.size - 1] * 2.0
                    val lead = sin(2 * PI * leadFreq * time + 0.2 * sin(2 * PI * 4.0 * time)) * 0.2

                    // Pad
                    var pad = 0.0
                    for (f in chord) {
                        pad += sin(2 * PI * f * time) * 0.08
                    }

                    // Kick & Snare
                    val kick = if (currentBeat == 0 || currentBeat == 2) {
                        val kEnv = exp(-beatProgress * 15.0)
                        sin(2 * PI * (60.0 - beatProgress * 35.0) * time) * kEnv * 0.4
                    } else 0.0

                    val snare = if (currentBeat == 1 || currentBeat == 3) {
                        val sEnv = exp(-beatProgress * 8.0)
                        (sin(2 * PI * 180.0 * time) * 0.5 + ((time * 1000 % 2) - 1) * 0.2) * sEnv * 0.3
                    } else 0.0

                    val mixed = (bass + lead + pad + kick + snare) * 0.75
                    leftSample = mixed * 0.95
                    rightSample = mixed * 1.05
                }

                MusicStyle.LOFI_CHILL -> {
                    // Mellow Rhodes-like tone
                    var rhodes = 0.0
                    for (f in chord) {
                        rhodes += (sin(2 * PI * f * time) + 0.2 * sin(4 * PI * f * time)) * 0.12
                    }
                    val padEnv = (1.0 - chordProgress * 0.4)
                    rhodes *= padEnv

                    // Soft lofi bass
                    val lofiBass = sin(2 * PI * (chord[0] * 0.5) * time) * 0.25

                    // Soft kick on beat 0
                    val kick = if (currentBeat == 0) {
                        exp(-beatProgress * 10.0) * sin(2 * PI * 55.0 * time) * 0.3
                    } else 0.0

                    // Vinyl subtle crackle
                    val noise = ((sin(time * 3543.0) * sin(time * 8721.0)) * 0.02)

                    leftSample = (rhodes + lofiBass + kick + noise) * 0.8
                    rightSample = (rhodes * 1.05 + lofiBass + kick + noise) * 0.8
                }

                MusicStyle.AMBIENT_SPACE -> {
                    // Slow undulating lush tones
                    var ambient = 0.0
                    val lfo = (sin(2 * PI * 0.2 * time) + 1.0) * 0.5
                    for ((idx, f) in chord.withIndex()) {
                        val detune = 1.0 + (idx * 0.003)
                        ambient += sin(2 * PI * (f * detune) * time) * (0.15 / chord.size)
                    }
                    ambient *= (0.7 + 0.3 * lfo)

                    // Resonant shimmer
                    val shimmer = sin(2 * PI * (chord[0] * 4.0) * time) * 0.05 * lfo

                    leftSample = (ambient + shimmer) * 0.85
                    rightSample = (ambient * 0.95 + shimmer * 1.1) * 0.85
                }

                MusicStyle.ACOUSTIC_CHORD -> {
                    // Plucked notes
                    val strumStep = (beatSampleCounter * 3 / samplesPerBeat) % chord.size
                    val noteFreq = chord[strumStep]
                    val pluckEnv = exp(-beatProgress * 4.5)
                    val note = (sin(2 * PI * noteFreq * time) +
                            0.3 * sin(4 * PI * noteFreq * time) +
                            0.15 * sin(6 * PI * noteFreq * time)) * pluckEnv * 0.35

                    val sub = sin(2 * PI * (chord[0] * 0.5) * time) * 0.15

                    leftSample = (note + sub) * 0.9
                    rightSample = (note * 1.05 + sub) * 0.9
                }

                MusicStyle.CYBER_PULSE -> {
                    // Fast driving electro synth
                    val pulseEnv = exp(-beatProgress * 5.0)
                    val pulseFreq = chord[currentBeat % chord.size]
                    val sawWave = ((time * pulseFreq) % 1.0) * 2.0 - 1.0
                    val syn = sawWave * pulseEnv * 0.35

                    // High pulse beep
                    val beep = sin(2 * PI * 880.0 * time) * (if (beatSampleCounter < 800) 0.15 else 0.0)

                    val kick = exp(-beatProgress * 18.0) * sin(2 * PI * 65.0 * time) * 0.45

                    val total = (syn + beep + kick) * 0.75
                    leftSample = total
                    rightSample = total
                }
            }

            // Clamping
            val leftShort = (leftSample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
            val rightShort = (rightSample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()

            if (buffer.remaining() < 4) {
                fos.write(buffer.array(), 0, buffer.position())
                buffer.clear()
            }

            buffer.putShort(leftShort)
            buffer.putShort(rightShort)
        }

        if (buffer.position() > 0) {
            fos.write(buffer.array(), 0, buffer.position())
            buffer.clear()
        }

        fos.flush()
        fos.close()

        // Patch WAV header with actual lengths
        val totalAudioLen = totalSamples * CHANNELS * (BITS_PER_SAMPLE / 8)
        val totalDataLen = totalAudioLen + 36
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeInt(Integer.reverseBytes(totalDataLen))
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeInt(Integer.reverseBytes(16)) // subchunk1size
        raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM = 1
        raf.writeShort(java.lang.Short.reverseBytes(CHANNELS.toShort()).toInt())
        raf.writeInt(Integer.reverseBytes(SAMPLE_RATE))
        raf.writeInt(Integer.reverseBytes(byteRate))
        raf.writeShort(java.lang.Short.reverseBytes((CHANNELS * BITS_PER_SAMPLE / 8).toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(BITS_PER_SAMPLE.toShort()).toInt())
        raf.writeBytes("data")
        raf.writeInt(Integer.reverseBytes(totalAudioLen))
        raf.close()
    }
}
