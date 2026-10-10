package com.novatv.player

import androidx.compose.runtime.*
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
fun Playback(channel: Channel, modifier: Modifier = Modifier, trakt: (String, Double) -> Unit = { _, _ -> }, report: (String) -> Unit) {
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
   override fun onPlayerError(error: PlaybackException) { latestReport("Afspelen mislukt (${error.errorCodeName}). Controleer de bron en probeer opnieuw.") }
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
 AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true; setShowSubtitleButton(true); setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); requestFocus() } }, update = { it.player = player }, modifier = modifier)
}
