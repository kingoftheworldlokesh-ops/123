package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FocusPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("Shorts Shield", appName)
  }

  @Test
  fun `verify focus preferences default values`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = FocusPreferences(context)
    assertEquals(60, prefs.timeLimitSeconds.value)
    assertTrue(prefs.isShieldActive.value)
    assertTrue(prefs.strictOneShort.value)
  }
}

