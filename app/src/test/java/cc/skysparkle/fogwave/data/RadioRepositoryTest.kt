package cc.skysparkle.fogwave.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import cc.skysparkle.fogwave.data.repository.RadioRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RadioRepositoryTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun clearPrefs() {
    context.getSharedPreferences("fogwave_prefs", Context.MODE_PRIVATE).edit { clear() }
  }

  @Test
  fun favoritesToggleAndSurviveARestart() {
    val repository = RadioRepository(context)
    assertTrue(repository.toggleFavorite("zip"))
    assertTrue(repository.toggleFavorite("lrt"))
    assertFalse(repository.toggleFavorite("zip"))

    assertEquals(setOf("lrt"), RadioRepository(context).favoritesFlow.value)
  }

  @Test
  fun unknownStationsAreDroppedFromSavedFavorites() {
    context.getSharedPreferences("fogwave_prefs", Context.MODE_PRIVATE)
      .edit { putStringSet("favorites", setOf("zip", "removed_station")) }

    assertEquals(setOf("zip"), RadioRepository(context).favoritesFlow.value)
  }

  @Test
  fun recentsPutTheLatestFirstWithoutDuplicatesAndKeepTen() {
    val repository = RadioRepository(context)
    val ids = repository.stations.map { it.id }
    ids.forEach { repository.pushRecent(it) }
    repository.pushRecent(ids[3])

    val recents = RadioRepository(context).recentsFlow.value
    assertEquals(10, recents.size)
    assertEquals(ids[3], recents.first())
    assertEquals(recents.size, recents.toSet().size)
  }

  @Test
  fun mostPlayedStationsComeFirst() {
    val repository = RadioRepository(context)
    repeat(3) { repository.recordStationPlayed("lr2") }
    repository.recordStationPlayed("rc")

    val sorted = RadioRepository(context).stationsSortedByPopularity()
    assertEquals(listOf("lr2", "rc"), sorted.take(2).map { it.id })
    assertEquals(repository.stations.size, sorted.size)
  }
}
