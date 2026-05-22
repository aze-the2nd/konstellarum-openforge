# TaskClock

**TaskClock** is a small stand-alone desktop app for stamping project time against tasks.

It is intentionally simple: create a task, stamp it active, switch tasks when your work changes, export the sessions as CSV.

## Current version

`TaskClock v1.3.2`

The app window title includes the release version so downloaded builds can be identified easily.

## Features

- Tasks/todos
- One active task at a time
- Start/stop by stamping
- Live timer
- CSV export
- Pin window on top
- Cross-platform user-data storage:
  - Windows: `%APPDATA%\TaskClock`
  - macOS: `~/Library/Application Support/TaskClock`
  - Linux: `$XDG_DATA_HOME/TaskClock` or `~/.local/share/TaskClock`

## Run from source

Requires Python with Tkinter.

```bash
python taskclock.py
```

No third-party runtime dependencies are required.

## Stand-alone builds

Releases are built by GitHub Actions via PyInstaller for Windows and Linux.

Download builds from:
https://github.com/aze-the2nd/blackforest-openforge/releases

## Error log

If the app cannot start, it writes a startup log to the TaskClock app-data directory.
On Windows:

```text
%APPDATA%\TaskClock\taskclock_error.log
```

## Source-of-truth

This public directory is a curated publication target. Canonical development and internal planning stay in the private BlackForest Engineering workspace.
