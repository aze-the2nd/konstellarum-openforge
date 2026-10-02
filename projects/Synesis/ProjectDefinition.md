# Synesis

## Zweck

Synesis ist die private All-in-One-Plattform: eine schlanke Android-Basis-App (Shell) mit eigenem
Update-Mechanismus, die über Module erweitert wird. Persönliche Daten bleiben ausschließlich lokal
auf dem Gerät.

## Architektur

```text
Synesis/
├── app/                 Shell: Modul-Registry, Navigation, Chat-Button, Update-System
│                        (GitHub Releases)
├── core/                Plattformunabhängige Logik: Modul-Vertrag, Domänen-Modelle,
│                        Kalender-, Sensor-, Chat- und Transkript-Logik, JSON-Speicher —
│                        vollständig JVM-getestet
├── feature/notes/       Modul: Notizen (ohne Kalender-Verknüpfung)
├── feature/todos/       Modul: Aufgaben (optional mit Kalender-Verknüpfung)
├── feature/calendar/    Modul: Monatskalender (Termine + verknüpfte Aufgaben)
├── feature/transcribe/  Modul: Transkription über Android-Spracherkennung
├── feature/cellar/      Modul: Kellertemperatur (Sensor keller_temp über die IoT-Bridge)
└── feature/controller/  Modul: Sensor-WLAN-Provisioning über Bluetooth
```

## Modul-Vertrag

Ein Modul besteht aus

1. einem `FeatureModule`-Deskriptor (id, Titel, Beschreibung) und
2. einer Compose-UI.

Neue Module werden als eigenes Gradle-Modul angelegt und in `AppModules` registriert. Persistenz
läuft über `FileBackedListStore` (JSON-Dateien im privaten App-Verzeichnis, `core`-getestet).
Der Shell gehören Navigation, Update-System und die Repository-Instanzen (eine Instanz je Datentyp,
an Module durchgereicht — so bleibt z. B. der Kalender live, wenn eine Aufgabe verknüpft wird).

## MVP 0.1.0

- Shell mit Modul-Navigation und Update-Check beim Start
- Notizen: anlegen, bearbeiten, löschen
- Aufgaben: anlegen, erledigen, löschen
- Kalender: Monatsansicht (Montag bis Sonntag), Termine anlegen/löschen

## Ausbaustufe 0.2.0 — Keller-Temperaturmodul

- Modul `feature/cellar`: Temperaturüberwachung des Sensors `keller_temp`
- Datenquelle: IoT-Bridge auf dem Tailnet-Host `aurora` (`GET http://100.101.80.34:5005/keller_temp?since=&limit=`)
- Anzeige: aktueller Wert, Min/Ø/Max, Verlaufsdiagramm, Auto-Refresh (60 s)
- Lokaler Cache (letzte 2000 Messwerte) — das Diagramm überlebt Neustarts
- Netzwerk: Klartext-HTTP ist ausschließlich für die Tailnet-Adressen der Bridge erlaubt
  (Network-Security-Config); alles andere bleibt HTTPS-only

## Ausbaustufe 0.3.0 — Aufgaben-Kalender, Transkription, Chat, Logo

- Kalender-Verknüpfung gehört zu Aufgaben (`TodoItem.linkedDate`), nicht zu Notizen
- Kalender zeigt Termine und verknüpfte Aufgaben; Notizen bleiben reine Notizen
- Modul `feature/transcribe`: Android-Spracherkennung starten, beste erkannte Kandidaten normalisieren,
  lokale Transkript-Historie speichern, Transkripte kopieren, löschen oder als KI-Präzisierungsauftrag
  für Thomas/Hermes vorbereiten
- Startseiten-Button `Chat mit Thomas öffnen`: öffnet `tg://resolve?domain=tommy_watson_bot` mit
  HTTPS-Fallback auf `https://t.me/tommy_watson_bot`
