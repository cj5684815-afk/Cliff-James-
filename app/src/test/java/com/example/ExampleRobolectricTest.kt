package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PulseMusic", appName)
    }

    @Test
    fun `track duration formatting is correct`() {
        val track = Track(
            title = "Test Beat",
            artist = "Artist",
            durationMs = 125000L, // 2 mins 5 seconds
            filePath = "/fake/path/test.wav"
        )
        assertEquals("02:05", track.durationFormatted)
    }
}
