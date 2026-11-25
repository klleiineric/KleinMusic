package app.kleinmusic.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val durationMs: Long = 0L
)

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(uri)
    // Guardamos o endereço também aqui: o serviço o usa para reconstruir o item.
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(uri)).build())
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).build())
    .build()
