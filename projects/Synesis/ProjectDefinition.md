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

## Sensor-Vertrag (iot-db-bridge)

```text
GET /keller_temp?since=<unixsec>&limit=<n>  (Default 500, max 5000)
→ {"ok":true,"count":N,"values":[{"t":<unixsec>,"temp_c":<float>},…]}  (neueste zuerst)
Fehler: HTTP 400/500 mit {"ok":false,"error":"…"}
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
- Transkription nutzt die auf dem Gerät installierte Android-Spracherkennung; ohne entsprechende App zeigt Synesis
  einen Gerätehinweis statt selbst Audio an einen Cloud-Dienst zu senden
- KI-Präzisierung bettet keine Modell- oder API-Schlüssel in die App ein: Synesis kopiert einen präzisen
  Auftrag in die Zwischenablage und öffnet den Thomas/Hermes-Chat zur bewussten Übergabe
- Alte Notiz-`linkedDate`-Werte werden als Legacy-Feld erhalten, aber nicht mehr im Kalender angezeigt
