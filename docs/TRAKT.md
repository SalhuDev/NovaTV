# Trakt.tv in NOVA TV v0.3.0

## Eenmalig instellen

1. Registreer een eigen Trakt API-app via https://developer.trakt.tv/apps.
2. Registreer als redirect-URI `https://github.com/SalhuDev/NovaTV` (of vul je eigen geregistreerde HTTPS-URI in NOVA TV in).
3. Open **Trakt** in NOVA TV (vanaf v0.4: kies **Geavanceerd: Trakt-app instellen**), vul de publieke **Client ID** en dezelfde redirect-URI in, en kies **Aanmelden bij Trakt** (v0.3: **Opslaan en aanmelden**). Er is geen Client Secret nodig; neem die nooit op in de Android-app.
4. Open het getoonde Trakt-adres op je telefoon, voer de code in en geef toestemming. NOVA TV wacht automatisch op goedkeuring.
5. Open een film of aflevering in de bibliotheek en kies **Trakt** naast de titel. Zoek en bevestig de juiste titel en het jaar. Voor series: vul seizoen en aflevering in, kies de serie en bevestig daarna de gevonden aflevering.

Een echte Client ID is niet meegeleverd. Zonder geregistreerde Trakt-app werkt aanmelden niet. Er zijn geen accountgegevens in de broncode of APK opgenomen.

## Functies

- Device-codeaanmelding met annuleren, vervaldatum en interval/rate-limitafhandeling.
- Versleutelde opslag met Android Keystore, automatische verversing van toegangstokens en ontkoppelen.
- Kijklijst ophalen met paginering, toevoegen en verwijderen; laatste 100 kijkregistraties ophalen. Herhaalde registraties van dezelfde titel worden in de weergave samengevoegd.
- Expliciete koppeling van een lokale film of aflevering aan een Trakt-ID; opgeslagen koppelingen blijven bij herstart bewaard.
- Handmatig als bekeken registreren.
- Optionele automatische scrobbling: start bij afspelen/hervatten, pauze bij gebruikerspauze of achtergrond, stop bij einde of sluiten van de speler. Trakt bepaalt of de stop als bekeken telt (volgens de API bij meer dan 80%). Buffering veroorzaakt geen herhaalde startmeldingen. Een stop onder 1% wordt niet verzonden.
- Alleen films en afleveringen met bevestigde titelkoppeling worden geregistreerd. Live TV wordt uitgesloten. Favorieten en Trakt-kijklijst zijn afzonderlijke lijsten.

Trakt levert metadata, geen videostreams. De kijklijst opent in deze versie niet rechtstreeks een stream; afspelen gaat vanuit je eigen bibliotheek. Offline kijkregistraties worden niet in een blijvende wachtrij bewaard. Er is nog geen automatisch hervatten vanaf een Trakt-positie of import van volledige kijkgeschiedenis. De Client ID moet voor deze ontwikkelbuild eenmaal op de tv worden ingevoerd.

## Techniek en privacy

OAuth gebruikt `https://auth.trakt.tv/oauth/...`; metadata en synchronisatie gebruiken `https://api.trakt.tv`. Redirects worden niet gevolgd. Refresh-aanvragen worden met een mutex geserialiseerd omdat refresh-tokens eenmalig bruikbaar zijn. De geroteerde tokencombinatie wordt duurzaam opgeslagen vóór de volgende API-aanvraag.

De app stuurt zoektermen, bevestigde Trakt-ID's, kijkpercentages en kijktijden naar Trakt. IPTV-wachtwoorden en stream-URL's worden niet naar Trakt gestuurd. Titelkoppelingen en tokens worden in een afzonderlijk versleuteld voorkeurenbestand bewaard; app-backups zijn uitgeschakeld. Automatische registratie staat standaard uit. Ontkoppelen wist de lokale tokens en probeert de toestemming bij Trakt in te trekken; bij netwerkfalen meldt de app dat intrekken via je Trakt-account nodig is.

## Validatie

Unit tests gebruiken een nagebootste Trakt-transportlaag: pending/429-polling, verlopen codes, gelijktijdige tokenvernieuwing, duurzame opslag vóór gebruik, ingetrokken refresh-token, paginering, afleveringpayloads, ontbrekende IDs en afspeeltransities. Een werkelijke Trakt-accountkoppeling en afstandsbedieningstest op Android TV moeten nog worden uitgevoerd met een geregistreerde Client ID.

API-contract gecontroleerd op 10 oktober 2026:

- https://developer.trakt.tv/openapi.json
- https://developer.trakt.tv/docs/authentication-oauth
- https://developer.trakt.tv/docs/about-scrobble
