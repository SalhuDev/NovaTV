package com.novatv.player

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

object Sources {
 fun read(url: String): String {
  require(URI(url).scheme == "https") { "Gebruik een HTTPS-bron" }
  var current = url
  repeat(6) {
   val connection = URL(current).openConnection() as HttpURLConnection
   connection.connectTimeout = 10000; connection.readTimeout = 20000
   connection.instanceFollowRedirects = false
   try {
    val code = connection.responseCode
    if (code in listOf(301, 302, 303, 307, 308)) {
     current = URI(current).resolve(connection.getHeaderField("Location") ?: error("Redirect zonder locatie")).toString()
     require(URI(current).scheme == "https") { "Onveilige redirect geweigerd" }
    } else {
     require(code in 200..299) { "Bron antwoordt met HTTP $code" }
     val stream = connection.inputStream
     val input = if (connection.contentEncoding.equals("gzip", true) || URI(current).path.endsWith(".gz")) GZIPInputStream(stream) else stream
     return input.bufferedReader().use { reader ->
      val text = StringBuilder(); val buffer = CharArray(8192)
      while (true) { val count = reader.read(buffer); if (count < 0) break; text.append(buffer, 0, count); require(text.length <= 32 * 1024 * 1024) { "Bron groter dan 32 MB" } }
      text.toString()
     }
    }
   } finally { connection.disconnect() }
  }
  error("Te veel redirects")
 }
 private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
 data class Xtream(val server: String, val username: String, val password: String) {
  val base: String get() = server.trimEnd('/')
  fun api(action: String = "", extra: String = ""): String = "$base/player_api.php?username=${encode(username)}&password=${encode(password)}" + if (action.isBlank()) "" else "&action=$action$extra"
  fun epg(): String = "$base/xmltv.php?username=${encode(username)}&password=${encode(password)}"
  fun stream(kind: String, id: String, extension: String): String = "$base/$kind/${encode(username)}/${encode(password)}/${encode(id)}.${encode(extension)}"
 }
 data class Catalog(val live: List<Channel>, val films: List<Channel>, val series: List<Series>)
 data class Series(val id: String, val name: String)
 fun catalog(account: Xtream): Catalog {
  val info = JSONObject(read(account.api()))
  require(info.optJSONObject("user_info")?.optInt("auth") == 1) { "Aanmelden bij provider mislukt" }
  fun streams(action: String, kind: String): List<Channel> {
   val array = JSONArray(read(account.api(action)))
   return (0 until array.length()).map { i ->
    val item = array.getJSONObject(i)
    Channel(item.optString("name"), account.stream(kind, item.getString("stream_id"), if (kind == "live") "ts" else item.optString("container_extension", "mp4")), item.optString("category_id", "Overig"), item.optString("epg_channel_id"), item.optString("stream_icon"), kind)
   }
  }
  val series = JSONArray(read(account.api("get_series")))
  return Catalog(streams("get_live_streams", "live"), streams("get_vod_streams", "movie"), (0 until series.length()).map { i -> val s = series.getJSONObject(i); Series(s.getString("series_id"), s.optString("name")) })
 }
 fun episodes(account: Xtream, series: Series): List<Channel> {
  val seasons = JSONObject(read(account.api("get_series_info", "&series_id=${encode(series.id)}"))).optJSONObject("episodes") ?: return emptyList()
  return seasons.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: 0 }.flatMap { season ->
   val items = seasons.getJSONArray(season)
   (0 until items.length()).map { i -> val item = items.getJSONObject(i); Channel(item.optString("title", "Aflevering ${i + 1}"), account.stream("series", item.getString("id"), item.optString("container_extension", "mp4")), "${series.name} · seizoen $season", kind = "series") }
  }
 }
}
