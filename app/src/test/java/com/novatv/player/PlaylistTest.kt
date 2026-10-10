package com.novatv.player

import org.junit.Assert.*
import org.junit.Test

class PlaylistTest {
 @Test fun metadataCommasBomAndRelativeStreams() {
  val text = "\uFEFF#EXTM3U\n#EXTINF:-1 tvg-id=\"n1\" group-title=\"News, local\" tvg-logo=\"logo.png\",News One\nstreams/1.m3u8\n"
  val item = Playlist.parse(text, "https://example.com/list.m3u").single()
  assertEquals("News One", item.name)
  assertEquals("News, local", item.group)
  assertEquals("n1", item.epgId)
  assertEquals("https://example.com/streams/1.m3u8", item.url)
 }
 @Test fun ignoresCommentsRejectsUnsafeSchemesAndDeduplicates() {
  val text = "#EXTM3U\n#EXTINF:-1,One\n#comment\nhttps://example.com/one\n#EXTINF:-1,Duplicate\nhttps://example.com/one\n#EXTINF:-1,Unsafe\nfile:///etc/passwd\n"
  assertEquals(1, Playlist.parse(text).size)
 }
 @Test fun handlesHttpAndExtgrpWithoutLeakingPreviousMetadata() {
  val text = "#EXTINF:-1 tvg-name=\"Fallback\",\n#EXTGRP:Sports\nhttp://example.com/a\n#EXTINF:-1,Second\nhttps://example.com/b"
  val items = Playlist.parse(text)
  assertEquals("Fallback", items[0].name)
  assertEquals("Sports", items[0].group)
  assertEquals("Overig", items[1].group)
  assertEquals("", items[1].epgId)
 }
}
