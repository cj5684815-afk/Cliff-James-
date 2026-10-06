package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.audio.AudioGenerator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SynthesizerDialog(
    onDismiss: () -> Unit,
    onGenerate: (title: String, genre: String, bpm: Int, style: AudioGenerator.MusicStyle, durationSeconds: Int, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(AudioGenerator.MusicStyle.SYNTHWAVE) }
    var bpmSlider by remember { mutableFloatStateOf(118f) }
    var durationSlider by remember { mutableFloatStateOf(24f) }

    val colors = listOf("#FF007F", "#7C4DFF", "#00E5FF", "#00E676", "#FF9100", "#3D5AFE")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    val styles = listOf(
        AudioGenerator.MusicStyle.SYNTHWAVE to "Synthwave",
        AudioGenerator.MusicStyle.LOFI_CHILL to "Lo-Fi Chill",
        AudioGenerator.MusicStyle.AMBIENT_SPACE to "Ambient Space",
        AudioGenerator.MusicStyle.ACOUSTIC_CHORD to "Acoustic Folk",
        AudioGenerator.MusicStyle.CYBER_PULSE to "Cyber Pulse"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Synthesize Offline Track") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Track Title") },
                    placeholder = { Text("e.g. Midnight Highway") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_synth_title")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Acoustic Style & Genre",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    styles.forEach { (style, label) ->
                        FilterChip(
                            selected = style == selectedStyle,
                            onClick = {
                                selectedStyle = style
                                bpmSlider = when (style) {
                                    AudioGenerator.MusicStyle.LOFI_CHILL -> 82f
                                    AudioGenerator.MusicStyle.AMBIENT_SPACE -> 60f
                                    AudioGenerator.MusicStyle.SYNTHWAVE -> 118f
                                    AudioGenerator.MusicStyle.CYBER_PULSE -> 132f
                                    AudioGenerator.MusicStyle.ACOUSTIC_CHORD -> 96f
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tempo: ${bpmSlider.toInt()} BPM",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = bpmSlider,
                    onValueChange = { bpmSlider = it },
                    valueRange = 60f..150f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Duration: ${durationSlider.toInt()} seconds",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = durationSlider,
                    onValueChange = { durationSlider = it },
                    valueRange = 10f..45f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Track Theme Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = hex == selectedColor
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank {
                        when (selectedStyle) {
                            AudioGenerator.MusicStyle.SYNTHWAVE -> "Synthwave Dream"
                            AudioGenerator.MusicStyle.LOFI_CHILL -> "Lo-Fi Sunset"
                            AudioGenerator.MusicStyle.AMBIENT_SPACE -> "Cosmic Drift"
                            AudioGenerator.MusicStyle.ACOUSTIC_CHORD -> "Acoustic Breeze"
                            AudioGenerator.MusicStyle.CYBER_PULSE -> "Cyber Overdrive"
                        }
                    }
                    val genre = when (selectedStyle) {
                        AudioGenerator.MusicStyle.SYNTHWAVE -> "Synthwave"
                        AudioGenerator.MusicStyle.LOFI_CHILL -> "Lo-Fi Beats"
                        AudioGenerator.MusicStyle.AMBIENT_SPACE -> "Ambient"
                        AudioGenerator.MusicStyle.ACOUSTIC_CHORD -> "Acoustic"
                        AudioGenerator.MusicStyle.CYBER_PULSE -> "Electro EDM"
                    }
                    onGenerate(
                        finalTitle,
                        genre,
                        bpmSlider.toInt(),
                        selectedStyle,
                        durationSlider.toInt(),
                        selectedColor
                    )
                },
                modifier = Modifier.testTag("btn_confirm_synthesize")
            ) {
                Text("Synthesize & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
