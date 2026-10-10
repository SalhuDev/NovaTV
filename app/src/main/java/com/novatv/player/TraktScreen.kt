package com.novatv.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.novatv.player.trakt.*
import kotlinx.coroutines.*

object TraktSession {
 private var instance: TraktRepository? = null
 @Synchronized fun get(context: android.content.Context): TraktRepository = instance ?: run {
  val storage = Storage(context.applicationContext, "nova.trakt")
  TraktRepository(TraktHttp(), runCatching { storage.load() }.getOrDefault(org.json.JSONObject()),
   { storage.save(it, durable = true) }, CoroutineScope(SupervisorJob() + Dispatchers.IO)).also { instance = it }
 }
}

@Composable fun TraktScreen(repository: TraktRepository) {
 val state by repository.state.collectAsState()
 var advanced by remember { mutableStateOf(false) }
 var id by remember(state.clientId) { mutableStateOf(state.clientId) }
 var redirect by remember(state.redirectUri) { mutableStateOf(state.redirectUri) }
 LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
  Text(state.status)
  if (state.actionBusy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Trakt bijwerken…") }
  if (!state.connected) {
   Text("Koppel je account om je kijklijst en kijkgeschiedenis te gebruiken.")
   RemoteButton(if (advanced) "Geavanceerde instellingen sluiten" else "Geavanceerd: Trakt-app instellen", enabled = !state.busy && !state.actionBusy) { advanced = !advanced }
   if (advanced) {
   Text("Registreer een app via developer.trakt.tv/apps. Gebruik de publieke Client ID; er is geen Client Secret nodig.")
   OutlinedTextField(id, { id = it }, label = { Text("Trakt Client ID") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
   OutlinedTextField(redirect, { redirect = it }, label = { Text("Geregistreerde redirect-URI") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
   }
   if (id.isBlank()) Text("Eenmalige configuratie nodig. Open Geavanceerd en vul je publieke Client ID in.")
   RemoteButton("Aanmelden bij Trakt", enabled = !state.busy && !state.actionBusy && id.isNotBlank()) {
    advanced = false
    repository.runTask { repository.configure(id, redirect); repository.connect() }
   }
   state.prompt?.let { prompt ->
    Text("Scan met je telefoon en voer de code in:")
    TraktQr(prompt.verificationUrl)
    Text(prompt.verificationUrl, style = MaterialTheme.typography.headlineSmall)
    Text(prompt.code, style = MaterialTheme.typography.displaySmall)
    var remaining by remember(prompt) { mutableLongStateOf(0) }
    LaunchedEffect(prompt) { while (true) { remaining = ((prompt.expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0); delay(1000) } }
    Text("Code geldig: $remaining seconden")
   }
   if (state.busy) RemoteButton("Aanmelden annuleren") { repository.cancelLogin() }
  } else {
   Text("Gekoppeld: ${state.username.ifBlank { "Trakt-account" }}")
   RemoteButton("Kijklijst en geschiedenis vernieuwen", enabled = !state.actionBusy) { repository.runTask { repository.refresh() } }
   RemoteButton("Automatische kijkregistratie: ${if (state.autoScrobble) "aan" else "uit"}") { repository.runTask { repository.autoScrobble(!state.autoScrobble) } }
   Text("Koppel een film of aflevering via de Trakt-knop in je bibliotheek. Alleen bevestigde titels geven voortgang door. Live TV wordt niet geregistreerd.")
   RemoteButton("Account ontkoppelen", enabled = !state.actionBusy) { repository.runTask { repository.disconnect() } }
  }
  } }
  if (state.connected) {
   item { HorizontalDivider(); Text("Kijklijst", style = MaterialTheme.typography.headlineSmall) }
   if (state.watchlist.isEmpty()) item { Text("Geen kijklijst geladen.") }
   items(state.watchlist, key = { "watchlist:${it.key}" }) { media ->
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
     Text(media.label)
     RemoteButton("Verwijder uit kijklijst", enabled = !state.actionBusy) { repository.runTask { repository.watchlist(media, true) } }
    }
   }
   item { Text("Recente kijkgeschiedenis (laatste 100 registraties)", style = MaterialTheme.typography.headlineSmall) }
   items(state.history, key = { "history:${it.key}" }) { ReadingText(it.label) }
   item { Text("Trakt levert geen videostreams. Je films en series komen uit je eigen bronnen.") }
  }
 }
}

@Composable fun TraktMatchDialog(channel: Channel, repository: TraktRepository, close: () -> Unit) {
 val state by repository.state.collectAsState()
 val scope = rememberCoroutineScope()
 var query by remember(channel.key) { mutableStateOf(channel.showTitle.ifBlank { channel.name }) }
 var results by remember { mutableStateOf<List<TraktMedia>>(emptyList()) }
 var season by remember { mutableStateOf(channel.season?.toString().orEmpty()) }
 var number by remember { mutableStateOf(channel.episode?.toString().orEmpty()) }
 var message by remember { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 fun task(block: suspend () -> Unit) { if (!busy) scope.launch {
  busy = true
  try { block() } catch (e: CancellationException) { throw e }
  catch (e: Exception) { message = if (e is TraktFailure) e.message.orEmpty() else "Trakt-actie mislukt. Controleer invoer en verbinding." }
  finally { busy = false }
 } }
 Dialog(onDismissRequest = close) {
  Surface(shape = MaterialTheme.shapes.large) {
   Column(Modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(channel.name, style = MaterialTheme.typography.headlineSmall)
    Text(message.ifBlank { state.status })
    val linked = state.links[channel.key]
    if (!state.connected) Text("Koppel eerst je account in het Trakt-menu.")
    else if (linked != null) {
     Text("Gekoppeld aan: ${linked.label}")
     RemoteButton("Markeer als bekeken", enabled = !busy) { task { repository.watched(linked); message = "Als bekeken geregistreerd." } }
     val listed = state.watchlist.any { it.key == linked.key }
     RemoteButton(if (listed) "Uit kijklijst verwijderen" else "Aan kijklijst toevoegen", enabled = !busy) { task { repository.watchlist(linked, listed) } }
     RemoteButton("Titelkoppeling verwijderen", enabled = !busy) { task { repository.link(channel.key, null); results = emptyList() } }
    } else {
     OutlinedTextField(query, { query = it }, label = { Text(if (channel.kind == "series") "Serietitel" else "Filmtitel") }, singleLine = true)
     if (channel.kind == "series") {
      OutlinedTextField(season, { season = it }, label = { Text("Seizoen (0 = specials)") }, singleLine = true)
      OutlinedTextField(number, { number = it }, label = { Text("Aflevering") }, singleLine = true)
     }
     RemoteButton("Zoeken op Trakt", enabled = !busy && query.isNotBlank()) { task {
      results = repository.search(query, if (channel.kind == "series") "show" else "movie")
      message = if (results.isEmpty()) "Geen titels gevonden." else "Controleer titel en jaar; selecteer de juiste titel."
     } }
     results.forEach { media ->
      RemoteButton(if (media.type == "show") "Aflevering zoeken: ${media.label}" else "Bevestig: ${media.label}", enabled = !busy) { task {
       if (media.type == "show") {
        val s = season.toIntOrNull(); val n = number.toIntOrNull()
        if (s == null || s < 0 || n == null || n <= 0) message = "Vul seizoen en aflevering in."
        else { results = listOf(repository.episode(media, s, n)); message = "Bevestig de juiste aflevering hieronder." }
       } else { repository.link(channel.key, media); message = "Titel gekoppeld." }
      } }
     }
    }
    RemoteButton("Sluiten") { close() }
   }
  }
 }
}

@Composable private fun TraktQr(url: String) {
 val bitmap = remember(url) {
  val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(url, com.google.zxing.BarcodeFormat.QR_CODE, 256, 256)
  android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888).apply {
   for (y in 0 until 256) for (x in 0 until 256) setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
  }.asImageBitmap()
 }
 Image(bitmap, "QR-code voor Trakt-aanmelden; het adres staat ook als tekst op dit scherm", Modifier.size(144.dp))
}
