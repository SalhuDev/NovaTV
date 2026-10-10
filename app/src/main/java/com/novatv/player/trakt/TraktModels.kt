package com.novatv.player.trakt

import org.json.JSONArray
import org.json.JSONObject

data class TraktMedia(val type: String, val id: Long, val title: String, val year: Int? = null,
 val showTitle: String = "", val season: Int? = null, val number: Int? = null) {
 init { require(type in listOf("movie", "show", "episode") && id > 0) }
 val label: String get() = if (type == "episode") "$showTitle · S${season ?: "?"}E${number ?: "?"} · $title" else "$title${year?.let { " ($it)" }.orEmpty()}"
 val key: String get() = "$type:$id"
 fun idObject() = JSONObject().put("ids", JSONObject().put("trakt", id))
 fun toJson(): JSONObject = JSONObject().put("type", type).put("id", id).put("title", title).put("year", year).put("showTitle", showTitle).put("season", season).put("number", number)
 companion object {
  fun fromJson(o: JSONObject) = TraktMedia(o.getString("type"), o.getLong("id"), o.getString("title"), o.optInt("year").takeIf { it > 0 }, o.optString("showTitle"), o.optInt("season", -1).takeIf { it >= 0 }, o.optInt("number").takeIf { it > 0 })
  fun fromApi(type: String, o: JSONObject, show: JSONObject? = null): TraktMedia? {
   if (type !in listOf("movie", "show", "episode")) return null
   val id = o.optJSONObject("ids")?.optLong("trakt") ?: 0
   if (id <= 0) return null
   return TraktMedia(type, id, o.optString("title", "Onbekende titel"), o.optInt("year").takeIf { it > 0 }, show?.optString("title").orEmpty(), o.optInt("season", -1).takeIf { it >= 0 }, o.optInt("number").takeIf { it > 0 })
  }
  fun list(body: String): List<TraktMedia> {
   val array = JSONArray(body)
   return (0 until array.length()).mapNotNull { i ->
    val o = array.getJSONObject(i); val type = o.optString("type")
    o.optJSONObject(type)?.let { fromApi(type, it, o.optJSONObject("show")) }
   }.distinctBy { it.key }
  }
 }
}

// Tokens deliberately have no generated toString() implementation.
class TraktTokens(val access: String, val refresh: String, val expiresAt: Long) {
 fun toJson() = JSONObject().put("access_token", access).put("refresh_token", refresh).put("expires_at", expiresAt)
 companion object {
  fun fromResponse(o: JSONObject, now: Long): TraktTokens {
   val access = o.getString("access_token"); val refresh = o.getString("refresh_token")
   require(access.isNotBlank() && refresh.isNotBlank())
   val expires = o.getLong("expires_in"); require(expires in 1..31536000)
   return TraktTokens(access, refresh, (o.optLong("created_at", now / 1000) + expires) * 1000)
  }
  fun restore(o: JSONObject) = TraktTokens(o.getString("access_token"), o.getString("refresh_token"), o.getLong("expires_at"))
 }
}

data class DevicePrompt(val code: String, val verificationUrl: String, val expiresAt: Long)
data class TraktUiState(
 val clientId: String = "", val redirectUri: String = "https://github.com/SalhuDev/NovaTV",
 val connected: Boolean = false, val username: String = "", val autoScrobble: Boolean = false,
 val actionBusy: Boolean = false, val busy: Boolean = false, val prompt: DevicePrompt? = null, val status: String = "Koppel je Trakt-account om je kijklijst en kijkgeschiedenis te gebruiken.",
 val watchlist: List<TraktMedia> = emptyList(), val history: List<TraktMedia> = emptyList(),
 val links: Map<String, TraktMedia> = emptyMap()
)

/** Emits transitions, never a start on buffering and never duplicate stops. */
class ScrobbleSession {
 private var started = false
 private var paused = true
 private var finished = false
 fun update(position: Long, duration: Long, playing: Boolean, userPaused: Boolean = false, ended: Boolean = false): Pair<String, Double>? {
  if (finished || duration <= 0 || position < 0) return null
  val progress = (position.toDouble() / duration * 100).coerceIn(0.0, 100.0)
  if (ended) return stop(position, duration)
  if (playing && paused) { started = true; paused = false; return "start" to progress }
  if (userPaused && started && !paused) { paused = true; return "pause" to progress }
  return null
 }
 fun stop(position: Long, duration: Long): Pair<String, Double>? {
  if (finished) return null
  finished = true
  if (!started || duration <= 0 || position < 0) return null
  val progress = (position.toDouble() / duration * 100).coerceIn(0.0, 100.0)
  return if (progress >= 1) "stop" to progress else null
 }
}
