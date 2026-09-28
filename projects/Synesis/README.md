# Synesis

Synesis ist eine kleine Android-App mit GitHub-basierter Update-Prüfung.

## Was die erste Version kann
- App startet in einer schlanken Compose-UI
- prüft beim Start GitHub Releases
- zeigt ein Update an, wenn ein neuer Release-Tag verfügbar ist
- lädt das APK direkt herunter
- öffnet den Android-Installer
- startet nach der Installation automatisch neu

## GitHub-Konvention
- Repo für die Update-Quelle: `aze-the2nd/blackforest-openforge`
- Release-Tags: `synesis-vX.Y.Z`
- APK-Assets: `Synesis-vX.Y.Z.apk`

## Projektstruktur
```text
Synesis/
├── ProjectDefinition.md
├── README.md
├── docs/Update-Flow.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── .github/workflows/synesis-release.yml
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/de.konstellarum.synesis/
        │   ├── MainActivity.kt
        │   ├── AppVersion.kt
        │   ├── SynesisApp.kt
        │   └── update/
        │       ├── GitHubReleaseUpdateRepository.kt
        │       ├── PackageReplacedReceiver.kt
        │       ├── UpdateCoordinator.kt
        │       ├── UpdateDownloadReceiver.kt
        │       ├── UpdateLauncher.kt
        │       ├── UpdateModels.kt
        │       └── UpdateRepository.kt
        └── res/
            ├── values/
            │   ├── strings.xml
            │   └── themes.xml
            └── xml/
                └── file_paths.xml
```
