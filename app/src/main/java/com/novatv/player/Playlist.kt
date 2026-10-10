package com.novatv.player

import java.net.URI

data class Channel(
 val name: String, val url: String, val group: String = "Overig",
 val epgId: String = "", val logo: String = "", val kind: String = "live",
 val showTitle: String = "", val season: Int? = null, val episode: Int? = null
) { val key: String get() = "$kind:$url" }

object Playlist {
 private val attribute = Regex("([\\w-]+)\\s*=\\s*\"([^\"]*)\"")
 fun parse(text: String, baseUrl: String = ""): List<Channel> {
  val result = mutableListOf<Channel>()
  var metadata: Map<String, String>? = null
  var name = ""
  var group = "Overig"
  for (raw in text.removePrefix("\uFEFF").lineSequence()) {
   val line = raw.trim()
   when {
    line.startsWith("#EXTINF:", true) -> {
     metadata = attribute.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
     // Commas inside quoted attributes are not title separators.
     var quoted = false
     val separator = line.indexOfFirst { c -> if (c == '"') quoted = !quoted; c == ',' && !quoted }
     name = if (separator >= 0) line.substring(separator + 1).trim() else ""
     if (name.isBlank()) name = metadata["tvg-name"].orEmpty().ifBlank { "Onbekende zender" }
     group = metadata["group-title"].orEmpty().ifBlank { "Overig" }
    }
    line.startsWith("#EXTGRP:", true) -> group = line.substringAfter(':').ifBlank { "Overig" }
    line.isNotBlank() && !line.startsWith('#') && metadata != null -> {
     val resolved = runCatching { if (baseUrl.isBlank()) URI(line) else URI(baseUrl).resolve(line) }.getOrNull()
     if (resolved?.scheme?.lowercase() in listOf("http", "https")) {
      result += Channel(name, resolved.toString(), group, metadata["tvg-id"].orEmpty(), metadata["tvg-logo"].orEmpty())
     }
     metadata = null
    }
   }
  }
  return result.distinctBy { it.key }
 }
}
