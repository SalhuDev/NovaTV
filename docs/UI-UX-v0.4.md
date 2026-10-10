# UI/UX-controle en verbeteringen — v0.4.0

10 oktober 2026. Beoordeling op basis van de Android-broncode; geen bevestigde praktijktest op een televisie.

## Uitgevoerde verbeteringen

- Speler vult het scherm; het navigatiemenu verdwijnt tijdens afspelen. Terug verbergt zichtbare bediening en sluit daarna de speler. Afspelen/pauze en audio/ondertitels blijven via Media3 beschikbaar. CH+/CH− en Page Up/Down blijven live-zenders wisselen met dezelfde speler.
- Spelerfouten krijgen een zichtbare melding, focus op Opnieuw proberen en een terugknop.
- Focus blijft herkenbaar met een witte rand; geselecteerde navigatie heeft bovendien een pijl. Tekst op paarse knoppen is nu donker voor meer contrast. Standaard leestekst is 18sp met 26sp regelhoogte, schermmarge 32dp.
- Bibliotheek heeft zoeken en groepsfilters. Film- en serieoverzichten tonen beschikbare bronafbeeldingen in adaptieve rasters; ontbrekende of onbereikbare afbeeldingen tonen initialen. Gemengde favorieten en live-zenders blijven lijsten.
- Filters en scrollposities worden per scherm bewaard zolang de app actief is, ook tijdens afspelen. De serielijst heeft een eigen herstelpunt en afleveringen hebben een terugknop. Herstel van mediafocus gebruikt het laatst gekozen item.
- EPG-zendernamen staan buiten de horizontaal scrollende tijdlijn. Focus toont de volledige programmatitel boven de gids. Via een zendernaam opent een leesbare programmalijst voor het zichtbare tijdvenster; ook korte programma's zijn zo te lezen. De lijst heeft een knop Live kijken. Het geselecteerde programma is een herstelpunt bij terugkeer uit de speler.
- Trakt-aanmelden toont een QR-code naast het adres, de code en de vervaltijd. Client ID en redirect-URI staan onder Geavanceerd. De QR-code opent het verificatieadres; de gebruiker voert de tv-code op de telefoon in.
- Trakt-acties tonen voortgang. Herhaalde algemene netwerkacties worden tijdens een lopende actie genegeerd; na een fout is opnieuw proberen mogelijk. Twee regressietests controleren dit gedrag.
- Geschiedenis, programma-informatie en instellingen hebben focusbare tekstblokken, zodat lange teksten met D-pad bereikbaar zijn. Favorietenknoppen bij posters hebben beschrijvende toegankelijkheidslabels.

Het originele logo en de vier bestaande basiskleuren zijn behouden. Posters komen uitsluitend uit de ingestelde bron. Er worden geen fictieve films, afbeeldingen of kijkgegevens toegevoegd.

## Nog op hardware controleren

- D-pad door menu, filters, postergrid, dialoog, EPG en speler; geen focusval; terugkeer naar hetzelfde item.
- 1080p/4K met passende Android-dichtheid, lange titels, ondertitels en afstand tot het scherm.
- Focusherstel als een favoriet verdwijnt, filters wijzigen of een EPG-programma uit het tijdvenster schuift.
- QR-code scannen vanaf kijkafstand en echte Trakt-aanmelding met een geregistreerde Client ID.
- Netwerkverlies tijdens een bronimport, posterdownload en videostream; opnieuw proberen.
- Grote bibliotheken en EPG: scrollprestaties en gesynchroniseerde horizontale tijdlijn.

Er zijn in deze versie geen echte runtime-screenshots gemaakt. De eerdere HTML-preview is geen screenshot van v0.4. Automatische unit tests en Lint bewijzen niet dat alle D-padinteracties op hardware correct zijn.

## Resterende productverbeteringen

Broninvoer gebeurt nog via het tv-toetsenbord. Providercategorieën kunnen numerieke labels hebben. Eenmalige Trakt-configuratie vereist nog een publieke Client ID. Trakt-kijklijstitems spelen niet rechtstreeks een providerstream af. Focusherstel kan op de navigatie terugvallen als het oorspronkelijke item niet meer beschikbaar is. Deze grenzen worden niet verborgen achter demo-inhoud.
