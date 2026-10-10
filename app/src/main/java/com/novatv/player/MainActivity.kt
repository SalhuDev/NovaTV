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
private fun RemoteButton(text: String, modifier: Modifier = Modifier, active: Boolean = false, enabled: Boolean = true, action: () -> Unit) {
 var focused by remember { mutableStateOf(false) }
 TvButton(onClick = action, enabled = enabled,
  modifier = modifier.onFocusChanged { focused = it.isFocused }.border(if (focused) 3.dp else 0.dp, if (focused) white else Color.Transparent, RoundedCornerShape(12.dp)),
  colors = TvButtonDefaults.colors(containerColor = if (active) purple else panel, contentColor = white, focusedContainerColor = purple, focusedContentColor = white)
 ) { Text(text, color = white, maxLines = 2, overflow = TextOverflow.Ellipsis) }
}

@Composable fun NovaApp() {
 val context = LocalContext.current
 val storage = remember { Storage(context) }
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
  for (i in 0 until array.length()) { val item = array.getJSONObject(i); add(Channel(item.getString("name"), item.getString("url"), item.optString("group"), item.optString("epgId"), item.optString("logo"), item.optString("kind", "live"))) }
 }) }
 val favorites = favoriteItems.map { it.key }.toSet()
 val scope = rememberCoroutineScope()
 val menuFocus = remember { FocusRequester() }
 val contentFocus = remember { FocusRequester() }
 var restoreContent by remember { mutableStateOf(false) }
 fun persist() { storage.save(JSONObject().put("playlist", playlistUrl).put("epg", epgUrl).put("server", server).put("username", username).put("password", password).put("favoriteItems", JSONArray(favoriteItems.map { item -> JSONObject().put("name", item.name).put("url", item.url).put("group", item.group).put("epgId", item.epgId).put("logo", item.logo).put("kind", item.kind) }))) }
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
 BackHandler(enabled = selected != null || openedSeries != null || tab != "Home") {
  when { selected != null -> { selected = null; restoreContent = true }; openedSeries != null -> { openedSeries = null; episodes = emptyList(); restoreContent = true }; else -> { tab = "Home"; menuFocus.requestFocus() } }
 }
 LaunchedEffect(Unit) { menuFocus.requestFocus() }
 LaunchedEffect(restoreContent, selected, tab) {
  if (restoreContent && selected == null) { runCatching { contentFocus.requestFocus() }.onFailure { menuFocus.requestFocus() }; restoreContent = false }
 }
 MaterialTheme(colorScheme = darkColorScheme(primary = purple, surface = panel, background = bg, onBackground = white, onSurface = white)) {
  Row(Modifier.fillMaxSize().background(bg).padding(24.dp)) {
   Column(Modifier.width(200.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val bitmap = remember { context.assets.open("nova_mascot.png").use { BitmapFactory.decodeStream(it) } }
    Image(bitmap.asImageBitmap(), "Origineel NOVA-logo", Modifier.size(86.dp))
    Text("NOVA TV", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = white)
    listOf("Home", "Live TV", "EPG", "Films", "Series", "Favorieten", "Bronnen", "Instellingen").forEachIndexed { i, item ->
     RemoteButton(item, Modifier.fillMaxWidth().then(if (i == 0) Modifier.focusRequester(menuFocus) else Modifier), tab == item) { tab = item; selected = null; openedSeries = null }
    }
   }
   Spacer(Modifier.width(24.dp))
   Column(Modifier.weight(1f).fillMaxHeight()) {
    Text(tab, fontSize = 34.sp, color = white, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    val playing = selected
    if (playing != null) {
     Text(playing.name, color = white, fontSize = 22.sp)
     Box(Modifier.fillMaxWidth().weight(1f).onPreviewKeyEvent {
      if (it.type == KeyEventType.KeyDown && playing.kind == "live") when (it.key) {
       Key.ChannelUp, Key.PageUp -> { switch(1); true }
       Key.ChannelDown, Key.PageDown -> { switch(-1); true }
       else -> false
      } else false
     }) { Playback(playing, Modifier.fillMaxSize()) { playbackStatus = it } }
     Text(playbackStatus, color = white)
     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (playing.kind == "live") { RemoteButton("Vorige") { switch(-1) }; RemoteButton("Volgende") { switch(1) } }
      RemoteButton("Opnieuw") { val c = selected; selected = null; scope.launch { delay(50); selected = c } }
      RemoteButton("Terug") { selected = null; restoreContent = true }
     }
    } else when (tab) {
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
      val entries = when (tab) { "Films" -> films; "Favorieten" -> favoriteItems; else -> channels }
      ChannelList(entries, favorites, contentFocus, returnKey, { returnKey = it.key; selected = it }, { item -> favoriteItems = if (item.key in favorites) favoriteItems.filterNot { it.key == item.key } else favoriteItems + item; runCatching { persist() }.onFailure { status = "Favorieten konden niet worden opgeslagen." } })
     }
     "Series" -> {
      if (openedSeries != null) ChannelList(episodes, favorites, contentFocus, returnKey, { returnKey = it.key; selected = it }, { item -> favoriteItems = if (item.key in favorites) favoriteItems.filterNot { it.key == item.key } else favoriteItems + item; runCatching { persist() } })
      else if (series.isEmpty()) Text("Verbind een Xtream-bron om series te laden.", color = white)
      else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
       items(series, key = { it.id }) { item -> RemoteButton(item.name, Modifier.fillMaxWidth(), enabled = !loading) {
        val provider = account
        if (provider != null) task { val loaded = withContext(Dispatchers.IO) { Sources.episodes(provider, item) }; episodes = loaded; openedSeries = item; restoreContent = true }
       } }
      }
      Text(if (loading) "Afleveringen laden…" else status, color = Color.LightGray)
     }
     "EPG" -> GuideTimeline(channels, programmes) { selected = it }
     "Instellingen" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("AndroidX Media3 · automatische audioformaatselectie", color = white)
      Text("Passthrough en Dolby Atmos worden door Media3 gekozen als bron, Android-apparaat en HDMI/eARC-keten het formaat ondersteunen. Er wordt geen surroundformaat geforceerd.", color = Color.LightGray)
      Text("Sonos: sluit een geschikte Sonos-soundbar aan via HDMI ARC/eARC. Dolby Digital Plus/Atmos en TrueHD hangen af van TV, speler en soundbar. Direct casten naar Sonos is niet ingebouwd.", color = Color.LightGray)
      Text("Gebruik de spelerbediening voor afspelen/pauze, audio en ondertitels. CH+/CH− of Page Up/Down wisselt live-zenders. Terug sluit de speler.", color = white)
     }
    }
   }
  }
 }
}

