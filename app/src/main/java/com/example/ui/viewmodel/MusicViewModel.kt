package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioGenerator
import com.example.data.audio.MusicPlayerManager
import com.example.data.audio.RepeatMode
import com.example.data.db.PulseMusicDatabase
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DialogType {
    data object CreatePlaylist : DialogType()
    data class AddToPlaylist(val track: Track) : DialogType()
    data class TrackDetails(val track: Track) : DialogType()
    data class DeleteTrackConfirm(val track: Track) : DialogType()
    data class DeletePlaylistConfirm(val playlist: Playlist) : DialogType()
    data object SleepTimerPicker : DialogType()
    data object SynthesizerStudio : DialogType()
}

enum class SortOrder {
    DATE_ADDED,
    TITLE,
    ARTIST,
    DURATION,
    PLAY_COUNT
}

class MusicViewModel(
    application: Application,
    private val repository: MusicRepository,
    val playerManager: MusicPlayerManager
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    val activeDialog: StateFlow<DialogType?> = _activeDialog.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTracks: StateFlow<List<Track>> = combine(
        allTracks,
        _searchQuery,
        _selectedGenre,
        _sortOrder
    ) { tracks, query, genre, sort ->
        var result = tracks

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.genre.lowercase().contains(q)
            }
        }

        if (genre != null) {
            result = result.filter { it.genre.equals(genre, ignoreCase = true) }
        }

        when (sort) {
            SortOrder.DATE_ADDED -> result.sortedByDescending { it.dateAdded }
            SortOrder.TITLE -> result.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> result.sortedBy { it.artist.lowercase() }
            SortOrder.DURATION -> result.sortedByDescending { it.durationMs }
            SortOrder.PLAY_COUNT -> result.sortedByDescending { it.playCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player manager delegated flows
    val currentTrack: StateFlow<Track?> = playerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val sleepTimerSeconds: StateFlow<Int?> = playerManager.sleepTimerSeconds
    val equalizerPreset: StateFlow<String> = playerManager.equalizerPreset
    val bassBoostActive: StateFlow<Boolean> = playerManager.bassBoostActive

    init {
        viewModelScope.launch {
            repository.initDefaultDataIfEmpty()
        }

        playerManager.onTrackPlayed = { track ->
            viewModelScope.launch {
                repository.incrementPlayCount(track.id)
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        playerManager.playTrack(track, queue)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun playNext() = playerManager.playNext()
    fun playPrevious() = playerManager.playPrevious()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun setEqualizerPreset(preset: String) = playerManager.setEqualizerPreset(preset)
    fun toggleBassBoost() = playerManager.toggleBassBoost()
    fun setSleepTimer(minutes: Int) = playerManager.setSleepTimer(minutes)
    fun cancelSleepTimer() = playerManager.cancelSleepTimer()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenreFilter(genre: String?) {
        _selectedGenre.value = if (_selectedGenre.value == genre) null else genre
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun openDialog(dialog: DialogType) {
        _activeDialog.value = dialog
    }

    fun dismissDialog() {
        _activeDialog.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            if (currentTrack.value?.id == track.id) {
                playerManager.pause()
            }
            repository.deleteTrack(track)
            showSnackbar("Track \"${track.title}\" deleted")
        }
    }

    fun importAudioFile(uri: Uri) {
        viewModelScope.launch {
            val track = repository.importAudioFile(uri)
            if (track != null) {
                showSnackbar("Imported \"${track.title}\" to offline library!")
            } else {
                showSnackbar("Failed to import audio file")
            }
        }
    }

    fun createPlaylist(name: String, description: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            repository.createPlaylist(name, description, colorHex, iconName)
            showSnackbar("Created playlist \"$name\"")
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            showSnackbar("Playlist \"${playlist.name}\" deleted")
        }
    }

    fun addTrackToPlaylist(playlist: Playlist, track: Track) {
        viewModelScope.launch {
            if (repository.isTrackInPlaylist(playlist.id, track.id)) {
                showSnackbar("Track already in \"${playlist.name}\"")
            } else {
                repository.addTrackToPlaylist(playlist.id, track.id)
                showSnackbar("Added to \"${playlist.name}\"")
            }
            dismissDialog()
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, track.id)
            showSnackbar("Removed from playlist")
        }
    }

    fun getTracksForPlaylist(playlistId: Long): StateFlow<List<Track>> {
        return repository.getTracksForPlaylist(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun createCustomSynthesizedTrack(
        title: String,
        genre: String,
        bpm: Int,
        style: AudioGenerator.MusicStyle,
        durationSeconds: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            try {
                showSnackbar("Synthesizing audio track...")
                val track = AudioGenerator.createCustomTrack(
                    context = getApplication(),
                    title = title,
                    genre = genre,
                    bpm = bpm,
                    style = style,
                    durationSeconds = durationSeconds,
                    colorHex = colorHex
                )
                repository.saveCustomSynthTrack(track)
                showSnackbar("Added \"${track.title}\" to offline library!")
                dismissDialog()
            } catch (e: Exception) {
                showSnackbar("Synthesis failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = PulseMusicDatabase.getDatabase(application)
                    val repo = MusicRepository(application, db.trackDao(), db.playlistDao())
                    val playerManager = MusicPlayerManager(application)
                    return MusicViewModel(application, repo, playerManager) as T
                }
            }
    }
}
