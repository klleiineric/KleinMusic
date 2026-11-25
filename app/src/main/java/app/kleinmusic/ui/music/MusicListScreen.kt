package app.kleinmusic.ui.music

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kleinmusic.music.MusicViewModel
import app.kleinmusic.ui.theme.KleinColors

@Composable
fun MusicListScreen(vm: MusicViewModel) {
    val tracks by vm.tracks.collectAsState()
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

    val askAudio = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.loadLocal()
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Android 13+: sem esta permissão a notificação de reprodução não aparece.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize().background(KleinColors.Background).padding(16.dp)) {
        Text("Músicas", color = KleinColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (tracks.isEmpty()) {
            Text("Carregue as músicas que estão no seu aparelho.", Modifier.padding(vertical = 12.dp),
                color = KleinColors.Muted)
            Button(onClick = { askAudio.launch(audioPermission) }) { Text("Carregar músicas do aparelho") }
        }
        LazyColumn {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                Column(Modifier.fillMaxWidth().clickable { vm.playTracks(tracks, index) }.padding(vertical = 10.dp)) {
                    Text(track.title, color = KleinColors.Text, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = KleinColors.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}
