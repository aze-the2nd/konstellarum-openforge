# BlackForest OpenForge

Public tools from **BlackForest Engineering** — small, focused software built for everyday productivity.

## Positioning

BlackForest OpenForge is the public workshop for selected BlackForest Engineering utilities:

- **Useful before flashy** — practical tools that solve a concrete workflow problem.
- **Minimal by default** — small surface area, low setup friction, no unnecessary dependencies.
- **Desktop-friendly** — when possible, releases are shipped as stand-alone builds.
- **Brand-safe public channel** — the canonical source-of-truth remains BlackForest Engineering’s private workspace; this repo is the curated public release surface.

## Projects

### TaskClock

A minimal desktop time-stamping app for project tasks.

- Create tasks/todos
- Stamp exactly one active task at a time
- See a live timer
- Export sessions as CSV
- Pin the window on top
- Stores data in the user app-data directory, not next to the executable

Source: [`apps/taskclock`](apps/taskclock)

Latest release:
https://github.com/aze-the2nd/blackforest-openforge/releases

### TaskClock Qt

The modern Qt-based edition of TaskClock.

- Same task-stamping workflow as the original app
- Modern dark widget UI
- CSV export
- Window pinning
- Linux fallback for missing `libEGL.so.1`

Source: [`apps/taskclock_qt`](apps/taskclock_qt)

Latest release:
https://github.com/aze-the2nd/blackforest-openforge/releases

## Repository strategy

This repo should stay clean and marketable:

```text
apps/                 Public apps and tools
docs/                 Brand, release, and repo-management notes
.github/workflows/    Public CI/release automation
LICENSE               Repo-wide MIT license
README.md             Public landing page
```

Rules:

1. Only publish curated, self-contained project snapshots.
2. Keep private planning, client context, credentials, and internal operations out of this repo.
3. Public releases use semantic/versioned tags, e.g. `taskclock-v1.3.2`.
4. The visible app version should match the release tag.
5. The private BlackForest Engineering repo remains the operational source-of-truth.
6. Private releases/tags come first; public releases are a curated follow-up, not the primary source.
7. Do not publish the same public release twice; the public release workflow blocks duplicate tags.

## Release order for TaskClock Qt

1. Create and push the release tag in the private BlackForest Engineering repo first.
2. Verify the private tag exists locally with `scripts/taskclock-qt-release-guard.sh <tag>`.
3. Publish the public release only after the private tag exists and no public release already exists.

## License

MIT License. See [`LICENSE`](LICENSE).
