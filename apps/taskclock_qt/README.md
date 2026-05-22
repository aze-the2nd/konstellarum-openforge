# TaskClock Qt

**TaskClock Qt** is the modern Qt-based edition of TaskClock.

It keeps the same core workflow as the lightweight Python version:

- create tasks
- stamp one active task at a time
- watch a live timer
- stop or switch tasks
- export sessions as CSV
- pin the window on top

## Current version

`TaskClock Qt v1.4.0`

The window title includes the version so standalone builds can be identified easily.

## Run from source

Requirements:

- Python 3.12+
- PySide6

```bash
python -m apps.taskclock_qt
```

## Notes

- User data is stored in the platform-specific TaskClock app-data directory.
- If the host system does not provide `libEGL.so.1`, the app falls back to a bundled compatibility shim on Linux.
- The Qt app is self-contained and does not depend on the older Tkinter UI.
