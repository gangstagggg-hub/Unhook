# Unhook

En app som hjelper deg å slutte med doom scrolling. Unhook varsler deg når du har scrollet i 5 minutter, foreslår noe annet å gjøre, og viser fremgangen din uke for uke.

## Funksjoner

- Varsel med et forslag fra alternativlisten («Hva med å gå en tur?») når du åpner en av de valgte appene, og igjen etter 5, 10 og 15 minutter
- Daglig grense: en øvre grense for hvor lenge du kan bruke de valgte appene per dag. Når den er nådd, sperres appene til midnatt
- Streak som teller tiden siden sist du scrollet mer enn 5 minutter i strekk
- Guidet pusteøvelse for å ri ut trangen
- Egen liste med alternativer til scrolling
- Logging av triggere som kjedsomhet, stress og utsettelse
- Blokkering av valgte apper i faste perioder
- Tidsavbrudd: etter 2 minutter totalt sperres alle valgte apper, og de åpnes igjen etter 10 (begge tallene kan endres, og funksjonen kan slås av)
- Oversikt over når på døgnet du scroller
- Denne uka sammenlignet med forrige uke, med grafer
- Ditt eget «hvorfor» som vises når du trenger det
- Lys og mørk modus
- Alle data lagres bare på enheten

## Struktur

```
docs/index.html        Web-appen (HTML, CSS og JS i én fil). Brukes også som grensesnitt i Android-appen.
docs/manifest.json     PWA-manifest, så appen kan installeres
docs/sw.js             Service worker for installasjon og bruk uten nett
docs/icons/            App-ikoner
android/               Android-appen (Kotlin)
  app/src/main/java/com/unhook/app/
    MainActivity.kt          Viser web-appen i en WebView
    Bridge.kt                Kobler web-appen til Android (window.Android)
    UsageMonitorService.kt   Følger med på apper med UsageStatsManager
    Apps.kt                  Kobler app-navn til pakkenavn
    Schedule.kt              Faste blokkeringsperioder
    BlockActivity.kt         Skjermen som vises over blokkerte apper
    Notifier.kt              Varsler
    BootReceiver.kt          Starter overvåkingen etter omstart
.github/workflows/     Bygger APK automatisk på GitHub
```

## Web-versjonen

Åpne `docs/index.html` i nettleseren. I web-versjonen starter du økten selv, fordi en nettside ikke kan se andre apper.

**Publiser med GitHub Pages:** Gå til *Settings → Pages*, velg *Deploy from a branch*, branch `main` og mappe `/docs`.

**Installer som app (PWA):** Første gang du åpner GitHub Pages-lenken, spør Unhook om du vil installere den. På Android i Chrome installeres den med ett trykk. På iPhone viser appen hvordan du legger den til via *Del → Legg til på Hjem-skjerm*. Valget finnes også under *Mer*.

`index.html` hentes alltid fra nettet først, så oppdateringer kommer uten at du må endre cache-navnet i `sw.js`. Bump `CACHE` bare når du endrer ikoner eller manifest.

Merk: PWA-versjonen kan ikke se andre apper. Automatiske økter og blokkering krever Android-appen i `android/`.

## Android-appen

Android-appen viser den samme web-appen, men registrerer økter automatisk med `UsageStatsManager`.

### Bygg

**Android Studio:** Åpne mappen `android/` og trykk *Run*. Android Studio lager Gradle wrapper selv.

**Kommandolinje:** Krever JDK 17, Android SDK og Gradle 8.9.

```bash
cd android
gradle wrapper --gradle-version 8.9   # første gang
./gradlew assembleDebug
```

APK-en havner i `android/app/build/outputs/apk/debug/`.

**GitHub Actions:** Hver push til `main` bygger en debug-APK. Last den ned fra fanen *Actions* under *Artifacts*.

### Tillatelser

Første gang appen åpnes, viser den en oppsettsveiledning som går gjennom alle tillatelsene én etter én. Den kan åpnes igjen under *Mer → Oppsett*.

| Tillatelse | Hvorfor |
|---|---|
| Tilgang til bruksdata | Se hvilken app som er åpen. Slås på under *Innstillinger → Tilgang til bruksdata*. |
| Varsler | Varsel etter 5 minutter. |
| Vis over andre apper | Vise blokkeringsskjermen. Uten den får du et varsel i stedet. |
| Forgrunnstjeneste | Holde overvåkingen i gang i bakgrunnen. |

### Slik fungerer overvåkingen

`UsageMonitorService` leser app-hendelser hvert 3. sekund. En økt starter når en sosial app eller nettleser kommer i forgrunnen, og fortsetter hvis du bytter mellom slike apper. Den avsluttes når du har vært borte i 20 sekunder, eller når skjermen slås av. Økter under 30 sekunder lagres ikke.

Kjente apper står i `Apps.kt`. Andre apper velger du fra listen over installerte apper under *Mer → Apper*.

For nettlesere vet appen bare at nettleseren er åpen, ikke hvilken side du er på.

### Google Play

Play krever begrunnelse for tilgang til bruksdata, visning over andre apper og forgrunnstjeneste av typen `specialUse`. Beskriv at appen hjelper brukeren med å redusere egen skjermtid.

## Videre arbeid

- Ekte widget på hjemskjermen (i dag vises bare en forhåndsvisning)
- iOS-versjon med Screen Time API (FamilyControls og DeviceActivity)

## Lisens

MIT. Se `LICENSE`.
