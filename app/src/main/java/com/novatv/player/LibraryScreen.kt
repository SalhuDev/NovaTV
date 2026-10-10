package com.novatv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable private fun Artwork(url: String, title: String, modifier: Modifier) {
 SubcomposeAsyncImage(model = url.takeIf { it.startsWith("https://") || it.startsWith("http://") }, contentDescription = null,
  contentScale = ContentScale.Crop, modifier = modifier,
  loading = { Placeholder(title) }, error = { Placeholder(title) })
}
@Composable private fun Placeholder(title: String) {
 Box(Modifier.fillMaxSize().background(Color(0xFF211735)), contentAlignment = Alignment.Center) {
  Text(title.take(2).uppercase(), style = MaterialTheme.typography.headlineLarge, color = Color(0xFFAA73FA))
 }
}
@Composable private fun LibraryFilters(query: String, changeQuery: (String) -> Unit, group: String, groups: List<String>, changeGroup: (String) -> Unit) {
 var expanded by remember { mutableStateOf(false) }
 Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
  OutlinedTextField(query, changeQuery, singleLine = true, label = { Text("Zoeken op titel") }, modifier = Modifier.weight(1f))
  Box {
   RemoteButton("Groep: $group") { expanded = true }
   DropdownMenu(expanded, { expanded = false }) {
    groups.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { changeGroup(item); expanded = false }) }
   }
  }
 }
}
@Composable fun MediaLibrary(entries: List<Channel>, favorites: Set<String>, focus: FocusRequester, returnKey: String,
 trakt: (Channel) -> Unit, play: (Channel) -> Unit, favorite: (Channel) -> Unit) {
 var query by rememberSaveable { mutableStateOf("") }
 var group by rememberSaveable { mutableStateOf("Alle") }
 val groups = remember(entries) { listOf("Alle") + entries.map { it.group }.distinct().sorted() }
 val visible = remember(entries, query, group) { entries.filter { it.name.contains(query, true) && (group == "Alle" || it.group == group) } }
 val restoreKey = visible.firstOrNull { it.key == returnKey }?.key ?: visible.firstOrNull()?.key
 Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
  LibraryFilters(query, { query = it }, group, groups, { group = it })
  Text("${visible.size} titels", color = Color.LightGray)
  if (visible.isEmpty()) {
   Text(if (entries.isEmpty()) "Je bibliotheek is nog leeg. Laad een bron via Bronnen of voeg favorieten toe." else "Geen titels gevonden. Pas je zoekterm of groep aan.")
   RemoteButton("Filters wissen", Modifier.focusRequester(focus)) { query = ""; group = "Alle" }
  } else if (entries.all { it.kind == "movie" }) {
   val state = rememberLazyGridState(initialFirstVisibleItemIndex = visible.indexOfFirst { it.key == restoreKey }.coerceAtLeast(0))
   LazyVerticalGrid(modifier = Modifier.weight(1f), columns = GridCells.Adaptive(170.dp), state = state, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    items(visible, key = { it.key }) { item ->
     Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Artwork(item.logo, item.name, Modifier.fillMaxWidth().height(180.dp))
      RemoteButton(item.name, Modifier.fillMaxWidth().then(if (item.key == restoreKey) Modifier.focusRequester(focus) else Modifier)) { play(item) }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
       RemoteButton(if (item.key in favorites) "★" else "☆", Modifier.weight(1f).semantics { contentDescription = if (item.key in favorites) "Verwijder ${item.name} uit favorieten" else "Bewaar ${item.name} in favorieten" }) { favorite(item) }
       RemoteButton("Trakt", Modifier.weight(1f)) { trakt(item) }
      }
     }
    }
   }
  } else {
   val state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = visible.indexOfFirst { it.key == restoreKey }.coerceAtLeast(0))
   LazyColumn(modifier = Modifier.weight(1f), state = state, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    items(visible, key = { it.key }) { item ->
     Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Artwork(item.logo, item.name, Modifier.size(52.dp))
      RemoteButton("${item.name} · ${item.group}", Modifier.weight(1f).then(if (item.key == restoreKey) Modifier.focusRequester(focus) else Modifier)) { play(item) }
      if (item.kind != "live") RemoteButton("Trakt") { trakt(item) }
      RemoteButton(if (item.key in favorites) "★ Bewaard" else "☆ Bewaar") { favorite(item) }
     }
    }
   }
  }
 }
}
@Composable fun SeriesLibrary(entries: List<Sources.Series>, focus: FocusRequester, returnId: String, busy: Boolean, open: (Sources.Series) -> Unit) {
 var query by rememberSaveable { mutableStateOf("") }
 var group by rememberSaveable { mutableStateOf("Alle") }
 val groups = remember(entries) { listOf("Alle") + entries.map { it.group }.distinct().sorted() }
 val visible = remember(entries, query, group) { entries.filter { it.name.contains(query, true) && (group == "Alle" || it.group == group) } }
 val restoreId = visible.firstOrNull { it.id == returnId }?.id ?: visible.firstOrNull()?.id
 Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
  LibraryFilters(query, { query = it }, group, groups, { group = it })
  if (visible.isEmpty()) {
   Text(if (entries.isEmpty()) "Verbind een Xtream-bron via Bronnen om series te laden." else "Geen series gevonden.")
   RemoteButton("Filters wissen", Modifier.focusRequester(focus)) { query = ""; group = "Alle" }
  }
  val state = rememberLazyGridState(initialFirstVisibleItemIndex = visible.indexOfFirst { it.id == restoreId }.coerceAtLeast(0))
  LazyVerticalGrid(modifier = Modifier.weight(1f), columns = GridCells.Adaptive(170.dp), state = state, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   items(visible, key = { it.id }) { item ->
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
     Artwork(item.cover, item.name, Modifier.fillMaxWidth().height(180.dp))
     RemoteButton(item.name, Modifier.fillMaxWidth().then(if (item.id == restoreId) Modifier.focusRequester(focus) else Modifier), enabled = !busy) { open(item) }
    }
   }
  }
 }
}

@Composable internal fun ReadingText(text: String) {
 var focused by remember { mutableStateOf(false) }
 Text(text, color = Color(0xFFF6F0FF), modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
  .border(2.dp, if (focused) Color(0xFFAA73FA) else Color.Transparent).focusable().padding(10.dp))
}
