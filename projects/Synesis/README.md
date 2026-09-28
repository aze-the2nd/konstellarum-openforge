# Synesis

Private All-in-One-Plattform für Android: Basis-App mit Update-Mechanismus und Modul-System.

## Module (Stand 0.3.0)

- **Notizen** — anlegen, bearbeiten, löschen; bewusst ohne Kalender-Verknüpfung
- **Aufgaben** — Aufgabenliste mit Erledigt-Status und optionaler Kalender-Verknüpfung
- **Kalender** — Monatsansicht (Mo–So) mit Terminen und verknüpften Aufgaben
- **Transkription** — Android-Spracherkennung starten, Texte lokal speichern, kopieren und per Thomas/Hermes-KI präzisieren lassen
- **Keller** — Temperaturüberwachung des Sensors `keller_temp` (IoT-Bridge im Tailnet)
- **Controller** — WLAN-Einstellungen des Sensors per Bluetooth ändern

## Startseite

- Update-Prüfung und Installation über GitHub Releases
- Chat-Button öffnet direkt den Telegram-Chat mit dem Hermes-Agenten (`@tommy_watson_bot`)

## Update-Mechanismus

- prüft beim Start GitHub Releases
- zeigt ein Update an, wenn ein neuer Release-Tag verfügbar ist
- lädt das APK direkt herunter und öffnet den Android-Installer
- startet nach der Installation automatisch neu

## GitHub-Konvention

- Repo für die Update-Quelle: `aze-the2nd/blackforest-openforge`
- Release-Tags: `synesis-vX.Y.Z`
- APK-Assets: `Synesis-vX.Y.Z.apk`

## Entwicklung

- Tests: `gradle :core:test` (JVM-Unit-Tests der plattformunabhängigen Logik)
- Debug-Build: `gradle :app:assembleDebug`
- CI: `synesis-ci.yml` führt Tests und Debug-Build bei jedem Push auf `main` aus
- Release: `synesis-release.yml` (Tag `synesis-vX.Y.Z`) führt vor dem Packaging die Tests aus

## Projektstruktur

```text
Synesis/
├── ProjectDefinition.md
├── README.md
├── docs/Update-Flow.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── app/                 Shell (Navigation, Chat-Button, Update, Modul-Registry)
├── core/                Plattform-Kern (Modelle, Kalender-/Sensor-/Chat-/Transkript-/KI-Auftragslogik, Speicher) + Unit-Tests
└── feature/
    ├── notes/           Modul Notizen
    ├── todos/           Modul Aufgaben
    ├── calendar/        Modul Kalender
    ├── transcribe/      Modul Transkription
    ├── cellar/          Modul Kellertemperatur
    └── controller/      Modul Sensor-Controller
```
