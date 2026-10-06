package cc.skysparkle.fogwave

import android.content.Context
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import cc.skysparkle.fogwave.data.repository.RadioRepository
import cc.skysparkle.fogwave.ui.components.AboutPanel
import cc.skysparkle.fogwave.ui.theme.FogwaveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppInfoTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun appNameIsFogwave() {
    assertEquals("Fogwave", context.getString(R.string.app_name))
  }

  @Test
  fun aboutScreenShowsTheBuildVersion() {
    composeTestRule.setContent { FogwaveTheme { AboutPanel() } }

    composeTestRule
      .onNodeWithTag("about_version")
      .assertTextEquals(context.getString(R.string.about_version, BuildConfig.VERSION_NAME))
  }

  @Test
  fun stationsAreUniqueSecureAndDoNotUseExpiringTokens() {
    val stations = RadioRepository(context).stations

    assertEquals(stations.size, stations.map { it.id }.toSet().size)
    stations.forEach { station ->
      assertTrue(station.streamUrl, station.streamUrl.startsWith("https://"))
      // Short-lived tokens (e.g. revma's rj-tok) stop working within minutes.
      assertFalse(station.streamUrl, station.streamUrl.contains("rj-tok"))
      assertTrue(context.getString(station.descriptionRes).isNotBlank())
      assertTrue(station.genres.isNotEmpty())
    }
  }
}