- Neues adaptives App-Logo: dunkle Basis, Horizontbogen, zentrale Achse und vernetzte Knoten
- App-Backup ist deaktiviert; lokale JSON-Dateien inklusive `.tmp`- und `.corrupt`-Varianten sind zusätzlich aus
  Backup-/Transfer-Regeln ausgeschlossen (Notizen, Aufgaben, Termine, Transkripte und Keller-Cache)

## Ausbaustufe 0.4.0 — IoT-Bereich, Diagramm-Achsen, Whisper-Transkription

- **Modul-Kategorien:** `FeatureModule.category` fasst Module zu einem Top-Level-Bereich im
  Hauptmenü zusammen. `Keller` und `Controller` gehören zum Bereich `IoT & Automation`;
  der Bereich öffnet eine Tab-Ansicht (ein Tab je Sensor/Gerät), erweiterbar um weitere Sensoren.
- **Diagramm (Keller):** Y-Achse mit beschrifteter Skala (runde Gradwerte, Gitterlinien),
  X-Achse mit absoluter Uhrzeit (`HH:mm`, bei langen Fenstern `dd.MM.`) statt relativer Abstände.
  Pinch-Zoom und Pan auf dem Canvas, Zeitbereich-Presets `1 h` / `24 h` / `7 d` / `Alle`,
  sichtbarer Fensterbereich als Beschriftung, `Zoom zurücksetzen` nach manuellem Zoom.
- **Datenfenster:** Abruf mit `limit=5000` (Vertragsmaximum) und Zusammenführen mit dem lokalen
  Cache (dedupliziert nach Zeitstempel, max. 10 080 Werte = eine Woche bei 1/min) — so wächst
  die Historie für die 7-Tage-Ansicht auch ohne Bridge-Änderung.
- **Diktat & Transkription (ersetzt Android-Spracherkennung und Small-Fast-Präzisierung):**
  - Aufnahme lokal per `MediaRecorder` (AAC in MP4-Container, `audio.m4a`); die Android-Spracherkennung
    (Google) wird nicht mehr verwendet.
  - Transkription über die private Whisper-Instanz auf der Bridge:
    `POST /transcripts/whisper` (rohe Audio-Bytes, `Content-Type: audio/mp4`, max. 25 MB,
    LAN zuerst, Tailnet als Fallback; Read-Timeout 300 s wegen CPU-Inferenz).
  - Speicherung als **Paket**: Ordner `filesDir/transcripts/<uuid>/` mit `audio.m4a` und
    `transcript.txt`; der Index-Eintrag referenziert den Ordner. Löschen entfernt beides.
  - Fehlgeschlagene Transkription behält die Aufnahme; `Erneut versuchen` läuft auf demselben Paket.
- Manifest: `RECORD_AUDIO`-Berechtigung (Laufzeit-Anfrage beim Start der Aufnahme);
  Audio-Pakete sind wie alle lokalen JSON-Dateien von Backup/Transfer ausgeschlossen.
- Der bisherige KI-Präzisierungsfluss (Chat-Handoff) ist vollständig entfernt; die Bridge-Route
  `POST /transcripts/refine` kann nach der Whisper-Umstellung entfallen.

## Ausbaustufe 0.4.1 — Hotfix Keller-Anzeige

- **Behoben:** Die „Aktuell“-Karte im Keller-Modul zeigte den **ältesten** Cache-Eintrag statt des
  neuesten Messwerts (eingefrorene Anzeige, z. B. dauerhaft 21,4 °C) — der Cache-Merge aus 0.4.0
  sortierte neueste-zuerst, die Anzeige liest aber das letzte Element.
- Neuer Kern `TempHistory` (aufsteigende Zeitordnung = neuester Wert zuletzt, Dedupe nach
  Zeitstempel, Cap auf die neuesten Werte) mit Unit-Tests; die Ordnungs-Invariante ist im
  `TempRepository`-Vertrag gepinnt. Der Markierungspunkt im Diagramm sitzt damit wieder am
  neuesten Wert (rechter Rand).

## Ausbaustufe 0.5.0 — Speicherintervall über BLE

