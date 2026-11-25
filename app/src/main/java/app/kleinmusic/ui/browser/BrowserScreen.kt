package app.kleinmusic.ui.browser

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
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

@Composable
fun BrowserScreen(vm: BrowserViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val tabs by vm.tabs.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeId } ?: return

    // Controlador (e WebView) da aba ativa. Cada aba tem o seu.
    val controller = remember(activeId) { vm.controller(activeId) }
    var field by remember(activeId, activeTab.url) { mutableStateOf(TextFieldValue(activeTab.url)) }

    // Quando a aba sai da tela, solta a Activity (evita vazamento de memória).
    DisposableEffect(controller) { onDispose { controller.detach() } }

    // Botão voltar do celular: volta na página da aba, se houver.
    BackHandler(enabled = controller.canGoBack) { controller.goBack() }

    val hasPage = activeTab.url.isNotBlank()
    val loading = hasPage && controller.loading
    val isFavorite = favorites.any { it.url == activeTab.url }

    fun submit() {
        val url = UrlUtils.toUrl(field.text)
        if (url.isNotBlank()) {
            vm.navigate(url)
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    Column(Modifier.fillMaxSize().background(KleinColors.Background)) {
        // Barra de endereço: lê o texto e abre o endereço ou pesquisa no DuckDuckGo
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("endereco")
                .onFocusChanged { if (it.isFocused) field = field.copy(selection = TextRange(0, field.text.length)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text("Buscar ou digitar endereço") },
            trailingIcon = {
                Text(
                    "→",
                    Modifier.semantics { contentDescription = "Ir" }.clickable { submit() }.padding(12.dp),
                    color = KleinColors.Orange, fontSize = 22.sp
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() })
        )

        // Indicador de carregamento
        if (loading) {
            LinearProgressIndicator(progress = { controller.progress / 100f }, modifier = Modifier.fillMaxWidth())
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

        // Conteúdo: o WebView da aba ativa. As telas abaixo o cobrem quando necessário.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(activeId) {
                AndroidView(
                    factory = {
                        controller.attach(context)
                        controller.webView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!hasPage) {
                NewTabPage(favorites) { vm.navigate(it) }
            } else if (controller.error != null) {
                ErrorPage(controller.error ?: "") { controller.reload() }
            }
        }

        // Barra de ferramentas: cada botão chama uma ação real do WebView da aba ativa
        Row(
            Modifier.fillMaxWidth().background(KleinColors.Surface),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton("‹", "Voltar", controller.canGoBack) { controller.goBack() }
            ToolButton("›", "Avançar", controller.canGoForward) { controller.goForward() }
            ToolButton(if (loading) "✕" else "⟳", if (loading) "Parar" else "Atualizar", hasPage) {
                if (loading) controller.stop() else controller.reload()
            }
            ToolButton("＋", "Nova aba", true) { vm.newTab() }
            ToolButton(
                if (isFavorite) "★" else "☆", "Favorito", hasPage,
                tint = KleinColors.Orange
            ) { vm.toggleFavorite(activeTab.url, activeTab.title) }
        }
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
            Text("✕", Modifier.semantics { contentDescription = "Fechar aba" }
                .clickable(onClick = onClose).padding(start = 8.dp),
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
