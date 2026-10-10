package com.novatv.player

import org.junit.Assert.*
import org.junit.Test

class GuideTest {
 @Test fun offsetsProduceSameInstant() {
  assertEquals(Guide.timestamp("20261010120000 +0200"), Guide.timestamp("20261010100000 +0000"))
 }
 @Test fun readsTitleAndRejectsInvalidIntervals() {
  val entries = Guide.parse("<tv><programme channel=\"n1\" start=\"20261010120000 +0000\" stop=\"20261010130000 +0000\"><title>News &amp; sport</title></programme><programme channel=\"n1\" start=\"broken\" stop=\"broken\"><title>Bad</title></programme></tv>")
  assertEquals(1, entries.size)
  assertEquals("News & sport", entries.single().title)
  assertEquals("n1", entries.single().channelId)
 }
 @Test(expected = IllegalArgumentException::class) fun rejectsEntities() {
  Guide.parse("<!DOCTYPE tv [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><tv/>")
 }
}
