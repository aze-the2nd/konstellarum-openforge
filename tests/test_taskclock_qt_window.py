from __future__ import annotations

import os

os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

from PySide6 import QtWidgets

from taskclock_qt.core import APP_VERSION, TaskClockStore
from taskclock_qt.window import TaskClockWindow


def test_window_title_and_status_text() -> None:
    app = QtWidgets.QApplication.instance() or QtWidgets.QApplication([])
    window = TaskClockWindow(TaskClockStore())
    try:
        assert window.windowTitle() == f"TaskClock Qt v{APP_VERSION}"
        assert window.status_label.text() == "Bereit"
    finally:
        window.close()
        app.processEvents()
