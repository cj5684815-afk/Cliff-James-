package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Track
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackDetailDialog(
    track: Track,
    onDismiss: () -> Unit
) {
    val dateAddedStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        .format(Date(track.dateAdded))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Track Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Title", track.title)
                DetailRow("Artist", track.artist)
                DetailRow("Album", track.album)
                DetailRow("Genre", track.genre)
                DetailRow("Duration", track.durationFormatted)
                DetailRow("File Size", track.fileSizeFormatted)
                DetailRow("Storage Type", if (track.isPreset) "Synthesized Offline Asset" else "Internal Offline Storage")
                DetailRow("Plays", "${track.playCount} times")
                DetailRow("Date Added", dateAddedStr)

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Local File Path:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = track.filePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}
