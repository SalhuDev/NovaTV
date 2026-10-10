package com.novatv.player

import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button as TvButton
import androidx.tv.material3.ButtonDefaults as TvButtonDefaults
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val bg = Color(0xFF110E21)
private val panel = Color(0xFF211735)
private val purple = Color(0xFFAA73FA)
private val white = Color(0xFFF6F0FF)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { NovaApp() } }
}

@Composable
internal fun RemoteButton(text: String, modifier: Modifier = Modifier, active: Boolean = false, enabled: Boolean = true, action: () -> Unit) {
 var focused by remember { mutableStateOf(false) }
 TvButton(onClick = action, enabled = enabled,
  modifier = modifier.onFocusChanged { focused = it.isFocused }.border(if (focused) 3.dp else 0.dp, if (focused) white else Color.Transparent, RoundedCornerShape(12.dp)),
  colors = TvButtonDefaults.colors(containerColor = if (active) purple else panel, contentColor = white, focusedContainerColor = purple, focusedContentColor = white)
 ) { Text((if (active) "▸ " else "") + text, color = if (!enabled) Color(0xFFB6AEC6) else if (focused || active) bg else white, maxLines = 2, overflow = TextOverflow.Ellipsis) }
}

@Composable fun NovaApp() {
 val context = LocalContext.current
 val screenState = rememberSaveableStateHolder()
 var seriesReturnId by rememberSaveable { mutableStateOf("") }
 val storage = remember { Storage(context) }
 val trakt = remember { TraktSession.get(context) }
 var traktChannel by remember { mutableStateOf<Channel?>(null) }
 val restored = remember { runCatching { storage.load() }.getOrDefault(JSONObject()) }
 var tab by rememberSaveable { mutableStateOf("Home") }
 var playlistUrl by remember { mutableStateOf(restored.optString("playlist")) }
 var epgUrl by remember { mutableStateOf(restored.optString("epg")) }
 var server by remember { mutableStateOf(restored.optString("server")) }
 var username by remember { mutableStateOf(restored.optString("username")) }
 // Password never enters the saved-instance-state Bundle.
 var password by remember { mutableStateOf(restored.optString("password")) }
 var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
 var films by remember { mutableStateOf<List<Channel>>(emptyList()) }
 var series by remember { mutableStateOf<List<Sources.Series>>(emptyList()) }
 var episodes by remember { mutableStateOf<List<Channel>>(emptyList()) }
 var openedSeries by remember { mutableStateOf<Sources.Series?>(null) }
 var account by remember { mutableStateOf<Sources.Xtream?>(null) }
 var programmes by remember { mutableStateOf<List<Programme>>(emptyList()) }
 var selected by remember { mutableStateOf<Channel?>(null) }
 var returnKey by remember { mutableStateOf("") }
 var status by remember { mutableStateOf("Voeg een HTTPS M3U- of Xtream-bron toe onder Bronnen.") }
 var playbackStatus by remember { mutableStateOf("") }
 var loading by remember { mutableStateOf(false) }
 var favoriteItems by remember { mutableStateOf(buildList {
  val array = restored.optJSONArray("favoriteItems") ?: JSONArray()
  for (i in 0 until array.length()) { val item = array.getJSONObject(i); add(Channel(item.getString("name"), item.getString("url"), item.optString("group"), item.optString("epgId"), item.optString("logo"), item.optString("kind", "live"), item.optString("showTitle"), item.optInt("season", -1).takeIf { it >= 0 }, item.optInt("episode", -1).takeIf { it > 0 })) }
 }) }
 val favorites = favoriteItems.map { it.key }.toSet()
 val scope = rememberCoroutineScope()
 val menuFocus = remember { FocusRequester() }
 val contentFocus = remember { FocusRequester() }
 var restoreContent by remember { mutableStateOf(false) }
 fun persist() { storage.save(JSONObject().put("playlist", playlistUrl).put("epg", epgUrl).put("server", server).put("username", username).put("password", password).put("favoriteItems", JSONArray(favoriteItems.map { item -> JSONObject().put("name", item.name).put("url", item.url).put("group", item.group).put("epgId", item.epgId).put("logo", item.logo).put("kind", item.kind).put("showTitle", item.showTitle).put("season", item.season).put("episode", item.episode) }))) }
 fun task(block: suspend () -> Unit) {
  if (loading) return
  loading = true
  scope.launch {
   try { block(); persist() }
   catch (e: CancellationException) { throw e }
   catch (_: Exception) { status = "Laden of opslaan mislukt. Controleer HTTPS-adres, account en verbinding." }
   finally { loading = false }
  }
 }
 fun switch(delta: Int) {
  val current = selected ?: return
  if (current.kind != "live" || channels.isEmpty()) return
  val index = channels.indexOfFirst { it.key == current.key }
  selected = channels[(index + delta + channels.size) % channels.size]
 }
 BackHandler(enabled = selected == null && (openedSeries != null || tab != "Home")) {
  when { selected != null -> { selected = null; restoreContent = true }; openedSeries != null -> { openedSeries = null; episodes = emptyList(); restoreContent = true }; else -> { tab = "Home"; menuFocus.requestFocus() } }
 }
 LaunchedEffect(Unit) { menuFocus.requestFocus() }
 LaunchedEffect(restoreContent, selected, tab) {
  if (restoreContent && selected == null) { runCatching { contentFocus.requestFocus() }.onFailure { menuFocus.requestFocus() }; restoreContent = false }
 }
 MaterialTheme(colorScheme = darkColorScheme(primary = purple, onPrimary = bg, surface = panel, background = bg, onBackground = white, onSurface = white), typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 26.sp), bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 26.sp))) {
  traktChannel?.let { TraktMatchDialog(it, trakt) { traktChannel = null } }
  val playing = selected
  if (playing != null) {
   Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent {
    if (it.type == KeyEventType.KeyDown && playing.kind == "live") when (it.key) {
     Key.ChannelUp, Key.PageUp -> { switch(1); true }
     Key.ChannelDown, Key.PageDown -> { switch(-1); true }
     else -> false
    } else false
   }) {
    key(if (playing.kind == "live") "live-player" else playing.key) {
     val tracking = remember { if (playing.kind != "live") trakt.playback(playing.key) else { _: String, _: Double -> } }
     Playback(playing, Modifier.fillMaxSize(), tracking, { selected = null; restoreContent = true }) { playbackStatus = it }
    }
   }
   return@MaterialTheme
  }

  Row(Modifier.fillMaxSize().background(bg).padding(32.dp)) {
   Column(Modifier.width(200.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val bitmap = remember { context.assets.open("nova_mascot.png").use { BitmapFactory.decodeStream(it) } }
    Image(bitmap.asImageBitmap(), "Origineel NOVA-logo", Modifier.size(86.dp))
    Text("NOVA TV", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = white)
    listOf("Home", "Live TV", "EPG", "Films", "Series", "Favorieten", "Trakt", "Bronnen", "Instellingen").forEachIndexed { i, item ->
     RemoteButton(item, Modifier.fillMaxWidth().then(if (i == 0) Modifier.focusRequester(menuFocus) else Modifier), tab == item) { tab = item; selected = null; openedSeries = null }
    }
   }
   Spacer(Modifier.width(24.dp))
   Column(Modifier.weight(1f).fillMaxHeight()) {
    Text(tab, fontSize = 34.sp, color = white, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    screenState.SaveableStateProvider(tab) {
    when (tab) {
     "Trakt" -> TraktScreen(trakt)
     "Home" -> {
      Text("Welkom bij NOVA TV", fontSize = 26.sp, color = white)
      Text("Jouw entertainment. Jouw scherm.", color = Color.LightGray)
      Spacer(Modifier.height(20.dp))
      RemoteButton("Bronnen openen", Modifier.focusRequester(contentFocus)) { tab = "Bronnen" }
      Text("${channels.size} zenders · ${films.size} films · ${series.size} series", color = white)
     }
     "Bronnen" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(playlistUrl, { playlistUrl = it }, label = { Text("HTTPS M3U-playlist") }, singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(contentFocus))
      RemoteButton("Playlist laden", enabled = !loading && playlistUrl.startsWith("https://")) {
       task { val parsed = withContext(Dispatchers.IO) { Playlist.parse(Sources.read(playlistUrl), playlistUrl) }; require(parsed.isNotEmpty()); channels = parsed; account = null; films = emptyList(); series = emptyList(); status = "${channels.size} zenders geladen." }
      }
      OutlinedTextField(epgUrl, { epgUrl = it }, label = { Text("HTTPS XMLTV (.xml of .gz)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      RemoteButton("Programmagids laden", enabled = !loading && epgUrl.startsWith("https://")) {
       task { val parsed = withContext(Dispatchers.IO) { Guide.parse(Sources.read(epgUrl)) }; programmes = parsed; status = "${programmes.size} programma’s geladen." }
      }
      HorizontalDivider()
      OutlinedTextField(server, { server = it }, label = { Text("Xtream HTTPS-server") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(username, { username = it }, label = { Text("Gebruikersnaam") }, singleLine = true)
      OutlinedTextField(password, { password = it }, label = { Text("Wachtwoord") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
      RemoteButton("Xtream verbinden", enabled = !loading && server.startsWith("https://") && username.isNotBlank() && password.isNotBlank()) {
       task { val candidate = Sources.Xtream(server, username, password); val catalog = withContext(Dispatchers.IO) { Sources.catalog(candidate) }; account = candidate; channels = catalog.live; films = catalog.films; series = catalog.series; epgUrl = candidate.epg(); status = "Xtream geladen. Laad de programmagids met de knop hierboven." }
      }
      Text(if (loading) "Laden…" else status, color = white)
     }
     "Live TV", "Films", "Favorieten" -> {
      if (status.contains("opgeslagen")) Text(status, color = white)
      val entries = when (tab) { "Films" -> films; "Favorieten" -> favoriteItems; else -> channels }
      ChannelList(entries, favorites, contentFocus, returnKey, { traktChannel = it }, { returnKey = it.key; selected = it }, { item -> favoriteItems = if (item.key in favorites) favoriteItems.filterNot { it.key == item.key } else favoriteItems + item; runCatching { persist() }.onFailure { status = "Favorieten konden niet worden opgeslagen." } })
     }
     "Series" -> {
      if (status.contains("mislukt", true)) ReadingText(status)
      if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Afleveringen laden…", color = white) }
      if (openedSeries != null) RemoteButton("← Alle series") { openedSeries = null; episodes = emptyList(); restoreContent = true }
      screenState.SaveableStateProvider("series:${openedSeries?.id ?: "catalog"}") {
      if (openedSeries != null) ChannelList(episodes, favorites, contentFocus, returnKey, { traktChannel = it }, { returnKey = it.key; selected = it }, { item -> favoriteItems = if (item.key in favorites) favoriteItems.filterNot { it.key == item.key } else favoriteItems + item; runCatching { persist() } })
      else SeriesLibrary(series, contentFocus, seriesReturnId, loading) { item ->
       val provider = account
       if (provider != null) task { val loaded = withContext(Dispatchers.IO) { Sources.episodes(provider, item) }; episodes = loaded; seriesReturnId = item.id; openedSeries = item; restoreContent = true }
      }
      }

     }
     "EPG" -> GuideTimeline(channels, programmes, contentFocus) { selected = it }
     "Instellingen" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      ReadingText("AndroidX Media3 · automatische audioformaatselectie")
      ReadingText("Passthrough en Dolby Atmos worden door Media3 gekozen als bron, Android-apparaat en HDMI/eARC-keten het formaat ondersteunen. Er wordt geen surroundformaat geforceerd.")
      ReadingText("Sonos: sluit een geschikte Sonos-soundbar aan via HDMI ARC/eARC. Dolby Digital Plus/Atmos en TrueHD hangen af van TV, speler en soundbar. Direct casten naar Sonos is niet ingebouwd.")
      ReadingText("Gebruik de spelerbediening voor afspelen/pauze, audio en ondertitels. CH+/CH− of Page Up/Down wisselt live-zenders. Terug verbergt eerst de spelerbediening en sluit daarna de speler.")
     }
    }
    }
   }
  }
 }
}

@Composable private fun ChannelList(entries: List<Channel>, favorites: Set<String>, focus: FocusRequester, returnKey: String, trakt: (Channel) -> Unit, play: (Channel) -> Unit, favorite: (Channel) -> Unit) {
 MediaLibrary(entries, favorites, focus, returnKey, trakt, play, favorite)
}

@Composable private fun GuideTimeline(channels: List<Channel>, programmes: List<Programme>, focus: FocusRequester, play: (Channel) -> Unit) {
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) { while (true) { delay(60000); now = System.currentTimeMillis() } }
 var windowOffset by rememberSaveable { mutableIntStateOf(0) }
 val start = now / 1800000 * 1800000 + windowOffset * 3600000L
 val end = start + 3 * 3600000
 val time = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()) }
 if (channels.isEmpty() || programmes.isEmpty()) { Text("Laad zenders en een XMLTV-bron onder Bronnen. Zenders worden gekoppeld via tvg-id.", color = white); return }
 val byChannel = remember(programmes) { programmes.groupBy { it.channelId } }
 val scroll = rememberScrollState()
 var focusedProgramme by rememberSaveable { mutableStateOf("") }
 var detail by remember { mutableStateOf<Programme?>(null) }
 var schedule by remember { mutableStateOf<Channel?>(null) }
 schedule?.let { channel ->
  androidx.compose.ui.window.Dialog(onDismissRequest = { schedule = null }) {
   Surface(shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(24.dp).heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Text(channel.name, fontSize = 24.sp)
     RemoteButton("Live kijken") { schedule = null; play(channel) }
     LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(byChannel[channel.epgId].orEmpty().filter { it.end > start && it.start < end }) { programme ->
       ReadingText("${time.format(Instant.ofEpochMilli(programme.start))} – ${time.format(Instant.ofEpochMilli(programme.end))} · ${programme.title}")
      }
     }
     RemoteButton("Sluiten") { schedule = null }
    }
   }
  }
 }
 Column {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   RemoteButton("← Eerder") { windowOffset -= 3; focusedProgramme = "" }
   RemoteButton("Nu", if (focusedProgramme.isBlank()) Modifier.focusRequester(focus) else Modifier) { windowOffset = 0; focusedProgramme = "" }
   RemoteButton("Later →") { windowOffset += 3; focusedProgramme = "" }
  }
  Spacer(Modifier.height(12.dp))
  detail?.let { Text("${time.format(Instant.ofEpochMilli(it.start))} – ${time.format(Instant.ofEpochMilli(it.end))} · ${it.title}", color = white, modifier = Modifier.padding(vertical = 8.dp)) }
  Row {
   Spacer(Modifier.width(200.dp))
   Row(Modifier.horizontalScroll(scroll)) { repeat(6) { Text(time.format(Instant.ofEpochMilli(start + it * 1800000)), Modifier.width(180.dp), color = purple) } }
  }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
   items(channels, key = { it.key }) { channel ->
    Row(verticalAlignment = Alignment.CenterVertically) {
     RemoteButton(channel.name, Modifier.width(200.dp)) { schedule = channel }
     Box(Modifier.weight(1f).horizontalScroll(scroll)) {
     Box(Modifier.width(1080.dp).height(70.dp)) {
      val entries = byChannel[channel.epgId].orEmpty().filter { it.end > start && it.start < end }
      if (entries.isEmpty()) Text("Geen gidsgegevens", Modifier.padding(8.dp), color = Color.LightGray)
      entries.forEach { programme ->
       val offset = ((programme.start.coerceAtLeast(start) - start) / 10000f).dp
       val width = ((programme.end.coerceAtMost(end) - programme.start.coerceAtLeast(start)) / 10000f).dp
       RemoteButton("${time.format(Instant.ofEpochMilli(programme.start))} ${programme.title}", Modifier.then(if (focusedProgramme == "${channel.key}:${programme.start}") Modifier.focusRequester(focus) else Modifier).offset(x = offset).width(width).fillMaxHeight().padding(1.dp).onFocusChanged { if (it.isFocused) { detail = programme; focusedProgramme = "${channel.key}:${programme.start}" } }, active = now in programme.start until programme.end) { play(channel) }
      }
      if (now in start until end) Box(Modifier.offset { IntOffset(((now - start) / 10000f).dp.roundToPx(), 0) }.width(2.dp).fillMaxHeight().background(purple))
     }
     }
    }
   }
  }
}
}
