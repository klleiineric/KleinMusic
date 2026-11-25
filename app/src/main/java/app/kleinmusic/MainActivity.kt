package app.kleinmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kleinmusic.browser.BrowserViewModel
import app.kleinmusic.music.MusicViewModel
import app.kleinmusic.ui.browser.BrowserScreen
import app.kleinmusic.ui.library.LibraryScreen
import app.kleinmusic.ui.music.MusicListScreen
import app.kleinmusic.ui.player.MiniPlayer
import app.kleinmusic.ui.player.PlayerScreen
import app.kleinmusic.ui.theme.KleinColors
import app.kleinmusic.ui.theme.KleinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KleinTheme { KleinApp() }
        }
    }
}

private enum class AppTab(val label: String, val icon: String) {
    Browser("Navegador", "🌐"),
    Library("Biblioteca", "▤"),
    Music("Músicas", "♫")
}

@Composable
private fun KleinApp(
    browserVm: BrowserViewModel = viewModel(),
    musicVm: MusicViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(AppTab.Browser) }
    var showPlayer by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(KleinColors.Background)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    AppTab.Browser -> BrowserScreen(browserVm)
                    AppTab.Library -> LibraryScreen(browserVm) { tab = AppTab.Browser }
                    AppTab.Music -> MusicListScreen(musicVm)
                }
            }
            MiniPlayer(musicVm) { showPlayer = true }
            BottomBar(tab) { tab = it }
        }

        if (showPlayer) {
            BackHandler { showPlayer = false }
            Box(Modifier.fillMaxSize().systemBarsPadding()) {
                PlayerScreen(musicVm) { showPlayer = false }
            }
        }
    }
}

@Composable
private fun BottomBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(KleinColors.Background)) {
        AppTab.values().forEach { t ->
            val active = t == current
            Column(
                Modifier.weight(1f).clickable { onSelect(t) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(t.icon, fontSize = 19.sp, color = if (active) KleinColors.Orange else KleinColors.Muted)
                Text(t.label, fontSize = 11.sp, color = if (active) KleinColors.Text else KleinColors.Muted)
            }
        }
    }
}
