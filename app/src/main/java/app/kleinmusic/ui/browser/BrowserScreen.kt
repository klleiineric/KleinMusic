package app.kleinmusic.ui.browser

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.kleinmusic.browser.BrowserTab
import app.kleinmusic.browser.BrowserViewModel
import app.kleinmusic.browser.UrlUtils
import app.kleinmusic.data.Favorite
import app.kleinmusic.ui.theme.KleinColors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun BrowserScreen(vm: BrowserViewModel) {
    val context = LocalContext.current
    val tabs by vm.tabs.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val pending by vm.pendingUrl.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeId } ?: return

    var canBack by remember { mutableStateOf(false) }
    var canForward by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(100) }
    var address by remember(activeId, activeTab.url) { mutableStateOf(activeTab.url) }

    val currentId = rememberUpdatedState(activeId)
    val clearNext = remember { AtomicBoolean(false) }

    // Um único WebView para todas as abas: economiza memória.
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    if (view == null || url.isNullOrBlank() || url == "about:blank") return
                    canBack = view.canGoBack()
                    canForward = view.canGoForward()
                    vm.update(currentId.value, url = url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (view == null || url == null) return
                    // Primeira página de uma aba nova: apaga o histórico da aba anterior.
                    if (clearNext.getAndSet(false)) {
                        view.clearHistory()
                        canBack = false
                        canForward = false
                    }
                    vm.update(currentId.value, title = view.title)
                    vm.recordVisit(view.title ?: url, url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress
                }
            }
        }
    }

    fun saveCurrent(id: Int) {
        val tab = vm.tab(id) ?: return
        if (tab.url.isBlank()) return
        val bundle = Bundle()
        webView.saveState(bundle)
        vm.saveState(id, bundle)
    }

    fun load(url: String) {
        if (url.isBlank()) return
        vm.update(activeId, url = url)
        webView.loadUrl(url)
    }

    // Troca de aba: guarda o estado da anterior e restaura a nova.
    var shownId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeId) {
        shownId?.let { if (it != activeId) saveCurrent(it) }
        val saved = vm.tab(activeId)?.state
        if (saved != null) {
            clearNext.set(false)
            webView.restoreState(saved)
        } else {
            clearNext.set(true)
            canBack = false
            canForward = false
        }
        shownId = activeId
    }

    // Endereço pedido por outra tela (Biblioteca).
    LaunchedEffect(pending) {
        pending?.let { load(it); vm.consumePending() }
    }

    DisposableEffect(Unit) {
        onDispose {
            saveCurrent(currentId.value)
            webView.destroy()
        }
    }

    BackHandler(enabled = canBack) { webView.goBack() }

    val isFavorite = favorites.any { it.url == activeTab.url }

    Column(Modifier.fillMaxSize().background(KleinColors.Background)) {
        // Barra superior: escudo, voltar, avançar, endereço, favorito
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("🛡", Modifier.padding(horizontal = 6.dp), fontSize = 18.sp) // bloqueador entra em outra etapa
            NavButton("‹", canBack) { webView.goBack() }
            NavButton("›", canForward) { webView.goForward() }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("Buscar ou digitar endereço") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { load(UrlUtils.toUrl(address)) })
            )
            Text(
                if (isFavorite) "★" else "☆",
                Modifier.clickable { vm.toggleFavorite(activeTab.url, activeTab.title) }.padding(8.dp),
                color = KleinColors.Orange, fontSize = 22.sp
            )
        }
        if (progress < 100 && activeTab.url.isNotBlank()) {
            LinearProgressIndicator(progress = { progress / 100f }, Modifier.fillMaxWidth())
        }

        // Faixa de abas
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabChip(tab, tab.id == activeId, { vm.select(tab.id) }, { vm.close(tab.id) })
            }
            item {
                Text("＋", Modifier.clickable { vm.newTab() }.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = KleinColors.Text, fontSize = 20.sp)
            }
        }

        // Conteúdo: WebView sempre presente; a página da aba nova cobre ele quando a aba está vazia.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            if (activeTab.url.isBlank()) {
                NewTabPage(favorites) { load(it) }
            }
        }
    }
}

@Composable
private fun NavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        Modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
        color = if (enabled) KleinColors.Text else KleinColors.Muted.copy(alpha = 0.4f),
        fontSize = 28.sp
    )
}

@Composable
private fun TabChip(tab: BrowserTab, active: Boolean, onSelect: () -> Unit, onClose: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KleinColors.Surface,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) KleinColors.Orange else KleinColors.Elevated),
        modifier = Modifier.clickable(onClick = onSelect)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tab.title, Modifier.widthIn(max = 120.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = KleinColors.Text, fontSize = 13.sp)
            Text("✕", Modifier.clickable(onClick = onClose).padding(start = 8.dp),
                color = KleinColors.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NewTabPage(favorites: List<Favorite>, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(KleinColors.Background).padding(16.dp)) {
        Text("Klein Music", Modifier.fillMaxWidth().padding(vertical = 28.dp),
            color = KleinColors.Text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("Favoritos", color = KleinColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (favorites.isEmpty()) {
            Text("Toque em ☆ ao lado do endereço para favoritar uma página.",
                Modifier.padding(top = 8.dp), color = KleinColors.Muted)
        }
        favorites.forEach { fav ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(fav.url) }.padding(vertical = 10.dp)) {
                Text(fav.title, color = KleinColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(UrlUtils.host(fav.url), color = KleinColors.Muted, fontSize = 13.sp)
            }
        }
    }
}
