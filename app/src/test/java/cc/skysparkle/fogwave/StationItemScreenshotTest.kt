package cc.skysparkle.fogwave

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import cc.skysparkle.fogwave.data.model.Genre
import cc.skysparkle.fogwave.data.model.IconSizeStep
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.data.model.ViewMode
import cc.skysparkle.fogwave.ui.components.StationItem
import cc.skysparkle.fogwave.ui.theme.FogwaveTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StationItemScreenshotTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val station = RadioStation(
    id = "zipfm",
    name = "ZIP FM",
    descriptionRes = R.string.station_desc_zip,
    streamUrl = "https://example.com/stream",
    iconResId = R.drawable.ic_station_zipfm,
    genres = listOf(Genre.HITS, Genre.POP),
    bitrate = "128 kbps"
  )

  @Test
  fun stationItem_list_screenshot() {
    composeTestRule.setContent {
      FogwaveTheme {
        StationItem(
          station = station,
          isCurrent = true,
          isPlaying = false,
          viewMode = ViewMode.LIST,
          sizeStep = IconSizeStep.M,
          onClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/station_item.png")
  }
}
