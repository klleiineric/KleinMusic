package app.kleinmusic.browser

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Uma aba = um WebView próprio + o estado que a tela precisa (voltar, avançar, carregando...).
 * Cada aba mantém o seu histórico de navegação dentro do próprio WebView.
 */
class TabController(
    app: Application,
    private val onUrl: (String) -> Unit,
    private val onTitle: (String) -> Unit,
    private val onVisit: (title: String, url: String) -> Unit
) {
    private val appContext: Context = app
    private val wrapper = MutableContextWrapper(app)
    val webView: WebView = WebView(wrapper)

    var canGoBack by mutableStateOf(false)
        private set
    var canGoForward by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        configure()
    }

    // ---------- Ações dos botões ----------

    fun load(url: String) {
        if (url.isBlank()) return
        error = null
        webView.loadUrl(url)
    }

    fun goBack() {
        if (webView.canGoBack()) {
            error = null
            webView.goBack()
        }
    }

    fun goForward() {
        if (webView.canGoForward()) {
            error = null
            webView.goForward()
        }
    }

    fun reload() {
        error = null
        webView.reload()
    }

    fun stop() {
        webView.stopLoading()
        loading = false
        progress = 100
    }

    // ---------- Ligação com a tela ----------

    /** Chamado quando a aba aparece: usa o contexto da Activity enquanto está na tela. */
    fun attach(host: Context) {
        wrapper.baseContext = host
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.onResume()
        refreshNav()
    }

    /** Chamado quando a aba sai da tela: solta a Activity para não vazar memória. */
    fun detach() {
        webView.onPause()
        (webView.parent as? ViewGroup)?.removeView(webView)
        wrapper.baseContext = appContext
    }

    fun destroy() {
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    private fun refreshNav() {
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()
    }

    private fun openExternal(url: String) {
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            wrapper.startActivity(intent)
        } catch (_: Exception) {
            // Nenhum app para abrir este tipo de link: ignora.
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        webView.settings.apply {
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
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.scheme?.lowercase() in EXTERNAL_SCHEMES) {
                    openExternal(uri.toString())
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                error = null
                loading = true
                refreshNav()
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                if (url.isNullOrBlank() || url == "about:blank") return
                refreshNav()
                onUrl(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loading = false
                refreshNav()
                if (view == null || url == null) return
                val title = view.title
                if (!title.isNullOrBlank()) onTitle(title)
                if (error == null && url != "about:blank") onVisit(title ?: url, url)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    this@TabController.error = error?.description?.toString() ?: "Erro desconhecido"
                    loading = false
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.cancel() // nunca continua com certificado inválido
                this@TabController.error = "O certificado de segurança do site é inválido."
                loading = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) onTitle(title)
            }
        }
    }

    private companion object {
        val EXTERNAL_SCHEMES = setOf("mailto", "tel", "sms", "geo", "market", "intent")
    }
}
