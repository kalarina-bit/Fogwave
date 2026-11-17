package cc.skysparkle.fogwave

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cc.skysparkle.fogwave.ui.screens.HomeScreen
import cc.skysparkle.fogwave.ui.screens.Onboarding
import cc.skysparkle.fogwave.ui.screens.OnboardingScreen
import cc.skysparkle.fogwave.ui.theme.AppThemeState
import cc.skysparkle.fogwave.ui.theme.FogwaveTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppThemeState.load(this)
    Onboarding.load(this)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    )
    setContent {
      FogwaveTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          // First-launch guide in place of the home screen; the player state lives in the
          // activity's view model, so nothing is lost while the guide is open.
          Crossfade(targetState = Onboarding.visible, label = "onboarding") { guide ->
            if (guide) OnboardingScreen(onDone = { Onboarding.finish(this@MainActivity) }) else HomeScreen()
          }
        }
      }
    }
  }
}
