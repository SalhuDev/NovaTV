# NOVA TV v0.4.0 — UI/UX

Datum: 10 oktober 2026. `com.novatv.player`, versionCode 4, minSdk 26, target/compileSdk 35.

## Wijzigingen

Zie [UI/UX-controle](UI-UX-v0.4.md) voor de wijzigingen en resterende praktijktests. Toegevoegd: Coil 2.7.0 voor bronafbeeldingen en ZXing 3.5.3 voor QR-generatie. Kotlin, Compose for TV en Media3 blijven de basis.

Origineel logo ongewijzigd, SHA-256: `194325f54d050b8aaf36255911d22f699462b77f950bd39f206b245dea4eb13f`.

## Validatie

Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, JDK 21 met Java 17-bytecode.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

**Geslaagd:** debug-APK gebouwd; 18 tests geslaagd; Android Lint 0 fouten / 11 bestaande waarschuwingen. APK-handtekening geldig.

De Gradle-log, JUnit XML-resultaten, Lint-resultaten en handtekeningcontrole staan in `docs/v0.4/`. Omgevingsmeldingen over Android-analytics en een Kotlin-daemonstart zijn hersteld of niet fataal.

Dit is een debug-ontwikkelbuild. D-pad, 1080p/4K-weergave, echte Trakt-aanmelding, providerstreams en Atmos/Sonos zijn niet op hardware gevalideerd. Er worden geen nieuwe native screenshots meegeleverd. De unit tests controleren parsers, Trakt-authenticatie/scrobbling en het voorkomen van dubbele netwerkacties; zij vormen geen volledige UI-test.
