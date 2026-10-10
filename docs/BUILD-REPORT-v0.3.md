# NOVA TV v0.3.0 — Trakt-integratie

Datum: 10 oktober 2026. Android applicationId: `com.novatv.player`, versionCode 3, minSdk 26, target/compileSdk 35.

## Wijzigingen

Trakt-deviceaanmelding, versleutelde OAuth-opslag, tokenvernieuwing, account ontkoppelen, gepagineerde kijklijst, recente geschiedenis, expliciete film/afleveringkoppeling en optionele Media3-scrobbling. Seizoen- en afleveringsgegevens blijven in favorieten bewaard. Live-zenderwisselingen behouden het bestaande Media3-spelerhergebruik.

Het originele logo is ongewijzigd: SHA-256 `194325f54d050b8aaf36255911d22f699462b77f950bd39f206b245dea4eb13f`. Bestaande donkerpaarse kleuren en TV-knoppen blijven in gebruik.

## Build en controles

Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, JDK 21 (bytecode Java 17).

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

**Geslaagd:** debug-APK gebouwd, 16 tests geslaagd, Android Lint 0 fouten / 11 bestaande waarschuwingen. APK v2-handtekening geldig en gelijk aan v0.2; installatie als update is mogelijk. De waarschuwingen betreffen verouderde dependencies, landscape, backupconfiguratie en het bestaande icoon. Een tijdelijke Kotlin-daemonstartmelding herstelde automatisch.

Resultaat en bewijs: zie de bijgevoegde Gradle-log, JUnit XML-bestanden, Lint-rapport en APK-handtekeningcontrole in `docs/v0.3/`.

De eerste compileerpoging vond een extra sluitende haak in het laden van favorieten. Die fout is hersteld. Omgevingsmeldingen over niet-schrijfbare Android-analyticsinstellingen zijn geen appfouten.

## Grenzen van de controle

De Trakt-tests gebruiken een nagebootste transportlaag. Er is geen gebruikersaccount gekoppeld en geen echte Trakt-mutatie uitgevoerd. Voor een volledige praktijktest zijn een geregistreerde Client ID en toestemming van de gebruiker via de tv-code nodig. Installatie, D-padbediening, providerstreams en Atmos/Sonos moeten op geschikte Android TV-hardware worden getest. Dit is een debug-APK, geen Play Store-release.

Zie [Trakt instellen](TRAKT.md) voor stappen, privacygedrag en ondersteunde functies.
