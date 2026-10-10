package com.novatv.player.trakt

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TraktTest {
 private class Fake(val respond: suspend (String, JSONObject?, String?) -> TraktHttpResult) : TraktTransport {
  override suspend fun call(clientId: String, path: String, method: String, body: JSONObject?, token: String?) = respond(path, body, token)
 }
 private fun response(body: String = "{}", status: Int = 200, pages: Int = 1) = TraktHttpResult(status, body, pageCount = pages)
 private fun initial(expiry: Long = 1000000) = JSONObject().put("clientId", "public-client")
  .put("tokens", TraktTokens("old-access", "old-refresh", expiry).toJson())
 private fun media(id: Int) = """{"type":"movie","movie":{"title":"Film $id","ids":{"trakt":$id}}}"""
 @Test fun deviceFlowHonorsPendingAndSlowdown() = runTest {
  val times = mutableListOf<Long>(); var polls = 0
  val fake = Fake { path, body, _ -> when (path) {
   "/oauth/device/code" -> response("""{"device_code":"private-code","user_code":"ABCD","verification_url":"https://trakt.tv/activate","expires_in":60,"interval":2}""")
   "/oauth/device/token" -> {
    assertEquals("private-code", body!!.getString("code")); assertFalse(body.has("client_secret")); times += testScheduler.currentTime
    when (++polls) { 1 -> response(status = 400); 2 -> TraktHttpResult(429, "", 9); else -> response("""{"access_token":"access","refresh_token":"refresh","expires_in":600}""") }
   }
   "/users/settings" -> response("""{"user":{"username":"test"}}""")
   else -> response("[]")
  } }
  var saved = JSONObject()
  val repo = TraktRepository(fake, JSONObject().put("clientId", "public-client"), { saved = it }, backgroundScope, { testScheduler.currentTime })
  repo.connect(); advanceTimeBy(14000); runCurrent()
  assertEquals(listOf(2000L, 4000L, 13000L), times)
  assertTrue(repo.state.value.connected); assertEquals("test", repo.state.value.username)
  assertEquals("refresh", saved.getJSONObject("tokens").getString("refresh_token"))
 }
 @Test fun expiredDeviceCodeDoesNotPollAgain() = runTest {
  var polls = 0
  val repo = TraktRepository(Fake { path, _, _ ->
   if (path.endsWith("/code")) response("""{"device_code":"x","user_code":"X","verification_url":"https://trakt.tv/activate","expires_in":2,"interval":3}""")
   else { polls++; response(status = 400) }
  }, JSONObject().put("clientId", "client"), {}, backgroundScope, { testScheduler.currentTime })
  repo.connect(); advanceTimeBy(4000); runCurrent()
  assertEquals(0, polls); assertFalse(repo.state.value.connected); assertNull(repo.state.value.prompt)
 }
 @Test fun refreshIsSerializedAndRotatedTokensPersistBeforeUse() = runTest {
  var refreshes = 0; var saved = JSONObject()
  val repo = TraktRepository(Fake { path, body, token -> when (path) {
   "/oauth/token" -> { refreshes++; assertEquals("old-refresh", body!!.getString("refresh_token")); delay(20); response("""{"access_token":"new-access","refresh_token":"new-refresh","expires_in":600}""") }
   "/users/settings" -> { assertEquals("new-access", token); assertEquals("new-refresh", saved.getJSONObject("tokens").getString("refresh_token")); response("""{"user":{"username":"test"}}""") }
   else -> response("[]")
  } }, initial(1), { saved = it }, backgroundScope, { testScheduler.currentTime })
  coroutineScope { repeat(2) { launch { repo.refresh() } } }
  assertEquals(1, refreshes)
 }
 @Test fun watchlistPaginationAndEpisodeHistoryPayload() = runTest {
  var payload: JSONObject? = null
  val repo = TraktRepository(Fake { path, body, _ -> when {
   path == "/users/settings" -> response("""{"user":{"username":"test"}}""")
   path.startsWith("/sync/watchlist") -> response("[${media(if (path.contains("page=1")) 1 else 2)}]", pages = 2)
   path == "/sync/history" -> { payload = body; response() }
   else -> response("[]")
  } }, initial(), {}, backgroundScope, { 0L })
  repo.watched(TraktMedia("episode", 42, "Pilot", showTitle = "Show", season = 1, number = 1))
  assertEquals(listOf(1L, 2L), repo.state.value.watchlist.map { it.id })
  assertFalse(payload!!.has("movies"))
  assertEquals(42, payload!!.getJSONArray("episodes").getJSONObject(0).getJSONObject("ids").getInt("trakt"))
  assertTrue(payload!!.getJSONArray("episodes").getJSONObject(0).has("watched_at"))
 }
 @Test fun revokedRefreshClearsLogin() = runTest {
  var saved = JSONObject()
  val repo = TraktRepository(Fake { _, _, _ -> response(status = 400) }, initial(1), { saved = it }, backgroundScope, { 0L })
  try { repo.refresh(); fail() } catch (_: TraktFailure) { }
  assertFalse(repo.state.value.connected); assertFalse(saved.has("tokens"))
 }
 @Test fun scrobbleTracksTransitionsNotBuffering() {
  val s = ScrobbleSession()
  assertNull(s.update(0, -1, true))
  assertEquals("start", s.update(0, 1000, true)!!.first)
  assertNull(s.update(200, 1000, false)) // buffering
  assertNull(s.update(210, 1000, true))
  assertEquals("pause", s.update(300, 1000, false, true)!!.first)
  assertNull(s.update(300, 1000, false, true))
  assertEquals("start", s.update(300, 1000, true)!!.first)
  assertEquals("stop" to 100.0, s.update(1100, 1000, false, ended = true))
  assertNull(s.stop(1100, 1000))
 }
 @Test fun noStopBeforePlaybackOrBelowOnePercent() {
  assertNull(ScrobbleSession().stop(100, 1000))
  val s = ScrobbleSession(); s.update(0, 1000, true)
  assertNull(s.stop(5, 1000))
 }
 @Test fun scrobblingRequiresOptInAndStopsAfterDisconnect() = runTest {
  var calls = 0
  val config = initial().put("links", JSONObject().put("local-key", TraktMedia("movie", 12, "Film").toJson()))
  val repo = TraktRepository(Fake { path, _, _ -> if (path.startsWith("/scrobble")) calls++; response() }, config, {}, backgroundScope, { 0L })
  val tracker = repo.playback("local-key")
  tracker("start", 0.0); runCurrent(); assertEquals(0, calls)
  repo.autoScrobble(true)
  tracker("start", 10.0); runCurrent(); assertEquals(1, calls)
  repo.disconnect()
  tracker("stop", 90.0); tracker("dispose", 0.0); runCurrent(); assertEquals(1, calls)
 }
 @Test fun canceledDeviceLoginDoesNotKeepPolling() = runTest {
  var polls = 0
  val repo = TraktRepository(Fake { path, _, _ ->
   if (path.endsWith("/code")) response("""{"device_code":"x","user_code":"X","verification_url":"https://trakt.tv/activate","expires_in":60,"interval":2}""")
   else { polls++; response(status = 400) }
  }, JSONObject().put("clientId", "client"), {}, backgroundScope, { testScheduler.currentTime })
  repo.connect(); runCurrent(); repo.cancelLogin(); advanceTimeBy(60000); runCurrent()
  assertEquals(0, polls); assertNull(repo.state.value.prompt); assertFalse(repo.state.value.busy)
 }
 @Test fun repeatedUiActionsAreIgnoredWhileRequestRuns() = runTest {
  val repo = TraktRepository(Fake { _, _, _ -> response() }, initial(), {}, backgroundScope, { 0L })
  var requests = 0
  repo.runTask { requests++; delay(1000) }
  runCurrent()
  assertTrue(repo.state.value.actionBusy)
  repo.runTask { requests++ }; runCurrent()
  assertEquals(1, requests)
  advanceTimeBy(1001); runCurrent()
  assertFalse(repo.state.value.actionBusy)
  repo.runTask { requests++ }; runCurrent()
  assertEquals(2, requests)
 }
 @Test fun failedUiActionAllowsRetryAndShowsSafeMessage() = runTest {
  val repo = TraktRepository(Fake { _, _, _ -> response() }, initial(), {}, backgroundScope, { 0L })
  repo.runTask { throw java.io.IOException("private server details") }; runCurrent()
  assertFalse(repo.state.value.actionBusy)
  assertFalse(repo.state.value.status.contains("private server details"))
  var retried = false
  repo.runTask { retried = true }; runCurrent()
  assertTrue(retried)
 }
 @Test fun missingOrInvalidTraktIdsAreNotMatched() {
  assertTrue(TraktMedia.list("""[{"type":"movie","movie":{"title":"No ID"}}]""").isEmpty())
 }
}
