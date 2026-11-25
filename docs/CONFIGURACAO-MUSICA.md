# Configuração necessária para o sistema musical

## AndroidManifest.xml (dentro de <manifest>)
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />

## AndroidManifest.xml (dentro de <application>)
<service
    android:name=".music.PlaybackService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>

## Dependências (app/build.gradle.kts), todas de código aberto
androidx.media3:media3-exoplayer
androidx.media3:media3-session
(versão estável atual de cada uma, a definir em gradle/libs.versions.toml)
