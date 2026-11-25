package app.kleinmusic.browser

import android.util.Patterns
import java.net.URI
import java.net.URLEncoder

object UrlUtils {
    private const val SEARCH = "https://duckduckgo.com/?q="
    private val LOCAL = Regex("""^(localhost|\d{1,3}(\.\d{1,3}){3})(:\d+)?(/.*)?$""")

    /** Transforma o que a pessoa digitou em um endereço ou em uma pesquisa. */
    fun toUrl(input: String): String {
        val text = input.trim()
        if (text.isEmpty()) return ""
        if (text.startsWith("http://") || text.startsWith("https://")) return text
        if (LOCAL.matches(text)) return "http://$text"
        val parecendoEndereco = !text.contains(' ') && Patterns.WEB_URL.matcher(text).matches()
        return if (parecendoEndereco) "https://$text"
        else SEARCH + URLEncoder.encode(text, "UTF-8")
    }

    fun host(url: String): String =
        runCatching { URI(url).host ?: url }.getOrDefault(url).removePrefix("www.")
}
