package app.kleinmusic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kleinmusic.music.MusicViewModel
import app.kleinmusic.ui.theme.KleinColors
import kotlin.math.absoluteValue

private fun hueOf(title: String) = (title.hashCode().absoluteValue % 360).toFloat()

private fun time(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun PlayerScreen(vm: MusicViewModel, onClose: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val volume by vm.volume.collectAsState()
    var drag by remember { mutableStateOf<Float?>(null) }
    val hue = hueOf(ui.title)
    val fraction = if (ui.durationMs > 0) ui.positionMs / ui.durationMs.toFloat() else 0f

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.hsl(hue, 0.55f, 0.26f), KleinColors.Background)))
            .padding(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⌄", Modifier.clickable(onClick = onClose).padding(4.dp), color = KleinColors.Text, fontSize = 24.sp)
            Text("Reproduzindo", color = KleinColors.Text, fontSize = 14.sp)
            Text("", Modifier.size(24.dp))
        }
        Box(
            Modifier.padding(vertical = 22.dp).fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color.hsl(hue, 0.75f, 0.55f), Color.hsl((hue + 50) % 360, 0.7f, 0.28f))))
        )
        Text(ui.title, color = KleinColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(ui.artist, color = KleinColors.Muted, maxLines = 1)

        Slider(
            value = drag ?: fraction,
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let { vm.seekTo((it * ui.durationMs).toLong()) }
                drag = null
            },
            colors = SliderDefaults.colors(thumbColor = KleinColors.Text, activeTrackColor = KleinColors.Text)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(time(ui.positionMs), color = KleinColors.Muted, fontSize = 13.sp)
            Text(time(ui.durationMs), color = KleinColors.Muted, fontSize = 13.sp)
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⇄", Modifier.clickable { vm.toggleShuffle() }.padding(8.dp),
                color = if (ui.shuffle) KleinColors.Orange else KleinColors.Muted, fontSize = 22.sp)
            Text("⏮", Modifier.clickable { vm.previous() }.padding(8.dp), color = KleinColors.Text, fontSize = 26.sp)
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(KleinColors.Text).clickable { vm.playPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (ui.isPlaying) "❚❚" else "▶", color = KleinColors.Background, fontSize = 22.sp)
            }
            Text("⏭", Modifier.clickable { vm.next() }.padding(8.dp), color = KleinColors.Text, fontSize = 26.sp)
            Text("↻", Modifier.clickable { vm.toggleRepeat() }.padding(8.dp),
                color = if (ui.repeatAll) KleinColors.Orange else KleinColors.Muted, fontSize = 22.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔈", fontSize = 16.sp)
            Slider(
                value = volume,
                onValueChange = { vm.setVolume(it) },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                colors = SliderDefaults.colors(thumbColor = KleinColors.Orange, activeTrackColor = KleinColors.Orange)
            )
        }
    }
}

@Composable
fun MiniPlayer(vm: MusicViewModel, onOpen: () -> Unit) {
    val ui by vm.ui.collectAsState()
    if (!ui.hasMedia) return
    val hue = hueOf(ui.title)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(KleinColors.Elevated)
            .clickable(onClick = onOpen).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color.hsl(hue, 0.75f, 0.55f), Color.hsl((hue + 50) % 360, 0.7f, 0.28f))))
        )
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(ui.title, color = KleinColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(ui.artist, color = KleinColors.Muted, fontSize = 13.sp, maxLines = 1)
        }
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(KleinColors.Text).clickable { vm.playPause() },
            contentAlignment = Alignment.Center
        ) {
            Text(if (ui.isPlaying) "❚❚" else "▶", color = KleinColors.Background, fontSize = 14.sp)
        }
    }
}
