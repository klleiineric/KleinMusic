package app.kleinmusic.music

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Uma fonte de músicas. Só entram fontes com direito de uso (arquivos seus, música livre/licenciada). */
interface AudioSource {
    val name: String
    suspend fun tracks(): List<Track>
}

/** Músicas guardadas no próprio aparelho (precisa da permissão de áudio). */
class LocalAudioSource(private val context: Context) : AudioSource {
    override val name = "Músicas do aparelho"

    override suspend fun tracks(): List<Track> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        context.contentResolver.query(
            collection, projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val artist = c.getString(artistCol)
                list += Track(
                    id = id.toString(),
                    title = c.getString(titleCol) ?: "Sem título",
                    artist = if (artist == null || artist == MediaStore.UNKNOWN_STRING) "Artista desconhecido" else artist,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    durationMs = c.getLong(durCol)
                )
            }
        }
        list
    }
}

/** Lista fixa de endereços diretos de áudio com licença livre (ex.: arquivos do Internet Archive ou Jamendo). */
class UrlAudioSource(override val name: String, private val items: List<Track>) : AudioSource {
    override suspend fun tracks(): List<Track> = items
}