@Composable private fun ChannelList(entries: List<Channel>, favorites: Set<String>, focus: FocusRequester, returnKey: String, play: (Channel) -> Unit, favorite: (Channel) -> Unit) {
 if (entries.isEmpty()) { Text("Geen items. Laad een bron of voeg favorieten toe.", color = white); return }
 val returnIndex = entries.indexOfFirst { it.key == returnKey }.coerceAtLeast(0)
 val state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = returnIndex)
 LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(8.dp)) {
  items(entries.size, key = { entries[it].key }) { index ->
   val item = entries[index]
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    RemoteButton("${item.name} · ${item.group}", Modifier.weight(1f).then(if (index == returnIndex) Modifier.focusRequester(focus) else Modifier)) { play(item) }
    RemoteButton(if (item.key in favorites) "★ Verwijder" else "☆ Bewaar") { favorite(item) }
   }
  }
 }
}

@Composable private fun GuideTimeline(channels: List<Channel>, programmes: List<Programme>, play: (Channel) -> Unit) {
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) { while (true) { delay(60000); now = System.currentTimeMillis() } }
 var windowOffset by remember { mutableIntStateOf(0) }
 val start = now / 1800000 * 1800000 + windowOffset * 3600000L
 val end = start + 3 * 3600000
 val time = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()) }
 if (channels.isEmpty() || programmes.isEmpty()) { Text("Laad zenders en een XMLTV-bron onder Bronnen. Zenders worden gekoppeld via tvg-id.", color = white); return }
 val byChannel = remember(programmes) { programmes.groupBy { it.channelId } }
 val scroll = rememberScrollState()
 Column {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   RemoteButton("← Eerder") { windowOffset -= 3 }
   RemoteButton("Nu") { windowOffset = 0 }
   RemoteButton("Later →") { windowOffset += 3 }
  }
  Spacer(Modifier.height(12.dp))
  Column(Modifier.horizontalScroll(scroll)) {
  Row(Modifier.padding(start = 200.dp)) { repeat(6) { Text(time.format(Instant.ofEpochMilli(start + it * 1800000)), Modifier.width(180.dp), color = purple) } }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(1280.dp)) {
   items(channels, key = { it.key }) { channel ->
    Row(verticalAlignment = Alignment.CenterVertically) {
     RemoteButton(channel.name, Modifier.width(200.dp)) { play(channel) }
     Box(Modifier.width(1080.dp).height(70.dp)) {
      val entries = byChannel[channel.epgId].orEmpty().filter { it.end > start && it.start < end }
      if (entries.isEmpty()) Text("Geen gidsgegevens", Modifier.padding(8.dp), color = Color.LightGray)
      entries.forEach { programme ->
       val offset = ((programme.start.coerceAtLeast(start) - start) / 10000f).dp
       val width = ((programme.end.coerceAtMost(end) - programme.start.coerceAtLeast(start)) / 10000f).dp
       RemoteButton("${time.format(Instant.ofEpochMilli(programme.start))} ${programme.title}", Modifier.offset(x = offset).width(width).fillMaxHeight().padding(2.dp), active = now in programme.start until programme.end) { play(channel) }
      }
      if (now in start until end) Box(Modifier.offset { IntOffset(((now - start) / 10000f).dp.roundToPx(), 0) }.width(2.dp).fillMaxHeight().background(purple))
     }
    }
   }
  }
 }
}
}
