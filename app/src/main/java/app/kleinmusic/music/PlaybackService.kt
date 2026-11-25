package app.kleinmusic.music

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Serviço que toca a música mesmo com o app fechado ou a tela desligada.
 * O MediaSessionService cria sozinho a notificação e os controles da tela bloqueada.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    private val callback = object : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map {
                it.buildUpon().setUri(it.requestMetadata.mediaUri ?: it.localConfiguration?.uri).build()
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true      // pausa em ligações e outros apps
            )
            .setHandleAudioBecomingNoisy(true)      // pausa ao desconectar o fone
            .setWakeMode(C.WAKE_MODE_NETWORK)       // mantém CPU e Wi-Fi ativos tocando
            .build()
        session = MediaSession.Builder(this, player).setCallback(callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        // Se o app foi fechado e nada está tocando, encerra o serviço.
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
