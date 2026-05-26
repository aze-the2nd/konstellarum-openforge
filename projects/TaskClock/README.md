# TaskClock

Minimales Python-Stand-alone-Tool zum Erfassen von Projektzeit per Task-Stempelung.

Der Projektbaum enthält inzwischen auch die Subprojektzweige `taskclock_qt/` und `rustyTaskClock/`.

## Umfang jetzt

- Tasks/Todos anlegen
- genau eine aktive Task gleichzeitig
- Start/Stop per „Stempeln“
- laufender Timer in der GUI
- Zeiten lokal speichern
- CSV-Export
- Fenster per Toggle anpinnbar

## Struktur

```text
TaskClock/
├── ProjectDefinition.md   # Vorgabe
├── README.md              # diese Kurzbeschreibung
├── taskclock.py           # klassische Stand-alone-App
├── taskclock_qt/          # Qt-Subprojekt
│   ├── README.md
│   ├── __main__.py
│   ├── core.py
│   ├── main.py
│   ├── qt_bootstrap.py
│   └── window.py
└── rustyTaskClock/        # Rust-Nachfolgeprojekt
    ├── Cargo.toml
    ├── README.md
    ├── REQUIREMENTS.md
    ├── src/
    │   ├── core.rs
    │   ├── gui.rs
    │   ├── lib.rs
    │   └── main.rs
    └── tests/
        └── core.rs
```

Alles Weitere wird erst ergänzt, wenn es wirklich gebraucht wird.

## Start

```bash
python3 taskclock.py
```

Die App erzeugt lokal `taskclock_data.json`; Exporte werden per Dateidialog gespeichert.

## Release

- Release-Tags folgen dem Muster `taskclock-v1.3.2`.
- Vor dem Publish prüft `scripts/taskclock-release-guard.sh <tag>` auf ein gültiges Tag und blockiert doppelte Releases.
- Der GitHub Actions Workflow erzeugt Assets und Release-Titel mit derselben Versionsnummer wie das Tag.
- Für das Rust-Subprojekt werden beim Release Linux- und Windows-Artefakte erzeugt.
