package app.kleinmusic.browser

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.kleinmusic.data.AppDatabase
import app.kleinmusic.data.Favorite
import app.kleinmusic.data.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrowserTab(
    val id: Int,
    val title: String = "Nova aba",
    val url: String = "",
    val state: Bundle? = null   // histórico interno da aba, guardado ao trocar de aba
)

class BrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private var nextId = 1
    private var lastRecorded = ""

    private val _tabs = MutableStateFlow(listOf(BrowserTab(nextId++)))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeId = MutableStateFlow(_tabs.value.first().id)
    val activeId: StateFlow<Int> = _activeId.asStateFlow()

    // Endereço pedido por outra tela (ex.: Biblioteca) para abrir na aba ativa
    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl.asStateFlow()

    val history: StateFlow<List<HistoryEntry>> = db.history().all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Favorite>> = db.favorites().all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tab(id: Int): BrowserTab? = _tabs.value.firstOrNull { it.id == id }

    fun newTab() {
        val t = BrowserTab(nextId++)
        _tabs.value = _tabs.value + t
        _activeId.value = t.id
    }

    fun select(id: Int) { _activeId.value = id }

    fun close(id: Int) {
        val list = _tabs.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val rest = list.filter { it.id != id }
        if (rest.isEmpty()) {
            val t = BrowserTab(nextId++)
            _tabs.value = listOf(t)
            _activeId.value = t.id
            return
        }
        _tabs.value = rest
        if (_activeId.value == id) _activeId.value = rest[minOf(index, rest.size - 1)].id
    }

    fun update(id: Int, title: String? = null, url: String? = null) {
        _tabs.value = _tabs.value.map {
            if (it.id == id) it.copy(title = title ?: it.title, url = url ?: it.url) else it
        }
    }

    fun saveState(id: Int, state: Bundle) {
        _tabs.value = _tabs.value.map { if (it.id == id) it.copy(state = state) else it }
    }

    fun requestOpen(url: String) { _pendingUrl.value = url }
    fun consumePending() { _pendingUrl.value = null }

    fun recordVisit(title: String, url: String) {
        if (url.isBlank() || url == "about:blank" || url == lastRecorded) return
        lastRecorded = url
        viewModelScope.launch {
            db.history().insert(
                HistoryEntry(title = title.ifBlank { url }, url = url, visitedAt = System.currentTimeMillis())
            )
        }
    }

    fun clearHistory() { viewModelScope.launch { db.history().clear() } }

    fun toggleFavorite(url: String, title: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            if (favorites.value.any { it.url == url }) db.favorites().remove(url)
            else db.favorites().add(Favorite(url, title.ifBlank { url }, System.currentTimeMillis()))
        }
    }
}
