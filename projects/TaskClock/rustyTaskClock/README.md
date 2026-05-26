# rustyTaskClock

Rust-Nachfolgeprojekt für TaskClock.

## Architekturentscheidung

- **UI stack:** `egui` via `eframe`
- **Core:** separate Rust library crate inside the same package
- **Persistence:** JSON in the OS-appropriate app-data directory
- **Export:** CSV export from the core, independent of the UI
- **Timer model:** single active task, persisted `active_since` timestamps
- **Release naming:** versioned artifacts and app title follow `rustyTaskClock v<version>`
- **Theme:** in-app dark/light toggle with persisted preference

## Status

- Phase: release candidate 0.2.2
- Core logic, GUI, inline task renaming, compact timer layout, theme toggle, and persistence are implemented
- Releases are published for Linux and Windows with matching versioned assets

## Run

```bash
source /opt/data/home/.cargo/env
cargo run
```

## Test

```bash
source /opt/data/home/.cargo/env
cargo test
```
