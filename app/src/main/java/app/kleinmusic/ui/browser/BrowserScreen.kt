package app.kleinmusic.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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

private val EXTERNAL_SCHEMES = setOf("mailto", "tel", "sms", "geo", "market", "intent")

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(vm: BrowserViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val tabs by vm.tabs.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val pending by vm.pendingUrl.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeId } ?: return

    var canBack by remember { mutableStateOf(false) }
    var canForward by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableStateOf(100) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var field by remember(activeId, activeTab.url) { mutableStateOf(TextFieldValue(activeTab.url)) }

    val currentId = rememberUpdatedState(activeId)
    val clearNext = remember { AtomicBoolean(false) }

    // Um único WebView para todas as abas: economiza memória.
    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                // Alguns sites (ex.: login) recusam o identificador "wv" de WebView.
                userAgentString = userAgentString.replace("; wv", "")
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    if (uri.scheme?.lowercase() in EXTERNAL_SCHEMES) {
                        openExternal(context, uri.toString())
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    loadError = null
                }

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
                    if (loadError == null) vm.recordVisit(view.title ?: url, url)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        loadError = error?.description?.toString() ?: "Erro desconhecido"
                    }
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.cancel() // nunca continua com certificado inválido
                    loadError = "O certificado de segurança do site é inválido."
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    loadProgress = newProgress
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) vm.update(currentId.value, title = title)
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
        loadError = null
        vm.update(activeId, url = url)
        webView.loadUrl(url)
    }

    // Troca de aba: guarda o estado da anterior e restaura a nova.
    var shownId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeId) {
        shownId?.let { if (it != activeId) saveCurrent(it) }
        loadError = null
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

    val hasPage = activeTab.url.isNotBlank()
    val loading = hasPage && loadProgress < 100
    val isFavorite = favorites.any { it.url == activeTab.url }

    Column(Modifier.fillMaxSize().background(KleinColors.Background)) {
        // Barra de endereço
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                .onFocusChanged { if (it.isFocused) field = field.copy(selection = TextRange(0, field.text.length)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text("Buscar ou digitar endereço") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                load(UrlUtils.toUrl(field.text))
                keyboard?.hide()
                focusManager.clearFocus()
            })
        )
        // Indicador de carregamento
        if (loading) {
            LinearProgressIndicator(progress = { loadProgress / 100f }, modifier = Modifier.fillMaxWidth())
        }

        // Faixa de abas
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabChip(tab, tab.id == activeId, { vm.select(tab.id) }, { vm.close(tab.id) })
            }
        }

        // Conteúdo: o WebView fica sempre presente; as outras telas o cobrem quando necessário.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            if (!hasPage) {
                NewTabPage(favorites) { load(it) }
            } else if (loadError != null) {
                ErrorPage(loadError ?: "") { webView.reload() }
            }
        }

        // Barra de ferramentas do navegador
        Row(
            Modifier.fillMaxWidth().background(KleinColors.Surface),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton("‹", "Voltar", canBack) { webView.goBack() }
            ToolButton("›", "Avançar", canForward) { webView.goForward() }
            ToolButton(if (loading) "✕" else "⟳", if (loading) "Parar" else "Atualizar", hasPage) {
                if (loading) webView.stopLoading() else webView.reload()
            }
            ToolButton("＋", "Nova aba", true) { vm.newTab() }
            ToolButton(
                if (isFavorite) "★" else "☆", "Favorito", hasPage,
                tint = KleinColors.Orange
            ) { vm.toggleFavorite(activeTab.url, activeTab.title) }
        }
    }
}

private fun openExternal(context: Context, url: String) {
    try {
        val intent = if (url.startsWith("intent:")) {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                component = null
                selector = null
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Nenhum app para abrir este tipo de link: ignora.
    }
}

@Composable
private fun RowScope.ToolButton(
    label: String,
    description: String,
    enabled: Boolean,
    tint: Color = KleinColors.Text,
    onClick: () -> Unit
) {
    Text(
        label,
        Modifier.weight(1f)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (enabled) tint else KleinColors.Muted.copy(alpha = 0.4f),
        fontSize = 26.sp,
        textAlign = TextAlign.Center
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
private fun ErrorPage(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(KleinColors.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Não foi possível abrir a página", color = KleinColors.Text, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, Modifier.padding(vertical = 12.dp), color = KleinColors.Muted, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text("Tentar novamente") }
    }
}

@Composable
private fun NewTabPage(favorites: List<Favorite>, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(KleinColors.Background).padding(16.dp)) {
        Text("Klein Music", Modifier.fillMaxWidth().padding(vertical = 28.dp),
            color = KleinColors.Text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center)
        Text("Favoritos", color = KleinColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (favorites.isEmpty()) {
            Text("Abra uma página e toque em ☆ para favoritar.",
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
