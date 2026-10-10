# NOVA TV — Android TV v0.2

## Download v0.2.0

- [Debug-APK downloaden](https://github.com/SalhuDev/NovaTV/raw/refs/heads/main/downloads/v0.2.0/NOVA_TV_v0.2_debug.apk)
- [Bronpakket downloaden](https://github.com/SalhuDev/NovaTV/raw/refs/heads/main/downloads/v0.2.0/NOVA_TV_v0.2_source.zip)
- [Buildrapport](docs/BUILD-REPORT.md) · [SHA-256-controlesommen](downloads/v0.2.0/SHA256SUMS.txt)

Gradle-build en zes unit tests geslaagd; Android Lint: nul fouten, elf waarschuwingen. APK-handtekening gecontroleerd. Dit is een debug-ontwikkelbuild voor Android TV 8+. Echte IPTV-provider-, afstandsbediening- en Atmos/Sonos-tests op hardware staan nog open.


Kotlin, Jetpack Compose for TV en AndroidX Media3. Het originele paarse druppellogo (`app/src/main/assets/nova_mascot.png`) en de bestaande kleuren zijn behouden.

## Aanwezig

- TV-knoppen met duidelijke D-padfocus; Terug sluit eerst speler/afleveringen en gaat daarna naar Home.
- M3U met tvg-id, groepen, relatieve stream-URL's en deduplicatie.
- Media3-speler met hergebruik bij zenderwisseling, CH+/CH−/Page Up/Down, pauze bij verlaten van de app, buffering, foutmelding en opnieuw proberen.
- XMLTV en XMLTV.gz, tijdzoneverwerking, drie-uursgids, huidig-tijdmarkering en Eerder/Nu/Later. Programmakeuze opent de live-zender, geen catch-up.
- Xtream-login, live, films, series en afleveringen.
- Favorieten inclusief afleveringen blijven na herstart beschikbaar.
- Broninstellingen en favorieten versleuteld met Android Keystore; backups uitgeschakeld.
- Android TV-launchericoon en banner met het originele logo.

## Bouwen

Installeer JDK 17 of 21 en Android SDK 35. Stel `ANDROID_HOME` in of configureer `sdk.dir` in je lokale `local.properties`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Android 8/API 26 of hoger vereist.

## Gebruik

Open Bronnen en laad een eigen HTTPS M3U, of verbind een HTTPS Xtream-server. Voor een gids laad je daarnaast een XMLTV-adres; bij Xtream wordt dat adres ingevuld. Opnieuw laden na een app-herstart is handmatig; bronvelden en favorieten zijn bewaard. HTTP- en HTTPS-mediastreams worden ondersteund, bron- en accountimport verloopt via HTTPS.

## Audio en grenzen

Media3 kiest ondersteunde audioformaten via Android. Dolby Atmos en passthrough vereisen geschikte stream, codec, TV-box, TV en HDMI ARC/eARC-keten. Sonos werkt via de aangesloten soundbar; directe Sonos-casting is niet ingebouwd. Er is geen eigen TrueHD/DTS-decoder en geen garantie dat ieder apparaat elk formaat doorgeeft.

Deze versie is een functionele ontwikkelbuild. Hardware-, afstandsbediening- en echte-providercontroles staan in `docs/TV-CHECKLIST.md`. De test- en buildresultaten staan in `docs/BUILD-REPORT.md`.
