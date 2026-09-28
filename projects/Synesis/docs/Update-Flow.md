# GitHub Update Flow

## Ziel

Die App prüft beim Start die GitHub Releases des OpenForge-Repos und zeigt ein Update an, wenn ein neuer Release-Tag vorhanden ist.

## Konvention

- Release-Tag: `synesis-vX.Y.Z`
- APK-Asset: `Synesis-vX.Y.Z.apk`
- Release-Titel: `Synesis vX.Y.Z`

## Ablauf

1. App startet.
2. App lädt `https://api.github.com/repos/aze-the2nd/blackforest-openforge/releases?per_page=30`.
3. App filtert nach `synesis-v*`.
4. App vergleicht SemVer mit der installierten Version.
5. Bei neuem Release zeigt die UI den Hinweis an.
6. Klick auf **Update installieren** startet den Download des APK-Assets.
7. Nach dem Download öffnet der Android-Installer.
8. Nach der Installation bringt der `MY_PACKAGE_REPLACED`-Receiver die App wieder nach vorne.

## Qualitätssicherung

- `synesis-ci.yml`: Unit-Tests (`:core:test`) und Debug-Build bei jedem Push auf `main`
- `synesis-release.yml`: führt die Unit-Tests vor dem Release-Packaging aus

## Nächste Ausbaustufen

- Release Notes besser formatieren
- Download-Fortschritt in der UI
- Fehlerzustände mit Retry und Offline-Erkennung
