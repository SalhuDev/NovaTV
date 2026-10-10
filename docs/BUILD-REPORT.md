# NOVA TV v0.2 — buildrapport

Datum: 10 oktober 2026.

## Gecontroleerde uitgangssituatie

De ZIP bevatte negen bestanden: een klein Android-project met één activity, eenvoudige M3U-parser, Gradle-configuratie en het originele logo. Er was geen Gradle-wrapper, APK of bestaande XMLTV/Xtream-implementatie. Alle oorspronkelijke tekstbestanden zijn gelezen en het logo is visueel gecontroleerd.

## Uitgevoerde stappen

1. Compose for TV-knoppen, D-padfocus, Teruggedrag en herstel naar het gekozen lijstitem toegevoegd.
2. Media3-speler hergebruikt bij zenderwissels, lifecycle-pauze en fout-/bufferstatus toegevoegd.
3. M3U-metadata, relatieve URL's en deduplicatie; XMLTV-parser en tijdlijn toegevoegd.
4. Xtream-login, live/VOD/series/afleveringen en versleutelde instellingen/favorieten toegevoegd.
5. Gradle-wrapper 8.9 inclusief officiële distributiechecksum en TV-launcherassets toegevoegd.
6. Debug-APK gebouwd; zes unit tests geslaagd; APK v2-handtekening geverifieerd.

## Buildomgeving

- Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21.
- JDK 21, bytecode-target Java 17.
- Android compile/target SDK 35, minimum SDK 26.
- Versie 0.2.0, package `com.novatv.player`.

Uitgevoerde taken: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
De omgeving gebruikt `ANDROID_HOME=/tmp/android-sdk`, `ANDROID_USER_HOME=/tmp/nova-android` en `GRADLE_USER_HOME=/tmp/nova-gradle`. De HTTPS-proxy is alleen voor deze omgeving ingesteld, niet in het geleverde project.

## Problemen en resultaten

Aanvankelijk ontbraken Gradle en de SDK. Downloads vereisten netwerktoegang en Java-proxyinstellingen. De standaard Android-cachemap was niet schrijfbaar; een map onder `/tmp` loste dat op. Daarna slaagden Kotlin-compilatie en APK-build.

Er waren geen resterende Kotlin-compilerfouten. Een niet-blokkerende melding over de Android-analyticsmap en een melding over ongewijzigd verpakken van `libandroidx.graphics.path.so` kwamen tijdens de build voor.

Tests: zes geslaagd, nul mislukt. De tests dekken M3U-metadata/komma's/BOM/relatieve URL's/deduplicatie/schemes en XMLTV-tijdzones/ongeldige intervallen/entiteiten.

Lint geslaagd: 0 errors, 11 warnings. De overgebleven waarschuwingen betreffen nieuwere bibliotheekversies, vaste TV-landscapeoriëntatie, een aanvullende backup-configuratieaanbeveling voor Android 8–11 (allowBackup staat reeds uit) en de vierkante vorm van het behouden originele logo. Het volledige rapport is bijgevoegd als `lint-results-debug.txt`.

## Logo en ontwerp

Het originele asset is byte-identiek bewaard en gecontroleerd in de APK.
SHA-256: `194325f54d050b8aaf36255911d22f699462b77f950bd39f206b245dea4eb13f`.
De oorspronkelijke kleuren `#110E21`, `#211735`, `#AA73FA` en `#F6F0FF` zijn behouden.

## Nog op hardware te verifiëren

Geen emulator, fysieke TV, IPTV-account of HDMI/Sonos-keten was beschikbaar voor end-to-endtesten. Daarom zijn D-padgedrag, echte streams, schakelsnelheid en audio-passthrough niet als hardwaregetest aangemerkt. Atmos/Sonos vereist passende hardware en mediabronnen; directe Sonos-casting ontbreekt. Zie `TV-CHECKLIST.md`.

Dit is een debug-ontwikkelbuild. Bronvelden en favorieten blijven bewaard; catalogi en EPG worden na een herstart handmatig opnieuw geladen. Er is geen DRM, catch-up of ouderlijk toezicht toegevoegd.
