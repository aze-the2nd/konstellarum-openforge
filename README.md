# BlackForest OpenForge

Public mirror of selected BlackForest Engineering project directories.

## Projects

### TaskClock

A minimal desktop time-stamping app for project tasks.

- Create tasks/todos
- Stamp exactly one active task at a time
- See a live timer
- Export sessions as CSV
- Pin the window on top
- Stores data in the user app-data directory

Source: [`projects/TaskClock`](projects/TaskClock)

### TaskClock Qt

The modern Qt-based edition of TaskClock.

- Same task-stamping workflow as the original app
- Clear, high-contrast widget UI
- Active-task timer plus live workday total
- CSV export with date/time-based default names
- Toggle-style window pinning
- Safer new-workday reset with double confirmation
- Linux fallback for missing `libEGL.so.1`

Source: [`projects/TaskClock/taskclock_qt`](projects/TaskClock/taskclock_qt)

## Repository strategy

- Private repo: canonical development, internal planning, and source of truth.
- Public repo: curated mirror of private project directories.
- Public currently mirrors all files under `projects/TaskClock/` as a temporary bridge.
- Sync direction is private → public only.

## Release policy

- Tag names are app-scoped, e.g. `taskclock-v1.3.2` or `taskclock-qt-v1.4.1`.
- App title, release tag, and artifact names should use the same version.
- Public releases are curated follow-ups to private-first releases.
- Do not republish the same public release twice.

## License

MIT License. See [`LICENSE`](LICENSE).