- Der Controller (Firmware v3) schreibt Messwerte jetzt im einstellbaren Intervall in seine
  Datenbank (auf dem Gerät derzeit 10 s, vorher fest 60 s); Bereich 5–3600 s, persistiert in
  NVS, übersteht Reboots. Der on-device 7-Tage-Ringpuffer bleibt fest bei 60 s Kadenz.
- Neue BLE-Charakteristik `StoreInterval` im bestehenden Provisioning-Service
  (`bdc0591c-2f3a-47c8-9d89-0288d52a6d0d`, READ|WRITE, uint16 little-endian Sekunden).
  Ungültige Writes werden vom Gerät still ignoriert — die App verifiziert jeden Write per
  Rücklesen und meldet Übernahme, Ablehnung oder Verifikationsfehler.
- App: Sektion „Speicherintervall“ im Controller-Modul (Eingabefeld mit 5–3600-s-Validierung,
  Kurzwahl 10 s / 60 s / 5 min / 60 min, Anzeige des aktuellen Werts beim Verbinden).
  Fehlt die Charakteristik (Firmware < v3), wird die Sektion deaktiviert angezeigt, die
  WLAN-Parametrierung bleibt voll funktionsfähig.

## Sensor-Vertrag (iot-db-bridge)

```text
GET /keller_temp?since=<unixsec>&limit=<n>  (Default 500, max 5000)
→ {"ok":true,"count":N,"values":[{"t":<unixsec>,"temp_c":<float>},…]}  (neueste zuerst)
Fehler: HTTP 400/500 mit {"ok":false,"error":"…"}

POST /transcripts/whisper   (rohe Audio-Bytes, Content-Type: audio/mp4, max. 25 MB)
→ {"ok":true,"text":"…","model":"whisper-<größe>","language":"de"}
Fehler: HTTP 400/413/422/503/500 mit {"ok":false,"error":"…"}
```

## Update-Quelle

- GitHub Releases im Repo `aze-the2nd/blackforest-openforge`
- Tag-Muster: `synesis-vX.Y.Z`
- Asset-Muster: `Synesis-vX.Y.Z.apk`

## Nicht im ersten Schritt

- Play Store / Play In-App Updates
- Accounts / Login / Cloud-Sync
- Benachrichtigungen, Widgets, Kalender-Provider-Integration
- Import/Export von Workspace-Bundles (eigene Ausbaustufe)

## Bekannte Grenzen

- Persistenz als JSON-Dateien; eine korrupte Datei wird nach `<name>.corrupt` verschoben statt still gelöscht
- Kein Laufzeit-Pluginsystem: Module werden zur Buildzeit eingebunden und registriert
- Lokale Daten sind persönliche Gerätedaten und nicht Teil des BlackForestWorkspace-SSOT
- Datei-I/O läuft synchron auf dem UI-Thread; für große Datenmengen folgt die Auslagerung auf einen IO-Dispatcher
- Repository-Implementierungen (Android-Teil) sind noch nicht getestet; getestet ist der plattformunabhängige Kern
- Die Whisper-Transkription setzt den privaten Bridge-Endpunkt `POST /transcripts/whisper` voraus
  (LAN/Tailnet). Bis dieser live auf Aurora bereitgestellt ist, meldet die UI den Fehler
  (`Whisper-Endpunkt nicht erreichbar`), behält die Aufnahme und bietet `Erneut versuchen` an
- Das 7-Tage-Fenster zeigt, was der lokale Cache enthält: ein Abruf liefert höchstens 5000 Werte
  (~3,5 Tage bei 1/min), die restliche Historie sammelt sich über laufende App-Sitzungen
- Die Aufnahme übersteht keine Activity-Neuanlage (z. B. Bildschirmrotation) — danach muss neu
  aufgenommen werden; Background-Recording ist bewusst nicht umgesetzt
- Alte Notiz-`linkedDate`-Werte werden als Legacy-Feld erhalten, aber nicht mehr im Kalender angezeigt
