package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.audio.AudioGenerator
import com.example.data.db.PlaylistDao
import com.example.data.db.TrackDao
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MusicRepository(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao
) {

    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracks()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> =
        playlistDao.getTracksForPlaylist(playlistId)

    fun searchTracks(query: String): Flow<List<Track>> =
        trackDao.searchTracks(query)

    suspend fun initDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = trackDao.getTracksCountOnce()
        if (count == 0) {
            // Generate offline preset tracks
            val presets = AudioGenerator.ensureDefaultOfflineTracks(context)
            val insertedIds = trackDao.insertTracks(presets)

            // Create initial curated playlists
            val playlist1Id = playlistDao.insertPlaylist(
                Playlist(
                    name = "Neon Synthetics",
                    description = "Electrifying retro-wave & cyber beats",
                    colorHex = "#FF007F",
                    iconName = "electric_bolt"
                )
            )

            val playlist2Id = playlistDao.insertPlaylist(
                Playlist(
                    name = "Lo-Fi Coffee Shop",
                    description = "Mellow relaxation & study flow",
                    colorHex = "#7C4DFF",
                    iconName = "headphones"
                )
            )

            val playlist3Id = playlistDao.insertPlaylist(
                Playlist(
                    name = "Zen Sanctuary",
                    description = "Deep ambient soundscapes for peace",
                    colorHex = "#00E5FF",
                    iconName = "spa"
                )
            )

            // Populate initial playlists
            if (insertedIds.size >= 3) {
                playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlist1Id, insertedIds[0], 0))
                playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlist1Id, insertedIds[4], 1))

                playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlist2Id, insertedIds[1], 0))
                playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlist2Id, insertedIds[3], 1))

                playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlist3Id, insertedIds[2], 0))
            }
        }
    }

    suspend fun toggleFavorite(trackId: Long, currentFav: Boolean) = withContext(Dispatchers.IO) {
        trackDao.setFavorite(trackId, !currentFav)
    }

    suspend fun incrementPlayCount(trackId: Long) = withContext(Dispatchers.IO) {
        trackDao.incrementPlayCount(trackId)
    }

    suspend fun deleteTrack(track: Track) = withContext(Dispatchers.IO) {
        trackDao.deleteTrack(track)
        // Delete underlying audio file
        try {
            val file = File(track.filePath)
            if (file.exists() && file.parentFile?.name == "offline_music") {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to delete track file: ${e.message}")
        }
    }

    suspend fun createPlaylist(name: String, description: String, colorHex: String, iconName: String): Long =
        withContext(Dispatchers.IO) {
            playlistDao.insertPlaylist(
                Playlist(
                    name = name.trim(),
                    description = description.trim(),
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }

    suspend fun updatePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        val maxPos = playlistDao.getMaxPositionInPlaylist(playlistId) ?: -1
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = maxPos + 1
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean = withContext(Dispatchers.IO) {
        playlistDao.isTrackInPlaylist(playlistId, trackId) > 0
    }

    suspend fun saveCustomSynthTrack(track: Track): Long = withContext(Dispatchers.IO) {
        trackDao.insertTrack(track)
    }

    suspend fun importAudioFile(uri: Uri): Track? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "imported_audio_${System.currentTimeMillis()}.mp3"

            // Query file display name
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }

            val musicDir = File(context.filesDir, "offline_music").apply { mkdirs() }
            val destFile = File(musicDir, "${System.currentTimeMillis()}_$fileName")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            // Extract metadata via MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            var title = fileName.substringBeforeLast(".")
            var artist = "Unknown Artist"
            var album = "Imported Tracks"
            var durationMs = 0L
            var genre = "Audio"

            try {
                retriever.setDataSource(destFile.absolutePath)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val metaGenre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaAlbum.isNullOrBlank()) album = metaAlbum
                if (!metaGenre.isNullOrBlank()) genre = metaGenre
                if (!metaDuration.isNullOrBlank()) durationMs = metaDuration.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                Log.w("MusicRepository", "Could not extract metadata: ${e.message}")
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }

            val sizeKb = destFile.length() / 1024
            val sizeFormatted = if (sizeKb >= 1024) String.format("%.1f MB", sizeKb / 1024.0) else "$sizeKb KB"

            val track = Track(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                filePath = destFile.absolutePath,
                genre = genre,
                colorHex = getRandomColorHex(),
                fileSizeFormatted = sizeFormatted,
                isPreset = false
            )

            val id = trackDao.insertTrack(track)
            track.copy(id = id)

        } catch (e: Exception) {
            Log.e("MusicRepository", "Error importing audio: ${e.message}", e)
            null
        }
    }

    private fun getRandomColorHex(): String {
        val colors = listOf("#FF007F", "#7C4DFF", "#00E5FF", "#00E676", "#FF9100", "#FF1744", "#3D5AFE", "#00B0FF")
        return colors.random()
    }
}
