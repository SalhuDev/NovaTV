package com.novatv.player.trakt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TraktHttpResult(val status: Int, val body: String, val retryAfterSeconds: Long = 0, val pageCount: Int = 1)
interface TraktTransport {
 suspend fun call(clientId: String, path: String, method: String = "GET", body: JSONObject? = null, token: String? = null): TraktHttpResult
}

/** Separate, fixed hosts: never forward provider URLs or OAuth headers through redirects. */
class TraktHttp : TraktTransport {
 override suspend fun call(clientId: String, path: String, method: String, body: JSONObject?, token: String?): TraktHttpResult = withContext(Dispatchers.IO) {
  require(path.startsWith('/') && !path.startsWith("//") && !path.contains(".."))
  val host = if (path.startsWith("/oauth/")) "https://auth.trakt.tv" else "https://api.trakt.tv"
  val connection = URL(host + path).openConnection() as HttpURLConnection
  connection.connectTimeout = 10000; connection.readTimeout = 15000
  connection.instanceFollowRedirects = false; connection.requestMethod = method
  connection.setRequestProperty("Content-Type", "application/json")
  connection.setRequestProperty("Accept", "application/json")
  connection.setRequestProperty("trakt-api-version", "2")
  connection.setRequestProperty("trakt-api-key", clientId)
  connection.setRequestProperty("User-Agent", "NOVA-TV/0.4.0")
  if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
  try {
   if (body != null) { connection.doOutput = true; connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) } }
   val status = connection.responseCode
   val stream = if (status in 200..299) connection.inputStream else connection.errorStream
   val text = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
    val output = StringBuilder(); val buffer = CharArray(8192)
    while (true) { val count = reader.read(buffer); if (count < 0) break; output.append(buffer, 0, count); check(output.length <= 4 * 1024 * 1024) { "Trakt-antwoord te groot" } }
    output.toString()
   }.orEmpty()
   TraktHttpResult(status, text, connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceIn(0, 3600) ?: 0,
    connection.getHeaderField("X-Pagination-Page-Count")?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
  } finally { connection.disconnect() }
 }
}

class TraktFailure(val status: Int, message: String): Exception(message)
fun TraktHttpResult.checked(): TraktHttpResult {
 if (status in 200..299) return this
 throw TraktFailure(status, when (status) {
  401 -> "Je Trakt-aanmelding is verlopen. Koppel opnieuw."
  403 -> "Trakt weigert toegang voor dit account of deze app."
  404 -> "Deze titel of koppeling is niet gevonden op Trakt."
  409 -> "Deze actie is al verwerkt door Trakt."
  410 -> "De aanmeldcode is verlopen."
  418 -> "De Trakt-koppeling is geweigerd."
  420 -> "De limiet van je Trakt-kijklijst is bereikt."
  429 -> "Trakt vraagt om even te wachten. Probeer het later opnieuw."
  in 500..599 -> "Trakt is tijdelijk niet bereikbaar."
  else -> "Trakt kon de aanvraag niet verwerken (HTTP $status)."
 })
}
