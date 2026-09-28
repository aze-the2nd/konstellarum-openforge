# Synesis

## Zweck

Synesis ist die private All-in-One-Plattform: eine schlanke Android-Basis-App (Shell) mit eigenem
Update-Mechanismus, die über Module erweitert wird. Persönliche Daten bleiben ausschließlich lokal
auf dem Gerät.

## Architektur

```text
Synesis/
├── app/                 Shell: Modul-Registry, Navigation, Update-System (GitHub Releases)
├── core/                Plattformunabhängige Logik: Modul-Vertrag, Domänen-Modelle,
│                        Kalender-Logik, JSON-Speicher — vollständig JVM-getestet
├── feature/notes/       Modul: Notizen (mit optionaler Kalender-Verknüpfung)
├── feature/todos/       Modul: Aufgaben
└── feature/calendar/    Modul: Monatskalender (Termine + verknüpfte Notizen)
```

## Modul-Vertrag

Ein Modul besteht aus

1. einem `FeatureModule`-Deskriptor (id, Titel, Beschreibung) und
2. einer Compose-UI.

Neue Module werden als eigenes Gradle-Modul angelegt und in `AppModules` registriert. Persistenz
läuft über `FileBackedListStore` (JSON-Dateien im privaten App-Verzeichnis, `core`-getestet).
Der Shell gehören Navigation, Update-System und die Repository-Instanzen (eine Instanz je Datentyp,
an Module durchgereicht — so bleibt z. B. der Kalender live, wenn eine Notiz verknüpft wird).

## MVP 0.1.0

- Shell mit Modul-Navigation und Update-Check beim Start
- Notizen: anlegen, bearbeiten, löschen; optionale Verknüpfung mit einem Kalendertag
- Aufgaben: anlegen, erledigen, löschen
- Kalender: Monatsansicht (Montag bis Sonntag), Termine anlegen/löschen,
  verknüpfte Notizen erscheinen am jeweiligen Tag

## Update-Quelle

- GitHub Releases im Repo `aze-the2nd/blackforest-openforge`
- Tag-Muster: `synesis-vX.Y.Z`
- Asset-Muster: `Synesis-vX.Y.Z.apk`

## Nicht im ersten Schritt

- Play Store / Play In-App Updates
- Accounts / Login / Cloud-Sync
- Benachrichtigungen, Widgets, Kalender-Provider-Integration
- Import/Export von Workspace-Bundles (eigene Ausbaustufe)

## Bekannte Grenzen (0.1.0)

- Persistenz als JSON-Dateien; eine korrupte Datei wird nach `<name>.corrupt` verschoben statt still gelöscht
- Kein Laufzeit-Pluginsystem: Module werden zur Buildzeit eingebunden und registriert
- Lokale Daten sind persönliche Gerätedaten und nicht Teil des BlackForestWorkspace-SSOT
- Datei-I/O läuft synchron auf dem UI-Thread; für große Datenmengen folgt die Auslagerung auf einen IO-Dispatcher
- Repository-Implementierungen (Android-Teil) sind noch nicht getestet; getestet ist der plattformunabhängige Kern
