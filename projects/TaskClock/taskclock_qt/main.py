from __future__ import annotations

import sys
import traceback
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from taskclock_qt.qt_bootstrap import ensure_qt_runtime

ensure_qt_runtime()

from PySide6 import QtWidgets  # noqa: E402

from taskclock_qt.core import APP_TITLE, log_file  # noqa: E402
from taskclock_qt.window import TaskClockWindow  # noqa: E402


def main() -> int:
    app = QtWidgets.QApplication(sys.argv)
    window = TaskClockWindow()
    window.show()
    return app.exec()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        log_file().write_text(traceback.format_exc(), encoding="utf-8")
        print(f"{APP_TITLE} konnte nicht starten. Fehlerlog: {log_file()}", file=sys.stderr)
        raise
