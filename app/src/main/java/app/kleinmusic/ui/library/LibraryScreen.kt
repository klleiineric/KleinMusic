package app.kleinmusic.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kleinmusic.browser.BrowserViewModel
import app.kleinmusic.browser.UrlUtils
import app.kleinmusic.ui.theme.KleinColors

@Composable
fun LibraryScreen(vm: BrowserViewModel, onOpenInBrowser: () -> Unit) {
    var filter by remember { mutableStateOf("fav") }
    val favorites by vm.favorites.collectAsState()
    val history by vm.history.collectAsState()

    fun open(url: String) {
        vm.requestOpen(url)   // a tela do navegador carrega o endereço
        onOpenInBrowser()     // e a navegação troca para a aba Navegador
    }

    Column(Modifier.fillMaxSize().background(KleinColors.Background).padding(16.dp)) {
        Text("Biblioteca", color = KleinColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == "fav", onClick = { filter = "fav" }, label = { Text("Favoritos") })
            FilterChip(selected = filter == "hist", onClick = { filter = "hist" }, label = { Text("Histórico") })
        }
        LazyColumn {
            if (filter == "fav") {
                items(favorites, key = { it.url }) { fav ->
                    SiteRow(fav.title, UrlUtils.host(fav.url)) { open(fav.url) }
                }
            } else {
                items(history, key = { it.id }) { h ->
                    SiteRow(h.title, UrlUtils.host(h.url)) { open(h.url) }
                }
                item { TextButton(onClick = { vm.clearHistory() }) { Text("Limpar histórico") } }
            }
        }
    }
}

@Composable
private fun SiteRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Text(title, color = KleinColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = KleinColors.Muted, fontSize = 13.sp)
    }
}
