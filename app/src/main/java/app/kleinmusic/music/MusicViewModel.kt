package app.kleinmusic.music

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class PlayerUi(
    val hasMedia: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatAll: Boolean = false
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var controller: MediaController? = null

    private val _ui = MutableStateFlow(PlayerUi())
    val ui: StateFlow<PlayerUi> = _ui.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _volume = MutableStateFlow(readVolume())
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
    }

    init {
        connect()
        // Atualiza a posição da música e o volume (que também muda pelos botões do aparelho).
        viewModelScope.launch {
            while (true) {
                delay(500)
                _volume.value = readVolume()
                if (controller?.isPlaying == true) refresh()
            }
        }
    }

    private fun connect() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(listener) }
            refresh()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun refresh() {
        val c = controller ?: return
        _ui.value = PlayerUi(
            hasMedia = c.mediaItemCount > 0,
            title = c.mediaMetadata.title?.toString() ?: "",
            artist = c.mediaMetadata.artist?.toString() ?: "",
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L),   // duração desconhecida vem negativa
            shuffle = c.shuffleModeEnabled,
            repeatAll = c.repeatMode == Player.REPEAT_MODE_ALL
        )
    }

    fun load(source: AudioSource) {
        viewModelScope.launch { _tracks.value = source.tracks() }
    }

    fun loadLocal() = load(LocalAudioSource(getApplication()))

    fun playTracks(list: List<Track>, startIndex: Int) {
        val c = controller ?: return
        c.setMediaItems(list.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_OFF
            else Player.REPEAT_MODE_ALL
        }
    }

    private fun readVolume(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max == 0) 0f else audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    /** Volume de mídia do aparelho, de 0f a 1f. */
    fun setVolume(fraction: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction * max).roundToInt(), 0)
        _volume.value = fraction
    }

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
