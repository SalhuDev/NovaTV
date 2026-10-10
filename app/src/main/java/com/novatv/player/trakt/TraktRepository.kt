package com.novatv.player.trakt

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.json.JSONArray
import java.net.URLEncoder

class TraktRepository(private val transport: TraktTransport, initial: JSONObject,
 private val persist: (JSONObject) -> Unit, private val scope: CoroutineScope,
 private val now: () -> Long = System::currentTimeMillis) {
 private val mutex = Mutex()
 private val actionMutex = Mutex()
 private var tokens = initial.optJSONObject("tokens")?.let { runCatching { TraktTokens.restore(it) }.getOrNull() }
 private val mutable = MutableStateFlow(TraktUiState(clientId = initial.optString("clientId"),
  redirectUri = initial.optString("redirectUri", "https://github.com/SalhuDev/NovaTV"), connected = tokens != null,
  autoScrobble = initial.optBoolean("autoScrobble"), links = buildMap {
   val links = initial.optJSONObject("links") ?: JSONObject()
   links.keys().forEach { key -> runCatching { TraktMedia.fromJson(links.getJSONObject(key)) }.getOrNull()?.let { put(key, it) } }
  }))
 val state = mutable.asStateFlow()
 private var login: Job? = null
 @Volatile private var generation = 0
 private fun update(block: (TraktUiState) -> TraktUiState) { mutable.update(block) }
 private fun save() {
  val s = state.value
  persist(JSONObject().put("clientId", s.clientId).put("redirectUri", s.redirectUri).put("tokens", tokens?.toJson())
   .put("autoScrobble", s.autoScrobble).put("links", JSONObject().apply { s.links.forEach { (key, value) -> put(key, value.toJson()) } }))
 }
 suspend fun configure(id: String, redirect: String) = mutex.withLock {
  require(tokens == null && login?.isActive != true)
  require(id.trim().matches(Regex("[A-Za-z0-9_-]{8,200}"))) { "Vul een geldige publieke Client ID in." }
  require(redirect.startsWith("https://")) { "Gebruik de geregistreerde HTTPS redirect-URI." }
  update { it.copy(clientId = id.trim(), redirectUri = redirect.trim()) }; save()
 }
 fun runTask(block: suspend () -> Unit) { scope.launch {
  if (!actionMutex.tryLock()) return@launch
  update { it.copy(actionBusy = true) }
  try { block() } catch (e: CancellationException) { throw e }
  catch (e: Exception) { update { it.copy(status = if (e is TraktFailure || e is IllegalArgumentException) e.message.orEmpty() else "Trakt niet bereikbaar of opslaan mislukt. Probeer opnieuw.") } }
  finally { update { it.copy(actionBusy = false) }; actionMutex.unlock() }
 } }
 fun connect() {
  if (login?.isActive == true || state.value.connected) return
  login = scope.launch {
   update { it.copy(busy = true, status = "Aanmeldcode ophalen…") }
   try {
    val id = state.value.clientId; require(id.isNotBlank()) { "Sla eerst je Client ID op." }
    val code = JSONObject(transport.call(id, "/oauth/device/code", "POST", JSONObject().put("client_id", id)).checked().body)
    val url = java.net.URI(code.getString("verification_url"))
    require(url.scheme == "https" && url.host in listOf("trakt.tv", "www.trakt.tv", "auth.trakt.tv"))
    val expires = now() + code.getLong("expires_in").coerceIn(1, 3600) * 1000
    var interval = code.optLong("interval", 5).coerceAtLeast(1)
    update { it.copy(prompt = DevicePrompt(code.getString("user_code"), url.toString(), expires), status = "Open het adres op je telefoon en voer de code in.") }
    while (now() < expires) {
     delay(interval * 1000)
     if (now() >= expires) break
     val response = transport.call(id, "/oauth/device/token", "POST", JSONObject().put("client_id", id).put("code", code.getString("device_code")))
     when (response.status) {
      200 -> {
       mutex.withLock {
        tokens = TraktTokens.fromResponse(JSONObject(response.body), now())
        try { save() } catch (e: Exception) { tokens = null; throw e }
        generation++; update { it.copy(connected = true, prompt = null, status = "Trakt gekoppeld.") }
       }
       refresh(); return@launch
      }
      400 -> Unit
      429 -> interval = maxOf(interval + 5, response.retryAfterSeconds)
      else -> response.checked()
     }
    }
    update { it.copy(status = "Aanmeldcode verlopen. Vraag een nieuwe code aan.") }
   } catch (e: CancellationException) { throw e }
   catch (e: Exception) { update { it.copy(status = if (e is TraktFailure || e is IllegalArgumentException) e.message.orEmpty() else "Aanmelden mislukt. Controleer Client ID en verbinding.") } }
   finally { update { it.copy(busy = false, prompt = null) } }
  }
 }
 fun cancelLogin() { login?.cancel(); update { it.copy(status = "Aanmelden geannuleerd.") } }
 private suspend fun api(path: String, method: String = "GET", body: JSONObject? = null, expectedGeneration: Int = generation): TraktHttpResult = mutex.withLock {
  if (expectedGeneration != generation) throw CancellationException("Account gewijzigd")
  suspend fun refreshToken() {
   val old = tokens ?: throw TraktFailure(401, "Koppel eerst je Trakt-account.")
   val response = transport.call(state.value.clientId, "/oauth/token", "POST", JSONObject()
    .put("client_id", state.value.clientId).put("redirect_uri", state.value.redirectUri).put("refresh_token", old.refresh).put("grant_type", "refresh_token"))
   if (response.status in listOf(400, 401)) { tokens = null; generation++; save(); update { it.copy(connected = false, watchlist = emptyList(), history = emptyList()) }; throw TraktFailure(401, "Meld opnieuw aan bij Trakt.") }
   val rotated = TraktTokens.fromResponse(JSONObject(response.checked().body), now())
   tokens = rotated
   try { save() } catch (e: Exception) { tokens = null; update { it.copy(connected = false) }; throw e }
  }
  if ((tokens ?: throw TraktFailure(401, "Koppel eerst je Trakt-account.")).expiresAt <= now() + 60000) refreshToken()
  var response = transport.call(state.value.clientId, path, method, body, tokens!!.access)
  if (response.status == 401) { refreshToken(); response = transport.call(state.value.clientId, path, method, body, tokens!!.access) }
  response.checked()
 }
 suspend fun refresh() {
  val accountGeneration = generation
  val profile = JSONObject(api("/users/settings", expectedGeneration = accountGeneration).body).getJSONObject("user").optString("username")
  val watchlist = mutableListOf<TraktMedia>()
  var page = 1
  do {
   val response = api("/sync/watchlist/all/added/desc?page=$page&limit=100", expectedGeneration = accountGeneration)
   watchlist += TraktMedia.list(response.body)
   require(response.pageCount <= 100) { "Kijklijst te groot (meer dan 10.000 items)." }
   page++
  } while (page <= response.pageCount)
  val history = TraktMedia.list(api("/sync/history/all?page=1&limit=100", expectedGeneration = accountGeneration).body)
  if (accountGeneration == generation) update { it.copy(username = profile, watchlist = watchlist.distinctBy { m -> m.key }, history = history, status = "Kijklijst en laatste 100 kijkregistraties bijgewerkt.") }
 }
 suspend fun disconnect() = mutex.withLock {
  login?.cancel()
  val token = tokens
  var revoked = true
  try { if (token != null) transport.call(state.value.clientId, "/oauth/revoke", "POST", JSONObject().put("client_id", state.value.clientId).put("token", token.access)).checked() }
  catch (e: CancellationException) { throw e } catch (_: Exception) { revoked = false }
  tokens = null; generation++
  update { it.copy(connected = false, username = "", autoScrobble = false, watchlist = emptyList(), history = emptyList(), status = if (revoked) "Trakt ontkoppeld." else "Lokaal ontkoppeld. Trek de toestemming ook in via je Trakt-accountinstellingen.") }; save()
 }
 suspend fun autoScrobble(value: Boolean) = mutex.withLock { update { it.copy(autoScrobble = value) }; save() }
 suspend fun link(key: String, media: TraktMedia?) = mutex.withLock {
  update { it.copy(links = if (media == null) it.links - key else it.links + (key to media), status = "Titelkoppeling opgeslagen.") }; save()
 }
 suspend fun search(query: String, type: String): List<TraktMedia> {
  require(type in listOf("movie", "show") && query.isNotBlank())
  return TraktMedia.list(transport.call(state.value.clientId, "/search/$type?query=${URLEncoder.encode(query, "UTF-8")}&page=1&limit=20").checked().body)
 }
 suspend fun episode(show: TraktMedia, season: Int, number: Int): TraktMedia {
  require(show.type == "show" && season >= 0 && number > 0)
  val o = JSONObject(transport.call(state.value.clientId, "/shows/${show.id}/seasons/$season/episodes/$number").checked().body)
  return requireNotNull(TraktMedia.fromApi("episode", o, JSONObject().put("title", show.title)))
 }
 suspend fun watchlist(media: TraktMedia, remove: Boolean) {
  api("/sync/watchlist${if (remove) "/remove" else ""}", "POST", payload(media)); refresh()
 }
 suspend fun watched(media: TraktMedia) {
  require(media.type != "show")
  api("/sync/history", "POST", payload(media, true)); refresh()
 }
 private fun payload(media: TraktMedia, watched: Boolean = false) = JSONObject().put(if (media.type == "movie") "movies" else if (media.type == "show") "shows" else "episodes", JSONArray().put(media.idObject().apply { if (watched) put("watched_at", java.time.Instant.ofEpochMilli(now()).toString()) }))
 fun playback(key: String): (String, Double) -> Unit {
  val media = state.value.links[key]
  val accountGeneration = generation
  val queue = kotlinx.coroutines.channels.Channel<Pair<String, Double>>(64)
  val worker = scope.launch {
   for ((action, progress) in queue) {
    if (accountGeneration != generation || !state.value.autoScrobble || !state.value.connected || media == null || media.type == "show") continue
    try { api("/scrobble/$action", "POST", JSONObject().put(media.type, media.idObject()).put("progress", progress.coerceIn(0.0, 100.0)), expectedGeneration = accountGeneration) }
    catch (e: CancellationException) { throw e }
    catch (e: Exception) { if (!(e is TraktFailure && e.status == 409)) update { it.copy(status = "Kijkvoortgang kon niet naar Trakt worden gestuurd. Controleer de verbinding.") } }
   }
  }
  return { action, progress ->
   if (action == "dispose") { queue.close() }
   else if (action in listOf("start", "pause", "stop") && progress.isFinite()) { if (queue.trySend(action to progress).isFailure) worker.cancel() }
  }
 }
}
