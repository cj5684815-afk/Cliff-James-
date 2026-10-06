package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TrackDetailDialog
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SynthesizerDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialogType
import com.example.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MusicViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this,
            MusicViewModel.provideFactory(application)
        )[MusicViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MusicViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val sleepTimerSeconds by viewModel.sleepTimerSeconds.collectAsStateWithLifecycle()
    val equalizerPreset by viewModel.equalizerPreset.collectAsStateWithLifecycle()
    val bassBoostActive by viewModel.bassBoostActive.collectAsStateWithLifecycle()

    val filteredTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val activeDialog by viewModel.activeDialog.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedNavIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                // Docked Mini Player
                MiniPlayer(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onPlayNext = { viewModel.playNext() },
                    onExpand = { viewModel.setNowPlayingExpanded(true) }
                )

                // Navigation Bar
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = selectedNavIndex == 0,
                        onClick = { selectedNavIndex = 0 },
                        icon = {
                            Icon(
                                if (selectedNavIndex == 0) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("nav_library")
                    )

                    NavigationBarItem(
                        selected = selectedNavIndex == 1,
                        onClick = { selectedNavIndex = 1 },
                        icon = {
                            Icon(
                                if (selectedNavIndex == 1) Icons.AutoMirrored.Filled.QueueMusic else Icons.AutoMirrored.Outlined.QueueMusic,
                                contentDescription = "Playlists"
                            )
                        },
                        label = { Text("Playlists") },
                        modifier = Modifier.testTag("nav_playlists")
                    )

                    NavigationBarItem(
                        selected = selectedNavIndex == 2,
                        onClick = { selectedNavIndex = 2 },
                        icon = {
                            Icon(
                                if (selectedNavIndex == 2) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites"
                            )
                        },
                        label = { Text("Favorites") },
                        modifier = Modifier.testTag("nav_favorites")
                    )

                    NavigationBarItem(
                        selected = selectedNavIndex == 3,
                        onClick = { selectedNavIndex = 3 },
                        icon = {
                            Icon(
                                if (selectedNavIndex == 3) Icons.Default.Tune else Icons.Outlined.Tune,
                                contentDescription = "Sound"
                            )
                        },
                        label = { Text("Sound") },
                        modifier = Modifier.testTag("nav_equalizer")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedNavIndex) {
                0 -> {
                    LibraryScreen(
                        tracks = filteredTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        searchQuery = searchQuery,
                        selectedGenre = selectedGenre,
                        sortOrder = sortOrder,
                        onTrackClick = { track -> viewModel.playTrack(track, filteredTracks) },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        onAddToPlaylist = { track -> viewModel.openDialog(DialogType.AddToPlaylist(track)) },
                        onViewDetails = { track -> viewModel.openDialog(DialogType.TrackDetails(track)) },
                        onDeleteTrack = { track -> viewModel.openDialog(DialogType.DeleteTrackConfirm(track)) },
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        onGenreSelect = { g -> viewModel.setGenreFilter(g) },
                        onSortOrderSelect = { s -> viewModel.setSortOrder(s) },
                        onImportAudio = { uri -> viewModel.importAudioFile(uri) },
                        onOpenSynthesizer = { viewModel.openDialog(DialogType.SynthesizerStudio) },
                        contentPadding = innerPadding
                    )
                }
                1 -> {
                    PlaylistsScreen(
                        viewModel = viewModel,
                        playlists = allPlaylists,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onCreatePlaylistClick = { viewModel.openDialog(DialogType.CreatePlaylist) },
                        onDeletePlaylistClick = { pl -> viewModel.openDialog(DialogType.DeletePlaylistConfirm(pl)) },
                        onTrackClick = { track, q -> viewModel.playTrack(track, q) },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        onViewDetails = { track -> viewModel.openDialog(DialogType.TrackDetails(track)) },
                        contentPadding = innerPadding
                    )
                }
                2 -> {
                    FavoritesScreen(
                        tracks = favoriteTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onTrackClick = { track, q -> viewModel.playTrack(track, q) },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        onAddToPlaylist = { track -> viewModel.openDialog(DialogType.AddToPlaylist(track)) },
                        onViewDetails = { track -> viewModel.openDialog(DialogType.TrackDetails(track)) },
                        onDeleteTrack = { track -> viewModel.openDialog(DialogType.DeleteTrackConfirm(track)) },
                        contentPadding = innerPadding
                    )
                }
                3 -> {
                    EqualizerScreen(
                        isPlaying = isPlaying,
                        equalizerPreset = equalizerPreset,
                        bassBoostActive = bassBoostActive,
                        sleepTimerSeconds = sleepTimerSeconds,
                        onPresetChange = { preset -> viewModel.setEqualizerPreset(preset) },
                        onToggleBassBoost = { viewModel.toggleBassBoost() },
                        onOpenSleepTimer = { viewModel.openDialog(DialogType.SleepTimerPicker) },
                        onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                        onOpenSynthesizer = { viewModel.openDialog(DialogType.SynthesizerStudio) },
                        contentPadding = innerPadding
                    )
                }
            }
        }
    }

    // Fullscreen Now Playing Bottom Sheet
    if (isNowPlayingExpanded && currentTrack != null) {
        NowPlayingSheet(
            track = currentTrack,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            playbackSpeed = playbackSpeed,
            sleepTimerSeconds = sleepTimerSeconds,
            onDismiss = { viewModel.setNowPlayingExpanded(false) },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekTo = { pos -> viewModel.seekTo(pos) },
            onPlayNext = { viewModel.playNext() },
            onPlayPrevious = { viewModel.playPrevious() },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onCycleSpeed = {
                val nextSpeed = when (playbackSpeed) {
                    0.75f -> 1.0f
                    1.0f -> 1.25f
                    1.25f -> 1.5f
                    1.5f -> 2.0f
                    else -> 0.75f
                }
                viewModel.setPlaybackSpeed(nextSpeed)
            },
            onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
            onAddToPlaylist = { currentTrack?.let { viewModel.openDialog(DialogType.AddToPlaylist(it)) } },
            onOpenSleepTimer = { viewModel.openDialog(DialogType.SleepTimerPicker) },
            onOpenDetails = { currentTrack?.let { viewModel.openDialog(DialogType.TrackDetails(it)) } }
        )
    }

    // Dialogs Controller
    when (val dialog = activeDialog) {
        is DialogType.CreatePlaylist -> {
            CreatePlaylistDialog(
                onDismiss = { viewModel.dismissDialog() },
                onCreate = { name, desc, color, icon ->
                    viewModel.createPlaylist(name, desc, color, icon)
                    viewModel.dismissDialog()
                }
            )
        }
        is DialogType.AddToPlaylist -> {
            AddToPlaylistDialog(
                track = dialog.track,
                playlists = allPlaylists,
                onSelectPlaylist = { pl -> viewModel.addTrackToPlaylist(pl, dialog.track) },
                onCreateNewPlaylist = { viewModel.openDialog(DialogType.CreatePlaylist) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogType.TrackDetails -> {
            TrackDetailDialog(
                track = dialog.track,
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogType.SleepTimerPicker -> {
            SleepTimerDialog(
                currentRemainingSeconds = sleepTimerSeconds,
                onSetTimerMinutes = { mins -> viewModel.setSleepTimer(mins) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogType.SynthesizerStudio -> {
            SynthesizerDialog(
                onDismiss = { viewModel.dismissDialog() },
                onGenerate = { title, genre, bpm, style, duration, color ->
                    viewModel.createCustomSynthesizedTrack(title, genre, bpm, style, duration, color)
                }
            )
        }
        is DialogType.DeleteTrackConfirm -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Delete Track?") },
                text = { Text("Are you sure you want to delete \"${dialog.track.title}\" from your offline storage?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTrack(dialog.track)
                            viewModel.dismissDialog()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        is DialogType.DeletePlaylistConfirm -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Delete Playlist?") },
                text = { Text("Are you sure you want to delete the playlist \"${dialog.playlist.name}\"? Songs will remain in your offline library.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePlaylist(dialog.playlist)
                            viewModel.dismissDialog()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        null -> {}
    }
}
