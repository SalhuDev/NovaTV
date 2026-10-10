# NOVA TV v0.2 — controle op Android TV

Voer deze controles uit op een Android TV/Google TV-apparaat met Android 8 of nieuwer.

- Start via de TV-launcher; controleer origineel logo, paarse kleuren en leesbaarheid op afstand.
- Bedien alle menu's, bronvelden, knoppen, lijsten en gids met D-pad/OK/Terug.
- Open een zender ver onderaan een lange lijst; Terug moet de lijst bij die zender openen.
- Importeer een eigen HTTPS M3U met tvg-id en group-title. Controleer dubbele en relatieve URL's.
- Speel HLS, DASH en MPEG-TS af; wissel via Vorige/Volgende en CH+/CH−/Page Up/Down.
- Controleer pauze, hervatten, audiotrack, ondertiteling, foutmelding en Opnieuw.
- Druk Home tijdens afspelen: audio moet stoppen. Keer terug naar NOVA TV.
- Laad XMLTV/XMLTV.gz, controleer tijdzones, huidig programma, Eerder/Nu/Later en zenderkoppeling via tvg-id.
- Verbind een Xtream-account en open live, film, serie en aflevering.
- Bewaar live, film en aflevering als favoriet. Herstart en controleer Favorieten.
- Test netwerkuitval, verlopen account, ongeldige XML en lege catalogus.
- Test Dolby Digital, DD+, DD+/Atmos en TrueHD/Atmos afzonderlijk, voor zover ondersteund.
- Controleer het ontvangen audioformaat in de TV/receiver/Sonos-app. Test HDMI ARC/eARC en lipsync.

Hardwaretests en tests met een echte IPTV-provider zijn niet uitgevoerd in de buildomgeving.
Media3 onderhandelt audioformaten met Android. Atmos, TrueHD en passthrough zijn geen garantie voor ieder apparaat. Er is geen directe Sonos-castfunctie.

## Trakt v0.3

- [ ] Eigen publieke Client ID en geregistreerde redirect-URI invoeren; code op telefoon goedkeuren.
- [ ] Aanmelden annuleren, code laten verlopen en toestemming weigeren.
- [ ] Film met gelijknamige alternatieven zoeken en juiste jaar bevestigen.
- [ ] Serie, seizoen (ook specials) en aflevering bevestigen.
- [ ] Kijklijst toevoegen/verwijderen en handmatig bekeken controleren op trakt.tv.
- [ ] Automatische registratie uit: geen wijziging; aan: start/pauze/hervatten/stop controleren.
- [ ] Buffering, Terug, app naar achtergrond en einde film controleren.
- [ ] Live TV registreren uitsluiten; snelle zenderwisselingen opnieuw controleren.
- [ ] App herstarten, tokenvernieuwing en ontkoppelen controleren.
- [ ] Grote kijklijst met afstandsbediening doorlopen; netwerkuitval toont foutmelding.
