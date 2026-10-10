# Wijzigingen ten opzichte van v0.1

- `MainActivity.kt`: TV-knoppen, focus/Back, bronnen, bibliotheken, favorieten en programmatijdlijn.
- `Playback.kt`: één Media3-speler per afspeelscherm, zenderwissel, audiobeleid, lifecycle en fouten.
- `Playlist.kt`: M3U-metadata, relatieve streams, dubbele URL's en schemacontrole.
- `Guide.kt`: XMLTV en tijdzoneverwerking.
- `Sources.kt`: HTTPS-import, gzip, Xtream-catalogus en afleveringen; maximaal 32 MB gedecomprimeerde broninhoud per verzoek.
- `Storage.kt`: versleutelde broninstellingen en favorieten met Android Keystore.
- Manifest/resources: TV-launcherlogo/banner en uitgesloten overdracht van accountvoorkeuren.
- Gradle: v0.2.0, Compose for TV, lifecycle, tests en wrapper met checksum.
- Tests: zes parser- en tijdzonetests.

Het originele logo en de vier oorspronkelijke UI-kleuren zijn behouden. Er is geen artwork- of huisstijlvervanging gedaan.
