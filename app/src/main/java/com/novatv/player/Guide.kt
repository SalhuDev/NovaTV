package com.novatv.player

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class Programme(val channelId: String, val title: String, val start: Long, val end: Long)
object Guide {
 fun timestamp(value: String): Long {
  val parts = value.trim().split(Regex("\\s+"))
  val digits = parts.first().padEnd(14, '0').take(14)
  val zone = parts.getOrNull(1) ?: "+0000"
  return OffsetDateTime.parse("$digits $zone", DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z")).toInstant().toEpochMilli()
 }
 fun parse(xml: String): List<Programme> {
  require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true)) { "XML-entiteiten zijn niet toegestaan" }
  val parser = XmlPullParserFactory.newInstance().newPullParser()
  parser.setInput(StringReader(xml))
  val result = mutableListOf<Programme>()
  var channel = ""; var start = 0L; var end = 0L; var title = ""; var inside = false
  var event = parser.eventType
  while (event != XmlPullParser.END_DOCUMENT) {
   if (event == XmlPullParser.START_TAG) when (parser.name) {
    "programme" -> {
     channel = parser.getAttributeValue(null, "channel").orEmpty()
     start = runCatching { timestamp(parser.getAttributeValue(null, "start").orEmpty()) }.getOrDefault(0)
     end = runCatching { timestamp(parser.getAttributeValue(null, "stop").orEmpty()) }.getOrDefault(0)
     title = ""; inside = true
    }
    "title" -> if (inside && title.isBlank()) title = parser.nextText()
   }
   if (event == XmlPullParser.END_TAG && parser.name == "programme") {
    if (channel.isNotBlank() && start > 0 && end > start) result += Programme(channel, title.ifBlank { "Geen titel" }, start, end)
    inside = false
   }
   event = parser.next()
  }
  return result.sortedBy { it.start }
 }
}
