package com.novatv.player

import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun Playback(channel: Channel, modifier: Modifier = Modifier, trakt: (String, Double) -> Unit = { _, _ -> }, close: () -> Unit = {}, report: (String) -> Unit) {
 val retryFocus = remember { FocusRequester() }
 var controlsVisible by remember { mutableStateOf(true) }
 var view by remember { mutableStateOf<PlayerView?>(null) }
 var errorText by remember { mutableStateOf("") }
 LaunchedEffect(errorText) { if (errorText.isNotBlank()) retryFocus.requestFocus() }
 BackHandler { if (controlsVisible && errorText.isBlank()) view?.hideController() else close() }
 val context = LocalContext.current
 val lifecycle = LocalLifecycleOwner.current.lifecycle
 val latestReport by rememberUpdatedState(report)
 val player = remember {
  ExoPlayer.Builder(context)
   .setTrackSelector(DefaultTrackSelector(context))
   .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(15000, 50000, 750, 1500).build())
   .build().apply {
    setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
    setHandleAudioBecomingNoisy(true)
   }
 }
 LaunchedEffect(channel.url) {
  errorText = ""
  latestReport("Laden…")
  player.setMediaItem(MediaItem.fromUri(channel.url)); player.prepare(); player.playWhenReady = true
 }
 DisposableEffect(player, lifecycle) {
  var resume = false
  val scrobble = com.novatv.player.trakt.ScrobbleSession()
  fun track(ended: Boolean = false) {
   if (channel.kind != "live") scrobble.update(player.currentPosition, player.duration, player.isPlaying, !player.playWhenReady, ended)?.let { trakt(it.first, it.second) }
  }
  val listener = object : Player.Listener {
   override fun onIsPlayingChanged(isPlaying: Boolean) { track() }
   override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { track() }
   override fun onPlayerError(error: PlaybackException) { errorText = "Afspelen lukt niet. Controleer je verbinding of probeer opnieuw."; latestReport("Afspelen mislukt (${error.errorCodeName}). Controleer de bron en probeer opnieuw.") }
   override fun onPlaybackStateChanged(state: Int) {
    track(state == Player.STATE_ENDED)
    latestReport(when (state) { Player.STATE_BUFFERING -> "Bufferen…"; Player.STATE_READY -> ""; Player.STATE_ENDED -> "Afspelen voltooid"; else -> "" })
   }
  }
  val observer = LifecycleEventObserver { _, event ->
   when (event) {
    Lifecycle.Event.ON_STOP -> { resume = player.playWhenReady; player.pause() }
    Lifecycle.Event.ON_START -> if (resume) player.play()
    else -> Unit
   }
  }
  player.addListener(listener); lifecycle.addObserver(observer)
  onDispose {
   scrobble.stop(player.currentPosition, player.duration)?.let { if (channel.kind != "live") trakt(it.first, it.second) }
   trakt("dispose", 0.0)
   lifecycle.removeObserver(observer); player.removeListener(listener); player.release()
  }
 }
 Box(modifier) {
 AndroidView(factory = { PlayerView(it).apply { view = this; setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { controlsVisible = it == android.view.View.VISIBLE }); this.player = player; useController = true; setShowSubtitleButton(true); setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); requestFocus() } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
 if (controlsVisible && errorText.isBlank()) Text(channel.name, Modifier.align(Alignment.TopStart).padding(32.dp).background(Color.Black.copy(alpha = 0.7f)).padding(12.dp), color = Color.White, fontSize = 24.sp)
 if (errorText.isNotBlank()) Column(Modifier.align(Alignment.Center).background(Color(0xFF110E21)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
  Text(errorText, color = Color.White)
  RemoteButton("Opnieuw proberen", Modifier.focusRequester(retryFocus)) { errorText = ""; player.prepare(); player.play() }
  RemoteButton("Terug naar bibliotheek") { close() }
 }
 }
}
