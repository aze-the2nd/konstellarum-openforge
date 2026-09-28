# BlackForest OpenForge

Public tools from **BlackForest Engineering** — small, focused software built for everyday productivity.

## Projects

### TaskClock
A minimal desktop time-stamping app for project tasks.

- Create tasks/todos
- Stamp exactly one active task at a time
- See a live timer
- Export sessions as CSV
- Pin the window on top
- Stores data in the user app-data directory, not next to the executable

Source: [`projects/TaskClock`](projects/TaskClock)

Latest release:
https://github.com/aze-the2nd/blackforest-openforge/releases

### TaskClock Qt
The modern Qt-based edition of TaskClock.

- Same task-stamping workflow as the original app
- Modern dark widget UI
- CSV export
- Window pinning
- Linux fallback for missing `libEGL.so.1`

Source: [`projects/TaskClock`](projects/TaskClock)

Latest release:
https://github.com/aze-the2nd/blackforest-openforge/releases

### Synesis
A private all-in-one Android platform: a slim base app with a GitHub-release update mechanism, extended through modules.

- Notes with optional calendar linking
- Todos with done state
- Month calendar (Mon–Sun) with events and linked notes
- Local-first storage: personal data never leaves the device

Source: [`projects/Synesis`](projects/Synesis)

Latest release:
https://github.com/aze-the2nd/blackforest-openforge/releases

## Repository strategy

This repo should stay clean and marketable:

```text
projects/             Public apps and tools
docs/                 Brand, release, and repo-management notes
.github/workflows/    Public CI/release automation
LICENSE               Repo-wide MIT license
README.md             Public landing page
```

Rules:

1. Only publish curated, self-contained project snapshots.
2. Keep private planning, client context, credentials, and internal operations out of this repo.
3. Public releases use semantic/versioned tags, e.g. `synesis-v0.1.0`.
4. The visible app version should match the release tag.
5. The private BlackForest Engineering repo remains the operational source-of-truth.

## License

MIT License. See [`LICENSE`](LICENSE).
