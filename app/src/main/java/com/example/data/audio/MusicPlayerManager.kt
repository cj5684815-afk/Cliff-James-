package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

class MusicPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("EDM Bass")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _bassBoostActive = MutableStateFlow(true)
    val bassBoostActive: StateFlow<Boolean> = _bassBoostActive.asStateFlow()

    var onTrackPlayed: ((Track) -> Unit)? = null

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        _queue.value = newQueue
        val index = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _queueIndex.value = index
        playTrackInternal(track)
    }

    private fun playTrackInternal(track: Track) {
        val file = File(track.filePath)
        if (!file.exists()) {
            Log.e("MusicPlayerManager", "File not found: ${track.filePath}")
            return
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            mediaPlayer = MediaPlayer().apply {
                setDataSource(track.filePath)
                prepare()
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MusicPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
            }

            applyPlaybackSpeed(_playbackSpeed.value)
            mediaPlayer?.start()

            _currentTrack.value = track
            _isPlaying.value = true
            _durationMs.value = mediaPlayer?.duration?.toLong()?.coerceAtLeast(track.durationMs) ?: track.durationMs
            _currentPositionMs.value = 0L

            startProgressUpdates()
            onTrackPlayed?.invoke(track)

        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Error playing track: ${e.message}", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            _currentTrack.value?.let { playTrackInternal(it) }
            return
        }

        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressUpdates()
        } else {
            player.start()
            _isPlaying.value = true
            startProgressUpdates()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopProgressUpdates()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startProgressUpdates()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val target = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
            it.seekTo(target.toInt())
            _currentPositionMs.value = target
        }
    }

    fun playNext() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        val nextIndex = if (_isShuffle.value) {
            (currentQ.indices).filter { it != _queueIndex.value }.randomOrNull() ?: 0
        } else {
            (_queueIndex.value + 1) % currentQ.size
        }

        _queueIndex.value = nextIndex
        playTrackInternal(currentQ[nextIndex])
    }

    fun playPrevious() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        // If played more than 3 seconds, replay current song
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (_isShuffle.value) {
            (currentQ.indices).random()
        } else {
            if (_queueIndex.value - 1 < 0) currentQ.size - 1 else _queueIndex.value - 1
        }

        _queueIndex.value = prevIndex
        playTrackInternal(currentQ[prevIndex])
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.playbackParams = PlaybackParams().apply {
                    this.speed = speed
                }
            } catch (e: Exception) {
                Log.w("MusicPlayerManager", "Cannot set playback speed: ${e.message}")
            }
        }
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
    }

    fun toggleBassBoost() {
        _bassBoostActive.value = !_bassBoostActive.value
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }

        val totalSeconds = minutes * 60
        _sleepTimerSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            if (isActive && remaining == 0) {
                pause()
                _sleepTimerSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerSeconds.value = null
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentTrack.value?.let { playTrackInternal(it) }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val currentQ = _queue.value
                if (_queueIndex.value < currentQ.size - 1) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    stopProgressUpdates()
                }
            }
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition.toLong()
                        if (player.duration > 0) {
                            _durationMs.value = player.duration.toLong()
                        }
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressUpdates()
        sleepTimerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
